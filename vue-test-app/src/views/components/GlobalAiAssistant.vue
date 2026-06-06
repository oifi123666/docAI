<template>
  <div>
    <div class="global-ai-fab" @click="toggleAiPanel">
      <el-icon size="24"><Cpu /></el-icon>
    </div>

    <el-drawer
        v-model="isAiPanelVisible"
        size="400px"
        :modal="false"
        :with-header="false"
        class="global-ai-drawer"
    >
      <div class="chat-layout">
        <div class="drawer-header">
          <div class="header-title">
            <div class="ai-icon-wrapper">
              <el-icon color="#fff" size="14"><Cpu /></el-icon>
            </div>
            <span>DocAI 全局助理</span>
          </div>

          <div class="header-actions">
            <el-select
                v-model="currentModel"
                size="small"
                placeholder="切换模型"
                style="width: 120px; margin-right: 8px;"
            >
              <el-option v-for="m in modelOptions" :key="m.code" :label="m.name" :value="m.code" />
            </el-select>

            <!-- 新对话按钮 -->
            <el-button
                link
                icon="Plus"
                @click="handleNewConversation"
                style="font-size: 16px; color: #3370ff; margin-right: 8px;"
                title="开始新对话"
            >
            </el-button>

            <el-button
                link
                icon="Close"
                @click="toggleAiPanel"
                style="font-size: 18px; color: #8f959e;"
            >
            </el-button>
          </div>
        </div>

        <div class="chat-scroll-area">
          <div class="message-row ai" v-if="chatHistory.length === 0">
            <div class="avatar-box ai-avatar">
              <el-icon size="16" color="#fff"><Cpu /></el-icon>
            </div>
            <div class="bubble ai-bubble">
              你好！我是 DocAI 全局助理。无论是管理云端文档、清理回收站，还是跨文件查阅资料，随时吩咐我。
            </div>
          </div>

          <div v-for="(msg, i) in chatHistory" :key="i" :class="['message-row', msg.role]">
            <div class="avatar-box" :class="msg.role === 'user' ? 'user-avatar' : 'ai-avatar'">
              <el-icon size="16" color="#fff">
                <User v-if="msg.role === 'user'" />
                <Cpu v-else />
              </el-icon>
            </div>

            <div class="message-content">
              <div :class="['bubble', msg.role === 'user' ? 'user-bubble' : 'ai-bubble', 'preserve-format']">
                {{ msg.text }}
              </div>

              <div v-if="msg.actionResult" class="agent-file-card" @click="handleDownload(msg.actionResult)">
                <el-icon size="24" color="#3370ff"><Document /></el-icon>
                <div class="file-info">
                  <div class="file-name">{{ msg.actionResult.fileName || '生成的文档' }}</div>
                  <div class="file-desc">点击查看或下载</div>
                </div>
              </div>
            </div>
          </div>

          <div v-if="isAiThinking" class="message-row ai">
            <div class="avatar-box ai-avatar">
              <el-icon size="16" color="#fff"><Cpu /></el-icon>
            </div>
            <div class="bubble ai-bubble thinking">
              <el-icon class="is-loading"><Loading /></el-icon> 正在调用智能体...
            </div>
          </div>
        </div>

        <div class="input-area">
          <div class="input-wrapper" :class="{ 'is-focused': isInputFocused }">
            <input
                v-model="userInput"
                class="custom-input"
                placeholder="发送全局指令，如：查阅资料、清理垃圾..."
                @keyup.enter="handleSend"
                @focus="isInputFocused = true"
                @blur="isInputFocused = false"
            />
            <div class="send-btn" :class="{ active: userInput.trim().length > 0 }" @click="handleSend">
              <el-icon size="18"><Top /></el-icon>
            </div>
          </div>
        </div>
      </div>
    </el-drawer>
  </div>
</template>

