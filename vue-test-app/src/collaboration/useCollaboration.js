import { ref, nextTick } from 'vue'

const USER_COLORS = [
  '#3370ff', '#f56c6c', '#67c23a', '#e6a23c', '#909399',
  '#b37feb', '#36cfc9', '#ff85c0', '#597ef7', '#ffc53d'
]

let colorIndex = 0
const userColorMap = {}

function getUserColor(userId) {
  if (!userColorMap[userId]) {
    userColorMap[userId] = USER_COLORS[colorIndex % USER_COLORS.length]
    colorIndex++
  }
  return userColorMap[userId]
}

export function useCollaboration(documentId, userId, userName) {
  const onlineUsers = ref([])
  const remoteCursors = ref([])
  const isConnected = ref(false)
  const isCollaborating = ref(false)
  const connectionError = ref('')
  const lastSyncTime = ref(0)

  let paperElement = null
  let isRemoteChange = false
  let pollTimer = null
  let autoSaveTimer = null
  let lastKnownContent = ''
  let lastSavedContent = ''
  let inputHandler = null
  let compositionHandler = null
  let isComposing = false
  let composeData = ''
  let isDisposed = false

  async function autoSave() {
    if (!paperElement || !isCollaborating.value || isDisposed) return
    const content = paperElement.innerHTML
    if (content === lastSavedContent) return

    try {
      const token = localStorage.getItem('accessToken') || ''
      await fetch(`/api/documents/${documentId}`, {
        method: 'PUT',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': token ? `Bearer ${token}` : ''
        },
        body: JSON.stringify({ content })
      })
      lastSavedContent = content
      lastSyncTime.value = Date.now()
    } catch (e) {
      console.warn('[Collaboration] 自动保存失败', e)
    }
  }

  function scheduleAutoSave() {
    if (autoSaveTimer) clearTimeout(autoSaveTimer)
    autoSaveTimer = setTimeout(autoSave, 1500)
  }

  async function pollChanges() {
    if (!isCollaborating.value || isDisposed) return
    try {
      const token = localStorage.getItem('accessToken') || ''
      const headers = {}
      if (token) headers['Authorization'] = `Bearer ${token}`
      const res = await fetch(`/api/documents/${documentId}`, { headers })
      const data = await res.json()
      const docData = data.data || data
      const remoteContent = docData.content || ''

      if (remoteContent !== lastKnownContent) {
        const localContent = paperElement ? paperElement.innerHTML : ''
        if (remoteContent !== localContent) {
          isRemoteChange = true
          if (paperElement) {
            paperElement.innerHTML = remoteContent
            paperElement.dispatchEvent(new Event('input', { bubbles: true }))
          }
          lastKnownContent = remoteContent
          lastSavedContent = remoteContent
          lastSyncTime.value = Date.now()
          nextTick(() => { isRemoteChange = false })
        } else {
          lastKnownContent = remoteContent
        }
      }
    } catch (e) {
    }
  }

  function setupPaperListeners() {
    paperElement = document.querySelector('.paper')
    if (!paperElement) {
      setTimeout(setupPaperListeners, 500)
      return
    }

    lastKnownContent = paperElement.innerHTML || ''
    lastSavedContent = lastKnownContent

    inputHandler = () => {
      if (isRemoteChange || isComposing || isDisposed) return
      scheduleAutoSave()
    }

    compositionHandler = {
      start: () => { isComposing = true },
      end: (e) => {
        isComposing = false
        composeData = e.data || ''
        if (composeData && paperElement && !isDisposed) {
          scheduleAutoSave()
        }
      }
    }

    paperElement.addEventListener('input', inputHandler)
    paperElement.addEventListener('compositionstart', compositionHandler.start)
    paperElement.addEventListener('compositionend', compositionHandler.end)
  }

  function startPolling() {
    pollTimer = setInterval(pollChanges, 2000)
  }

  function stopPolling() {
    if (pollTimer) {
      clearInterval(pollTimer)
      pollTimer = null
    }
    if (autoSaveTimer) {
      clearTimeout(autoSaveTimer)
      autoSaveTimer = null
    }
  }

  function disconnect() {
    isDisposed = true
    stopPolling()

    if (paperElement) {
      if (inputHandler) {
        paperElement.removeEventListener('input', inputHandler)
      }
      if (compositionHandler) {
        paperElement.removeEventListener('compositionstart', compositionHandler.start)
        paperElement.removeEventListener('compositionend', compositionHandler.end)
      }
    }

    isConnected.value = false
    isCollaborating.value = false
    onlineUsers.value = []
    remoteCursors.value = []
  }

  async function startCollaboration() {
    if (isCollaborating.value) return
    isDisposed = false
    try {
      setupPaperListeners()
      startPolling()
      isConnected.value = true
      isCollaborating.value = true
      connectionError.value = ''
      lastSyncTime.value = Date.now()
    } catch (e) {
      connectionError.value = e.message || '启动失败'
      console.error('[Collaboration] 启动失败', e)
    }
  }

  function stopCollaboration() {
    disconnect()
  }

  return {
    onlineUsers,
    remoteCursors,
    isConnected,
    isCollaborating,
    connectionError,
    lastSyncTime,
    startCollaboration,
    stopCollaboration,
    getUserColor
  }
}
