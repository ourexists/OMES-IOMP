import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import type { RouteRecordRaw } from 'vue-router'
import { STORAGE_KEYS } from '@/config'
import type { PermissionNode } from '@/types/permission'
import {
  buildDefaultViewRoutes,
  buildDefaultStandaloneViewRoutes,
  buildGroupedViewRoutes,
  buildStandaloneViewRoute,
  buildSupplementaryViewRoutes,
  buildViewRoute,
  canAccessSupplementaryPath,
  expandViewPathGroup,
  isStandalonePermission,
  resolveMenuViewPath,
  resolveViewComponent,
} from '@/router/view-map'
import {
  canAccessLocalFilePath,
  collectAccessiblePaths,
  collectPermissionCodes,
  filterMenuTree,
  flattenPermissions,
  isVisibleMenu,
  normalizeRoutePath,
} from '@/utils/permission'
import { getJson } from '@/utils/storage'

export const usePermissionStore = defineStore('permission', () => {
  const permissionTree = ref<PermissionNode[]>(getJson<PermissionNode[]>(STORAGE_KEYS.menu) || [])
  const routesRegistered = ref(false)

  const menuTree = computed(() => filterMenuTree(permissionTree.value))
  const permissionCodes = computed(() => collectPermissionCodes(permissionTree.value))
  const accessiblePaths = computed(() => {
    const paths = collectAccessiblePaths(permissionTree.value)
    expandViewPathGroup(paths)
    return paths
  })

  function setPermissionTree(tree: PermissionNode[]) {
    permissionTree.value = Array.isArray(tree) ? tree : []
    routesRegistered.value = false
  }

  function reset() {
    permissionTree.value = []
    routesRegistered.value = false
  }

  function markRoutesRegistered() {
    routesRegistered.value = true
  }

  function hasPermission(code?: string | string[]): boolean {
    if (!code) {
      return true
    }
    const codes = permissionCodes.value
    const targets = Array.isArray(code) ? code : [code]
    return targets.some((item) => codes.has(item))
  }

  function canAccessPath(path: string): boolean {
    const normalized = normalizeRoutePath(path)
    if (accessiblePaths.value.has(normalized)) {
      return true
    }
    if (canAccessSupplementaryPath(normalized, accessiblePaths.value)) {
      return true
    }
    return canAccessLocalFilePath(normalized, permissionTree.value, accessiblePaths.value)
  }

  function buildDynamicRoutes(): RouteRecordRaw[] {
    const routes: RouteRecordRaw[] = []
    const seen = new Set<string>()

    routes.push(...buildDefaultViewRoutes(seen))

    flattenPermissions(menuTree.value).forEach((node) => {
      if (!isVisibleMenu(node) || !node.url) {
        return
      }
      if (isStandalonePermission(node)) {
        return
      }
      const fullPath = resolveMenuViewPath(node.url)
      if (!fullPath || !resolveViewComponent(fullPath)) {
        return
      }
      if (seen.has(fullPath)) {
        return
      }
      seen.add(fullPath)

      routes.push({
        name: node.code || node.id,
        ...buildViewRoute(fullPath, {
          title: node.name,
          i18n: node.i18n,
          permission: node.code || node.id,
          keepAlive: node.keepAlive ?? false,
        }),
      } as RouteRecordRaw)
    })

    routes.push(...buildSupplementaryViewRoutes(seen))
    routes.push(...buildGroupedViewRoutes(seen))

    return routes
  }

  function buildStandaloneRoutes(): RouteRecordRaw[] {
    const routes: RouteRecordRaw[] = []
    const seen = new Set<string>()

    routes.push(...buildDefaultStandaloneViewRoutes(seen, accessiblePaths.value))

    flattenPermissions(menuTree.value).forEach((node) => {
      if (!isVisibleMenu(node) || !node.url || !isStandalonePermission(node)) {
        return
      }
      const fullPath = normalizeRoutePath(node.url)
      if (seen.has(fullPath)) {
        return
      }
      const childName = node.code ? `${node.code}Standalone` : `${node.id}Standalone`
      const route = buildStandaloneViewRoute(
        fullPath,
        {
          title: node.name,
          i18n: node.i18n,
          permission: node.code || node.id,
          keepAlive: node.keepAlive ?? false,
        },
        childName,
      )
      if (!route) {
        return
      }
      seen.add(fullPath)
      routes.push(route as RouteRecordRaw)
    })

    return routes
  }

  return {
    permissionTree,
    routesRegistered,
    menuTree,
    permissionCodes,
    accessiblePaths,
    setPermissionTree,
    reset,
    markRoutesRegistered,
    hasPermission,
    canAccessPath,
    buildDynamicRoutes,
    buildStandaloneRoutes,
  }
})
