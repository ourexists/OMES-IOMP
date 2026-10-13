import { get, post, postPage } from '@/api/request'
import type { AssignPayload, BindAccPayload, IdsPayload } from '@/types/api'
import type { PermissionNode } from '@/types/permission'

export interface PlatformNode {
  id: string
  code: string
  name: string
  children?: PlatformNode[]
}

export interface AccountRecord {
  id: string
  accName?: string
  nickName?: string
  userName?: string
  mobile?: string
  email?: string
  status?: number
  statusDesc?: string
  settledTime?: string
  expireTime?: string
  source?: string
  sex?: number
  idCard?: string
  platform?: string
  selfCode?: string
  password?: string
}

export interface RoleRecord {
  id: string
  code: string
  name: string
  description?: string
  selfCode?: string
}

export interface PermissionRecord extends PermissionNode {
  typeEnum?: string
  strategyEnum?: string
  selfCode?: string
}

export function fetchPlatforms() {
  return get<PlatformNode[]>('/platform/getAll')
}

export function savePlatform(data: PlatformNode) {
  return post<boolean>('/platform/addOrUpdate', data)
}

export function fetchAccountPage(params: Record<string, unknown>) {
  return postPage<AccountRecord>('/acc/selectByPage', params)
}

export function registerAccount(data: AccountRecord) {
  return post<boolean>('/acc/register', data)
}

export function modifyAccount(data: AccountRecord) {
  return post<boolean>('/acc/modify', data)
}

export function deleteAccounts(ids: string[]) {
  return post<boolean>('/acc/delete', { ids } satisfies IdsPayload)
}

export function invokeAccount(accId: string) {
  return get<boolean>('/acc/invoke', { accId })
}

export function frozenAccount(accId: string) {
  return get<boolean>('/acc/frozen', { accId })
}

export function fetchRolePage(params: Record<string, unknown>) {
  return postPage<RoleRecord>('/role/selectByPage', params)
}

export async function fetchAllRoles() {
  const result = await postPage<RoleRecord>('/role/selectByPage', { requirePage: false })
  return result.records || []
}

export function saveRole(data: RoleRecord) {
  return post<boolean>('/role/addOrUpdate', data)
}

export function deleteRoles(ids: string[]) {
  return post<boolean>('/role/delete', { ids } satisfies IdsPayload)
}

export function fetchRolePermissions(roleId: string) {
  return get<PermissionRecord[]>('/permission/selectRolePermission', { roleId })
}

export function fetchRolesByAccount(accId: string) {
  return get<RoleRecord[]>('/role/selectRoleWhichAccHoldOnly', { accId })
}

export function bindAccountRoles(payload: BindAccPayload) {
  return post<boolean>('/role/bindAcc', payload)
}

export function fetchPermissionTreeInPlatform(platform: string) {
  return get<PermissionRecord[]>('/permission/selectPermissionTreeInPlatform', { platform })
}

export function fetchTenantPermissionTree(tenantId: number | string, platform: string) {
  return get<PermissionRecord[]>('/permission/selectTenantPermissionTreeInPlatform', {
    tenantId,
    platform,
  })
}

export function fetchPermissionTypes() {
  return get<Record<string, string>>('/permission/permissionType')
}

export function fetchPermissionStrategies() {
  return get<Record<string, string>>('/permission/permissionStrategy')
}

export function addPermission(data: PermissionRecord) {
  return post<boolean>('/permission/add', data)
}

export function modifyPermission(data: PermissionRecord) {
  return post<boolean>('/permission/modify', data)
}

export function deletePermission(id: string) {
  return get<boolean>('/permission/delete', { id })
}

export function assignRolePermissions(payload: AssignPayload) {
  return post<boolean>('/permission/assignToRolePermissionTree', payload)
}

import { mapOptions } from '@/utils/options'

export { mapOptions }
