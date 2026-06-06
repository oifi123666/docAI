<template>
  <!-- 这是一个空壳页面，专门给 PPT 预览用 -->
  <div v-loading="!hasContent"></div>
</template>

<script setup>
import { onMounted, ref } from 'vue'

const hasContent = ref(false)

onMounted(() => {
  // 从 sessionStorage 拿到之前存好的PPT 源码
  const html = sessionStorage.getItem('temp_ppt_render_source')
  if (html) {
    hasContent.value = true
    // 直接把源码覆盖整个当前页面
    document.open()
    document.write(html)
    document.close()
  } else {
    window.close() // 没内容就关掉
  }
})
</script>