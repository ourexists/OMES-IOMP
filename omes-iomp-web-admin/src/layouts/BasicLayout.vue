<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  BgColorsOutlined,
  GlobalOutlined,
  LogoutOutlined,
  MenuFoldOutlined,
  MenuUnfoldOutlined,
  UserOutlined,
} from '@ant-design/icons-vue'
import { useLocale } from '@/composables/useLocale'
import type { AppLocale } from '@/i18n/locale'
import { resetDynamicRoutes } from '@/router/helper'
import { useAppStore } from '@/stores/app'
import { useThemeStore } from '@/stores/theme'
import type { ThemeVariantId } from '@/theme'
import { usePermissionStore } from '@/stores/permission'
import { useUserStore } from '@/stores/user'
import { getDisplayName } from '@/types/user'
import { buildSidebarMenus, findMenuOpenKeys, type SidebarMenuItem } from '@/utils/menu'
import { findPermissionByUrl } from '@/utils/permission'
import { isStandalonePermission, isStandaloneViewPath } from '@/router/view-map'
import { APP_ICON_ALT, APP_ICON_SRC } from '@/config/brand'
import MessageNotifyBell from '@/components/MessageNotifyBell.vue'
import { translateText } from '@/i18n'
import { isLayoutAutoHeightPath } from '@/router/view-map'

const route = useRoute()
const vueRouter = useRouter()
const appStore = useAppStore()
const themeStore = useThemeStore()
const userStore = useUserStore()
const permissionStore = usePermissionStore()
const { locale, t, localeOptions, setLocale } = useLocale()

const openKeys = ref<string[]>([])

const selectedKeys = computed(() => [route.path])
const displayName = computed(() => getDisplayName(userStore.userInfo))

const contentAutoHeight = computed(() => {
  if (route.meta.layoutAutoHeight === true) {
    return true
  }
  return isLayoutAutoHeightPath(route.path)
})

const pageTitle = computed(() => {
  const i18nKey = route.meta.i18n as string | undefined
  const fallback = route.meta.title as string | undefined
  if (i18nKey) {
    return translateText(i18nKey, fallback || t('app.title'))
  }
  return fallback || t('app.title')
})

const menuItems = computed<SidebarMenuItem[]>(() => {
  locale.value
  return buildSidebarMenus(permissionStore.menuTree)
})

watch(
  () => route.path,
  (path) => {
    openKeys.value = findMenuOpenKeys(path, permissionStore.menuTree)
  },
  { immediate: true },
)

function onOpenChange(keys: string[]) {
  openKeys.value = keys
}

function onMenuClick({ key }: { key: string }) {
  if (!key.startsWith('/')) {
    return
  }
  const node = findPermissionByUrl(permissionStore.menuTree, key)
  const standalone =
    isStandaloneViewPath(key) || (node != null && isStandalonePermission(node))
  if (standalone) {
    const resolved = vueRouter.resolve(key)
    window.open(resolved.href, '_blank', 'noopener,noreferrer')
    return
  }
  vueRouter.push(key)
}

function handleLogout() {
  userStore.logout()
  resetDynamicRoutes(vueRouter)
  vueRouter.push('/login')
}

function onLocaleChange({ key }: { key: string }) {
  setLocale(key as AppLocale)
}

function onThemeChange({ key }: { key: string }) {
  themeStore.setVariant(key as ThemeVariantId)
}

const currentThemeLabel = computed(() =>
  themeStore.isFluent ? t('layout.themeMicrosoft') : t('layout.themeClassic'),
)
</script>

<template>
  <a-layout class="basic-layout" :class="{ 'basic-layout--classic': !themeStore.isFluent }">
    <a-layout-sider
      v-model:collapsed="appStore.collapsed"
      collapsible
      :trigger="null"
      :theme="themeStore.menuTheme"
      width="240"
    >
      <div
        class="logo"
        :class="{
          'logo--collapsed': appStore.collapsed,
          'logo--dark': !themeStore.isFluent,
        }"
      >
        <img class="logo-icon" :src="APP_ICON_SRC" :alt="APP_ICON_ALT" />
        <span v-if="!appStore.collapsed" class="logo-text">{{ appStore.title }}</span>
      </div>
      <a-menu
        :theme="themeStore.menuTheme"
        mode="inline"
        :selected-keys="selectedKeys"
        :open-keys="openKeys"
        :items="menuItems"
        @click="onMenuClick"
        @open-change="onOpenChange"
      />
    </a-layout-sider>

    <a-layout>
      <a-layout-header class="header">
        <div class="header-left">
          <a-button type="text" class="trigger" @click="appStore.toggleCollapsed">
            <MenuUnfoldOutlined v-if="appStore.collapsed" />
            <MenuFoldOutlined v-else />
          </a-button>
          <span class="page-title">{{ pageTitle }}</span>
        </div>
        <div class="header-right">
          <MessageNotifyBell />

          <a-dropdown>
            <a-button type="text" class="lang-switch">
              <GlobalOutlined />
              <span>{{ localeOptions.find((item) => item.value === locale)?.label }}</span>
            </a-button>
            <template #overlay>
              <a-menu :selected-keys="[locale]" @click="onLocaleChange">
                <a-menu-item v-for="item in localeOptions" :key="item.value">
                  {{ item.label }}
                </a-menu-item>
              </a-menu>
            </template>
          </a-dropdown>

          <a-dropdown>
            <a-button type="text" class="theme-switch">
              <BgColorsOutlined />
              <span>{{ currentThemeLabel }}</span>
            </a-button>
            <template #overlay>
              <a-menu :selected-keys="[themeStore.variant]" @click="onThemeChange">
                <a-menu-item v-for="item in themeStore.variantOptions" :key="item.value">
                  {{ t(item.i18nKey) }}
                </a-menu-item>
              </a-menu>
            </template>
          </a-dropdown>

          <a-dropdown>
            <a-space class="user-entry">
              <a-avatar :size="32">
                <template #icon><UserOutlined /></template>
              </a-avatar>
              <span>{{ displayName }}</span>
            </a-space>
            <template #overlay>
              <a-menu>
                <a-menu-item key="logout" @click="handleLogout">
                  <LogoutOutlined />
                  {{ t('layout.logout') }}
                </a-menu-item>
              </a-menu>
            </template>
          </a-dropdown>
        </div>
      </a-layout-header>

      <a-layout-content class="content" :class="{ 'content--auto': contentAutoHeight }">
        <router-view v-slot="{ Component }">
          <transition name="fade" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </a-layout-content>
    </a-layout>
  </a-layout>
