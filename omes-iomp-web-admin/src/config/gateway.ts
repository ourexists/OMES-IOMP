/**
 * 业务 API 统一经 OMES-SAS 网关（默认 :9400）访问，再由网关转发 Admin 等上游。
 */

const GATEWAY_PORT = Number(import.meta.env.VITE_GATEWAY_PORT || 9400)

/** bootstrap 后由 frontend-config 写入；未加载前开发环境使用默认网关地址 */
let runtimeSasBaseUrl: string | undefined

export function getGatewayPort(): number {
  return GATEWAY_PORT
}

export function setRuntimeSasBaseUrl(url: string): void {
  runtimeSasBaseUrl = url.replace(/\/$/, '')
}

export function getRuntimeSasBaseUrl(): string {
  return runtimeSasBaseUrl ?? resolveGatewayBaseUrl()
}

function isLoopbackHost(hostname: string): boolean {
  return hostname === '127.0.0.1' || hostname === 'localhost' || hostname === '[::1]' || hostname === '::1'
}

/** 内网穿透访问 Vite 时：走同源相对路径 + dev proxy，避免跨域/混合内容 */
function useDevProxyBase(): boolean {
  return (
    import.meta.env.DEV &&
    typeof window !== 'undefined' &&
    !isLoopbackHost(window.location.hostname)
  )
}

/** 环境变量或开发默认网关根地址 */
export function defaultGatewayBaseUrl(): string {
  if (useDevProxyBase()) {
    return ''
  }
  const fromEnv = import.meta.env.VITE_SAS_BASE_URL?.trim()
  if (fromEnv) {
    return fromEnv.replace(/\/$/, '')
  }
  if (import.meta.env.DEV) {
    return `http://127.0.0.1:${GATEWAY_PORT}`
  }
  return ''
}

function resolveSasBaseUrl(configured: string): string {
  try {
    const parsed = new URL(configured)
    const pageHost = typeof window !== 'undefined' ? window.location.hostname : ''
    // shortcut: 配置里是本机地址、页面已是穿透/公网域名时，走同源（Docker Nginx / Vite proxy），勿拼 :9400
    if (pageHost && isLoopbackHost(parsed.hostname) && !isLoopbackHost(pageHost)) {
      return ''
    }
  } catch {
    // ignore invalid url
  }
  return configured.replace(/\/$/, '')
}

/**
 * 计算当前页面应使用的网关根地址（不含尾斜杠）。
 * - 构建时注入 VITE_SAS_BASE_URL：前后端分离，API 指向 SAS
 * - 开发本机：默认 http://127.0.0.1:9400；穿透访问 Vite：空串走 dev proxy
 * - 未注入时：空串同源（Docker Nginx 反代）；勿默认拼 host:9400（穿透后会 CORS）
 */
export function resolveGatewayBaseUrl(cfg?: { sasBaseUrl?: string; gatewayPort?: number }): string {
  // 显式传入 sasBaseUrl（含空串）时：空串 = 同源相对路径（Nginx/SAS 同域）
  if (cfg && Object.prototype.hasOwnProperty.call(cfg, 'sasBaseUrl')) {
    const configured = cfg.sasBaseUrl?.trim() ?? ''
    if (!configured) {
      return ''
    }
    return resolveSasBaseUrl(configured)
  }

  const envOrDevBase = defaultGatewayBaseUrl()
  if (envOrDevBase) {
    return envOrDevBase
  }
  return ''
}

/**
 * 将相对 API/静态资源路径解析为经网关访问的完整 URL 或同源路径。
 */
export function gatewayApiPath(path: string): string {
  if (!path || /^https?:\/\//i.test(path)) {
    return path
  }
  const normalized = path.startsWith('/') ? path : `/${path}`

  const base = runtimeSasBaseUrl ?? resolveGatewayBaseUrl()
  if (!base) {
    return normalized
  }
  return `${base}${normalized}`
}
