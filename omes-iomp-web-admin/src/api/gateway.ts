import { get, post, postPage } from '@/api/request'
import type { IdsPayload } from '@/types/api'

export interface ProtocolOption {
  id: string
  name: string
}

export interface GatewayRecord {
  id?: string
  serverName?: string
  uri?: string
  topic?: string
  protocol?: string
  params?: string | Record<string, unknown>
  enabled?: boolean
  /** up 已连通，down 连不上，pending 已启动还没有第一次通信结果 */
  linkState?: 'up' | 'down' | 'pending'
  linkMessage?: string
  collectCron?: string
  username?: string
  password?: string
  validType?: number
}

export interface GatewayPageQuery {
  requirePage?: boolean
  protocol?: string
  serverName?: string
  enabled?: boolean
}

export function fetchGatewayProtocols() {
  return get<ProtocolOption[]>('/gateway/protocols')
}

export async function fetchGatewayList(query: GatewayPageQuery = {}) {
  const result = await postPage<GatewayRecord>('/gateway/selectByPage', {
    requirePage: false,
    page: 1,
    pageSize: 10,
    ...query,
  })
  return result?.records ?? []
}

export function fetchGatewayById(id: string) {
  return get<GatewayRecord>('/gateway/selectById', { id })
}

export function saveGateway(data: GatewayRecord) {
  return post<boolean>('/gateway/addOrUpdate', data)
}

export function deleteGateways(ids: string[]) {
  return post<boolean>('/gateway/delete', { ids } satisfies IdsPayload)
}

export function startGateway(id: string) {
  return get<boolean>('/gateway/start', { id })
}

export function stopGateway(id: string) {
  return get<boolean>('/gateway/stop', { id })
}

export function normalizeProtocol(protocol?: string): string {
  return (protocol || '').trim().toLowerCase()
}

export function isMqttProtocol(protocol?: string): boolean {
  const p = normalizeProtocol(protocol)
  return p === 'mqtt' || p === 'tc_mqtt'
}

export function isDgwProtocol(protocol?: string): boolean {
  return normalizeProtocol(protocol) === 'dgw'
}

export function needsGatewayAuth(protocol?: string): boolean {
  const p = normalizeProtocol(protocol)
  return isMqttProtocol(protocol) || p === 'rest' || p === 'wincc' || p === 'dgw'
}
