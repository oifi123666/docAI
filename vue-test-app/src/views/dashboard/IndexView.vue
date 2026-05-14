<template>
  <div class="feishu-layout">
    <el-container class="full-height">

      <!-- 1. 左侧：极简侧边栏 -->
      <el-aside width="240px" class="feishu-aside">
        <div class="aside-top">
          <div class="brand">
            <div class="logo-box"><el-icon><Cpu /></el-icon></div>
            <span class="brand-text">DocAI 妙搭</span>
          </div>
          <div class="nav-menu">
            <div class="nav-item active"><el-icon><Monitor /></el-icon> 我的工作台</div>
            <div class="nav-item"><el-icon><FolderOpened /></el-icon> 云端文档库</div>
            <div class="nav-item"><el-icon><MagicStick /></el-icon> AI 技能发现</div>
            <div v-if="isAdminUser" class="nav-item aiops-nav" @click="goToAIOps">
              <el-icon><Cpu /></el-icon> AI Ops 运维中心
            </div>
          </div>
        </div>

        <!-- 底部：真实用户信息 (头像取首字母) -->
        <div class="aside-bottom">
          <el-dropdown trigger="click" placement="top-start" @command="handleUserCommand">
            <div class="user-profile">
              <el-avatar :size="32" style="background-color: #3370ff; font-weight: bold; color: white;">
                {{ currentUserName.charAt(0).toUpperCase() }}
              </el-avatar>
              <span class="username">{{ currentUserName }}</span>
              <el-icon class="more-icon"><MoreFilled /></el-icon>
            </div>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="logout" icon="SwitchButton" style="color: #F56C6C">退出系统</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-aside>

      <!-- 2. 右侧：主内容区 -->
      <el-main class="feishu-main" v-loading="loading">
        <div class="main-container">

          <!-- AI 欢迎与对话区 (对标飞书 aily) -->
          <div class="ai-hero">
            <el-avatar :size="64" style="background-color: #3370ff; font-size: 28px; font-weight: bold; margin-bottom: 24px; color: white;">
              {{ currentUserName.charAt(0).toUpperCase() }}
            </el-avatar>
            <h1 class="hero-title">Hi {{ currentUserName }}，今天需要我帮你分析什么？</h1>

            <!-- 飞书级交互：巨大输入框 + 气泡弹出菜单 -->
            <el-popover
                placement="bottom-start"
                :width="700"
                trigger="click"
                :show-arrow="false"
                popper-class="feishu-popover"
            >
              <template #reference>
                <div class="hero-search">
                  <el-icon class="prefix-icon"><Plus /></el-icon>
                  <input
                      v-model="aiTask"
                      class="hero-input"
                      placeholder="总结我的上周工作，或者让我帮你写一份大纲..."
                      @keyup.enter="handleAiTask"
                  />
                  <el-icon class="mic-icon"><Microphone /></el-icon>
                  <div class="send-action-btn" @click.stop="handleAiTask">
                    <el-icon size="18"><Top /></el-icon>
                  </div>
                </div>
              </template>

              <!-- 弹出菜单：上传入口隐藏在这里 -->
              <div class="popover-content">
                <div class="menu-list">
                  <input type="file" ref="fileInputRef" style="display: none;" @change="onFileSelected" />
                  <div class="menu-item" @click="triggerUpload">
                    <el-icon><Paperclip /></el-icon> 上传文件并由 AI 解析
                  </div>
                  <div class="menu-item" @click="mockSkill('写会议纪要')"><el-icon><Document /></el-icon> 编写会议纪要</div>
                  <div class="menu-item" @click="mockSkill('分析文档')"><el-icon><DataLine /></el-icon> 提取核心数据</div>
                </div>
              </div>
            </el-popover>

            <!-- 快捷胶囊按钮 -->
            <div class="quick-prompts">
              <div class="prompt-pill" @click="triggerUpload"><el-icon><Upload /></el-icon> 上传文档</div>
              <div class="prompt-pill" @click="mockSkill('搭建工作台')"><el-icon><DataAnalysis /></el-icon> 搭建仪表盘</div>
              <div class="prompt-pill" @click="mockSkill('对话')"><el-icon><ChatLineSquare /></el-icon> 开启对话</div>
            </div>
          </div>

          <!-- 最近文档列表 -->
          <div class="list-section">
            <div class="section-header">
              <span class="section-title">我的云端文档 <span class="count">({{ docList.length }})</span></span>
              <div class="header-actions">
                <el-button type="danger" size="small" plain icon="Delete" @click="clearDirtyData">一键清理文档</el-button>
                <el-input
                    v-model="search"
                    placeholder="输入关键词并按回车搜索..."
                    size="small"
                    style="width: 220px"
                    prefix-icon="Search"
                    clearable
                    @keyup.enter="handleSearch"
                    @clear="fetchFiles"
                />
              </div>
            </div>

            <!-- 卡片网格布局 -->
            <div class="card-grid" v-if="filteredList.length > 0">
              <div class="doc-card" v-for="doc in filteredList" :key="doc.id">

                <!-- 卡片上半部分渐变封面 -->
                <div class="card-cover" :class="doc.color" @click="goToEditor(doc.id, doc.name)">
                  <el-icon :size="48" color="rgba(255,255,255,0.9)"><Document /></el-icon>
                </div>

                <!-- 卡片下半部分 -->
                <div class="card-info">
                  <div class="info-top">
                    <el-tooltip :content="doc.name" placement="top" :show-after="500">
                      <div class="doc-name" @click="goToEditor(doc.id, doc.name)">{{ doc.name }}</div>
                    </el-tooltip>
                    <el-dropdown trigger="click" @command="(cmd) => handleCardCommand(cmd, doc)">
                      <el-icon class="more-btn"><MoreFilled /></el-icon>
                      <template #dropdown>
                        <el-dropdown-menu>
                          <!-- 下载、删除选项 -->
                          <el-dropdown-item command="download" icon="Download">下载到本地</el-dropdown-item>
                          <el-dropdown-item command="delete" icon="Delete" style="color: #F56C6C">删除文档</el-dropdown-item>
                        </el-dropdown-menu>
                      </template>
                    </el-dropdown>
                  </div>
                  <div class="doc-meta">
                    <span>{{ doc.time }}</span>
                    <el-tag size="small" :type="doc.analyzed ? 'success' : 'info'" round effect="light">
                      {{ doc.analyzed ? 'AI已就绪' : '待处理' }}
                    </el-tag>
                  </div>
                </div>
              </div>
            </div>

            <el-empty v-else description="暂无文档，点击上方对话框上传" />
          </div>

        </div>
      </el-main>
    </el-container>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { fileApi } from '../../api/file'
