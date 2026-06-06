import { reactive, ref } from 'vue'

// 将状态放在模块作用域，实现全局单例效果
const isAiPanelVisible = ref(false)
const chatHistory = ref([])
const isAiThinking = ref(false)
// 从 localStorage 读取，保证刷新页面后对话不丢失
const currentConvId = ref(localStorage.getItem('global_conv_id') || null)

export function useGlobalAi() {
    // 打开/关闭 AI 面板
    const toggleAiPanel = () => {
        isAiPanelVisible.value = !isAiPanelVisible.value
    }

    // 重置对话
    const resetConversation = () => {
        chatHistory.value = []
        currentConvId.value = null
        localStorage.removeItem('global_conv_id')
    }

    // 保存对话 ID
    const saveConvId = (convId) => {
        currentConvId.value = convId
        localStorage.setItem('global_conv_id', convId)
    }

    // 追加消息
    const appendMessage = (msg) => {
        chatHistory.value.push(msg)
    }

    // 清空历史
    const clearChat = () => {
        chatHistory.value = []
    }

    return {
        isAiPanelVisible,
        chatHistory,
        currentConvId,
        isAiThinking,
        toggleAiPanel,
        appendMessage,
        clearChat,
        resetConversation,
        saveConvId

    }
}