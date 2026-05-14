/**
 * @description Axios 全局请求封装工具
 */
import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '../router'
import { STORAGE_KEYS, RES_CODE } from '../constants'

const service = axios.create({
    baseURL: '/api',
    timeout: 300000
})

const AI_PATH_PREFIX = '/ai/'
const AIOPS_PATH_PREFIX = '/ai/aiops/'

function isAiRequest(url) {
  return url && url.startsWith(AI_PATH_PREFIX) && !url.startsWith(AIOPS_PATH_PREFIX)
}

function reportAiMetrics(config, hasError) {
  const url = config.url || ''
  if (!isAiRequest(url)) return

  const startTime = config._startTime
  const duration = startTime ? Date.now() - startTime : 0

  const baseURL = config.baseURL || '/api'

  axios.post(baseURL + '/ai/aiops/metrics/counter', null, {
    params: { name: 'ai.requests', delta: 1 }
  }).catch(() => {})

  if (duration > 0) {
    axios.post(baseURL + '/ai/aiops/metrics/timer', null, {
      params: { name: 'ai.request', duration }
    }).catch(() => {})
  }

  if (hasError) {
    axios.post(baseURL + '/ai/aiops/metrics/counter', null, {
      params: { name: 'ai.errors', delta: 1 }
    }).catch(() => {})
  }
}

service.interceptors.request.use(
    config => {
        config._startTime = Date.now()
        const token = localStorage.getItem(STORAGE_KEYS.ACCESS_TOKEN)
        if (token) {
            config.headers['Authorization'] = 'Bearer ' + token
        }
        return config
    },
    error => Promise.reject(error)
)

service.interceptors.response.use(
    response => {
        if (response.config.responseType === 'blob' || response.config.responseType === 'arraybuffer') {
            reportAiMetrics(response.config, false)
            return response.data
        }

        const res = response.data

        if (res && res.code === undefined) {
            reportAiMetrics(response.config, false)
            return { data: res, code: 200, message: 'success' }
        }

        if (res.code === 200 || res.code === 0) {
            reportAiMetrics(response.config, false)
            return res
        } else {
            reportAiMetrics(response.config, true)
            ElMessage.error(res.message || '操作失败')
            return Promise.reject(new Error(res.message || 'Error'))
        }
    },
    error => {
        if (error.config) {
            reportAiMetrics(error.config, true)
        }
        if (error.response && error.response.status === 401) {
            ElMessage.error('登录已过期，请重新登录')
            localStorage.removeItem('accessToken')
            router.push('/login')
        } else {
            ElMessage.error('服务器开了小差，请稍后再试')
        }
        return Promise.reject(error)
    }
)

export default service