import { userApi } from '../../api/user'
import { docApi } from '../../api/document'
import { STORAGE_KEYS } from '../../constants'
import { isAdmin } from '../../utils/jwt'

const router = useRouter()
const loading = ref(false)
const currentUserName = ref('User')
const docList = ref([])
const search = ref('')
const aiTask = ref('')
const fileInputRef = ref(null)
const isAdminUser = ref(isAdmin())

// 获取用户信息
const fetchUser = async () => {
  const uid = localStorage.getItem('userId')
  if (uid) {
    try {
      const res = await userApi.getUserInfo(uid)
      currentUserName.value = res.data.username || 'User'
    } catch (e) { currentUserName.value = '测试用户' }
  }
}

// 获取文档列表 (从 8084 获取)
const fetchFiles = async () => {
  loading.value = true
  try {
    // 暂时查 1 号用户的文件
    const res = await docApi.getUserDocs(1)
    if (res.data && Array.isArray(res.data)) {
      docList.value = res.data.map(item => ({
        id: item.id,
        fileId: item.fileId,
        name: item.title || '未命名文档',
        time: item.createTime ? new Date(item.createTime).toLocaleDateString() : '未知',
        analyzed: !!item.summary,
        color: ['bg-blue', 'bg-orange', 'bg-green', 'bg-purple'][Math.floor(Math.random() * 4)]
      }))
    }
  } catch (e) {
    console.error('拉取列表失败', e)
  } finally { loading.value = false }
}

onMounted(() => { fetchUser(); fetchFiles(); })

const filteredList = computed(() => docList.value.filter(d => d.name.toLowerCase().includes(search.value.toLowerCase())))

// 搜索逻辑
const handleSearch = async () => {
  if (!search.value.trim()) {
    return fetchFiles() // 如果搜空，恢复全量列表
  }

  loading.value = true
  try {
    // 调 8084 接口进行全文检索（搜标题+内容）
    const res = await docApi.searchDocs(search.value)

    if (res.data) {
      docList.value = res.data.map(item => ({
        id: item.id,
        fileId: item.fileId,
        name: item.title,
        time: new Date(item.createTime).toLocaleDateString(),
        color: ['bg-blue', 'bg-orange', 'bg-green', 'bg-purple'][Math.floor(Math.random() * 4)]
      }))
    }
    ElMessage.success(`搜索到 ${docList.value.length} 篇相关文档`)
  } catch (e) {
    ElMessage.error('搜索失败')
  } finally {
    loading.value = false
  }
}



