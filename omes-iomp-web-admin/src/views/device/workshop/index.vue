<script setup lang="ts">
import CompactSearchActions from '@/components/admin/CompactSearchActions.vue'
import AdminPanelTitle from '@/components/admin/AdminPanelTitle.vue'
import { computed, nextTick, onMounted, onUnmounted, ref, shallowRef, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import {
  ApartmentOutlined,
  DeleteOutlined,
  EditOutlined,
  EnvironmentOutlined,
  ExpandAltOutlined,
  FolderOutlined,
  LayoutOutlined,
  PlusOutlined,
  ReloadOutlined,
  SearchOutlined,
  SettingOutlined,
  ShrinkOutlined,
} from '@ant-design/icons-vue'
import type { WorkshopNode } from '@/api/device'
import { deleteWorkshops, fetchWorkshopTree } from '@/api/device'
import WorkshopCollectDrawer from './components/WorkshopCollectDrawer.vue'
import WorkshopFormModal from './components/WorkshopFormModal.vue'
import WorkshopScadaDrawer from './components/WorkshopScadaDrawer.vue'
import {
  buildWorkshopTreeIndex,
  collectExpandableKeys,
  collectSearchMatchKeys,
  filterWorkshopTree,
} from '@/utils/workshop-tree'
import { message, Modal } from 'ant-design-vue'

const SEARCH_DEBOUNCE_MS = 250
const DEFAULT_EXPAND_DEPTH = 1

const { t } = useI18n()

const loading = ref(false)
const treeData = shallowRef<WorkshopNode[]>([])
const nodeByCode = shallowRef(new Map<string, WorkshopNode>())
const nodeCount = ref(0)
const allExpandableKeys = shallowRef<string[]>([])

const selectedKeys = ref<string[]>([])
const expandedKeys = ref<string[]>([])
const expandAll = ref(false)
const searchKeyword = ref('')
const debouncedSearch = ref('')

const selectedNode = shallowRef<WorkshopNode | null>(null)

const formOpen = ref(false)
const editingRecord = ref<WorkshopNode | null>(null)
const parentCode = ref<string>()

const collectOpen = ref(false)
const scadaOpen = ref(false)
const configWorkshop = ref<WorkshopNode | null>(null)

const treeBodyRef = ref<HTMLElement | null>(null)
const treeScrollHeight = ref(320)
let treeResizeObserver: ResizeObserver | null = null
let resizeRafId = 0
let searchDebounceTimer: ReturnType<typeof setTimeout> | null = null

const treeScrollStyle = computed(() => ({
  height: `${treeScrollHeight.value}px`,
}))
const searchActive = computed(() => Boolean(debouncedSearch.value.trim()))
const selectedIsFolder = computed(() => (selectedNode.value?.children?.length ?? 0) > 0)
const selectedChildCount = computed(() => selectedNode.value?.children?.length ?? 0)
const selectedLocationText = computed(() => {
  const node = selectedNode.value
  if (!node) {
    return ''
  }
  const lng = node.lng != null && String(node.lng).trim() !== '' ? String(node.lng) : ''
  const lat = node.lat != null && String(node.lat).trim() !== '' ? String(node.lat) : ''
  if (lng && lat) {
    return `${lng} / ${lat}`
  }
  return ''
})

const displayTreeData = computed(() => {
  const keyword = debouncedSearch.value.trim().toLowerCase()
  if (!keyword) {
    return treeData.value
  }
  return filterWorkshopTree(treeData.value, keyword)
})

const searchMatchKeys = computed(() => {
  const keyword = debouncedSearch.value.trim().toLowerCase()
  if (!keyword) {
    return null
  }
  return collectSearchMatchKeys(treeData.value, keyword)
})

function syncExpandedKeys() {
  if (expandAll.value) {
    expandedKeys.value = allExpandableKeys.value.slice()
    return
  }
  const keyword = debouncedSearch.value.trim()
  if (keyword) {
    expandedKeys.value = collectExpandableKeys(displayTreeData.value)
    return
  }
  expandedKeys.value = collectExpandableKeys(treeData.value, DEFAULT_EXPAND_DEPTH)
}

function syncSelectionAfterLoad() {
  if (!selectedKeys.value.length) {
    selectedNode.value = null
    return
  }
  const node = nodeByCode.value.get(selectedKeys.value[0])
  if (node) {
    selectedNode.value = node
  } else {
    selectedKeys.value = []
    selectedNode.value = null
  }
}

function rebuildIndex(nodes: WorkshopNode[]) {
  const index = buildWorkshopTreeIndex(nodes)
  nodeByCode.value = index.nodeByCode
  nodeCount.value = index.nodeCount
  allExpandableKeys.value = index.expandableKeys
}

async function loadTree() {
  loading.value = true
  try {
    const data = await fetchWorkshopTree()
    const list = Array.isArray(data) ? data : []
    treeData.value = list
    rebuildIndex(list)
    syncExpandedKeys()
    syncSelectionAfterLoad()
  } finally {
    loading.value = false
  }
}

function flushSearch() {
  if (searchDebounceTimer) {
    clearTimeout(searchDebounceTimer)
    searchDebounceTimer = null
  }
  debouncedSearch.value = searchKeyword.value
}

function onSearch() {
  flushSearch()
  syncExpandedKeys()
}

function onReset() {
  searchKeyword.value = ''
  flushSearch()
  syncExpandedKeys()
}

function expandAllNodes() {
  expandAll.value = true
  expandedKeys.value = allExpandableKeys.value.slice()
}

function collapseAllNodes() {
  expandAll.value = false
  expandedKeys.value = []
}

function onExpandedKeysChange(keys: string[]) {
  expandedKeys.value = keys
  const targetCount = debouncedSearch.value.trim()
    ? collectExpandableKeys(displayTreeData.value).length
    : allExpandableKeys.value.length
  expandAll.value = targetCount > 0 && keys.length >= targetCount
}

function onSelect(keys: string[]) {
  selectedKeys.value = keys
  if (!keys.length) {
    selectedNode.value = null
    return
  }
  selectedNode.value = nodeByCode.value.get(keys[0]) ?? null
}

function openCreateRoot() {
  editingRecord.value = null
  parentCode.value = undefined
  formOpen.value = true
}

function openEdit(record: WorkshopNode) {
  editingRecord.value = record
  parentCode.value = undefined
  formOpen.value = true
}

function openCreateChild(record: WorkshopNode) {
  editingRecord.value = null
  parentCode.value = record.code || record.selfCode
  formOpen.value = true
}

function openCollect(record: WorkshopNode) {
  configWorkshop.value = record
  collectOpen.value = true
}

function openScada(record: WorkshopNode) {
  configWorkshop.value = record
  scadaOpen.value = true
}

function confirmDelete(record: WorkshopNode) {
  Modal.confirm({
    title: t('workshopPage.deleteConfirm'),
    content: t('workshopPage.deleteContent', { name: record.name }),
    onOk: async () => {
      await deleteWorkshops([record.id])
      message.success(t('workshopPage.deleteSuccess'))
      if (selectedKeys.value[0] === record.selfCode) {
        selectedKeys.value = []
        selectedNode.value = null
      }
      loadTree()
    },
  })
}

function updateTreeScrollHeight() {
  const body = treeBodyRef.value
  if (!body) {
    return
  }
  treeScrollHeight.value = Math.max(160, body.clientHeight)
}

function scheduleTreeScrollHeightUpdate() {
  if (resizeRafId) {
    cancelAnimationFrame(resizeRafId)
  }
  resizeRafId = requestAnimationFrame(() => {
    resizeRafId = 0
    updateTreeScrollHeight()
  })
}

async function scheduleTreeScrollUpdate() {
  await nextTick()
  scheduleTreeScrollHeightUpdate()
}

watch(searchKeyword, (value) => {
  if (searchDebounceTimer) {
    clearTimeout(searchDebounceTimer)
  }
  searchDebounceTimer = setTimeout(() => {
    searchDebounceTimer = null
    debouncedSearch.value = value
  }, SEARCH_DEBOUNCE_MS)
})

watch(debouncedSearch, () => {
  if (debouncedSearch.value.trim()) {
    expandedKeys.value = collectExpandableKeys(displayTreeData.value)
  } else {
    syncExpandedKeys()
  }
})

watch(loading, (spinning) => {
  if (!spinning) {
    scheduleTreeScrollUpdate()
  }
})

onMounted(async () => {
  await loadTree()
  await scheduleTreeScrollUpdate()
  if (treeBodyRef.value) {
    treeResizeObserver = new ResizeObserver(() => scheduleTreeScrollHeightUpdate())
    treeResizeObserver.observe(treeBodyRef.value)
  }
})

onUnmounted(() => {
  if (searchDebounceTimer) {
    clearTimeout(searchDebounceTimer)
  }
  if (resizeRafId) {
    cancelAnimationFrame(resizeRafId)
  }
  treeResizeObserver?.disconnect()
  treeResizeObserver = null
})
</script>

<template>
  <div class="workshop-page">
    <a-row :gutter="16" class="workshop-layout">
      <a-col :xs="24" :lg="15" class="tree-col">
        <a-card size="small" class="panel-card tree-card">
          <template #title>
            <AdminPanelTitle>
              <template #icon><ApartmentOutlined /></template>
              {{ t('workshopPage.title') }}
            </AdminPanelTitle>
          </template>
          <template #extra>
            <a-tag v-if="nodeCount" class="count-tag" color="processing">
              {{ t('workshopPage.total', { count: nodeCount }) }}
            </a-tag>
          </template>

          <p class="page-desc">{{ t('workshopPage.subtitle') }}</p>

          <div class="toolbar-strip search-toolbar--compact">
            <a-form layout="inline" class="search-form" @finish="onSearch">
              <a-form-item>
                <a-input
                  v-model:value="searchKeyword"
                  allow-clear
                  size="small"
                  class="search-input"
                  :placeholder="t('workshopPage.searchPlaceholder')"
                >
                  <template #prefix>
                    <SearchOutlined class="search-icon" />
                  </template>
                </a-input>
              </a-form-item>
              <CompactSearchActions
                :query-title="t('workshopPage.query')"
                :reset-title="t('workshopPage.reset')"
                :loading="loading"
                @reset="onReset"
              >
                <a-tooltip :title="t('workshopPage.refresh')">
                  <a-button size="small" :loading="loading" @click="loadTree">
                    <ReloadOutlined />
                  </a-button>
                </a-tooltip>
                <a-tooltip :title="t('workshopPage.expandAll')">
                  <a-button size="small" @click="expandAllNodes">
                    <ExpandAltOutlined />
                  </a-button>
                </a-tooltip>
                <a-tooltip :title="t('workshopPage.collapseAll')">
                  <a-button size="small" @click="collapseAllNodes">
                    <ShrinkOutlined />
                  </a-button>
                </a-tooltip>
              </CompactSearchActions>
            </a-form>
            <a-button type="primary" class="add-root-btn" @click="openCreateRoot">
              <template #icon><PlusOutlined /></template>
              {{ t('workshopPage.addRoot') }}
            </a-button>
          </div>

          <div ref="treeBodyRef" class="tree-body">
            <div
              class="tree-scroll tree-spin"
              :class="{ 'is-filtering': searchActive }"
              :style="treeScrollStyle"
            >
              <a-tree
                v-if="displayTreeData.length && treeScrollHeight > 0"
                :tree-data="displayTreeData"
                :selected-keys="selectedKeys"
                :expanded-keys="expandedKeys"
                :field-names="{ title: 'name', key: 'selfCode', children: 'children' }"
                block-node
                @select="onSelect"
                @update:expanded-keys="onExpandedKeysChange"
              >
                <template #title="node">
                  <span
                    class="tree-node"
                    :class="{
                      'is-leaf': !node.children?.length,
                      'is-match': searchMatchKeys?.has(node.selfCode),
                    }"
                  >
                    <span class="node-icon-wrap" :class="node.children?.length ? 'folder' : 'leaf'">
                      <FolderOutlined v-if="node.children?.length" />
                      <EnvironmentOutlined v-else />
                    </span>
                    <span class="node-name" :title="node.name">{{ node.name }}</span>
                    <span class="code-tag" :title="node.selfCode">{{ node.selfCode }}</span>
                  </span>
                </template>
              </a-tree>
              <a-empty v-else :description="t('workshopPage.empty')" class="tree-empty" />
            </div>
          </div>
        </a-card>
      </a-col>

      <a-col :xs="24" :lg="9" class="detail-col">
        <a-card size="small" class="panel-card detail-card">
          <template #title>
            <span class="card-title">{{ t('workshopPage.detailTitle') }}</span>
          </template>

          <div v-if="selectedNode" class="detail-body">
            <div class="detail-hero" :class="{ 'detail-hero--leaf': !selectedIsFolder }">
              <div class="detail-hero__icon">
                <FolderOutlined v-if="selectedIsFolder" />
                <EnvironmentOutlined v-else />
              </div>
              <div class="detail-hero__meta">
                <div class="detail-hero__tags">
                  <a-tag :color="selectedIsFolder ? 'processing' : 'success'" class="type-tag">
                    {{ selectedIsFolder ? t('workshopPage.nodeFolder') : t('workshopPage.nodeLeaf') }}
                  </a-tag>
                  <a-tag v-if="selectedIsFolder" class="child-tag">
                    {{ t('workshopPage.childCount', { count: selectedChildCount }) }}
                  </a-tag>
                </div>
                <h3 class="detail-name">{{ selectedNode.name }}</h3>
                <span class="detail-code">{{ selectedNode.selfCode }}</span>
                <div v-if="selectedNode.address || selectedLocationText" class="detail-location">
                  <div v-if="selectedNode.address" class="detail-location__row">
                    {{ t('workshopPage.address') }}：{{ selectedNode.address }}
                  </div>
                  <div v-if="selectedLocationText" class="detail-location__row">
                    {{ t('workshopPage.lngLat') }}：{{ selectedLocationText }}
                  </div>
                </div>
                <div v-else class="detail-location detail-location--empty">
                  {{ t('workshopPage.noLocation') }}
                </div>
              </div>
            </div>

            <div class="detail-section">
              <div class="section-label">{{ t('workshopPage.actionsManage') }}</div>
              <div class="action-grid">
                <button type="button" class="action-tile" @click="openEdit(selectedNode)">
                  <span class="action-tile__icon action-tile__icon--primary"><EditOutlined /></span>
                  <span class="action-tile__text">{{ t('workshopPage.edit') }}</span>
                </button>
                <button type="button" class="action-tile" @click="openCreateChild(selectedNode)">
                  <span class="action-tile__icon action-tile__icon--primary"><PlusOutlined /></span>
                  <span class="action-tile__text">{{ t('workshopPage.addChild') }}</span>
                </button>
              </div>
            </div>

            <div class="detail-section">
              <div class="section-label">{{ t('workshopPage.actionsConfig') }}</div>
              <div class="action-grid">
                <button type="button" class="action-tile" @click="openCollect(selectedNode)">
                  <span class="action-tile__icon action-tile__icon--cyan"><SettingOutlined /></span>
                  <span class="action-tile__text">{{ t('workshopPage.collect') }}</span>
                </button>
                <button type="button" class="action-tile" @click="openScada(selectedNode)">
                  <span class="action-tile__icon action-tile__icon--purple"><LayoutOutlined /></span>
                  <span class="action-tile__text">{{ t('workshopPage.scada') }}</span>
                </button>
              </div>
            </div>

            <div class="detail-danger">
              <a-button danger block size="large" @click="confirmDelete(selectedNode)">
                <template #icon><DeleteOutlined /></template>
                {{ t('workshopPage.delete') }}
              </a-button>
            </div>
          </div>

          <div v-else class="detail-placeholder">
            <div class="placeholder-icon">
              <ApartmentOutlined />
            </div>
            <p class="placeholder-text">{{ t('workshopPage.selectHint') }}</p>
          </div>
        </a-card>
      </a-col>
    </a-row>

    <WorkshopFormModal
      v-model:open="formOpen"
      :record="editingRecord"
      :parent-code="parentCode"
      @success="loadTree"
    />
    <WorkshopCollectDrawer v-model:open="collectOpen" :workshop="configWorkshop" />
    <WorkshopScadaDrawer v-model:open="scadaOpen" :workshop="configWorkshop" />
  </div>
