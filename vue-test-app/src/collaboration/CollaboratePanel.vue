<template>
  <div class="collaborate-panel" v-if="visible">
    <div class="panel-header" @click="collapsed = !collapsed">
      <div class="header-left">
        <el-icon><Connection /></el-icon>
        <span class="header-title">协作编辑</span>
        <el-tag :type="statusTagType" size="small" effect="dark" round>
          {{ statusText }}
        </el-tag>
      </div>
      <div class="header-right">
        <el-button
          v-if="!isCollaborating"
          type="primary"
          size="small"
          round
          :loading="isConnecting"
          @click.stop="handleStart"
        >
          加入协作
        </el-button>
        <el-button
          v-else
          type="danger"
          size="small"
          round
          plain
          @click.stop="handleStop"
        >
          退出协作
        </el-button>
        <el-icon class="collapse-icon" :class="{ rotated: collapsed }"><ArrowDown /></el-icon>
      </div>
    </div>

    <div class="panel-body" v-show="!collapsed">
      <div class="sync-info" v-if="isCollaborating">
        <div class="info-row">
          <el-icon><User /></el-icon>
          <span>{{ userName }} (我)</span>
        </div>
        <div class="info-row">
          <el-icon><Clock /></el-icon>
          <span>上次同步：{{ lastSyncText }}</span>
        </div>
        <div class="info-row">
          <el-icon><Refresh /></el-icon>
          <span>每 2 秒自动拉取远程更新</span>
        </div>
        <div class="info-row">
          <el-icon><Upload /></el-icon>
          <span>输入后 1.5 秒自动保存</span>
        </div>
      </div>
      <div class="no-collab" v-else-if="isConnected">
        <span>点击"加入协作"开始实时同步</span>
      </div>
      <div class="connection-error" v-if="connectionError">
        <el-icon><WarningFilled /></el-icon>
        <span>{{ connectionError }}</span>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onUnmounted } from 'vue'
import { useCollaboration } from './useCollaboration'

const props = defineProps({
  documentId: { type: String, required: true },
  userId: { type: String, default: '1' },
  userName: { type: String, default: 'User' }
})

const visible = ref(true)
const collapsed = ref(false)
const isConnecting = ref(false)

const {
  isConnected,
  isCollaborating,
  connectionError,
  lastSyncTime,
  startCollaboration,
  stopCollaboration
} = useCollaboration(props.documentId, props.userId, props.userName)

const statusTagType = computed(() => {
  if (!isConnected.value) return 'info'
  if (isCollaborating.value) return 'success'
  return 'warning'
})

const statusText = computed(() => {
  if (isCollaborating.value) return '协作中'
  if (isConnected.value) return '已就绪'
  return '未连接'
})

const lastSyncText = computed(() => {
  if (!lastSyncTime.value) return '暂无'
  const diff = Math.floor((Date.now() - lastSyncTime.value) / 1000)
  if (diff < 5) return '刚刚'
  if (diff < 60) return `${diff} 秒前`
  return `${Math.floor(diff / 60)} 分钟前`
})

async function handleStart() {
  isConnecting.value = true
  try {
    await startCollaboration()
  } finally {
    isConnecting.value = false
  }
}

function handleStop() {
  stopCollaboration()
}

onUnmounted(() => {
  stopCollaboration()
})
</script>

<style scoped>
.collaborate-panel {
  position: fixed;
  bottom: 24px;
  left: 24px;
  z-index: 9998;
  width: 280px;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.12);
  border: 1px solid #ebeef5;
  overflow: hidden;
  transition: all 0.3s;
}

.panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
  cursor: pointer;
  user-select: none;
  background: #fafbfc;
  border-bottom: 1px solid #f0f0f0;
}

.panel-header:hover {
  background: #f0f2f5;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 8px;
}

.header-title {
  font-size: 14px;
  font-weight: 600;
  color: #1f2329;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 8px;
}

.collapse-icon {
  transition: transform 0.3s;
  color: #8f959e;
}

.collapse-icon.rotated {
  transform: rotate(180deg);
}

.panel-body {
  padding: 12px 16px;
}

.sync-info {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.info-row {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  color: #646a73;
  padding: 4px 0;
}

.info-row .el-icon {
  color: #3370ff;
  font-size: 14px;
}

.no-collab {
  text-align: center;
  color: #8f959e;
  font-size: 13px;
  padding: 12px 0;
}

.connection-error {
  display: flex;
  align-items: center;
  gap: 6px;
  color: #f56c6c;
  font-size: 12px;
  padding: 8px 0;
}
</style>