import { fileURLToPath, URL } from 'node:url'
import { defineConfig, loadEnv } from 'vite'
import vue from '@vitejs/plugin-vue'
export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '')
  const gatewayPort = env.VITE_GATEWAY_PORT || '9400'
  const proxyTarget = env.VITE_SAS_BASE_URL?.trim() || `http://127.0.0.1:${gatewayPort}`

  return {
    base: '/',
    plugins: [vue()],
    resolve: {
      alias: {
        '@': fileURLToPath(new URL('./src', import.meta.url)),
        // 与 Vue 同包产物，避免 Rolldown 拆 chunk 后丢失 init_* 运行时
        'vue-i18n': 'vue-i18n/dist/vue-i18n.runtime.esm-bundler.js',
      },
      dedupe: ['vue', 'vue-i18n'],
    },
    // Vite 8 Rolldown dep optimizer may drop init_* imports for vue-i18n prebundle.
    optimizeDeps: {
      exclude: ['vue-i18n'],
    },
    server: {
      host: true,
      port: 5173,
      // 内网穿透域名访问 Vite 时放行 Host 检查
      allowedHosts: true,
      proxy: {
        '/api': {
          target: proxyTarget,
          changeOrigin: true,
          ws: true,
          rewrite: (path) => path.replace(/^\/api/, ''),
          configure: (proxy) => {
            proxy.on('proxyReq', (proxyReq) => {
              proxyReq.removeHeader('origin')
            })
          },
        },
        // 与生产 Nginx /wkim/ → WuKongIM :5001 对齐
        '/wkim': {
          target: env.VITE_WKIM_PROXY_TARGET?.trim() || 'http://127.0.0.1:5001',
          changeOrigin: true,
          rewrite: (path) => path.replace(/^\/wkim/, ''),
        },
      },
    },
    build: {
      emptyOutDir: true,
      // Vue / vue-i18n 必须同 chunk，否则生产环境白屏：
      // ReferenceError: init_runtime_dom_esm_bundler is not defined
      rollupOptions: {
        output: {
          manualChunks(id) {
            if (
              /node_modules[\\/](?:vue|@vue|vue-router|vue-i18n|pinia)[\\/]/.test(id)
            ) {
              return 'vue-vendor'
            }
          },
        },
      },
    },
  }
})
