<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import {
  PlusOutlined,
  ReloadOutlined,
  SearchOutlined,
  ApartmentOutlined,
  ExpandAltOutlined,
  ShrinkOutlined,
} from '@ant-design/icons-vue'
import type { PermissionRecord, PlatformNode } from '@/api/ucenter'
import { fetchPermissionTreeInPlatform } from '@/api/ucenter'
import PlatformManageTree from './components/PlatformManageTree.vue'
import AdminPanelTitle from '@/components/admin/AdminPanelTitle.vue'
import PermissionFormModal from './components/PermissionFormModal.vue'
import { deletePermission } from '@/api/ucenter'
import { PermissionStrategy, PermissionType } from '@/types/permission'
import { message, Modal } from 'ant-design-vue'
import { useI18n } from 'vue-i18n'

const { t } = useI18n()

const selectedPlatform = ref<PlatformNode | null>(null)
const loading = ref(false)
const treeData = ref<PermissionRecord[]>([])
const searchKeyword = ref('')
const expandedKeys = ref<string[]>([])
const expandAll = ref(true)

const formOpen = ref(false)
const editingRecord = ref<PermissionRecord | null>(null)
const parentCode = ref<string>()

const permissionCount = computed(() => countNodes(treeData.value))

const displayTreeData = computed(() => {
  const keyword = searchKeyword.value.trim().toLowerCase()
  if (!keyword) {
    return treeData.value
  }
  return filterTree(treeData.value, keyword)
})

function countNodes(nodes: PermissionRecord[]): number {
  let count = 0
  for (const node of nodes) {
    count += 1
    if (node.children?.length) {
      count += countNodes(node.children)
    }
  }
  return count
}

function collectIds(nodes: PermissionRecord[]): string[] {
  const ids: string[] = []
  for (const node of nodes) {
    ids.push(node.id)
    if (node.children?.length) {
      ids.push(...collectIds(node.children))
    }
  }
  return ids
}

function filterTree(nodes: PermissionRecord[], keyword: string): PermissionRecord[] {
  const result: PermissionRecord[] = []
  for (const node of nodes) {
    const children = node.children?.length ? filterTree(node.children, keyword) : []
    const selfMatch =
      node.name?.toLowerCase().includes(keyword) ||
      node.code?.toLowerCase().includes(keyword) ||
      node.i18n?.toLowerCase().includes(keyword)
    if (selfMatch || children.length) {
      result.push({
        ...node,
        children: children.length ? children : selfMatch ? node.children : undefined,
      })
    }
  }
  return result
}

function typeTagColor(type?: number) {
  if (type === PermissionType.MENU) {
    return 'blue'
  }
  if (type === PermissionType.BUTTON) {
    return 'purple'
  }
  return 'default'
}

function strategyTagColor(strategy?: number) {
  if (strategy === PermissionStrategy.ENABLE_SHOW) {
    return 'success'
  }
  if (strategy === PermissionStrategy.ENABLE_HIDE) {
    return 'warning'
  }
  if (strategy === PermissionStrategy.DISABLED) {
    return 'error'
  }
  return 'default'
}

async function loadTree() {
  if (!selectedPlatform.value?.code) {
    treeData.value = []
    return
  }
  loading.value = true
  try {
    const data = await fetchPermissionTreeInPlatform(selectedPlatform.value.code)
    treeData.value = Array.isArray(data) ? data : []
  } finally {
    loading.value = false
  }
}

function onPlatformChange() {
  searchKeyword.value = ''
  loadTree()
}

function toggleExpandAll() {
  expandAll.value = !expandAll.value
  expandedKeys.value = expandAll.value ? collectIds(treeData.value) : []
}

function onExpandedKeysChange(keys: string[]) {
  expandedKeys.value = keys
  expandAll.value = keys.length === collectIds(treeData.value).length
}

function openCreate() {
  editingRecord.value = null
  parentCode.value = undefined
  formOpen.value = true
}

function openEdit(record: PermissionRecord) {
  editingRecord.value = record
  parentCode.value = undefined
  formOpen.value = true
}

function openCreateChild(record: PermissionRecord) {
  editingRecord.value = null
  parentCode.value = record.code
  formOpen.value = true
}

function confirmDelete(record: PermissionRecord) {
  Modal.confirm({
    title: t('permissionPage.deleteConfirm'),
    content: t('permissionPage.deleteContent', { name: record.name }),
    onOk: async () => {
      await deletePermission(record.id)
      message.success(t('permissionPage.deleteSuccess'))
      loadTree()
    },
  })
}

watch(treeData, (data) => {
  if (expandAll.value) {
    expandedKeys.value = collectIds(data)
  }
})

watch(searchKeyword, (keyword) => {
  if (keyword.trim()) {
    expandedKeys.value = collectIds(displayTreeData.value)
  }
})

watch(selectedPlatform, () => {
  if (selectedPlatform.value) {
    loadTree()
  }
})
</script>

