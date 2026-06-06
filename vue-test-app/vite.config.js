import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

const apiTarget = process.env.VITE_API_TARGET || 'http://localhost:8080'
const aiTarget = process.env.VITE_AI_TARGET || 'http://localhost:8083'

export default defineConfig({
  plugins: [vue()],
  server: {
    port: 5173,
    proxy: {
      // 拦截 AI 核心接口
      '/api/ai': {
        target: aiTarget,
        changeOrigin: true, // 建议设为 true，方便本地和服务器间的主机头切换
      },

      // 拦截 AI 技能接口
      '/api/skills': {
        target: aiTarget,
        changeOrigin: true,
      },

      '/api': {
        target: apiTarget,
        changeOrigin: false,
        configure: (proxy) => {
          proxy.on('proxyReq', (proxyReq) => {
            proxyReq.removeHeader('Origin')
            proxyReq.setHeader('Origin', 'http://localhost:5173')
            proxyReq.setHeader('Connection', 'keep-alive')
          })
          proxy.on('error', (err) => {
            console.log('[proxy error]', err.message)
          })
        }
      }
    }
  }
})