</template>

<style scoped>
.basic-layout {
  height: 100vh;
  overflow: hidden;
  --omes-sider-scrollbar-thumb: color-mix(in srgb, var(--omes-color-text) 22%, transparent);
  --omes-sider-scrollbar-thumb-hover: color-mix(in srgb, var(--omes-color-text) 34%, transparent);
}

.basic-layout--classic {
  --omes-sider-scrollbar-thumb: rgba(255, 255, 255, 0.22);
  --omes-sider-scrollbar-thumb-hover: rgba(255, 255, 255, 0.36);
}

.basic-layout :deep(.ant-layout-sider) {
  height: 100vh;
  overflow: hidden;
  flex-shrink: 0;
}

.basic-layout :deep(.ant-layout-sider-children) {
  display: flex;
  flex-direction: column;
  height: 100%;
  overflow: hidden;
}

.basic-layout :deep(.ant-layout-sider .ant-menu) {
  flex: 1;
  min-height: 0;
  overflow-x: hidden;
  overflow-y: auto;
  border-inline-end: none !important;
  padding-bottom: 8px;
  scrollbar-width: thin;
  scrollbar-color: var(--omes-sider-scrollbar-thumb) transparent;
}

.basic-layout :deep(.ant-layout-sider .ant-menu::-webkit-scrollbar) {
  width: 6px;
}

.basic-layout :deep(.ant-layout-sider .ant-menu::-webkit-scrollbar-thumb) {
  background: var(--omes-sider-scrollbar-thumb);
  border-radius: 100px;
}

.basic-layout :deep(.ant-layout-sider .ant-menu::-webkit-scrollbar-thumb:hover) {
  background: var(--omes-sider-scrollbar-thumb-hover);
}

.basic-layout > :deep(.ant-layout) {
  flex: 1;
  min-width: 0;
  min-height: 0;
  height: 100vh;
  overflow: hidden;
}

.logo {
  height: 64px;
  display: flex;
  align-items: center;
  justify-content: flex-start;
  gap: 10px;
  padding: 0 16px;
  color: var(--omes-color-text);
  border-bottom: 1px solid var(--omes-color-border);
  overflow: hidden;
}

.logo--collapsed {
  justify-content: center;
  padding: 0;
}

.logo-icon {
  width: 32px;
  height: 32px;
  flex-shrink: 0;
  border-radius: 6px;
  object-fit: cover;
}

.logo-text {
  font-size: 16px;
  font-weight: var(--omes-font-weight-semibold);
  letter-spacing: 0.3px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.logo--dark {
  color: var(--omes-color-white);
  border-bottom-color: rgba(255, 255, 255, 0.08);
}

.header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  height: 64px;
  padding: 0 16px 0 8px;
  line-height: 1.4;
  overflow: hidden;
  background: var(--omes-color-bg-container);
  border-bottom: 1px solid var(--omes-color-border);
}

.header-left,
.header-right {
  display: flex;
  align-items: center;
  gap: 12px;
  min-width: 0;
}

.header-left {
  flex: 1 1 auto;
}

.header-right {
  flex: 0 1 auto;
}

.trigger {
  font-size: 18px;
  flex-shrink: 0;
}

.page-title {
  min-width: 0;
  overflow: hidden;
  font-size: 16px;
  font-weight: var(--omes-font-weight-semibold);
  white-space: nowrap;
  text-overflow: ellipsis;
}

@media (max-width: 1100px) {
  .lang-switch span,
  .theme-switch span {
    display: none;
  }
}

.lang-switch,
.theme-switch {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.user-entry {
  cursor: pointer;
}

.content {
  margin: 16px;
  padding: 24px;
  background: var(--omes-color-bg-container);
  height: calc(100vh - 64px - 32px);
  min-height: calc(100vh - 64px - 32px);
  border-radius: var(--omes-border-radius-x-large);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.basic-layout:not(.basic-layout--classic) .content {
  border: none;
  box-shadow: var(--omes-fluent-surface-shadow, 0 1px 2px rgba(0, 0, 0, 0.04));
}

.basic-layout:not(.basic-layout--classic) .logo,
.basic-layout:not(.basic-layout--classic) .header {
  border-bottom-color: var(--omes-fluent-divider, rgba(0, 0, 0, 0.04));
}

.basic-layout--classic .content {
  border-radius: var(--omes-border-radius-medium);
  border: none;
  box-shadow: none;
}

.content.content--auto {
  height: auto;
  min-height: 0;
  overflow: visible;
}

.content > * {
  flex: 1;
  min-height: 0;
  width: 100%;
}

.content.content--auto > * {
  flex: 0 1 auto;
  width: 100%;
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.2s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>
