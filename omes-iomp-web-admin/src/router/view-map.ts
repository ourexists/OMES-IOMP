import type { Component } from 'vue'
import type { RouteRecordRaw } from 'vue-router'
import type { PermissionNode } from '@/types/permission'
import { normalizeRoutePath } from '@/utils/permission'

export interface ViewRouteMeta {
  title?: string
  i18n?: string
  permission?: string
  keepAlive?: boolean
  /** 内容区高度随页面收缩，避免看板/卡片页底部大块留白 */
  layoutAutoHeight?: boolean
}

/** 高度随内容、不占满视口的页面 */
export const LAYOUT_AUTO_HEIGHT_VIEW_PATHS = new Set<string>([
  '/view/mps_tables',
  '/view/gw_tables',
])

export function isLayoutAutoHeightPath(path: string): boolean {
  const normalized = path.startsWith('/') ? path : `/${path}`
  return LAYOUT_AUTO_HEIGHT_VIEW_PATHS.has(normalized)
}

type ViewLoader = () => Promise<Component>

const VIEW_COMPONENTS: Record<string, ViewLoader> = {
  '/view/overview': () => import('@/views/overview/index.vue'),
  '/view/account_tables': () => import('@/views/ucenter/account/index.vue'),
  '/view/role_tables': () => import('@/views/ucenter/role/index.vue'),
  '/view/permission_tables': () => import('@/views/ucenter/permission/index.vue'),
  '/view/workshop_tables': () => import('@/views/device/workshop/index.vue'),
  '/view/workshop_collect_tables': () => import('@/views/device/workshop/report/index.vue'),
  '/view/workshop_scada': () => import('@/views/device/workshop/scada/index.vue'),
  '/view/workshop_meta2d_view': () => import('@/views/device/workshop/meta2d-view/index.vue'),
  '/view/equip_tables': () => import('@/views/device/equip/index.vue'),
  '/view/equipattr_form_edit': () => import('@/views/device/equip/attr.vue'),
  '/view/gw_tables': () => import('@/views/device/gateway/index.vue'),
  '/view/gw_binding_tables': () => import('@/views/device/gateway/binding.vue'),
  '/view/product_tables': () => import('@/views/device/product/index.vue'),
  '/view/notify_tables': () => import('@/views/message/notify/index.vue'),
  '/view/equip_health_template_tables': () => import('@/views/device/health/index.vue'),
  '/view/equip_health_indicator_tables': () => import('@/views/device/health/indicator.vue'),
  '/view/equip_realtime': () => import('@/views/device/realtime/index.vue'),
  '/view/equip_record_tables': () => import('@/views/device/record/index.vue'),
  '/view/equip_gis': () => import('@/views/device/gis/index.vue'),
  '/view/equip_screen_tech': () => import('@/views/device/screen/index.vue'),
  '/view/equip_detail': () => import('@/views/device/realtime/detail/index.vue'),
  '/view/inspect_item_tables': () => import('@/views/inspection/item/index.vue'),
  '/view/inspect_template_tables': () => import('@/views/inspection/template/index.vue'),
  '/view/inspect_template_detail': () => import('@/views/inspection/template/detail/index.vue'),
  '/view/inspect_person_tables': () => import('@/views/inspection/person/index.vue'),
  '/view/inspect_record_tables': () => import('@/views/inspection/record/index.vue'),
  '/view/inspect_record_form_edit': () => import('@/views/inspection/record/detail/index.vue'),
  '/view/inspect_plan_tables': () => import('@/views/inspection/plan/index.vue'),
  '/view/inspect_plan_form_edit': () => import('@/views/inspection/plan/index.vue'),
  '/view/inspect_task_tables': () => import('@/views/inspection/task/index.vue'),
  '/view/inspect_task_detail': () => import('@/views/inspection/task/detail/index.vue'),
  '/view/sync_tables': () => import('@/views/system/sync/index.vue'),
  '/view/task_tables': () => import('@/views/system/task/index.vue'),
  '/view/system_config': () => import('@/views/system/config/index.vue'),
  '/view/material_tables': () => import('@/views/material/index.vue'),
  '/view/material_classify_tables': () => import('@/views/material/index.vue'),
  '/view/bom_tree': () => import('@/views/bom/index.vue'),
  '/view/line_tables': () => import('@/views/line/index.vue'),
  '/view/line_flow': () => import('@/views/line/flow/index.vue'),
  '/view/devg_tables': () => import('@/views/devg/index.vue'),
  '/view/devg_flow': () => import('@/views/devg/flow/index.vue'),
  '/view/mo_tables': () => import('@/views/mo/index.vue'),
  '/view/mo_form_add': () => import('@/views/mo/index.vue'),
  '/view/mo_form_edit': () => import('@/views/mo/detail/index.vue'),
  '/view/mo_exec_edit': () => import('@/views/mo/exec/index.vue'),
  '/view/mps_tables': () => import('@/views/mps/index.vue'),
  '/view/mps_form_edit': () => import('@/views/mps/detail/index.vue'),
  '/view/mps_queue': () => import('@/views/mps/queue/index.vue'),
}

