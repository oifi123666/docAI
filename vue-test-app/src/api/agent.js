import request from '../utils/request'
import { createAsyncJobRunner } from './asyncJob.js'

const wrapResult = (data) => ({ code: 200, message: 'success', data })
const getAsyncJob = (jobId) => request.get(`/ai/async/jobs/${jobId}`)
const runAsyncAgentJob = (data) => createAsyncJobRunner({
    submit: () => request.post('/ai/async/agent/execute', data),
    getJob: getAsyncJob,
    timeoutMs: 300000
}).runAsyncJob().then(wrapResult)

export const agentApi = {
    // 获取工作台总览（包含工具列表、任务数等）
    getOverview: () => request.get('/ai/agent/workbench/overview'),

    // 获取工具 Schema 列表
    getTools: () => request.get('/ai/agent/tools'),

    // 执行 Agent 任务（核心交互）
    executeTask: (data) => runAsyncAgentJob(data),

    executeTaskSync: (data) => request.post('/ai/agent/execute', data),

    // 查询单个任务快照
    getTaskHistory: (traceId) => request.get(`/ai/agent/tasks/${traceId}`),

    // 审批危险操作：确认继续
    confirmApproval: (data) => request.post('/ai/agent/approvals/confirm', data),

    // 审批危险操作：取消
    cancelApproval: (token) => request.post(`/ai/agent/approvals/${token}/cancel`),

    // 列出任务历史
    listTasks: () => request.get('/ai/agent/tasks')
}
