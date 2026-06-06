/**
 * @description AI 核心功能接口 (直连 8083 服务)
 */
import request from '../utils/request'
import { createAsyncJobRunner } from './asyncJob.js'

const wrapResult = (data) => ({ code: 200, message: 'success', data })
const getAsyncJob = (jobId) => request.get(`/ai/async/jobs/${jobId}`)
const runAsyncJob = (submit) => createAsyncJobRunner({
    submit,
    getJob: getAsyncJob,
    timeoutMs: 300000
}).runAsyncJob().then(wrapResult)

export const aiApi = {
    // 统一 Agent 执行入口：规划、工具调用、RAG、审批和最终回答
    executeAgent: (payload) => {
        return runAsyncJob(() => request.post('/ai/async/agent/execute', payload))
    },

    // 仅生成执行计划，不实际调用工具
    planAgentTask: (task, context = {}, options = {}) => {
        return runAsyncJob(() => request.post('/ai/async/agent/execute', {
            task,
            context,
            dryRun: true,
            ...options
        }))
    },

    // 获取 Agent 可调用工具列表
    listAgentTools: () => {
        return request.get('/ai/agent/tools')
    },


    getModels: () => request.get('/ai/models'),

    // 开启一个带有记忆的新对话 (返回 conversationId)
    startChat: (userId) => {
        return request.post(`/ai/agent/chat/start?userId=${userId}`)
    },

    // 发送聊天消息给 AI (带着 conversationId 就能记住上下文)
    sendMessage: (conversationId, userInput) => {
        return runAsyncJob(() => request.post('/ai/async/agent/execute', {
            conversationId,
            task: userInput,
            context: {}
        }))
    },

    // 针对选中文本进行总结/润色 (单次调用，不需要记忆)
    summarizeText: (content, maxLength = 200, model) => {
        return runAsyncJob(() => request.post('/ai/async/summarize', { content, maxLength }, {
            params: { model: model || undefined }
        }))
    },

    // AI 文档分析/纠错接口
    analyzeText: (content) => {
        return request.post('/ai/analyze', { content })
    },

    // 关键词提取
    extractKeywords: (content, count = 5, model) => {
        return runAsyncJob(() => request.post('/ai/async/keywords', { content, count }, {
            params: { model: model || undefined }
        }))
    },

    getAsyncJob,


    // 获取分段策略列表
    getSegmentStrategies: () => request.get('/ai/rag/segment/strategies'),
    // 带有分段策略的文档索引
    indexWithSegment: (documentId, content, strategy) => request.post(`/ai/rag/index/segment?documentId=${documentId}&strategy=${strategy}`, content, { headers: { 'Content-Type': 'text/plain' } }),
    // 获取已分段的列表
    getDocumentSegments: (documentId) => request.get(`/ai/rag/document/${documentId}/segments`),
    // 获取统计信息
    getRagStatistics: () => request.get('/ai/rag/statistics'),

    /**
     * RAG 问答
     * 基于知识库直接搜索并回答，不带历史记忆，追求极致准确
     */
    ragQuery: (question) => {
        // 后端 @RequestBody 是 String，需要发纯文本
        return request.post('/ai/rag/query?strategy=HYBRID', question, {
            headers: { 'Content-Type': 'text/plain' }
        })
    },

// ================= PPT 生成 Skill 接口 =================
    // 下载 PPT
    downloadPpt: (data) => request.post('/skills/html-ppt/download', data, { responseType: 'blob' }),
    // 预览 PPT (返回 HTML 字符串)
    previewPpt: (data) => request.post('/skills/html-ppt/view', data)
}
