import { API_ROUTES } from '@/config'
import { setBaiduMapRuntimeAk } from '@/config/baidu-map'
import {
  defaultGatewayBaseUrl,
  gatewayApiPath,
  getGatewayPort,
  getRuntimeSasBaseUrl,
  resolveGatewayBaseUrl,
  setRuntimeSasBaseUrl,
} from '@/config/gateway'

export interface FrontendConfig {
  sasBaseUrl: string
  gatewayPort: number
  viaGateway: boolean
  baiduMapAk?: string
}

export { gatewayApiPath, getGatewayPort as resolveGatewayPort }

export function getSasBaseUrl(): string {
  return getRuntimeSasBaseUrl()
}

function applyGatewayBase(cfg: Partial<FrontendConfig> = {}): FrontendConfig {
  const gatewayPort = cfg.gatewayPort ?? getGatewayPort()
  let sasBaseUrl = resolveGatewayBaseUrl({ ...cfg, gatewayPort })
  // SAS 空基址：穿透访问时保持同源走 Vite proxy；本机开发回退到网关绝对地址
  if (!sasBaseUrl && import.meta.env.DEV) {
    sasBaseUrl = defaultGatewayBaseUrl()
  }
  setRuntimeSasBaseUrl(sasBaseUrl)
  return { sasBaseUrl, gatewayPort, viaGateway: true }
}

export async function loadFrontendConfig(): Promise<FrontendConfig> {
  const gatewayPort = getGatewayPort()
  const apiBase = defaultGatewayBaseUrl()
  // 开发优先走绝对网关地址；生产可用同源相对路径（Docker Nginx 反代）
  const configUrl = gatewayApiPath(API_ROUTES.frontendConfig)

  try {
    const response = await fetch(configUrl)
    if (response.ok) {
      const cfg = (await response.json()) as Partial<FrontendConfig>
      const merged: Partial<FrontendConfig> = {
        gatewayPort: cfg.gatewayPort ?? gatewayPort,
        sasBaseUrl: cfg.sasBaseUrl ?? '',
        baiduMapAk: cfg.baiduMapAk,
      }
      if (merged.baiduMapAk?.trim()) {
        setBaiduMapRuntimeAk(merged.baiduMapAk)
      }
      // 开发环境：忽略服务端空 sasBaseUrl，避免请求打到 Vite :5173
      if (import.meta.env.DEV && !merged.sasBaseUrl?.trim() && apiBase) {
        merged.sasBaseUrl = apiBase
      } else if (cfg.sasBaseUrl === undefined && apiBase) {
        merged.sasBaseUrl = apiBase
      }
      return applyGatewayBase(merged)
    }
  } catch {
    // ignore
  }

  return applyGatewayBase({
    gatewayPort,
    sasBaseUrl: apiBase || '',
  })
}