</template>

<style scoped>
.workshop-page {
  height: calc(100vh - 64px - 32px - 48px);
  max-height: calc(100vh - 64px - 32px - 48px);
  overflow: hidden;
}

.workshop-layout {
  height: 100%;
  min-height: 0;
  overflow: hidden;
}

.workshop-layout :deep(> .ant-col) {
  height: 100%;
  min-width: 0;
}

.tree-col,
.detail-col {
  height: 100%;
  min-width: 0;
  display: flex;
  flex-direction: column;
}

.tree-col :deep(> .ant-card),
.detail-col :deep(> .ant-card) {
  flex: 1;
  min-height: 0;
}

.tree-card :deep(.ant-card-body) {
  padding-bottom: 14px;
}

.panel-card {
  display: flex;
  flex-direction: column;
  height: 100%;
  border-radius: 12px;
  border: 1px solid var(--omes-color-border);
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.04);
  overflow: hidden;
}

.panel-card :deep(.ant-card-head) {
  flex-shrink: 0;
  min-height: 52px;
  border-bottom: 1px solid var(--omes-color-border);
  background: linear-gradient(180deg, #fafbff 0%, #fff 100%);
}

.panel-card :deep(.ant-card-body) {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  padding: 14px 16px 16px;
}

.card-title {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  font-weight: 600;
  font-size: 15px;
}

.title-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  border-radius: var(--omes-radius-md);
  background: var(--omes-color-primary-bg);
  color: var(--omes-color-primary);
  font-size: 15px;
}

