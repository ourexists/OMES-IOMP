import { h, type Component, type VNode } from 'vue'
import {
  ApartmentOutlined,
  AppstoreOutlined,
  AuditOutlined,
  BarChartOutlined,
  BellOutlined,
  BookOutlined,
  CalendarOutlined,
  CarryOutOutlined,
  ClockCircleOutlined,
  CloudOutlined,
  CloudSyncOutlined,
  ClusterOutlined,
  CommentOutlined,
  CustomerServiceOutlined,
  DashboardOutlined,
  DatabaseOutlined,
  DesktopOutlined,
  EnvironmentOutlined,
  ExperimentOutlined,
  FileOutlined,
  FileTextOutlined,
  FolderOpenOutlined,
  FolderOutlined,
  FormOutlined,
  GatewayOutlined,
  HeartOutlined,
  HistoryOutlined,
  LineChartOutlined,
  NodeIndexOutlined,
  PartitionOutlined,
  SettingOutlined,
  TableOutlined,
  TeamOutlined,
  ToolOutlined,
  UserOutlined,
} from '@ant-design/icons-vue'
import type { PermissionNode } from '@/types/permission'
import { resolvePermissionLabel } from '@/utils/i18n-helper'
import { filterMenuTree, normalizeRoutePath } from '@/utils/permission'

/** 权限表 icon 字段：Ant Design 图标组件名 */
const ICON_BY_NAME: Record<string, Component> = {
  ApartmentOutlined,
  AppstoreOutlined,
  AuditOutlined,
  BarChartOutlined,
  BellOutlined,
  BookOutlined,
  CalendarOutlined,
  CarryOutOutlined,
  ClockCircleOutlined,
  CloudOutlined,
  CloudSyncOutlined,
  ClusterOutlined,
  CommentOutlined,
  CustomerServiceOutlined,
  DashboardOutlined,
  DatabaseOutlined,
  DesktopOutlined,
  EnvironmentOutlined,
  ExperimentOutlined,
  FileOutlined,
  FileTextOutlined,
  FolderOpenOutlined,
  FolderOutlined,
  FormOutlined,
  GatewayOutlined,
  HeartOutlined,
  HistoryOutlined,
  LineChartOutlined,
  NodeIndexOutlined,
  PartitionOutlined,
  SettingOutlined,
  TableOutlined,
  TeamOutlined,
  ToolOutlined,
  UserOutlined,
}

/** 历史 layui 图标名兼容 */
const ICON_MAP: Record<string, Component> = {
  'layui-icon-website': AppstoreOutlined,
  'layui-icon-template-1': TableOutlined,
  'layui-icon-console': BarChartOutlined,
  'layui-icon-component': ToolOutlined,
  'layui-icon-chart-screen': BarChartOutlined,
  'layui-icon-set': SettingOutlined,
  'layui-icon-list': FormOutlined,
  'layui-icon-read': FileOutlined,
  'layui-icon-form': FormOutlined,
  'layui-icon-aim': CloudOutlined,
  'layui-icon-template': TableOutlined,
  'layui-icon-table': TableOutlined,
  'layui-icon-user': TeamOutlined,
  'layui-icon-group': TeamOutlined,
}

const FALLBACK_ICONS = [
  AppstoreOutlined,
  TableOutlined,
  BarChartOutlined,
  ToolOutlined,
  SettingOutlined,
  FormOutlined,
  FileOutlined,
  CloudOutlined,
]

export interface SidebarMenuItem {
  key: string
  label: string
  title?: string
  icon?: () => VNode
  children?: SidebarMenuItem[]
}

export function resolveMenuIcon(icon?: string, index = 0): Component {
  const normalized = icon?.trim()
  if (normalized) {
    if (ICON_BY_NAME[normalized]) {
      return ICON_BY_NAME[normalized]
    }
    if (ICON_MAP[normalized]) {
      return ICON_MAP[normalized]
    }
  }
  return FALLBACK_ICONS[index % FALLBACK_ICONS.length]
}

export function buildSidebarMenus(nodes: PermissionNode[] = []): SidebarMenuItem[] {
  const visibleMenus = filterMenuTree(nodes)
  return visibleMenus.map((node, index) => toSidebarMenuItem(node, index))
}

function toSidebarMenuItem(node: PermissionNode, index: number): SidebarMenuItem {
  const routePath = node.url ? normalizeRoutePath(node.url) : node.code || node.id
  const label = resolvePermissionLabel(node)
  const item: SidebarMenuItem = {
    key: routePath,
    label,
    title: label,
    icon: () => h(resolveMenuIcon(node.icon, index)),
  }

  if (node.children?.length) {
    item.children = node.children.map((child, childIndex) => toSidebarMenuItem(child, childIndex))
  }

  return item
}

export function findMenuOpenKeys(path: string, nodes: PermissionNode[] = []): string[] {
  const keys: string[] = []

  const walk = (list: PermissionNode[], parents: string[]) => {
    for (const node of list) {
      const currentKey = node.url ? normalizeRoutePath(node.url) : node.code || node.id
      const nextParents = [...parents, currentKey]
      if (node.url && normalizeRoutePath(node.url) === path) {
        keys.push(...parents)
        return true
      }
      if (node.children?.length && walk(node.children, nextParents)) {
        return true
      }
    }
    return false
  }

  walk(filterMenuTree(nodes), [])
  return keys
}
