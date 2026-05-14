<template>
  <!-- ================= 模式 A：全局沉浸式 AI 对话模式 (对标飞书 aily) ================= -->
  <div v-if="isChatMode" class="feishu-chat-layout">
    <!-- 左侧会话历史导航 -->
    <aside class="chat-sidebar">
      <div class="sidebar-top">
        <div class="brand-back" @click="$router.push('/dashboard')">
          <el-icon><ArrowLeft /></el-icon> 返回工作台
        </div>
        <el-button class="new-chat-btn" icon="Plus" plain @click="startNewChat">开启新话题</el-button>

        <div class="history-group">
          <div class="group-title">历史会话</div>
          <div class="history-item active">
            <el-icon><ChatLineRound /></el-icon>
            <span class="text">{{ chatTitle }}</span>
          </div>
        </div>
      </div>
      <div class="sidebar-bottom">
        <div class="user-profile">
          <el-avatar :size="30" style="background-color: #3370ff; font-weight: bold; color: white;">
            {{ currentUserName.charAt(0).toUpperCase() }}
          </el-avatar>
          <span class="username">{{ currentUserName }}</span>
        </div>
      </div>
    </aside>

    <!-- 主聊天区域 -->
    <main class="chat-main">
      <header class="chat-header">
        <div class="header-inner">
          <span class="title">{{ chatTitle }}</span>
          <div class="header-actions">
            <el-button icon="Share" link>分享</el-button>
            <el-button icon="MoreFilled" link></el-button>
          </div>
        </div>
      </header>

      <!-- 聊天内容滚动区 -->
      <div class="chat-scroll-area" ref="globalChatRef">
        <div class="chat-content-container">
          <div v-for="(msg, i) in chatHistory" :key="i" :class="['message-row', msg.role]">
            <div class="avatar-col" v-if="msg.role === 'ai'">
              <el-avatar :size="36" src="https://api.dicebear.com/7.x/avataaars/svg?seed=AIAssistant" class="ai-avatar" />
            </div>
            <div class="message-content-col">
              <div v-if="msg.role === 'user'" class="user-bubble preserve-format">{{ msg.text }}</div>
              <div v-else class="ai-structured-card">
                <div class="card-body preserve-format">{{ msg.text }}</div>
              </div>
            </div>
          </div>

          <!-- 思考中状态 -->
          <div v-if="isAiThinking" class="message-row ai">
            <div class="avatar-col"><el-avatar :size="36" src="https://api.dicebear.com/7.x/avataaars/svg?seed=AIAssistant" /></div>
            <div class="message-content-col">
              <div class="ai-structured-card thinking">
                <el-icon class="is-loading"><Loading /></el-icon> 正在执行：搜索知识库并生成分析...
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 底部悬浮输入区 (支持 RAG 切换) -->
      <div class="chat-input-container">
        <div class="input-wrapper-inner" :class="{ 'is-focused': isInputFocused, 'rag-active': isRagMode }">
          <!-- 模式切换：气泡/书本图标 -->
          <el-tooltip :content="isRagMode ? '当前：基于云端文档库问答' : '当前：普通对话模式'" placement="top">
            <el-icon class="mode-icon" @click="isRagMode = !isRagMode">
              <Collection v-if="isRagMode" /><ChatDotRound v-else />
            </el-icon>
          </el-tooltip>

          <el-icon class="plus-icon"><Plus /></el-icon>
          <input
              v-model="userMsg"
              class="chat-input"
              placeholder="问问我关于工作的任何指令..."
              @keyup.enter="handleSend"
              @focus="isInputFocused = true"
              @blur="isInputFocused = false"
          />
          <div class="input-right">
            <el-icon class="mic-icon"><Microphone /></el-icon>
            <div class="send-btn-circle" :class="{ 'active': userMsg.length > 0 }" @click="handleSend">
              <el-icon size="20"><Top /></el-icon>
            </div>
          </div>
        </div>
        <div class="ai-hint">AI 可能生成错误内容，仅供参考</div>
      </div>
    </main>
  </div>

  <!-- ================= 模式 B：传统的文档阅读模式 (格式保护 + 悬浮球 + 优化侧边栏) ================= -->
  <div v-else class="editor-page">
    <header class="toolbar">
      <div class="left">
        <el-button icon="ArrowLeft" link @click="$router.push('/dashboard')"></el-button>
        <el-divider direction="vertical" />
        <span class="doc-title">{{ docName }}</span>
        <span class="save-status">{{ saveStatusText }}</span>
        <el-button circle icon="Clock" size="small" style="margin-left:10px;" @click="openHistoryDrawer"></el-button>
      </div>
      <div class="header-right">
        <el-button type="primary" size="small" round icon="Check" @click="handleManualSave" :loading="isSaving">手动保存</el-button>
        <el-button size="small" round icon="Download" @click="handleDownload">下载最新版</el-button>
        <el-button type="primary" size="small" round icon="Share">协作</el-button>
        <el-avatar :size="30" style="background-color: #3370ff; font-weight: bold; color: white; margin-left:10px;">{{ currentUserName.charAt(0).toUpperCase() }}</el-avatar>
      </div>
    </header>

    <div class="workspace">
      <!-- A4 纸编辑区 -->
      <main class="editor-main" v-loading="docLoading" @scroll="handleScroll">
        <div class="paper-container">
          <div class="paper preserve-format" contenteditable="true" v-html="docContent" @mouseup="handleTextSelection" @input="handleInput"></div>
        </div>
        <!-- 核心：精准定位的 AI 悬浮球 -->
        <transition name="el-zoom-in-center">
          <div v-if="showAiBall" class="ai-float-ball" :style="ballStyle">
            <div class="ball-inner">
              <div class="menu-opt" @click.stop="askAiWithContext('润色')"><el-icon><MagicStick /></el-icon> 润色</div>
              <el-divider direction="vertical" />
              <div class="menu-opt" @click.stop="askAiWithContext('纠错')"><el-icon><Aim /></el-icon> 纠错</div>
              <div class="ball-arrow"></div>
            </div>
          </div>
        </transition>
      </main>

      <!-- 侧边栏布局：顶中底固定 -->
      <aside class="ai-sidebar">
        <!-- 顶部：摘要和关键词 (固定) -->
        <div class="ai-sidebar-top">
          <div class="ai-header"><span><el-icon color="#409EFF"><MagicStick /></el-icon> AI 灵感助理</span></div>

          <!-- 基础文本分析数据看板 -->
          <div class="stats-box" v-if="textStats">
            <div class="stats-grid">
              <div class="stat-item">
                <span class="num">{{ textStats.totalCharacters || 0 }}</span>
                <span class="desc">总字符数</span>
              </div>
              <div class="stat-item">
                <span class="num">{{ textStats.lines || 0 }}</span>
                <span class="desc">段落数</span>
              </div>
              <div class="stat-item">
                <span class="num">{{ textStats.chineseCharacters || 0 }}</span>
                <span class="desc">中文字符</span>
              </div>
              <div class="stat-item">
                <span class="num">{{ textStats.punctuations || 0 }}</span>
                <span class="desc">标点符号</span>
              </div>
            </div>
          </div>

          <div class="summary-box" v-if="aiSummary">
            <div class="label">✨ 智能摘要</div>
            <div class="text preserve-format">{{ aiSummary }}</div>
            <!-- 关键词展示 -->
            <div class="keywords-area" v-loading="keywordsLoading">
              <el-tag v-for="(tag, index) in keywords" :key="index" class="keyword-tag" size="small" round effect="light" :type="['', 'success', 'warning', 'danger', 'info'][index % 5]"># {{ tag }}</el-tag>
            </div>
          </div>
          <el-divider v-if="aiSummary" style="margin: 0" />
        </div>

        <!-- 中间：聊天记录 (自适应滚动) -->
        <div class="chat-area" ref="sidebarChatRef">
          <div v-for="(msg, i) in chatHistory" :key="i" :class="['chat-bubble', msg.role]">
            <div class="preserve-format">{{ msg.text }}</div>
          </div>
          <div v-if="isAiThinking" class="chat-bubble ai thinking"><el-icon class="is-loading"><Loading /></el-icon> AI 正在为您生成...</div>
        </div>

        <!-- 底部：输入框 (固定) -->
        <div class="input-area">
          <div class="selected-context" v-if="selectedText">已选文本：{{ selectedText.substring(0, 10) }}...</div>
          <div class="input-actions" style="margin-bottom: 8px;">
            <el-switch v-model="isRagMode" active-text="基于文档库问答" size="small" />
          </div>
          <el-input v-model="userMsg" type="textarea" :rows="3" placeholder="问问 AI..." @keyup.enter.native="handleSend" resize="none" />
          <el-button type="primary" class="send-btn" :loading="isAiThinking" @click="handleSend">发送指令</el-button>
        </div>
      </aside>

      <el-drawer v-model="showHistory" title="文档历史版本" size="350px" direction="rtl">
        <el-timeline v-if="versionList.length > 0">
          <el-timeline-item v-for="ver in versionList" :key="ver.id" :timestamp="new Date(ver.createTime).toLocaleString()" placement="top">
            <el-card shadow="hover" class="version-card">
              <p>版本号：V{{ ver.versionNumber }}</p>
              <el-button size="small" type="primary" plain style="margin-top:10px" @click="handleRestore(ver.versionNumber)">恢复此版本</el-button>
            </el-card>
          </el-timeline-item>
        </el-timeline>
        <el-empty v-else description="暂无历史记录" />
      </el-drawer>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { fileApi } from '../api/file'