.count-tag {
  margin: 0;
  border-radius: 999px;
}

.page-desc {
  flex-shrink: 0;
  margin: 0 0 12px;
  font-size: 13px;
  color: var(--omes-color-text-quaternary);
  line-height: 1.6;
}

.toolbar-strip {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 10px;
  margin-bottom: 12px;
  padding: 10px 12px;
  background: var(--omes-color-bg-elevated);
  border: 1px solid var(--omes-color-border);
  border-radius: var(--omes-radius-lg);
}

.search-input {
  flex: 1;
  min-width: 160px;
  max-width: 280px;
}

.search-icon {
  color: var(--omes-color-text-placeholder);
}

.toolbar-actions {
  flex-shrink: 0;
}

.add-root-btn {
  margin-left: auto;
  flex-shrink: 0;
}

.tree-body {
  flex: 1 1 0;
  min-height: 0;
  height: 0;
  overflow: hidden;
}

.tree-scroll.tree-spin {
  display: flex;
  flex-direction: column;
  height: 100%;
}

.tree-scroll {
  box-sizing: border-box;
  overflow-x: hidden;
  overflow-y: auto;
  padding: 6px 4px 6px 6px;
  border: 1px solid var(--omes-color-border-secondary);
  border-radius: var(--omes-radius-lg);
  background: linear-gradient(180deg, #f8faff 0%, #fff 24%);
  transition: border-color 0.2s ease;
}

.tree-scroll::-webkit-scrollbar {
  width: 6px;
}

.tree-scroll::-webkit-scrollbar-thumb {
  background: rgba(22, 119, 255, 0.35);
  border-radius: 3px;
}

.tree-scroll::-webkit-scrollbar-thumb:hover {
  background: rgba(22, 119, 255, 0.5);
}

.tree-scroll.is-filtering {
  border-color: #91caff;
  background: linear-gradient(180deg, #f0f7ff 0%, #fff 28%);
}

.tree-scroll :deep(.ant-tree) {
  background: transparent;
  font-size: 13px;
}

.tree-scroll :deep(.ant-tree-list-holder-inner) {
  padding-bottom: 4px;
}

.tree-scroll :deep(.ant-tree-treenode) {
  align-items: center;
  padding: 2px 0;
  width: 100%;
  margin-bottom: 2px;
}

.tree-scroll :deep(.ant-tree-node-content-wrapper) {
  flex: 1;
  min-width: 0;
  display: flex !important;
  align-items: center;
  min-height: 36px;
  height: 36px;
  line-height: 36px;
  padding: 0 8px !important;
  border-radius: var(--omes-radius-md);
  transition: background 0.15s ease;
}

.tree-scroll :deep(.ant-tree-node-content-wrapper:hover) {
  background: rgba(22, 119, 255, 0.06);
}

.tree-scroll :deep(.ant-tree-treenode-selected .ant-tree-node-content-wrapper) {
  background: var(--omes-color-primary-bg) !important;
  border-left: 3px solid var(--omes-color-primary);
  padding-left: 5px !important;
}

.tree-scroll :deep(.ant-tree-switcher) {
  display: flex;
  align-items: center;
  justify-content: center;
  align-self: center;
  line-height: 1;
  color: var(--omes-color-text-placeholder);
}

.tree-scroll :deep(.ant-tree-title) {
  flex: 1;
  min-width: 0;
}

.tree-node {
  display: flex;
  align-items: center;
  gap: 8px;
  width: 100%;
  min-width: 0;
  padding-right: 4px;
}

.tree-node.is-match .node-name {
  color: var(--omes-color-primary);
}

.tree-node.is-match .code-tag {
  background: #fff7e6;
  color: #d46b08;
  border: 1px solid #ffd591;
}

.node-icon-wrap {
  flex-shrink: 0;
  width: 26px;
  height: 26px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 7px;
  font-size: 14px;
}

.node-icon-wrap.folder {
  background: var(--omes-color-primary-bg);
  color: var(--omes-color-primary);
}

.node-icon-wrap.leaf {
  background: #f6ffed;
  color: var(--omes-color-success);
}

.node-name {
  flex: 1;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  color: var(--omes-color-text);
  font-weight: 500;
}

.tree-node.is-leaf .node-name {
  font-weight: 400;
}

.tree-scroll :deep(.ant-tree-treenode-selected) .node-name {
  color: var(--omes-color-primary);
  font-weight: 600;
}

.code-tag {
  flex-shrink: 0;
  max-width: 108px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-family: ui-monospace, SFMono-Regular, 'SF Mono', Menlo, Consolas, monospace;
  font-size: 11px;
  color: var(--omes-color-primary-hover);
  background: rgba(22, 119, 255, 0.08);
  padding: 0 7px;
  line-height: 20px;
  border-radius: 4px;
  border: 1px solid transparent;
}

.tree-empty {
  padding: 56px 0;
}

.detail-card :deep(.ant-card-body) {
  padding-top: 12px;
}

.detail-body {
  display: flex;
  flex-direction: column;
  gap: 16px;
  flex: 1;
  min-height: 0;
  overflow-y: auto;
}

.detail-hero {
  display: flex;
  gap: 14px;
  padding: 16px;
  border-radius: 12px;
  background: linear-gradient(135deg, var(--omes-color-primary-bg) 0%, var(--omes-color-bg-table-hover-alt) 48%, #fff 100%);
  border: 1px solid var(--omes-color-primary-border);
}

.detail-hero--leaf {
  background: linear-gradient(135deg, #f6ffed 0%, #fcfff8 48%, #fff 100%);
  border-color: #d9f7be;
}

.detail-hero__icon {
  flex-shrink: 0;
  width: 52px;
  height: 52px;
  border-radius: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  background: var(--omes-color-bg-container);
  color: var(--omes-color-primary);
  box-shadow: 0 4px 12px rgba(22, 119, 255, 0.15);
}

.detail-hero--leaf .detail-hero__icon {
  color: var(--omes-color-success);
  box-shadow: 0 4px 12px rgba(82, 196, 26, 0.15);
}

.detail-hero__meta {
  min-width: 0;
  flex: 1;
}

.detail-hero__tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-bottom: 8px;
}

.type-tag,
.child-tag {
  margin: 0;
  border-radius: 999px;
}

.detail-name {
  margin: 0 0 8px;
  font-size: 17px;
  font-weight: 600;
  line-height: 1.35;
  color: var(--omes-color-text);
  word-break: break-word;
}

.detail-code {
  display: inline-block;
  font-family: ui-monospace, SFMono-Regular, 'SF Mono', Menlo, Consolas, monospace;
  font-size: 12px;
  color: var(--omes-color-primary);
  background: rgba(255, 255, 255, 0.85);
  padding: 4px 10px;
  border-radius: var(--omes-radius-sm);
  border: 1px solid rgba(22, 119, 255, 0.2);
}

.detail-hero--leaf .detail-code {
  color: #389e0d;
  border-color: rgba(82, 196, 26, 0.35);
}

.detail-location {
  margin-top: 10px;
  font-size: 12px;
  line-height: 1.6;
  color: var(--omes-color-text-secondary);
}

.detail-location__row {
  word-break: break-word;
}

.detail-location--empty {
  color: var(--omes-color-text-quaternary);
}

.detail-section {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.section-label {
  font-size: 12px;
  font-weight: 600;
  color: var(--omes-color-text-quaternary);
  letter-spacing: 0.02em;
  text-transform: uppercase;
}

.action-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}

.action-tile {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
  min-width: 0;
  min-height: 72px;
  padding: 12px 8px;
  border: 1px solid var(--omes-color-border);
  border-radius: var(--omes-radius-lg);
  background: var(--omes-color-bg-container);
  cursor: pointer;
  transition:
    border-color 0.2s ease,
    box-shadow 0.2s ease,
    transform 0.15s ease;
}

.action-tile:hover {
  border-color: var(--omes-color-primary-border);
  box-shadow: 0 4px 12px rgba(22, 119, 255, 0.1);
  transform: translateY(-1px);
}

.action-tile:active {
  transform: translateY(0);
}

.action-tile__icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  border-radius: var(--omes-radius-lg);
  font-size: 17px;
}

.action-tile__icon--primary {
  background: var(--omes-color-primary-bg);
  color: var(--omes-color-primary);
}

.action-tile__icon--cyan {
  background: #e6fffb;
  color: var(--omes-color-accent-cyan-from);
}

.action-tile__icon--purple {
  background: #f9f0ff;
  color: var(--omes-color-accent-purple-from);
}

.action-tile__text {
  max-width: 100%;
  font-size: 13px;
  color: var(--omes-color-text-label);
  line-height: 1.2;
  text-align: center;
  overflow-wrap: anywhere;
}

.detail-danger {
  margin-top: auto;
  padding-top: 4px;
}

.detail-danger :deep(.ant-btn) {
  height: 44px;
  border-radius: var(--omes-radius-lg);
}

.detail-placeholder {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 280px;
  padding: 32px 20px;
  text-align: center;
}

.placeholder-icon {
  width: 72px;
  height: 72px;
  margin-bottom: 16px;
  border-radius: 20px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 32px;
  color: #91caff;
  background: linear-gradient(135deg, var(--omes-color-primary-bg) 0%, #f0f5ff 100%);
}

.placeholder-text {
  margin: 0;
  max-width: 220px;
  font-size: 14px;
  color: var(--omes-color-text-quaternary);
  line-height: 1.6;
}

@media (max-width: 992px) {
  .workshop-page {
    height: auto;
    max-height: none;
    overflow: visible;
  }

  .workshop-layout {
    flex: none;
    overflow: visible;
  }

  .tree-col,
  .detail-col {
    height: auto;
  }

  .panel-card {
    height: auto;
  }

  .tree-col .panel-card {
    margin-bottom: 16px;
  }

  .tree-body {
    flex: none;
    height: 360px;
    min-height: 360px;
  }

  .add-root-btn {
    width: 100%;
    margin-left: 0;
  }

  .toolbar-strip {
    flex-direction: column;
    align-items: stretch;
  }

  .search-input {
    max-width: none;
  }
}

@media (max-width: 576px) {
  .action-grid {
    grid-template-columns: 1fr;
  }
}
</style>