<template>
  <div class="permission-page">
    <a-row :gutter="16" class="page-body">
      <a-col :xs="24" :lg="5">
        <a-card size="small" class="panel-card platform-panel">
          <template #title>
            <AdminPanelTitle>
              <template #icon><ApartmentOutlined /></template>
              {{ t('permissionPage.platform') }}
            </AdminPanelTitle>
          </template>
          <PlatformManageTree v-model="selectedPlatform" @change="onPlatformChange" />
        </a-card>
      </a-col>

      <a-col :xs="24" :lg="19">
        <a-card size="small" class="panel-card permission-panel">
          <template #title>
            <span class="card-title">{{ t('permissionPage.management') }}</span>
          </template>
          <template #extra>
            <a-tag v-if="selectedPlatform" color="processing">
              {{ selectedPlatform.name }}
            </a-tag>
            <a-tag v-if="selectedPlatform && permissionCount">
              {{ t('permissionPage.total', { count: permissionCount }) }}
            </a-tag>
          </template>

          <template v-if="selectedPlatform">
            <div class="tree-toolbar search-toolbar--compact">
              <div class="toolbar-left">
                <a-input
                  v-model:value="searchKeyword"
                  allow-clear
                  size="small"
                  :placeholder="t('permissionPage.searchPlaceholder')"
                  class="search-input"
                >
                  <template #prefix>
                    <SearchOutlined class="search-icon" />
                  </template>
                </a-input>
              </div>
              <div class="toolbar-right">
                <a-space wrap>
                  <a-button @click="toggleExpandAll">
                    <template #icon>
                      <ExpandAltOutlined v-if="!expandAll" />
                      <ShrinkOutlined v-else />
                    </template>
                    {{ expandAll ? t('permissionPage.collapseAll') : t('permissionPage.expandAll') }}
                  </a-button>
                  <a-button :loading="loading" @click="loadTree">
                    <template #icon><ReloadOutlined /></template>
                    {{ t('permissionPage.refresh') }}
                  </a-button>
                  <a-button type="primary" @click="openCreate">
                    <template #icon><PlusOutlined /></template>
                    {{ t('permissionPage.add') }}
                  </a-button>
                </a-space>
              </div>
            </div>

            <div class="tree-header">
              <span class="col-name">{{ t('permissionPage.colName') }}</span>
              <span class="col-code">{{ t('permissionPage.colCode') }}</span>
              <span class="col-type">{{ t('permissionPage.colType') }}</span>
              <span class="col-strategy">{{ t('permissionPage.colStrategy') }}</span>
              <span class="col-action">{{ t('permissionPage.colAction') }}</span>
            </div>

            <div class="tree-container tree-spin">
              <a-tree
                  v-if="displayTreeData.length"
                  :tree-data="displayTreeData"
                  :expanded-keys="expandedKeys"
                  :field-names="{ key: 'id', title: 'name', children: 'children' }"
                  block-node
                  @update:expanded-keys="onExpandedKeysChange"
                >
                  <template #title="record">
                    <div class="permission-node">
                      <div class="node-info">
                        <span class="node-name">{{ record.name }}</span>
                        <span v-if="record.i18n" class="node-i18n">{{ record.i18n }}</span>
                      </div>
                      <span class="col-code">
                        <span class="code-cell">{{ record.code }}</span>
                      </span>
                      <span class="col-type">
                        <a-tag :color="typeTagColor(record.type)" class="node-tag">
                          {{ record.typeEnum || record.type }}
                        </a-tag>
                      </span>
                      <span class="col-strategy">
                        <a-tag :color="strategyTagColor(record.strategy)" class="node-tag">
                          {{ record.strategyEnum || record.strategy }}
                        </a-tag>
                      </span>
                      <span class="col-action node-actions" @click.stop>
                        <a-button type="link" size="small" @click="openEdit(record)">{{ t('permissionPage.edit') }}</a-button>
                        <a-button type="link" size="small" @click="openCreateChild(record)">{{ t('permissionPage.addChild') }}</a-button>
                        <a-button type="link" size="small" danger @click="confirmDelete(record)">{{ t('permissionPage.delete') }}</a-button>
                      </span>
                    </div>
                  </template>
                </a-tree>
              <a-empty v-else :description="t('permissionPage.empty')" />
            </div>
          </template>

          <div v-else class="empty-panel">
            <a-empty :description="t('permissionPage.selectPlatform')" />
          </div>
        </a-card>
      </a-col>
    </a-row>

    <PermissionFormModal
      v-model:open="formOpen"
      :record="editingRecord"
      :platform="selectedPlatform"
      :parent-code="parentCode"
      @success="loadTree"
    />
  </div>
</template>

<style scoped>
.permission-page {
  display: flex;
  flex-direction: column;
  min-height: 100%;
}

.page-body {
  flex: 1;
}

.panel-card {
  height: 100%;
  border-radius: var(--omes-radius-md);
  box-shadow: var(--omes-shadow-card-sm);
}