import { userApi } from '../api/user'
import { docApi } from '../api/document'
import { aiApi } from '../api/ai'
import { ElMessage, ElMessageBox } from 'element-plus'

const route = useRoute(); const router = useRouter(); const docId = route.params.id

// ===== 状态定义 =====
const isChatMode = computed(() => docId === 'chat-mode')
const chatTitle = ref('新对话'); const isInputFocused = ref(false); const isSaving = ref(false); const saveStatusText = ref('已同步')
const docName = ref(sessionStorage.getItem('currentDocName') || '未命名文档'); const currentUserName = ref('User')
const docContent = ref(''); const docLoading = ref(false); const aiSummary = ref('')
const userMsg = ref(''); const isAiThinking = ref(false); const chatHistory = ref([]); const currentConvId = ref('')
const showAiBall = ref(false); const ballStyle = reactive({ top: '0px', left: '0px' }); const selectedText = ref('')
const showHistory = ref(false); const versionList = ref([])
const keywords = ref([]); const keywordsLoading = ref(false); const isRagMode = ref(false)
const textStats = ref(null)

// ===== 1. 加载文档与初始化 AI =====
const loadDocData = async () => {
  const uid = localStorage.getItem('userId') || '1'
  userApi.getUserInfo(uid).then(r => currentUserName.value = r.data.username || 'aa').catch(()=>{})

  // 初始化 AI 对话
  aiApi.startChat(uid).then(res => currentConvId.value = res.data).catch(() => console.warn('AI 会话开启失败'))

  if (isChatMode.value) {
    const task = sessionStorage.getItem('global_ai_task')
    if (task) {
      chatTitle.value = task.length > 10 ? task.substring(0, 10) + '...' : task
      userMsg.value = task; sessionStorage.removeItem('global_ai_task'); setTimeout(() => handleSend(), 500)
    } else { chatHistory.value.push({ role: 'ai', text: 'Hi！我是 DocAI 智能伙伴，今天需要我帮你完成什么工作？' }) }
    return
  }

  chatHistory.value.push({ role: 'ai', text: '你好！我是接入了大语言模型的助手。你可以向我提问，或选中文本进行润色。' })
  docLoading.value = true
  try {
    const docRes = await docApi.getDocDetail(docId)
    const data = docRes.data || docRes
    docName.value = data.title; aiSummary.value = data.summary; docContent.value = data.content || ''

    // 如果文档有内容，并行去请求“文本分析(字数统计)”接口
    if (data.content && data.content.length > 5) {
      // 获取纯文本用于分析 (剥离可能存在的 HTML 标签)
      const plainText = data.content.replace(/<[^>]+>/g, '')

      aiApi.analyzeText(plainText).then(res => {
        // 将后端的 TextAnalyzeVO 数据赋给前端变量
        textStats.value = res.data
      }).catch(e => {
        console.warn('文本分析接口调用失败', e)
        // Mock
        textStats.value = {
          totalCharacters: plainText.length,
          lines: plainText.split('\n').filter(line => line.trim() !== '').length,
          chineseCharacters: Math.floor(plainText.length * 0.8),
          punctuations: Math.floor(plainText.length * 0.15)
        }
      })
    }

    // 并行拉取关键词 (不阻塞 A4 纸显示)
    if (data.content && data.content.length > 5) {
      keywordsLoading.value = true
      aiApi.extractKeywords(data.content).then(res => {
        keywords.value = res.data.keywords.map(k => k.word)
      }).finally(() => { keywordsLoading.value = false })
    }
  } catch (e) {
    docContent.value = `<div style="text-align:center;color:red;padding:100px;">读取文档失败</div>`
  } finally { docLoading.value = false }
}

