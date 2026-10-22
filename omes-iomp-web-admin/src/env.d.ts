/// <reference types="vite/client" />

interface ImportMetaEnv {
  readonly VITE_APP_TITLE: string
  readonly VITE_GATEWAY_PORT: string
  readonly VITE_SAS_BASE_URL?: string
  /** WuKongIM 基址，生产默认 /wkim（Nginx 反代） */
  readonly VITE_WKIM_BASE_URL?: string
  /** 仅开发：Vite 将 /wkim 代理到该地址，默认 http://127.0.0.1:5001 */
  readonly VITE_WKIM_PROXY_TARGET?: string
  readonly VITE_BAIDU_MAP_AK?: string
  readonly VITE_ENABLE_REMOTE_I18N?: string
}

interface ImportMeta {
  readonly env: ImportMetaEnv
}

export {}
