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

function isSameOriginGatewayPort(gatewayPort: number): boolean {
  if (typeof window === 'undefined') {
    return false
  }
  const port = window.location.port || (window.location.protocol === 'https:' ? '443' : '80')
  return port === String(gatewayPort)
}

function resolveSasBaseUrl(configured: string, gatewayPort: number): string {
  try {
    const parsed = new URL(configured)
    const pageHost = typeof window !== 'undefined' ? window.location.hostname : ''
    if (pageHost && isLoopbackHost(parsed.hostname) && !isLoopbackHost(pageHost)) {
      // shortcut: 仅穿透前端时用 Vite proxy；若同时穿透了 :9400，改设 VITE_SAS_BASE_URL 为该公网地址
      if (import.meta.env.DEV) {
        return ''
      }
      parsed.hostname = pageHost
      parsed.protocol = window.location.protocol
      if (!parsed.port) {
        parsed.port = String(gatewayPort)
      }
      return parsed.toString().replace(/\/$/, '')
    }
  } catch {
    // ignore invalid url
  }
  return configured.replace(/\/$/, '')
}

/**
 * 计算当前页面应使用的网关根地址（不含尾斜杠）。
 * - 构建时注入 VITE_SAS_BASE_URL：前后端分离，API 指向 SAS
 * - 开发：默认 http://127.0.0.1:9400
 * - 生产未注入且页面端口即网关端口：空字符串（同源相对路径，兼容反代同域）
 * - 生产未注入且页面在其它端口：当前 host + 网关端口
 */
export function resolveGatewayBaseUrl(cfg?: { sasBaseUrl?: string; gatewayPort?: number }): string {
  const gatewayPort = cfg?.gatewayPort ?? GATEWAY_PORT

  // 显式传入 sasBaseUrl（含空串）时：空串 = 同源相对路径（Nginx/SAS 同域）
  if (cfg && Object.prototype.hasOwnProperty.call(cfg, 'sasBaseUrl')) {
    const configured = cfg.sasBaseUrl?.trim() ?? ''
    if (!configured) {
      return ''
    }
    return resolveSasBaseUrl(configured, gatewayPort)
  }

  const envOrDevBase = defaultGatewayBaseUrl()
  if (envOrDevBase) {
    return envOrDevBase
  }
  // 穿透访问 Vite：空基址走同源 proxy，不要拼 :9400
  if (useDevProxyBase()) {
    return ''
  }

  if (typeof window !== 'undefined' && !isSameOriginGatewayPort(gatewayPort)) {
    const host = window.location.hostname || '127.0.0.1'
    return `${window.location.protocol}//${host}:${gatewayPort}`
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