onMounted(() => loadDocData())

// ===== 2. 交互逻辑（打字机效果） =====
const handleSend = async () => {
  if (!userMsg.value.trim() || isAiThinking.value) return

  const q = userMsg.value
  chatHistory.value.push({ role: 'user', text: q })
  userMsg.value = ''
  isAiThinking.value = true
  clearSelection()

  if (isChatMode.value && chatTitle.value === '新对话') {
    chatTitle.value = q.substring(0, 10) + '...'
  }
  scrollToBottom()

  try {
    let answer = ""

    if (isRagMode.value) {
      // RAG 模式：走知识库接口 mock
      // 获取 A4 纸上的纯文本内容作为“知识库”
      const contextText = document.querySelector('.paper').innerText || '无内容'

      // 自己拼装一 Prompt（提示词），模拟 RAG 的流程
      const superPrompt = `你是一个专业的企业文档助手。请严格基于以下【文档内容】回答用户的问题。如果文档中没有相关信息，请回答“文档中未提及此信息”。\n\n【文档内容】\n${contextText.substring(0, 3000)}\n\n【用户问题】\n${q}`

      // 直接去调那个chat
      const res = await aiApi.sendMessage(currentConvId.value || '123', superPrompt)
      answer = res.data.answer || res.data
    } else {
      // 总结，summarize 接口
      if (q.includes('总结') || q.includes('摘要')) {
        const res = await aiApi.summarizeText(q).catch(()=>null)
        answer = res?.data?.summary || res?.data || ""
      }
      else {
        const res = await aiApi.sendMessage(currentConvId.value || '123', q)
        answer = res.data.answer || res.data
      }
    }

    // Mock
    if (!answer || typeof answer !== 'string') throw new Error('后端返回数据格式异常')

    typeEffect(answer)
  } catch (error) {
    // Mock 模式
    const lowerQ = q.toLowerCase()
    let mockReply = "建议在此处增加更多量化指标以提升专业度。"

    if (lowerQ.includes('纠错') || lowerQ.includes('错别字') || lowerQ.includes('原因')) {
      mockReply = `【智能纠错分析】\n发现 1 处拼写错误：\n- ❌ “严尽” 应修改为 ✅ “严禁”。\n\n原因说明：“严禁”表示严格禁止某项行为，是规范用语；“严尽”属于同音错别字。建议修正以保证通知的严肃性。`
    } else if (lowerQ.includes('润色') || lowerQ.includes('优化')) {
      mockReply = `【智能润色建议】建议修改为：“为了杜绝消防安全隐患，严禁在办公区域私拉乱接电线或串联使用多孔插座，以免造成电路过载引发短路。”`
    } else if (lowerQ.includes('迟到') || lowerQ.includes('罚款')) {
      mockReply = `根据检索文档，如果发现私拉电线，公司将由后勤人员直接处以 200 元罚款。`
    }

    typeEffect(mockReply)
  }
}