// 飞书化上传交互
const triggerUpload = () => fileInputRef.value?.click()

const onFileSelected = async (event) => {
  const rawFile = event.target.files[0]
  if (!rawFile) return

  loading.value = true
  try {
    // 先传文件仓库
    const fileRes = await fileApi.upload(rawFile)
    const fileId = fileRes.data.fileId || fileRes.data.id

    // 如果是 TXT，前端提取文字发给后端解析
    let content = ''
    if (rawFile.name.endsWith('.txt') || rawFile.name.endsWith('.md')) {
      content = await rawFile.text()
    }

    // 在文档服务注册
    await docApi.createDoc({
      title: rawFile.name,
      fileId: fileId,
      content: content,
      category: 'default'
    })

    ElMessage.success(`《${rawFile.name}》已成功存入云端！`)
    fetchFiles()
    event.target.value = ''
  } catch (e) {
    ElMessage.error('上传链路异常')
  } finally { loading.value = false }
}

// 卡片逻辑(删除下载)
const handleCardCommand = async (cmd, doc) => {
  if (cmd === 'delete') {
    ElMessageBox.confirm(`确定要永久删除《${doc.name}》吗？`).then(async () => {
      loading.value = true
      try {
        await docApi.deleteDoc(doc.id)
        if (doc.fileId) await fileApi.delete(doc.fileId).catch(()=>{})
        ElMessage.success('已删除')
        fetchFiles()
      } catch (e) { ElMessage.error('删除失败') }
      finally { loading.value = false }
    }).catch(() => {})
  }
  // 下载部分
  else if (cmd === 'download') {
    loading.value = true
    try {
      const res = await docApi.getDocDetail(doc.id)
      const content = res.data.content || ""

      // 使用 pre 标签保护格式
      const wordHtml = `
        <html xmlns:o='urn:schemas-microsoft-com:office:office' xmlns:w='urn:schemas-microsoft-com:office:office:word' xmlns='http://www.w3.org/TR/REC-html40'>
        <head><meta charset='utf-8'></head>
        <body style="font-family: sans-serif;">
          <pre style="white-space: pre-wrap;">${content}</pre>
        </body>
        </html>`

      const blob = new Blob([wordHtml], { type: 'application/msword' })
      const url = window.URL.createObjectURL(blob)
      const link = document.createElement('a')
      link.href = url; link.setAttribute('download', `[最新]_${doc.name}.doc`)
      document.body.appendChild(link); link.click(); document.body.removeChild(link)
      window.URL.revokeObjectURL(url); ElMessage.success('下载完成')
    } catch (e) { ElMessage.error('获取最新内容失败') }
    finally { loading.value = false }
  }
}

// 一键清理文档
const clearDirtyData = () => {
  ElMessageBox.confirm('这会强行清空列表里所有的文档记录，确定吗？').then(async () => {
    loading.value = true
    for (const doc of docList.value) {
      await docApi.deleteDoc(doc.id).catch(()=>{})
    }
    docList.value = []
    loading.value = false
    ElMessage.success('清理完成')
  })
}

// 跳转编辑器
const goToEditor = (id, name) => {
  sessionStorage.setItem('currentDocName', name)
  router.push(`/editor/${id}`)
}

const handleAiTask = () => {
  if (!aiTask.value.trim()) return
  sessionStorage.setItem('global_ai_task', aiTask.value)
  aiTask.value = ''
  router.push('/editor/chat-mode')
}

const mockSkill = (name) => {
  aiTask.value = name
  handleAiTask()
}

const handleUserCommand = (cmd) => {
  if (cmd === 'logout') { localStorage.clear(); router.push('/login'); }
}

const goToAIOps = () => {
  router.push('/aiops')
}
</script>

