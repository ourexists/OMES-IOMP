import type { Router, RouteLocationNormalized } from 'vue-router'
import { useAppStore } from '@/stores/app'
import { usePermissionStore } from '@/stores/permission'
import { useUserStore } from '@/stores/user'
import { setupDynamicRoutes } from './dynamic'

const whiteList = ['/login', '/403']

function loginRedirect(to: RouteLocationNormalized) {
  return {
    path: '/login',
    query: { redirect: to.fullPath },
  }
}

export function setupRouterGuards(router: Router) {
  router.beforeEach(async (to) => {
    const appStore = useAppStore()
    if (!appStore.ready) {
      await appStore.bootstrap()
    }

    const titleBase = to.meta.standalone ? '' : ' - OMES ADMIN'
    document.title = `${String(to.meta.title || 'OMES')}${titleBase}`

    const standalonePage = Boolean(to.meta.standalone)
    document.documentElement.classList.toggle('standalone-page', standalonePage)
    document.body.classList.toggle('standalone-page', standalonePage)

    const userStore = useUserStore()
    const permissionStore = usePermissionStore()
    const isPublic = Boolean(to.meta.public) || whiteList.includes(to.path)

    if (!userStore.isLoggedIn) {
      if (isPublic) {
        return true
      }
      return loginRedirect(to)
    }

    if (to.path === '/login') {
      return { path: '/' }
    }

    try {
      if (!userStore.sessionReady) {
        const ok = await userStore.initSession()
        if (!ok) {
          return loginRedirect(to)
        }
      }

      await setupDynamicRoutes(router)

      // 刷新时先匹配到无标题的占位路由，动态路由替换后要再解析一次，否则顶栏标题停在 OMES
      const resolved = router.resolve({ path: to.path, query: to.query, hash: to.hash })
      const matchedPlaceholder = to.matched.some((record) => record.meta.constantView)
      const resolvedReal =
        resolved.matched.some((record) => record.meta.title || record.meta.i18n || record.meta.permission) &&
        !resolved.matched.some((record) => record.meta.constantView)
      if (to.matched.length === 0 || (matchedPlaceholder && resolvedReal)) {
        return { path: to.path, query: to.query, hash: to.hash, replace: true }
      }

      if (!isPublic && !permissionStore.canAccessPath(to.path)) {
        return { name: 'Forbidden' }
      }

      return true
    } catch {
      userStore.logout()
      permissionStore.reset()
      return loginRedirect(to)
    }
  })
}
