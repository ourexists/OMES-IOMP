/**
 * WuKongIM 同源入口。生产经 Nginx `/wkim/` 反代到 :5001；开发经 Vite 同路径代理。
 * 客服业务仍走 Admin `/im/**`（再经 `/api`）；本模块仅用于直连 WuKongIM HTTP。
 */

const WKIM_PREFIX = '/wkim'

/** 去掉尾斜杠；空串表示走同源 `/wkim` */
export function resolveWkimBaseUrl(): string {
  const fromEnv = import.meta.env.VITE_WKIM_BASE_URL?.trim()
  if (fromEnv) {
    return fromEnv.replace(/\/$/, '')
  }
  // 未注入时：生产 / Docker Nginx / Vite 穿透均走同源前缀
  return WKIM_PREFIX
}

/**
 * 将 WuKongIM 相对路径拼到 `/wkim`（或绝对基址）上。
 * 例：wkimApiPath('/health') → `/wkim/health` 或 `http://127.0.0.1:5001/health`
 */
export function wkimApiPath(path: string): string {
  if (!path || /^https?:\/\//i.test(path)) {
    return path
  }
  const normalized = path.startsWith('/') ? path : `/${path}`
  const base = resolveWkimBaseUrl()
  if (!base || base === WKIM_PREFIX) {
    return normalized === WKIM_PREFIX || normalized.startsWith(`${WKIM_PREFIX}/`)
      ? normalized
      : `${WKIM_PREFIX}${normalized}`
  }
  return `${base}${normalized}`
}