/** 默认以独立窗口打开的 Vue 页面（与后端 url 对齐） */
export const STANDALONE_VIEW_PATHS = new Set<string>(['/view/equip_screen_tech'])

export function resolveViewComponent(url: string): ViewLoader | null {
  const normalized = url.startsWith('/') ? url : `/${url}`
  return VIEW_COMPONENTS[normalized] || null
}

/** 同一 Vue 页面对应的多个权限 URL（任一路径有权限则均可访问） */
export const VIEW_PATH_GROUPS: Record<string, string[]> = {}

/** 将权限菜单 URL 解析为 view-map 中的标准路径 */
export function resolveMenuViewPath(url: string): string | null {
  const normalized = normalizeRoutePath(url)
  if (resolveViewComponent(normalized)) {
    return normalized
  }
  const trimmed = normalized.replace(/^\//, '')
  if (!trimmed.startsWith('view/')) {
    const withViewPrefix = `/view/${trimmed}`
    if (resolveViewComponent(withViewPrefix)) {
      return withViewPrefix
    }
  }
  for (const [canonical, aliases] of Object.entries(VIEW_PATH_GROUPS)) {
    const group = [canonical, ...aliases]
    if (group.includes(normalized)) {
      return canonical
    }
  }
  return null
}

export function expandViewPathGroup(paths: Set<string>): void {
  for (const [canonical, aliases] of Object.entries(VIEW_PATH_GROUPS)) {
    const group = [canonical, ...aliases]
    if (group.some((item) => paths.has(item))) {
      group.forEach((item) => paths.add(item))
    }
  }
}

export function buildGroupedViewRoutes(seen: Set<string>): RouteRecordRaw[] {
  const routes: RouteRecordRaw[] = []
  for (const [canonical, aliases] of Object.entries(VIEW_PATH_GROUPS)) {
    const group = [canonical, ...aliases]
    if (!resolveViewComponent(canonical)) {
      continue
    }
    for (const path of group) {
      if (seen.has(path)) {
        continue
      }
      seen.add(path)
      routes.push({
        name: `View${path.replace(/\//g, '_')}`,
        ...buildViewRoute(path, {
          title: '文件管理',
          i18n: 'filePage.title',
        }),
      } as RouteRecordRaw)
    }
  }
  return routes
}

/** @deprecated 使用 buildGroupedViewRoutes */
export function buildViewAliasRoutes(
  seen: Set<string>,
  _accessiblePaths: Set<string>,
): RouteRecordRaw[] {
  return buildGroupedViewRoutes(seen)
}

export function isViewReplaced(url: string): boolean {
  return resolveViewComponent(url) != null
}

export interface SupplementaryViewRoute {
  path: string
  name: string
  /** 用户拥有其中任一路径权限时可访问（不在侧栏菜单中注册的页面） */
  requireAnyPath?: string[]
  meta: ViewRouteMeta
}

/** 登录后默认首页等：不在权限菜单里也需要注册路由 */
export const DEFAULT_VIEW_ROUTES: SupplementaryViewRoute[] = [
  {
    path: '/view/overview',
    name: 'ViewOverview',
    meta: {
      title: '设备管理平台',
      i18n: 'overviewPage.pageTitle',
    },
  },
]

export function buildDefaultViewRoutes(seen: Set<string>): RouteRecordRaw[] {
  const routes: RouteRecordRaw[] = []
  for (const item of DEFAULT_VIEW_ROUTES) {
    if (seen.has(item.path) || !resolveViewComponent(item.path)) {
      continue
    }
    seen.add(item.path)
    const built = buildViewRoute(item.path, item.meta)
    routes.push({
      name: item.name,
      ...built,
      meta: { ...built.meta, defaultView: true },
    } as RouteRecordRaw)
  }
  return routes
}

/** 由列表/详情跳转打开、通常不在权限菜单里的 Vue 页面 */
export const SUPPLEMENTARY_VIEW_ROUTES: SupplementaryViewRoute[] = [
  {
    path: '/view/equip_detail',
    name: 'ViewEquipDetail',
    requireAnyPath: ['/view/equip_realtime', '/view/equip_tables'],
    meta: {
      title: '设备详情',
      i18n: 'equipDetailPage.tabInfo',
    },
  },
  {
    path: '/view/workshop_meta2d_view',
    name: 'ViewWorkshopMeta2dPreview',
    requireAnyPath: ['/view/workshop_tables'],
    meta: {
      title: '2D组态预览',
      i18n: 'workshopMeta2dViewerPage.title',
    },
  },
  {
    path: '/view/inspect_template_detail',
    name: 'ViewInspectTemplateDetail',
    requireAnyPath: ['/view/inspect_template_tables'],
    meta: {
      title: '巡检模板详情',
      i18n: 'inspectTemplateDetailPage.title',
    },
  },
  {
    path: '/view/inspect_record_form_edit',
    name: 'ViewInspectRecordDetail',
    requireAnyPath: ['/view/inspect_record_tables', '/view/inspect_task_tables'],
    meta: {
      title: '巡检记录明细',
      i18n: 'inspectRecordPage.detailTitle',
    },
  },
  {
    path: '/view/inspect_task_detail',
    name: 'ViewInspectTaskDetail',
    requireAnyPath: ['/view/inspect_task_tables'],
    meta: {
      title: '巡检任务明细',
      i18n: 'inspectTaskDetailPage.title',
    },
  },
  {
    path: '/view/mo_form_edit',
    name: 'ViewMoDetail',
    requireAnyPath: ['/view/mo_tables'],
    meta: {
      title: '生产订单详情',
      i18n: 'moPage.detailTitle',
    },
  },
  {
    path: '/view/mo_exec_edit',
    name: 'ViewMoExec',
    requireAnyPath: ['/view/mo_tables'],
    meta: {
      title: '执行生产',
      i18n: 'moPage.execTitle',
    },
  },
  {
    path: '/view/mps_form_edit',
    name: 'ViewMpsDetail',
    requireAnyPath: ['/view/mps_tables'],
    meta: {
      title: '生产计划详情',
      i18n: 'mpsPage.detailTitle',
    },
  },
  {
    path: '/view/mps_queue',
    name: 'ViewMpsQueue',
    requireAnyPath: ['/view/mps_tables'],
    meta: {
      title: '生产队列管理',
      i18n: 'mpsPage.queueManage',
    },
  },
  {
    path: '/view/line_flow',
    name: 'ViewLineFlow',
    requireAnyPath: ['/view/line_tables'],
    meta: {
      title: '工艺工序',
      i18n: 'lineFlowPage.title',
    },
  },
  {
    path: '/view/devg_flow',
    name: 'ViewDevgFlow',
    requireAnyPath: ['/view/devg_tables'],
    meta: {
      title: '设备能力配置',
      i18n: 'devgFlowPage.title',
    },
  },
]

export function resolveSupplementaryViewRoute(path: string): SupplementaryViewRoute | null {
  const normalized = path.startsWith('/') ? path : `/${path}`
  return SUPPLEMENTARY_VIEW_ROUTES.find((item) => item.path === normalized) || null
}

export function buildSupplementaryViewRoutes(seen: Set<string>): RouteRecordRaw[] {
  const routes: RouteRecordRaw[] = []
  for (const item of SUPPLEMENTARY_VIEW_ROUTES) {
    if (seen.has(item.path) || !resolveViewComponent(item.path)) {
      continue
    }
    seen.add(item.path)
    const built = buildViewRoute(item.path, item.meta)
    routes.push({
      name: item.name,
      ...built,
      meta: {
        ...built.meta,
        supplementary: true,
        requireAnyPath: item.requireAnyPath,
      },
    } as RouteRecordRaw)
  }
  return routes
}

export function canAccessSupplementaryPath(path: string, accessiblePaths: Set<string>): boolean {
  const item = resolveSupplementaryViewRoute(path)
  if (!item) {
    return false
  }
  if (!item.requireAnyPath?.length) {
    return true
  }
  return item.requireAnyPath.some((parent) => accessiblePaths.has(parent))
}

/** 启动时预注册的 /view 路径（排除独立全屏页），供动态路由注册后替换 */
export const CONSTANT_VIEW_PATHS = Object.freeze(
  Object.keys(VIEW_COMPONENTS).filter((path) => !STANDALONE_VIEW_PATHS.has(path)),
)

export function constantViewRouteName(fullPath: string): string {
  const normalized = fullPath.startsWith('/') ? fullPath : `/${fullPath}`
  return `View${normalized.replace(/\//g, '_')}`
}

/** 登录前预注册，避免直接打开 /view/* 时 Router install 报 No match found */
export function buildConstantViewRoutes(): RouteRecordRaw[] {
  return CONSTANT_VIEW_PATHS.map((path) => {
    const built = buildViewRoute(path, {})
    return {
      name: constantViewRouteName(path),
      ...built,
      meta: { ...built.meta, constantView: true },
    } as RouteRecordRaw
  })
}

export function buildViewRoute(
  fullPath: string,
  meta: ViewRouteMeta,
): Pick<RouteRecordRaw, 'path' | 'component' | 'meta'> {
  const loader = resolveViewComponent(fullPath)
  const layoutAutoHeight = isLayoutAutoHeightPath(fullPath)
  return {
    path: fullPath.replace(/^\//, ''),
    component: loader || (() => import('@/views/error/404.vue')),
    meta: loader
      ? { ...meta, replaced: true, layoutAutoHeight }
      : { ...meta, replaced: false, layoutAutoHeight },
  }
}

export const DEFAULT_STANDALONE_VIEW_ROUTES: SupplementaryViewRoute[] = [
  {
    path: '/view/equip_screen_tech',
    name: 'ViewEquipScreenTechStandalone',
    meta: {
      title: '设备大屏',
      i18n: 'equipScreenPage.title',
    },
  },
]

/** 启动时预注册独立全屏页，避免 Router install / 菜单 resolve 报 No match found */
export function buildConstantStandaloneViewRoutes(): RouteRecordRaw[] {
  const routes: RouteRecordRaw[] = []
  for (const item of DEFAULT_STANDALONE_VIEW_ROUTES) {
    if (!resolveViewComponent(item.path)) {
      continue
    }
    const built = buildStandaloneViewRoute(item.path, item.meta, item.name)
    if (!built) {
      continue
    }
    routes.push({
      ...built,
      meta: { ...built.meta, constantStandalone: true },
    } as RouteRecordRaw)
  }
  return routes
}

export function buildDefaultStandaloneViewRoutes(
  seen: Set<string>,
  accessiblePaths: Set<string>,
): RouteRecordRaw[] {
  const routes: RouteRecordRaw[] = []
  for (const item of DEFAULT_STANDALONE_VIEW_ROUTES) {
    if (!accessiblePaths.has(item.path) || seen.has(item.path) || !resolveViewComponent(item.path)) {
      continue
    }
    seen.add(item.path)
    const built = buildStandaloneViewRoute(item.path, item.meta, item.name)
    if (!built) {
      continue
    }
    routes.push({
      ...built,
      meta: { ...built.meta, defaultStandalone: true },
    } as RouteRecordRaw)
  }
  return routes
}

const STANDALONE_COMPONENT_VALUES = new Set([
  'blank',
  '_blank',
  'window',
  'newwindow',
  'new_window',
  'standalone',
])

/** 是否在新窗口全屏打开（读取权限 component + 内置大屏路径） */
export function isStandalonePermission(node: Pick<PermissionNode, 'url' | 'component'>): boolean {
  const path = node.url ? normalizeRoutePath(node.url) : ''
  if (path && STANDALONE_VIEW_PATHS.has(path)) {
    return true
  }
  const comp = (node.component ?? '').trim().toLowerCase()
  return comp.length > 0 && STANDALONE_COMPONENT_VALUES.has(comp)
}

export function isStandaloneViewPath(path: string): boolean {
  return STANDALONE_VIEW_PATHS.has(normalizeRoutePath(path))
}

/** 注册在根路由上的独立全屏页（无 BasicLayout 侧栏） */
export function buildStandaloneViewRoute(
  fullPath: string,
  meta: ViewRouteMeta,
  routeName?: string | symbol,
): RouteRecordRaw | null {
  const loader = resolveViewComponent(fullPath)
  if (!loader) {
    return null
  }
  const pageMeta = {
    ...meta,
    standalone: true,
    replaced: true,
  }
  return {
    path: fullPath,
    name: routeName ?? constantViewRouteName(fullPath),
    component: loader,
    meta: pageMeta,
  }
}