<style scoped>
.feishu-layout { height: 100vh; background-color: #f5f6f7; }
.full-height { height: 100%; }

/* 侧边栏样式 */
.feishu-aside { background: #fff; border-right: 1px solid #dee0e3; display: flex; flex-direction: column; justify-content: space-between; }
.aside-top { padding: 24px 16px; }
.brand { display: flex; align-items: center; gap: 12px; margin-bottom: 20px; font-weight: bold; font-size: 18px; padding: 0 8px; }
.logo-box { width: 32px; height: 32px; background: linear-gradient(135deg, #3370ff, #5c8dff); border-radius: 8px; display: flex; align-items: center; justify-content: center; color: #fff; font-size: 20px;}
.nav-item { padding: 12px 16px; border-radius: 8px; cursor: pointer; display: flex; align-items: center; gap: 10px; font-size: 14px; margin-bottom: 4px; color: #444; }
.nav-item:hover { background: #f2f3f5; }
.nav-item.active { background: #eef3ff; color: #3370ff; font-weight: 500; }
.nav-item.aiops-nav { color: #e6a23c; }
.nav-item.aiops-nav:hover { background: #fdf6ec; color: #e6a23c; }

.aside-bottom { padding: 16px; border-top: 1px solid #dee0e3; }
.user-profile { display: flex; align-items: center; gap: 10px; padding: 8px; cursor: pointer; border-radius: 8px; transition: 0.2s; }
.user-profile:hover { background: #f2f3f5; }
.username { font-size: 14px; font-weight: 500; color: #1f2329;}

/* AI 欢迎区 */
.feishu-main { padding: 0; display: flex; justify-content: center; overflow-y: auto; }
.main-container { width: 100%; max-width: 1050px; padding: 40px 32px; }
.ai-hero { display: flex; flex-direction: column; align-items: center; margin-bottom: 50px; padding-top: 40px;}
.hero-title { font-size: 26px; color: #1f2329; margin-bottom: 36px; font-weight: 600; letter-spacing: 0.5px;}

/* 飞书风巨大输入框 */
.hero-search {
  width: 100%; max-width: 700px; height: 64px; background: #fff; border-radius: 32px;
  box-shadow: 0 6px 20px rgba(31, 35, 41, 0.08); display: flex; align-items: center; padding: 0 12px 0 24px;
  border: 1px solid transparent; transition: 0.3s; cursor: text;
}
.hero-search:focus-within { border-color: #3370ff; box-shadow: 0 8px 32px rgba(51,112,255,0.15); }
.prefix-icon { font-size: 22px; color: #8f959e; margin-right: 12px; }
.hero-input { flex: 1; border: none; outline: none; font-size: 16px; color: #1f2329; background: transparent; }
.send-action-btn { width: 40px; height: 40px; border-radius: 50%; background: #3370ff; color: white; display: flex; justify-content: center; align-items: center; cursor: pointer; }

/* 菜单项样式 */
.menu-item { padding: 12px; border-radius: 6px; cursor: pointer; display: flex; align-items: center; gap: 10px; font-size: 14px; transition: 0.2s; }
.menu-item:hover { background: #f2f3f5; }

/* 快捷胶囊 */
.quick-prompts { display: flex; gap: 12px; margin-top: 24px; justify-content: center; }
.prompt-pill { padding: 8px 16px; background: #fff; border: 1px solid #dee0e3; border-radius: 20px; font-size: 13px; font-weight: 500; cursor: pointer; }
.prompt-pill:hover { border-color: #3370ff; color: #3370ff; }

/* 文档卡片区 */
.list-section { margin-top: 40px; }
.section-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 24px; padding-bottom: 12px; border-bottom: 1px solid #ebeef5;}
.section-title { font-size: 18px; font-weight: 600; color: #1f2329;}

.card-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(240px, 1fr)); gap: 24px; }
.doc-card { background: #fff; border-radius: 12px; border: 1px solid #ebeef5; overflow: hidden; transition: 0.3s; }
.doc-card:hover { transform: translateY(-4px); box-shadow: 0 12px 24px rgba(31, 35, 41, 0.08); }
.card-cover { height: 130px; display: flex; align-items: center; justify-content: center; position: relative; }
.card-info { padding: 12px 16px; }
.info-top { display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px; }
.doc-name { font-weight: 500; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; color: #1f2329; cursor: pointer; flex: 1; }
.more-btn { color: #8f959e; cursor: pointer; font-size: 18px; }

.bg-blue { background: linear-gradient(135deg, #5182ff, #a0cfff); }
.bg-orange { background: linear-gradient(135deg, #edab56, #f3d19e); }
.bg-green { background: linear-gradient(135deg, #7bcf52, #b3e19d); }
.bg-purple { background: linear-gradient(135deg, #9b72f7, #c0a8f9); }
</style>
<style>
.feishu-popover { padding: 8px !important; border-radius: 12px !important; box-shadow: 0 8px 24px rgba(31, 35, 41, 0.12) !important; }
</style>
