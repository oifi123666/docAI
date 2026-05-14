<template>
  <div class="aiops-container">
    <div class="aiops-header">
      <h1>
        <el-icon :size="28"><Monitor /></el-icon>
        AI Ops 智能运维中心
      </h1>
      <div class="header-actions">
        <el-button type="primary" @click="refreshAll" :loading="loading">
          <el-icon><Refresh /></el-icon> 刷新数据
        </el-button>
        <el-button type="warning" @click="handleResetMetrics">
          <el-icon><Delete /></el-icon> 重置指标
        </el-button>
      </div>
    </div>

    <el-row :gutter="20" class="metrics-row">
      <el-col :span="6">
        <el-card shadow="hover" class="metric-card">
          <div class="metric-label">AI 请求总数</div>
          <div class="metric-value">{{ getMetricValue('ai.requests.count') }}</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="metric-card">
          <div class="metric-label">AI 错误数</div>
          <div class="metric-value error">{{ getMetricValue('ai.errors.count') }}</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="metric-card">
          <div class="metric-label">平均响应时间</div>
          <div class="metric-value">{{ getAvgResponseTime() }} ms</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="metric-card">
          <div class="metric-label">系统健康状态</div>
          <div class="metric-value" :class="healthStatus === 'UP' ? 'success' : 'error'">
            {{ healthStatus }}
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" class="detail-row">
      <el-col :span="12">
        <el-card shadow="hover" class="section-card">
          <template #header>
            <div class="card-header">
              <span><el-icon><DataLine /></el-icon> 监控指标详情</span>
            </div>
          </template>
          <el-table :data="metricsTableData" stripe size="small" empty-text="暂无监控数据">
            <el-table-column prop="name" label="指标名称" />
            <el-table-column prop="value" label="数值" width="120" />
          </el-table>
        </el-card>
      </el-col>

      <el-col :span="12">
        <el-card shadow="hover" class="section-card">
          <template #header>
            <div class="card-header">
              <span><el-icon><WarningFilled /></el-icon> 故障列表</span>
              <el-button type="primary" size="small" text @click="detectFaults">
                <el-icon><Search /></el-icon> 检测故障
              </el-button>
            </div>
          </template>
          <el-table :data="faults" stripe size="small" empty-text="暂无故障记录">
            <el-table-column prop="type" label="故障类型" width="160" />
            <el-table-column prop="message" label="故障描述" show-overflow-tooltip />
            <el-table-column prop="status" label="状态" width="100">
              <template #default="{ row }">
                <el-tag :type="row.status === 'resolved' ? 'success' : 'danger'" size="small">
                  {{ row.status === 'resolved' ? '已处理' : '待处理' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="100">
              <template #default="{ row }">
                <el-button
                  v-if="row.status !== 'resolved'"
                  type="primary"
                  size="small"
                  text
                  @click="resolveFault(row.faultId)"
                >
                  处理
                </el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { aiopsApi } from '../../api/aiops'

const loading = ref(false)
const metrics = ref({})
const healthStatus = ref('UNKNOWN')
const faults = ref([])

const metricsTableData = computed(() => {
  return Object.entries(metrics.value).map(([name, value]) => {
    if (typeof value === 'object') {
      return { name, value: JSON.stringify(value) }
    }
    return { name, value }
  })
})

const getMetricValue = (key) => {
  return metrics.value[key] ?? 0
}

const getAvgResponseTime = () => {
  const stats = metrics.value['ai.request.stats']
  if (stats && stats.avg !== undefined) {
    return Math.round(stats.avg)
  }
  return 0
}

const fetchMetrics = async () => {
  try {
    const res = await aiopsApi.getMetrics()
    metrics.value = res.data || {}
  } catch {
    ElMessage.error('获取监控指标失败')
  }
}

const fetchHealth = async () => {
  try {
    const res = await aiopsApi.healthCheck()
    healthStatus.value = res.data?.status || 'UNKNOWN'
  } catch {
    healthStatus.value = 'DOWN'
  }
}

const fetchFaults = async () => {
  try {
    const res = await aiopsApi.getAllFaults()
    faults.value = res.data || []
  } catch {
    ElMessage.error('获取故障列表失败')
  }
}

const detectFaults = async () => {
  loading.value = true
  try {
    const res = await aiopsApi.detectFaults()
    ElMessage.success(`检测完成，发现 ${res.data?.length || 0} 个故障`)
    await fetchFaults()
  } catch {
    ElMessage.error('故障检测失败')
  } finally {
    loading.value = false
  }
}

const resolveFault = async (faultId) => {
  loading.value = true
  try {
    await aiopsApi.resolveFault(faultId)
    ElMessage.success('故障已处理')
    await fetchFaults()
  } catch {
    ElMessage.error('处理故障失败')
  } finally {
    loading.value = false
  }
}

const handleResetMetrics = () => {
  ElMessageBox.confirm('确定要重置所有监控指标吗？').then(async () => {
    loading.value = true
    try {
      await aiopsApi.resetMetrics()
      ElMessage.success('指标已重置')
      await fetchMetrics()
    } catch {
      ElMessage.error('重置失败')
    } finally {
      loading.value = false
    }
  }).catch(() => {})
}

const refreshAll = async () => {
  loading.value = true
  await Promise.all([fetchMetrics(), fetchHealth(), fetchFaults()])
  loading.value = false
  ElMessage.success('数据已刷新')
}

onMounted(() => {
  refreshAll()
})
</script>

<style scoped>
.aiops-container {
  padding: 24px 32px;
  max-width: 1400px;
  margin: 0 auto;
}

.aiops-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 28px;
}

.aiops-header h1 {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 22px;
  font-weight: 600;
  color: #1f2329;
  margin: 0;
}

.header-actions {
  display: flex;
  gap: 10px;
}

.metrics-row {
  margin-bottom: 20px;
}

.metric-card {
  text-align: center;
  border-radius: 12px;
}

.metric-label {
  font-size: 13px;
  color: #8f959e;
  margin-bottom: 8px;
}

.metric-value {
  font-size: 28px;
  font-weight: 700;
  color: #1f2329;
}

.metric-value.error {
  color: #f56c6c;
}

.metric-value.success {
  color: #67c23a;
}

.detail-row {
  margin-top: 0;
}

.section-card {
  border-radius: 12px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-weight: 600;
  font-size: 15px;
}

.card-header span {
  display: flex;
  align-items: center;
  gap: 6px;
}
</style>