<script setup>
import { ref, nextTick, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { useGlobalAi } from '../../utils/useGlobalAi.js'
import { agentApi } from '../../api/agent'
import { aiApi } from '../../api/ai'
import { ElMessage, ElMessageBox} from 'element-plus'
import { docApi } from '../../api/document.js'

const route = useRoute()
const userInput = ref('')
const isInputFocused = ref(false)

// 模型选择相关的变量
const currentModel = ref('glm-5') // 默认绑定智谱 GLM-5
const modelOptions = ref([])

const { isAiPanelVisible, chatHistory, isAiThinking, currentConvId, toggleAiPanel, appendMessage, resetConversation, saveConvId} = useGlobalAi()

// 组件挂载时自动拉取模型列表
onMounted(async () => {
  try {
    const res = await aiApi.getModels()
    modelOptions.value = res.data
    currentModel.value = 'glm-5'
  } catch {
    // 接口报错时的本地保底数据
    modelOptions.value = [
      { code: 'glm-5', name: '智谱GLM-5', available: true },
      { code: 'qwen3.6-plus', name: '通义千问 Plus', available: true },
    ]
    currentModel.value = 'glm-5'
  }
})

const scrollToBottom = () => {
  nextTick(() => {
    const c = document.querySelector('.global-ai-drawer .chat-scroll-area')
    if (c) c.scrollTop = c.scrollHeight
  })
}

const handleSend = async () => {
  if (!userInput.value.trim() || isAiThinking.value) return

  const q = userInput.value
  appendMessage({ role: 'user', text: q })
  userInput.value = ''
  isAiThinking.value = true
  scrollToBottom()

  const uid = localStorage.getItem('userId') || '1'

  try {
    if (!currentConvId.value) {
      try {
        const chatRes = await aiApi.startChat(uid)
        saveConvId(chatRes.data)
      } catch (err) {
        console.error('创建对话失败:', err)
        const tempId = `temp_${Date.now()}`
        saveConvId(tempId)
      }
    }

    let pageContext = `用户当前所处页面路径：${route.path}。`
    const paperElement = document.querySelector('.paper')
    if (paperElement && paperElement.innerText) {
      pageContext += `\n当前页面文档内容：${paperElement.innerText.substring(0, 1500)}`
    }

    const res = await agentApi.executeTask({
      task: q,
      model: currentModel.value,
      conversationId: currentConvId.value,
      userId: uid,
      context: {
        isGlobalContext: true,
        pageContext: pageContext
      }
    })

    const agentResult = res.data

    // 检测到需要审批
    if (agentResult.status === 'action_required' && agentResult.pendingApproval) {
      const approval = agentResult.pendingApproval

      appendMessage({
        role: 'ai',
        text: agentResult.answer || '检测到需要确认的操作。',
        actionResult: parseToolResults(agentResult.toolResults),
        toolCalls: agentResult.toolResults || []
      })
      scrollToBottom()

      await handleApproval(approval, agentResult, uid)
      return
    }

    const answerText = agentResult?.answer || agentResult?.text || "指令已执行完毕。"
    appendMessage({
      role: 'ai',
      text: answerText,
      actionResult: parseToolResults(agentResult?.toolResults),
      toolCalls: agentResult?.toolResults || []
    })
    scrollToBottom()

  } catch (error) {
    console.error("Agent 报错监控:", error)

    if (error.message?.includes('对话不存在') || error.message?.includes('无权访问')) {
      resetConversation()
      appendMessage({
        role: 'ai',
        text: '对话已过期，已自动开始新对话，请重新发送指令。'
      })
    } else {
      appendMessage({ role: 'ai', text: '全局服务响应异常，请检查后端服务运行状态或认证 Token。' })
    }
    scrollToBottom()
  } finally {
    isAiThinking.value = false
  }
}

const handleApproval = async (approval, agentResult, userId) => {
  const toolName = approval.toolName
  const params = approval.params

  if (toolName === 'document-delete') {
    try {
      const fileName = params.objectName
      const keyword = fileName.replace('.docx', '').replace('.doc', '')

      const searchRes = await docApi.searchDocs(keyword)

      const docs = searchRes.data.data
      if (!docs || docs.length === 0) {
        appendMessage({ role: 'ai', text: '未找到文档: ' + fileName })
        scrollToBottom()
        return
      }

      const targetDoc = docs[0]

      ElMessageBox.confirm(
          `即将删除文档：${targetDoc.title}\n\n确认继续吗？`,
          '删除确认',
          { confirmButtonText: '确认删除', cancelButtonText: '取消', type: 'warning' }
      ).then(async () => {
        await executeApproval(approval, agentResult, userId, {
          documentId: targetDoc.id,
          bucketName: targetDoc.bucketName || 'documents',
          objectName: targetDoc.objectName || targetDoc.fileName || params.objectName
        })
      }).catch(() => {
        appendMessage({ role: 'ai', text: '操作已取消' })
        scrollToBottom()
      })

    } catch (err) {
      console.error('搜索文档失败:', err)
      appendMessage({ role: 'ai', text: '搜索文档失败: ' + err.message })
      scrollToBottom()
    }
  } else {
    ElMessageBox.confirm(
        `即将执行操作：${toolName}\n\n确认继续吗？`,
        '操作确认',
        { confirmButtonText: '确认', cancelButtonText: '取消', type: 'warning' }
    ).then(async () => {
      await executeApproval(approval, agentResult, userId)
    }).catch(() => {
      appendMessage({ role: 'ai', text: '操作已取消' })
      scrollToBottom()
    })
  }
}

const executeApproval = async (approval, agentResult, userId, extraParams = {}) => {
  isAiThinking.value = true
  try {
    const confirmRes = await agentApi.confirmApproval({
      task: agentResult.task,
      conversationId: agentResult.conversationId,
      userId: userId,
      model: currentModel.value,
      agentApprovalToken: approval.agentApprovalToken,
      ragEnabled: true,
      dryRun: false,
      maxIterations: 3,
      maxToolCalls: 8,
      knowledgeBaseId: 'default',
      autoReplan: true,
      returnIntermediateSteps: true,
      context: {
        ...agentResult.context,
        agentApprovalToken: approval.agentApprovalToken,
        documentId: extraParams.documentId,
        bucketName: 'doc-ai',
        ...extraParams
      }
    })

    appendMessage({
      role: 'ai',
      text: confirmRes.data.answer || '操作已执行',
      actionResult: parseToolResults(confirmRes.data.toolResults),
      toolCalls: confirmRes.data.toolResults || []
    })
    ElMessage.success('操作已完成')
    scrollToBottom()
  } catch (err) {
    console.error('操作失败:', err)
    appendMessage({
      role: 'ai',
      text: '操作失败：' + (err.response?.data?.message || err.message)
    })
    scrollToBottom()
  } finally {
    isAiThinking.value = false
  }
}


const parseToolResults = (toolResults) => {
  if (!toolResults || !Array.isArray(toolResults)) return null

  for (const tr of toolResults) {
    if (tr.status === 'error') continue
    const d = tr.data || {}

    // PPT
    if (tr.toolName?.includes('ppt') || d.htmlContent || d.fileName?.endsWith('.html')) {
      return {
        type: 'ppt',
        title: d.title || 'PPT演示文稿',
        slideCount: d.slideCount || 0,
        htmlContent: d.htmlContent || null,
        fileName: d.fileName || null,
      }
    }

    // Word
    if (d.fileName?.endsWith('.docx') || d.objectName?.endsWith('.docx')) {
      return {
        type: 'doc',
        fileName: d.fileName || d.objectName,
        objectName: d.objectName || d.fileName,
        bucketName: 'doc-ai',
        url: d.url || d.downloadUrl || null
      }
    }

    // 通用文件
    if (d.fileName || d.url) {
      return {
        type: 'file',
        fileName: d.fileName,
        url: d.url || d.downloadUrl
      }
    }
  }

  return null
}


const handleDownload = (actionResult) => {
  if (!actionResult) return

  // PPT 类型：直接用 htmlContent 生成 Blob 下载
  if (actionResult.type === 'ppt') {
    if (actionResult.htmlContent) {
      const blob = new Blob([actionResult.htmlContent], { type: 'text/html;charset=utf-8' })
      const url = URL.createObjectURL(blob)
      const a = document.createElement('a')
      a.href = url
      a.download = `${actionResult.title || 'PPT'}.html`
      document.body.appendChild(a)
      a.click()
      document.body.removeChild(a)
      URL.revokeObjectURL(url)
      ElMessage.success('PPT 下载成功')
    } else {
      ElMessage.warning('PPT 内容为空，无法下载')
    }
    return
  }

  // Word 类型下载修改：
  if (actionResult.type === 'doc') {
    if (actionResult.url) {
      window.open(actionResult.url, '_blank')
    } else if (actionResult.objectName || actionResult.fileName) {
      const name = actionResult.objectName || actionResult.fileName
      window.location.href = `/api/skills/file/download?bucket=doc-ai&objectName=${encodeURIComponent(name)}`
    } else {
      ElMessage.warning('无法获取文件下载地址')
    }
    return
  }

// 通用文件下载修改：
  if (actionResult.url) {
    window.open(actionResult.url, '_blank')
  } else if (actionResult.fileName) {
    window.location.href = `/api/skills/file/download?objectName=${encodeURIComponent(actionResult.fileName)}`
  }
}

const handleNewConversation = () => {
  ElMessageBox.confirm(
      '确定要开始新对话吗？当前对话记录将被清空。',
      '提示',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning',
      }
  ).then(() => {
    resetConversation()  // 清空对话历史和 ID
    ElMessage.success('已开始新对话')
  }).catch(() => {
    // 用户取消
  })
}