const typeEffect = (text) => {
  isAiThinking.value = false; const aiMsg = reactive({ role: 'ai', text: '' }); chatHistory.value.push(aiMsg)
  let i = 0; const t = setInterval(() => { if(i < text.length) { aiMsg.text += text.charAt(i); i++; scrollToBottom() } else clearInterval(t) }, 30)
}

// ===== 3. 手动保存与恢复逻辑 =====
const handleManualSave = async () => {
  if (isChatMode.value) return
  isSaving.value = true; saveStatusText.value = '正在保存...'
  try {
    const content = document.querySelector('.paper').innerHTML
    await docApi.updateDoc(docId, { title: docName.value, content: content, category: 'default' })
    saveStatusText.value = '已同步'; ElMessage.success('保存成功')
  } catch (e) { saveStatusText.value = '保存失败'; ElMessage.error('同步失败') }
  finally { isSaving.value = false }
}

const handleRestore = async (verNum) => {
  ElMessageBox.confirm(`确定要将文档恢复到 V${verNum} 吗？当前未保存的修改将丢失。`, '版本回溯').then(async () => {
    docLoading.value = true
    try {
      await docApi.restoreVersion(docId, verNum)
      ElMessage.success('版本已回溯')

      // 恢复成功后重新拉取数据
      await loadDocData()

      showHistory.value = false
    } catch (e) {
      ElMessage.error('回溯失败')
    } finally {
      docLoading.value = false
    }
  }).catch(() => {})
}