.panel-card :deep(.ant-card-head) {
  min-height: 48px;
  border-bottom: 1px solid var(--omes-color-border);
}

.panel-card :deep(.ant-card-body) {
  padding: 16px;
}

.card-title {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  font-weight: 600;
}

.platform-panel :deep(.ant-card-body) {
  display: flex;
  flex-direction: column;
  min-height: calc(100vh - 280px);
  padding: 12px;
  background: var(--omes-color-bg-elevated);
}

.permission-panel :deep(.ant-card-body) {
  display: flex;
  flex-direction: column;
  min-height: calc(100vh - 280px);
}

.tree-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 12px;
  margin-bottom: 12px;
}

.toolbar-left,
.toolbar-right {
  display: flex;
  align-items: center;
  gap: 8px;
}

.search-input {
  width: 280px;
}

.search-icon {
  color: var(--omes-color-text-placeholder);
}

.tree-header,
.permission-node {
  display: grid;
  grid-template-columns: minmax(140px, 1.4fr) minmax(100px, 1fr) 88px 108px 168px;
  align-items: center;
  gap: 12px;
  width: 100%;
}

.tree-header {
  padding: 8px 12px 8px 28px;
  margin-bottom: 4px;
  font-size: 12px;
  font-weight: 600;
  color: var(--omes-color-text-quaternary);
  background: var(--omes-color-bg-elevated);
  border: 1px solid var(--omes-color-border);
  border-radius: var(--omes-radius-md) var(--omes-radius-md) 0 0;
}

.tree-spin {
  flex: 1;
  min-height: 0;
}

.tree-container {
  max-height: calc(100vh - 360px);
  overflow: auto;
  border: 1px solid var(--omes-color-border);
  border-top: none;
  border-radius: 0 0 8px 8px;
  background: var(--omes-color-bg-container);
}

.tree-container :deep(.ant-tree) {
  padding: 8px 12px 12px;
  background: transparent;
}

.tree-container :deep(.ant-tree-treenode) {
  width: 100%;
  padding: 2px 0;
  align-items: center;
}

.tree-container :deep(.ant-tree-node-content-wrapper) {
  flex: 1;
  min-width: 0;
  line-height: 32px;
  border-radius: var(--omes-radius-sm);
  transition: background 0.2s;
}

.tree-container :deep(.ant-tree-node-content-wrapper:hover) {
  background: var(--omes-color-primary-bg-hover);
}

.tree-container :deep(.ant-tree-title) {
  flex: 1;
  min-width: 0;
}

.permission-node {
  min-width: 520px;
  padding-right: 4px;
}

.node-info {
  display: flex;
  flex-direction: column;
  gap: 1px;
  min-width: 0;
  line-height: 1.4;
}

.node-name {
  font-weight: 500;
  color: var(--omes-color-text);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.node-i18n {
  font-size: 12px;
  color: var(--omes-color-text-quaternary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.code-cell {
  display: inline-block;
  max-width: 100%;
  font-family: ui-monospace, SFMono-Regular, 'SF Mono', Menlo, Consolas, monospace;
  font-size: 12px;
  color: var(--omes-color-primary);
  background: var(--omes-color-primary-bg);
  border: 1px solid var(--omes-color-primary-border);
  padding: 0 8px;
  border-radius: 4px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  vertical-align: middle;
}

.node-tag {
  margin: 0;
}

.node-actions {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  flex-wrap: nowrap;
}

.node-actions :deep(.ant-btn-link) {
  padding-inline: 4px;
  height: 24px;
}

.col-action {
  text-align: right;
}

.empty-panel {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 360px;
}

@media (max-width: 992px) {
  .platform-panel {
    margin-bottom: 16px;
  }

  .platform-panel :deep(.ant-card-body),
  .permission-panel :deep(.ant-card-body) {
    min-height: auto;
  }

  .search-input {
    width: 100%;
  }

  .tree-toolbar {
    flex-direction: column;
    align-items: stretch;
  }

  .toolbar-left,
  .toolbar-right {
    width: 100%;
  }

  .toolbar-right :deep(.ant-space) {
    width: 100%;
    justify-content: flex-end;
  }

  .tree-header {
    display: none;
  }

  .permission-node {
    grid-template-columns: 1fr;
    gap: 6px;
    min-width: 0;
  }

  .col-code,
  .col-type,
  .col-strategy {
    display: flex;
    align-items: center;
    gap: 8px;
  }

  .col-code::before {
    content: '编号';
    font-size: 12px;
    color: var(--omes-color-text-quaternary);
    flex-shrink: 0;
  }

  .col-type::before {
    content: '类型';
    font-size: 12px;
    color: var(--omes-color-text-quaternary);
    flex-shrink: 0;
  }

  .col-strategy::before {
    content: '策略';
    font-size: 12px;
    color: var(--omes-color-text-quaternary);
    flex-shrink: 0;
  }

  .col-action {
    text-align: left;
  }

  .node-actions {
    opacity: 1;
  }
}
</style>