</script>

<style scoped>
:deep(.el-drawer__body) { padding: 0; overflow: hidden; }

/* 悬浮球 */
.global-ai-fab {
  position: fixed; bottom: 40px; right: 40px; width: 52px; height: 52px;
  border-radius: 50%; background: #3370ff; color: white;
  display: flex; justify-content: center; align-items: center;
  box-shadow: 0 4px 16px rgba(51, 112, 255, 0.3); cursor: pointer; z-index: 9999; transition: all 0.25s;
}
.global-ai-fab:hover { transform: scale(1.06); background: #2b5cd9; }

.chat-layout { display: flex; flex-direction: column; height: 100%; background: #f4f5f7; }

/* 顶部 Header */
.drawer-header {
  height: 56px; padding: 0 16px; background: #fff;
  display: flex; justify-content: space-between; align-items: center;
  border-bottom: 1px solid #edeef0; z-index: 10;
}
.header-title { display: flex; align-items: center; gap: 8px; font-size: 15px; font-weight: 600; color: #1f2329; }
.ai-icon-wrapper {
  width: 24px; height: 24px; background: #3370ff; border-radius: 6px;
  display: flex; justify-content: center; align-items: center;
}
.header-actions {
  display: flex;
  align-items: center;
}

/* 滚动区域 */
.chat-scroll-area { flex: 1; overflow-y: auto; padding: 20px; display: flex; flex-direction: column; gap: 16px; }

.message-row { display: flex; gap: 12px; align-items: flex-start; }
.message-row.user { flex-direction: row-reverse; }
.message-content { max-width: 78%; display: flex; flex-direction: column; gap: 6px; }

.avatar-box {
  width: 32px; height: 32px; border-radius: 50%;
  display: flex; justify-content: center; align-items: center; flex-shrink: 0;
}
.ai-avatar { background: #3370ff; box-shadow: 0 2px 6px rgba(51, 112, 255, 0.2); }
.user-avatar { background: #646a73; box-shadow: 0 2px 6px rgba(100, 106, 115, 0.2); }

/* 气泡 */
.bubble { padding: 10px 14px; font-size: 14px; line-height: 1.5; border-radius: 8px; word-break: break-all; }
.user-bubble { background: #3370ff; color: #fff; border-top-right-radius: 2px; }
.ai-bubble { background: #fff; color: #1f2329; border-top-left-radius: 2px; border: 1px solid #e8eaec; }
.ai-bubble.thinking { color: #8f959e; display: flex; align-items: center; gap: 6px; background: #fafafa; }

/* 文件卡片 */
.agent-file-card {
  display: flex; align-items: center; gap: 10px; background: #fff; border: 1px solid #dde0e3;
  padding: 10px 14px; border-radius: 6px; cursor: pointer; transition: all 0.2s;
}
.agent-file-card:hover { border-color: #3370ff; background: #fdfeff; }
.file-info .file-name { font-weight: 600; font-size: 13px; color: #1f2329; }
.file-info .file-desc { font-size: 11px; color: #8f959e; margin-top: 2px; }

/* 输入区 */
.input-area { background: #fff; padding: 14px 16px; border-top: 1px solid #edeef0; }
.input-wrapper {
  display: flex; align-items: center; background: #f4f5f7; border-radius: 20px;
  padding: 4px 4px 4px 14px; border: 1px solid transparent; transition: all 0.2s;
}
.input-wrapper.is-focused { border-color: #3370ff; background: #fff; box-shadow: 0 0 0 2px rgba(51,112,255,0.08); }
.custom-input { flex: 1; border: none; background: transparent; outline: none; font-size: 14px; color: #1f2329; }
.send-btn {
  width: 28px; height: 28px; border-radius: 50%; background: #dee0e3; color: #fff;
  display: flex; justify-content: center; align-items: center; cursor: not-allowed; transition: all 0.2s;
}
.send-btn.active { background: #3370ff; cursor: pointer; }
.send-btn.active:hover { background: #2b5cd9; }
.preserve-format { white-space: pre-wrap !important; }
.header-actions {
  display: flex;
  align-items: center;
  gap: 4px;
}

.header-actions .el-button {
  padding: 4px;
}

</style>