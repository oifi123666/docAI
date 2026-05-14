import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig({
  plugins: [vue()],
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://${DOC_AI_MANAGER_HOST}:8080',
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