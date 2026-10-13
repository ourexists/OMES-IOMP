import { fileURLToPath, URL } from 'node:url'
import { defineConfig, loadEnv } from 'vite'
import vue from '@vitejs/plugin-vue'
/** 与 OMES-SAS 转发上游一致的业务路径前缀（开发态相对路径兜底代理到网关） */
const GATEWAY_PROXY_PREFIXES = [
  '/oauth2',
  '/open',
  '/authentication',
  '/expose',
  '/acc',
  '/role',
  '/platform',
  '/permission',
  '/depart',
  '/tenant',
  '/workshop',
  '/equip',
  '/equipHealth',
  '/equipStateSnapshot',
  '/equipRecordOnline',
  '/equipRecordRun',
  '/equipRecordAlarm',
  '/notify',
  '/message',
  '/product',
  '/productModel',
  '/gateway',
  '/device',
  '/devg',
  '/line',
  '/tf',
  '/tfEdge',
  '/plc',
  '/localFile',
  '/files',
  '/static',
  '/inspection',
  '/task',
  '/sync',
  '/systemConfig',
  '/mo',
  '/mps',
  '/flow',
  '/mat',
  '/mc',
  '/BOM',
  '/BOMC',
  '/processes',
]

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
      proxy: Object.fromEntries(
        GATEWAY_PROXY_PREFIXES.map((prefix) => [
          prefix,
          { target: proxyTarget, changeOrigin: true, ws: true },
        ]),
      ),
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
