import { get, post, postPage } from '@/api/request'
import { filterEquipWithCoords } from '@/utils/equip-gis'
import type { IdsPayload } from '@/types/api'
import type { EquipGwBinding } from '@/types/equip-config'

export interface WorkshopNode {
  id: string
  name: string
  selfCode: string
  code?: string
  pcode?: string
  lng?: string | number | null
  lat?: string | number | null
  address?: string | null
  children?: WorkshopNode[]
}

export interface EquipAttrItem {
  name?: string
  value?: unknown
  unit?: string
  map?: string
}

export interface EquipControlItem {
  name?: string
  map?: string
  type?: number
  unit?: string
  min?: string | number
  max?: string | number
  value?: unknown
}

export interface EquipRecord {
  id: string
  name?: string
  selfCode?: string
  type?: string | number
  typeDesc?: string
  /** 所属产品图片，分页/详情由后端按产品回填 */
  productImage?: string
  modelId?: string
  modelName?: string
  enableDate?: string
  createTime?: string
  workshopCode?: string
  healthTemplateId?: string | null
  workshop?: {
    name?: string
    selfCode?: string
    lng?: string | number | null
    lat?: string | number | null
    address?: string | null
  }
  config?: EquipGwBinding
  onlineState?: number
  runState?: number
  alarmState?: number
  onlineChangeTime?: string
  runChangeTime?: string
  alarmChangeTime?: string
  alarmTexts?: string[]
  attrs?: EquipAttrItem[]
  controls?: EquipControlItem[]
}

export interface EquipRealtimeQuery {
  workshopCode?: string | null
  name?: string
  selfCode?: string
  type?: string | number
}

export interface ProductOption {
  id?: string
  code?: string
  name?: string
}

export type { HealthRuleTemplate as HealthTemplate } from '@/api/equip-health'

export function fetchWorkshopTree() {
  return get<WorkshopNode[]>('/workshop/selectTree')
}

export function saveWorkshop(data: Partial<WorkshopNode>) {
  return post<boolean>('/workshop/addOrUpdate', data)
}

export function deleteWorkshops(ids: string[]) {
  return post<boolean>('/workshop/delete', { ids } satisfies IdsPayload)
}

export function fetchEquipPage(params: Record<string, unknown>) {
  return postPage<EquipRecord>('/equip/selectByPage', params)
}

export async function fetchEquipRealtimeList(query: EquipRealtimeQuery = {}) {
  const result = await postPage<EquipRecord>('/equip/selectByPage', {
    page: 1,
    pageSize: 10,
    requirePage: false,
    needRealtime: true,
    needWorkshopCascade: true,
    queryWorkshop: true,
    queryAttrs: true,
    ...query,
  })
  return result?.records ?? []
}

/** GIS 地图：拉取全部匹配设备（非分页），仅返回所属末级场景已标注经纬度的记录 */
export async function fetchEquipGisList(query: EquipRealtimeQuery = {}) {
  const result = await postPage<EquipRecord>('/equip/selectByPage', {
    requirePage: false,
    needRealtime: true,
    needWorkshopCascade: true,
    queryWorkshop: true,
    queryAttrs: true,
    ...query,
  })
  return filterEquipWithCoords(result?.records ?? [])
}

export function fetchEquipById(id: string) {
  return get<EquipRecord>('/equip/selectById', { id })
}

export function saveEquip(data: EquipRecord) {
  return post<boolean>('/equip/addOrUpdate', data)
}

export function deleteEquips(ids: string[]) {
  return post<boolean>('/equip/delete', { ids } satisfies IdsPayload)
}

export function fetchEquipTypes() {
  return get<Record<string, string>>('/equip/equipType')
}

export function fetchProductListAll() {
  return get<ProductOption[]>('/product/listAll')
}

export { fetchHealthRuleTemplates as fetchHealthTemplates } from '@/api/equip-health'

export function fetchEquipAlarmLevels() {
  return get<{ id: string; name: string }[]>('/equip/alarmLevels')
}

export function fetchEquipConfig(equipId: string) {
  return get<EquipGwBinding>('/equip/queryEquipConfig', { equipId })
}

export function saveEquipConfig(payload: EquipGwBinding) {
  return post<boolean>('/equip/setEquipConfig', payload)
}

export function fetchEquipRealtimeById(id: string) {
  return get<EquipRecord>('/equip/selectRealtimeById', { id })
}
