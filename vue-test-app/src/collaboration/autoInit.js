import { createApp, h } from 'vue'
import CollaboratePanel from './CollaboratePanel.vue'

let panelApp = null
let panelContainer = null

function getEditorInfo() {
  const path = window.location.pathname
  const match = path.match(/^\/editor\/(.+)$/)
  if (!match) return null

  const docId = match[1]
  if (docId === 'chat-mode') return null

  const userId = localStorage.getItem('userId') || '1'
  const userName = localStorage.getItem('userName') || 'User'

  return { docId, userId, userName }
}

async function fetchUserName(userId) {
  try {
    const token = localStorage.getItem('accessToken') || ''
    const headers = token ? { Authorization: `Bearer ${token}` } : {}
    const res = await fetch(`/api/users/${userId}`, { headers })
    const data = await res.json()
    if (data.code === 200 && data.data && data.data.username) {
      localStorage.setItem('userName', data.data.username)
      return data.data.username
    }
  } catch (e) {
    console.warn('[Collaboration] 获取用户名失败', e)
  }
  return null
}

async function injectPanel(info) {
  if (panelContainer) return

  let userName = info.userName
  if (userName === 'User' && info.userId !== '1') {
    const fetched = await fetchUserName(info.userId)
    if (fetched) userName = fetched
  }

  panelContainer = document.createElement('div')
  panelContainer.id = '__collaborate_panel__'
  document.body.appendChild(panelContainer)

  panelApp = createApp({
    render() {
      return h(CollaboratePanel, {
        documentId: info.docId,
        userId: info.userId,
        userName: userName
      })
    }
  })
  panelApp.mount(panelContainer)
}

function removePanel() {
  if (panelApp) {
    panelApp.unmount()
    panelApp = null
  }
  if (panelContainer) {
    panelContainer.remove()
    panelContainer = null
  }
}

function checkAndInject() {
  const info = getEditorInfo()
  if (info) {
    injectPanel(info)
  } else {
    removePanel()
  }
}

let lastPath = window.location.pathname
setInterval(() => {
  if (window.location.pathname !== lastPath) {
    lastPath = window.location.pathname
    setTimeout(checkAndInject, 300)
  }
}, 500)

document.addEventListener('DOMContentLoaded', () => {
  setTimeout(checkAndInject, 500)
})

if (document.readyState === 'interactive' || document.readyState === 'complete') {
  setTimeout(checkAndInject, 500)
}

export function initCollaboration() {
  checkAndInject()
}
