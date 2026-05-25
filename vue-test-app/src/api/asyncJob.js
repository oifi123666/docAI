const DEFAULT_POLL_INTERVAL_MS = 1200
const DEFAULT_TIMEOUT_MS = 180000

const delayMs = (ms) => new Promise(resolve => setTimeout(resolve, ms))

function unwrapData(response) {
  return response?.data ?? response
}

function requireJobId(response) {
  const job = unwrapData(response)
  if (!job?.jobId) {
    throw new Error('异步任务提交失败：后端未返回 jobId')
  }
  return job.jobId
}

function isRunning(status) {
  return ['queued', 'running', 'processing', 'pending'].includes(String(status || '').toLowerCase())
}

export function createAsyncJobRunner({
  submit,
  getJob,
  delay = delayMs,
  pollIntervalMs = DEFAULT_POLL_INTERVAL_MS,
  timeoutMs = DEFAULT_TIMEOUT_MS
}) {
  return {
    async runAsyncJob(...args) {
      const jobId = requireJobId(await submit(...args))
      const startedAt = Date.now()

      while (Date.now() - startedAt <= timeoutMs) {
        const job = unwrapData(await getJob(jobId))
        const status = String(job?.status || '').toLowerCase()

        if (status === 'success') {
          return job.result
        }

        if (status === 'failed' || status === 'error') {
          throw new Error(job?.error || '异步任务执行失败')
        }

        if (!isRunning(status)) {
          throw new Error(`未知异步任务状态：${job?.status || 'empty'}`)
        }

        await delay(pollIntervalMs)
      }

      throw new Error('异步任务查询超时')
    }
  }
}
