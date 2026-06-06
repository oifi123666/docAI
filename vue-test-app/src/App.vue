<template>
  <router-view />

  <GlobalAiAssistant v-if="showAiAssistant" />
</template>

<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import GlobalAiAssistant from './views/components/GlobalAiAssistant.vue'

const route = useRoute()

// 判断当前是否应该显示 AI 助理
const showAiAssistant = computed(() => {
  // 如果路由还没加载好，先不显示
  if (!route.path) return false

  // 定义不需要显示 AI 的黑名单路径
  const hiddenPaths = ['/login', '/register', '/auth', '/editor']

  // 如果当前路径包含在黑名单里，就不显示
  const isHidden = hiddenPaths.some(path => route.path.startsWith(path))

  return !isHidden
})
</script>

<style>
</style>