// ===== 4. 文件导出与选中逻辑 =====
const handleDownload = async () => {
  isSaving.value = true
  saveStatusText.value = '正在导出最新版...'
  try {
    // 获取 A4 纸里的纯文本（innerText 会自动保留换行和空格）
    const latestText = document.querySelector('.paper').innerText

    // 包装成 Word 认得的格式，并强制要求保留空白符
    const wordHtml = `
      <html xmlns:o='urn:schemas-microsoft-com:office:office' xmlns:w='urn:schemas-microsoft-com:office:office:word' xmlns='http://www.w3.org/TR/REC-html40'>
      <head><meta charset='utf-8'></head>
      <body style="font-family: sans-serif;">
        <pre style="white-space: pre-wrap; word-break: break-all;">${latestText}</pre>
      </body>
      </html>`

    const blob = new Blob([wordHtml], { type: 'application/msword' })
    const url = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.setAttribute('download', `[最新]_${docName.value}.doc`)
    document.body.appendChild(link); link.click(); document.body.removeChild(link)
    window.URL.revokeObjectURL(url)

    ElMessage.success('导出成功')
    saveStatusText.value = '已同步'
  } catch (e) { ElMessage.error('下载失败') }
  finally { isSaving.value = false }
}

const handleTextSelection = () => {
  const s = window.getSelection(); const t = s.toString().trim()
  if (t && s.rangeCount > 0) {
    selectedText.value = t; const r = s.getRangeAt(0).getBoundingClientRect()
    ballStyle.top = `${r.top - 55}px`; ballStyle.left = `${r.left + r.width / 2}px`; showAiBall.value = true
  } else { showAiBall.value = false }
}

const askAiWithContext = (type) => {
  userMsg.value = type === '纠错' ? `纠错并分析原因："${selectedText.value}"` : `润色使表达更专业："${selectedText.value}"`
  showAiBall.value = false;
  handleSend()
}
const handleScroll = () => { if(showAiBall.value) showAiBall.value = false }
const clearSelection = () => { selectedText.value = ''; showAiBall.value = false }
const handleInput = () => { saveStatusText.value = '修改未保存' }
const openHistoryDrawer = async () => { const res = await docApi.getVersions(docId); versionList.value = res.data; showHistory.value = true }
const startNewChat = () => { chatHistory.value = []; chatTitle.value = '新对话' }
const scrollToBottom = () => { nextTick(() => {
  const c = isChatMode.value ? document.querySelector('.chat-scroll-area') : document.querySelector('.chat-area'); if(c) c.scrollTop = c.scrollHeight
}) }
</script>


<style scoped>
/* ==================== 全局格式保护 ==================== */
.preserve-format {
  white-space: pre-wrap !important;
  word-break: break-word;
  text-align: left;
}

/* ==================== 模式 A：飞书全局对话样式 ==================== */
.feishu-chat-layout {
  height: 100vh;
  width: 100vw;
  display: flex;
  background: #fff;
  overflow: hidden; /* 防止出现外层滚动条 */
}

