/**
 * 已由 omes-web-admin 接管的 /view 路径（与 view-map VIEW_COMPONENTS 保持同步）。
 * 供 Vite 开发代理判断：整页 /view 请求应返回 SPA index.html（生产环境由 omes-runner-sas 处理）。
 */
export const VUE_SPA_VIEW_PATHS = [
  '/view/overview',
  '/view/account_tables',
  '/view/role_tables',
  '/view/permission_tables',
  '/view/workshop_tables',
  '/view/workshop_collect_tables',
  '/view/workshop_scada',
  '/view/workshop_meta2d_view',
  '/view/equip_tables',
  '/view/equipattr_form_edit',
  '/view/gw_tables',
  '/view/gw_binding_tables',
  '/view/product_tables',
  '/view/notify_tables',
  '/view/equip_health_template_tables',
  '/view/equip_health_indicator_tables',
  '/view/equip_realtime',
  '/view/equip_record_tables',
  '/view/equip_gis',
  '/view/equip_screen_tech',
  '/view/equip_detail',
  '/view/inspect_item_tables',
  '/view/inspect_template_tables',
  '/view/inspect_template_detail',
  '/view/inspect_person_tables',
  '/view/inspect_record_tables',
  '/view/inspect_record_form_edit',
  '/view/inspect_plan_tables',
  '/view/inspect_plan_form_edit',
  '/view/inspect_task_tables',
  '/view/inspect_task_detail',
  '/view/sync_tables',
  '/view/task_tables',
  '/view/local_file',
  '/view/local_file_tables',
  '/view/material_tables',
  '/view/material_classify_tables',
  '/view/bom_tree',
  '/view/line_tables',
  '/view/line_flow',
  '/view/devg_tables',
  '/view/devg_flow',
  '/view/mo_tables',
  '/view/mo_form_add',
  '/view/mo_form_edit',
  '/view/mo_exec_edit',
  '/view/mps_tables',
  '/view/mps_form_edit',
  '/view/mps_queue',
] as const

const VUE_SPA_VIEW_PATH_SET = new Set<string>(VUE_SPA_VIEW_PATHS)

/** Thymeleaf 模板名（/view/{viewName} 中的 viewName） */
export const VUE_SPA_VIEW_NAMES = VUE_SPA_VIEW_PATHS.map((path) => path.replace(/^\/view\//, ''))

export function matchVueSpaViewRequest(url: string): boolean {
  const raw = (url.split('?')[0].split('#')[0] || '').trim()
  if (!raw) {
    return false
  }
  const path = raw.endsWith('/') && raw.length > 1 ? raw.slice(0, -1) : raw
  return VUE_SPA_VIEW_PATH_SET.has(path)
}