/* 聊天侧边栏 (固定宽度，上下分布) */
.chat-sidebar {
  width: 260px;
  flex-shrink: 0; /* 绝对不被压缩 */
  background: #f9fafb;
  border-right: 1px solid #ebeef5;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
}
.sidebar-top { padding: 24px 20px; }
.brand-back { display: flex; align-items: center; gap: 8px; color: #5c5f66; font-size: 14px; cursor: pointer; margin-bottom: 30px; font-weight: 500;}
.brand-back:hover { color: #3370ff; }
.new-chat-btn { width: 100%; border-radius: 8px; margin-bottom: 30px; font-weight: 600; height: 36px; }
.group-title { font-size: 12px; color: #8f959e; margin-bottom: 12px; padding-left: 4px; }
.history-item { padding: 12px 16px; border-radius: 8px; display: flex; align-items: center; gap: 10px; font-size: 14px; background: #e1eaff; color: #3370ff; cursor: pointer; font-weight: 500; }
.history-item .text { white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }

/* 侧边栏底部用户 */
.sidebar-bottom { padding: 20px; border-top: 1px solid #ebeef5; }
.user-profile { display: flex; align-items: center; gap: 12px; }
.user-profile .username { font-size: 14px; font-weight: 500; color: #1f2329; }

/* 主聊天区 (占据剩余空间) */
.chat-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  position: relative;
  background: #fff;
  min-width: 0; /* 防止内容过长撑破 Flex 布局 */
}
.chat-header { height: 64px; display: flex; justify-content: center; border-bottom: 1px solid #f0f0f0; flex-shrink: 0;}
.header-inner { width: 100%; max-width: 900px; display: flex; justify-content: space-between; align-items: center; padding: 0 24px; }
.chat-header .title { font-size: 16px; font-weight: 600; color: #1f2329; }

/* 聊天滚动区 */
.chat-scroll-area {
  flex: 1;
  overflow-y: auto;
  padding: 40px 0 160px 0; /* 底部留出巨大空间给输入框 */
  display: flex;
  justify-content: center;
}
.chat-content-container { width: 100%; max-width: 850px; padding: 0 24px; display: flex; flex-direction: column; }

.message-row { display: flex; gap: 16px; margin-bottom: 32px; }
.message-row.user { flex-direction: row-reverse; }
.message-content-col { max-width: 80%; }

.user-bubble { background: #3370ff; color: #fff; padding: 14px 20px; border-radius: 16px 4px 16px 16px; font-size: 15px; line-height: 1.6; box-shadow: 0 4px 12px rgba(51, 112, 255, 0.2); }
.ai-structured-card { background: #fff; border: 1px solid #dee0e3; border-radius: 4px 16px 16px 16px; padding: 24px; box-shadow: 0 6px 18px rgba(0,0,0,0.04); transition: all 0.3s; }
.ai-structured-card:hover { border-color: #3370ff; box-shadow: 0 8px 24px rgba(51, 112, 255, 0.08); }
.ai-card-content { font-size: 15px; color: #1f2329; line-height: 1.8; font-family: inherit; margin: 0; }
.ai-structured-card.thinking { color: #8f959e; font-style: italic; display: flex; align-items: center; gap: 10px; padding: 16px 24px; }

/* 底部居中悬浮输入框 */
.chat-input-container {
  position: absolute;
  bottom: 0; left: 0; width: 100%;
  display: flex; flex-direction: column; align-items: center;
  padding: 24px 0 32px 0;
  background: linear-gradient(to top, #fff 80%, rgba(255,255,255,0));
}
.input-wrapper-inner {
  width: 100%; max-width: 800px; height: 60px; background: #fff; border-radius: 30px;
  box-shadow: 0 6px 24px rgba(31,35,41,0.08); display: flex; align-items: center; padding: 0 10px 0 24px;
  border: 1px solid #dee0e3; transition: all 0.3s;
}
.input-wrapper-inner.is-focused { border-color: #3370ff; box-shadow: 0 8px 32px rgba(51,112,255,0.15); }
.chat-input { flex: 1; border: none; outline: none; font-size: 16px; color: #1f2329; background: transparent; }
.input-right { display: flex; align-items: center; gap: 15px; }
.send-btn-circle {
  width: 44px; height: 44px; border-radius: 50%; background: #f2f3f5; color: #fff;
  display: flex; justify-content: center; align-items: center; cursor: pointer; transition: 0.3s;
}
.send-btn-circle.active { background: #3370ff; }
.ai-hint { font-size: 12px; color: #bbbfc4; margin-top: 16px; }


/* ==================== 模式 B：文档阅读模式样式 (保持不变) ==================== */
.editor-page { height: 100vh; display: flex; flex-direction: column; background: #f5f6f7; }
.toolbar { height: 56px; background: #fff; border-bottom: 1px solid #dee0e3; display: flex; align-items: center; padding: 0 24px; justify-content: space-between; flex-shrink: 0;}
.workspace { flex: 1; display: flex; overflow: hidden; position: relative; }
.editor-main { flex: 1; overflow-y: auto; display: flex; justify-content: center; padding: 50px 0; background: #f0f2f5; position: relative; }
.paper-container { position: relative; }
.paper { width: 780px; min-height: 1100px; background: #fff; padding: 80px 100px; box-shadow: 0 4px 16px rgba(0,0,0,0.06); outline: none; line-height: 1.8; font-size: 16px; color: #1f2329; }

.ai-sidebar { width: 360px; background: #fff; border-left: 1px solid #dee0e3; display: flex; flex-direction: column; height: 100%; flex-shrink: 0;}
.ai-sidebar-top { flex-shrink: 0; }
.ai-header { padding: 16px 20px; font-weight: 600; border-bottom: 1px solid #f0f0f0; color: #1f2329;}
.chat-area { flex-grow: 1; padding: 20px; overflow-y: auto; background: #fafbfc; display: flex; flex-direction: column; }
.chat-bubble { max-width: 90%; padding: 12px 16px; border-radius: 14px; margin-bottom: 16px; font-size: 14px; box-shadow: 0 2px 8px rgba(0,0,0,0.04); }
.chat-bubble.ai { background: #fff; border: 1px solid #dee0e3; align-self: flex-start; border-bottom-left-radius: 2px;}
.chat-bubble.user { background: #3370ff; color: #fff; align-self: flex-end; margin-left: auto; border-bottom-right-radius: 2px;}
.summary-box { background: #fff; padding: 18px; border-radius: 12px; border: 1px solid #e1eaff; border-left: 5px solid #3370ff; margin: 15px; box-shadow: 0 4px 12px rgba(51,112,255,0.06); }
.input-area { flex-shrink: 0; padding: 20px; border-top: 1px solid #f0f0f0; background: #fff; }
.send-btn { width: 100%; margin-top: 12px; height: 42px; font-weight: 600; letter-spacing: 1px;}

.ai-float-ball { position: fixed; z-index: 9999; transform: translateX(-50%); cursor: pointer; filter: drop-shadow(0 4px 12px rgba(51, 112, 255, 0.4)); }
.ball-inner {background: #3370ff;color: #fff;padding: 0 12px;border-radius: 8px;display: flex;align-items: center;height: 36px;font-size: 13px;font-weight: 600;}
.ball-inner:after { content:''; position: absolute; bottom: -6px; left: 50%; transform: translateX(-50%); border-left: 6px solid transparent; border-right: 6px solid transparent; border-top: 6px solid #3370ff; }
.menu-opt {padding: 0 8px;cursor: pointer;display: flex;align-items: center;gap: 4px;transition: opacity 0.2s;}
/* 🚨 文本分析看板样式 */
.stats-box {
  padding: 16px 20px 0 20px;
  background: #fff;
}
.stats-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  background: #f8f9fa;
  border-radius: 8px;
  padding: 12px 0;
  border: 1px solid #ebeef5;
}
.stat-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  border-right: 1px solid #ebeef5;
}
.stat-item:last-child {
  border-right: none;
}
.stat-item .num {
  font-size: 15px;
  font-weight: 700;
  color: #3370ff;
  font-family: monospace; /* 让数字更整齐 */
}
.stat-item .desc {
  font-size: 11px;
  color: #8f959e;
  margin-top: 4px;
}
</style>
