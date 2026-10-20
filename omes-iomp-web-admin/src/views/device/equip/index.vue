<script setup lang="ts">
import CompactSearchActions from '@/components/admin/CompactSearchActions.vue'
import AdminPanelTitle from '@/components/admin/AdminPanelTitle.vue'
import { computed, onMounted, reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import {
  ApartmentOutlined,
  ApiOutlined,
  ClusterOutlined,
  DeleteOutlined,
  EditOutlined,
  HeartOutlined,
  PlusOutlined, SettingOutlined,
} from '@ant-design/icons-vue'
import type { EquipRecord, HealthTemplate, WorkshopNode } from '@/api/device'
import {
  deleteEquips,
  fetchEquipPage,
  fetchEquipTypes,
  fetchHealthTemplates,
} from '@/api/device'
import { fetchGatewayList, type GatewayRecord } from '@/api/gateway'
import EquipProductThumb from '@/components/EquipProductThumb.vue'
import WorkshopTree from '@/components/WorkshopTree.vue'
import TableScrollWrap from '@/components/common/TableScrollWrap.vue'
import EquipAttrConfigModal from './components/EquipAttrConfigModal.vue'
import EquipFormModal from './components/EquipFormModal.vue'
import HealthTemplateModal from './components/HealthTemplateModal.vue'
import { mapOptions } from '@/utils/options'
import { message, Modal } from 'ant-design-vue'
import type { TablePaginationConfig } from 'ant-design-vue/es/table'

const { t } = useI18n()

const selectedWorkshop = ref<WorkshopNode | null>(null)
const selectedEquip = ref<EquipRecord | null>(null)
/** 仅设备列表区域 loading，不遮整页 spin 层 */
const listLoading = ref(false)
const dataSource = ref<EquipRecord[]>([])
const selectedRowKeys = ref<string[]>([])
const pagination = reactive({ current: 1, pageSize: 10, total: 0 })
const typeOptions = ref<{ value: string | number; label: string }[]>([])
const healthTemplates = ref<HealthTemplate[]>([])
const gatewayOptions = ref<{ value: string; label: string }[]>([])
const gatewayNameById = ref<Record<string, string>>({})

const searchForm = reactive({
  name: '',
  selfCode: '',
  type: undefined as string | number | undefined,
  gwId: undefined as string | undefined,
})

const formOpen = ref(false)
const editingRecord = ref<EquipRecord | null>(null)
const healthOpen = ref(false)
const healthEquip = ref<EquipRecord | null>(null)
const attrOpen = ref(false)
const attrEquip = ref<EquipRecord | null>(null)
let loadTableRequestId = 0
let lastLoadedWorkshopCode = ''

const columns = computed(() => [
  { title: t('equipPage.colName'), dataIndex: 'name', key: 'name', ellipsis: true, minWidth: 120 },
  { title: t('equipPage.colCode'), dataIndex: 'selfCode', key: 'selfCode', width: 130, ellipsis: true },
  { title: t('equipPage.colType'), dataIndex: 'typeDesc', key: 'typeDesc', width: 100, ellipsis: true },
  { title: t('equipPage.colModel'), dataIndex: 'modelName', key: 'modelName', width: 110, ellipsis: true },
  { title: t('equipPage.colGateway'), key: 'gateway', width: 120, ellipsis: true },
  { title: t('equipPage.colHealthTemplate'), key: 'healthTemplate', width: 120, ellipsis: true },
])

function onRowSelectionChange(keys: string[]) {
  selectedRowKeys.value = keys
}

const rowSelection = computed(() => ({
  selectedRowKeys: selectedRowKeys.value,
  onChange: onRowSelectionChange,
}))

const hasSelection = computed(() => selectedRowKeys.value.length > 0)
const hasWorkshop = computed(() => Boolean(selectedWorkshop.value?.selfCode))
function templateName(id?: string | null) {
  if (!id) {
    return t('equipPage.unlinked')
  }
  const item = healthTemplates.value.find((item) => item.id === id)
  return item?.name || id
}

function gatewayLabel(record: EquipRecord): string {
  const gwId = record.config?.gwId
  if (!gwId) {
    return t('equipPage.unbound')
  }
  return gatewayNameById.value[gwId] || gwId
}

function customTableRow(record: EquipRecord) {
  return {
    onClick: (event: MouseEvent) => {
      const target = event.target as HTMLElement
      if (target.closest('.ant-table-selection-column, .ant-checkbox-wrapper')) {
        return
      }
      selectedEquip.value = record
    },
    class: selectedEquip.value?.id === record.id ? 'equip-row--selected' : '',
  }
}

function syncSelectedEquip() {
  if (!selectedEquip.value?.id) {
    return
  }
  const fresh = dataSource.value.find((item) => item.id === selectedEquip.value!.id)
  selectedEquip.value = fresh ?? null
}

async function loadGateways() {
  const list = (await fetchGatewayList()) || []
  gatewayOptions.value = list
    .filter((g) => g.id)
    .map((g) => ({ value: g.id!, label: g.serverName || g.id! }))
  const map: Record<string, string> = {}
  list.forEach((g: GatewayRecord) => {
    if (g.id) {
      map[g.id] = g.serverName || g.id
    }
  })
  gatewayNameById.value = map
}

async function loadMeta() {
  const [types, templates] = await Promise.all([fetchEquipTypes(), fetchHealthTemplates()])
  typeOptions.value = mapOptions(types).map((item) => ({
    ...item,
    value: String(item.value),
  }))
  healthTemplates.value = Array.isArray(templates) ? templates : []
  await loadGateways()
}

async function loadTable() {
  const workshopCode = selectedWorkshop.value?.selfCode
  if (!workshopCode) {
    dataSource.value = []
    pagination.total = 0
    selectedEquip.value = null
    lastLoadedWorkshopCode = ''
    return
  }

  const requestId = ++loadTableRequestId
  listLoading.value = true
  try {
    const result = await fetchEquipPage({
      page: pagination.current,
      pageSize: pagination.pageSize,
      workshopCode,
      name: searchForm.name || undefined,
      selfCode: searchForm.selfCode || undefined,
      type: searchForm.type || undefined,
      gwId: searchForm.gwId || undefined,
      needWorkshopCascade: true,
      queryWorkshop: true,
      queryConfig: true,
    })
    if (requestId !== loadTableRequestId) {
      return
    }
    dataSource.value = result.records || []
    pagination.total = result.total || 0
    lastLoadedWorkshopCode = workshopCode
    syncSelectedEquip()
  } finally {
    if (requestId === loadTableRequestId) {
      listLoading.value = false
    }
  }
}

function onWorkshopChange(node: WorkshopNode | null) {
  const code = node?.selfCode ?? ''
  if (!code) {
    loadTableRequestId += 1
    listLoading.value = false
    dataSource.value = []
    pagination.total = 0
    selectedEquip.value = null
    selectedRowKeys.value = []
    lastLoadedWorkshopCode = ''
    return
  }
  if (code === lastLoadedWorkshopCode) {
    return
  }
  pagination.current = 1
  selectedRowKeys.value = []
  void loadTable()
}

function onSearch() {
  if (!hasWorkshop.value) {
    return
  }
  pagination.current = 1
  void loadTable()
}

function onReset() {
  searchForm.name = ''
  searchForm.selfCode = ''
  searchForm.type = undefined
  searchForm.gwId = undefined
  onSearch()
}

function onTableChange(page: TablePaginationConfig) {
  pagination.current = page.current || 1
  pagination.pageSize = page.pageSize || 10
  loadTable()
}

function openCreate() {
  if (!selectedWorkshop.value) {
    message.warning(t('equipPage.selectWorkshop'))
    return
  }
  editingRecord.value = null
  formOpen.value = true
}

function openEdit(record: EquipRecord) {
  editingRecord.value = record
  formOpen.value = true
}

function openHealth(record: EquipRecord) {
  healthEquip.value = record
  healthOpen.value = true
}

function openAttrs(record: EquipRecord) {
  attrEquip.value = record
  attrOpen.value = true
}

function confirmDelete(ids: string[]) {
  Modal.confirm({
    title: t('equipPage.deleteConfirm'),
    content: t('equipPage.deleteContent', { count: ids.length }),
    onOk: async () => {
      await deleteEquips(ids)
      message.success(t('equipPage.deleteSuccess'))
      selectedRowKeys.value = selectedRowKeys.value.filter((id) => !ids.includes(id))
      if (selectedEquip.value && ids.includes(selectedEquip.value.id)) {
        selectedEquip.value = null
      }
      loadTable()
    },
  })
}

onMounted(async () => {
  await loadMeta()
})
</script>

<template>
  <div class="admin-page equip-page">
    <a-row :gutter="16" class="equip-layout">
      <a-col :xs="24" :lg="5" class="sidebar-col">
        <a-card size="small" class="panel-card sidebar-card">
          <template #title>
            <AdminPanelTitle>
              <template #icon><ApartmentOutlined /></template>
              {{ t('equipPage.workshop') }}
            </AdminPanelTitle>
          </template>
          <template v-if="selectedWorkshop" #extra>
            <a-tag color="processing" class="workshop-tag">{{ selectedWorkshop.name }}</a-tag>
          </template>
          <div class="sidebar-tree-body">
            <WorkshopTree v-model="selectedWorkshop" fill @change="onWorkshopChange" />
          </div>
        </a-card>
      </a-col>

      <a-col :xs="24" :lg="13" class="list-col">
        <a-card size="small" class="panel-card list-card">
          <template #title>
            <AdminPanelTitle icon-class="card-title__icon--purple">
              <template #icon><ClusterOutlined /></template>
              {{ t('equipPage.title') }}
            </AdminPanelTitle>
          </template>
          <template #extra>
            <a-space :size="6" wrap>
              <a-tag v-if="hasSelection" color="blue">
                {{ t('equipPage.selectedCount', { count: selectedRowKeys.length }) }}
              </a-tag>
              <a-tag v-if="pagination.total" color="processing">
                {{ t('equipPage.total', { count: pagination.total }) }}
              </a-tag>
            </a-space>
          </template>

          <div class="list-card-inner">
          <div class="toolbar-strip toolbar-strip--fixed search-toolbar--compact">
            <div class="toolbar-row toolbar-row--filters">
              <a-form layout="inline" class="search-form search-form--compact" :model="searchForm" @finish="onSearch">
                <a-form-item name="name">
                  <a-input
                    v-model:value="searchForm.name"
                    allow-clear
                    size="small"
                    :placeholder="t('equipPage.searchName')"
                    class="search-input"
                  />
                </a-form-item>
                <a-form-item name="selfCode">
                  <a-input
                    v-model:value="searchForm.selfCode"
                    allow-clear
                    size="small"
                    :placeholder="t('equipPage.searchCode')"
                    class="search-input"
                  />
                </a-form-item>
                <a-form-item name="type">
                  <a-select
                    v-model:value="searchForm.type"
                    allow-clear
                    size="small"
                    :placeholder="t('equipPage.type')"
                    class="search-select"
                    :options="typeOptions"
                  />
                </a-form-item>
                <a-form-item name="gwId">
                  <a-select
                    v-model:value="searchForm.gwId"
                    allow-clear
                    size="small"
                    :placeholder="t('equipPage.gateway')"
                    class="search-select-wide"
                    :options="gatewayOptions"
                  />
                </a-form-item>
                <CompactSearchActions
                  :query-title="t('equipPage.query')"
                  :reset-title="t('equipPage.reset')"
                  :disabled="!hasWorkshop"
                  @reset="onReset"
                />
              </a-form>
            </div>
            <div class="toolbar-row toolbar-row--actions">
              <a-tooltip :title="t('equipPage.batchDelete')">
                <a-button
                  danger
                  size="small"
                  :disabled="!hasSelection"
                  @click="confirmDelete(selectedRowKeys)"
                >
                  <template #icon><DeleteOutlined /></template>
                  {{ t('equipPage.batchDelete') }}
                </a-button>
              </a-tooltip>
              <a-button type="primary" size="small" :disabled="!hasWorkshop" @click="openCreate">
                <template #icon><PlusOutlined /></template>
                {{ t('equipPage.add') }}
              </a-button>
            </div>
          </div>

          <TableScrollWrap :refresh-keys="[dataSource.length, pagination.total]">
            <template #default="{ scrollY }">
            <a-table
              row-key="id"
              size="middle"
              bordered
              class="scroll-table equip-table"
              :columns="columns"
              :data-source="dataSource"
              :row-selection="hasWorkshop ? rowSelection : undefined"
              :custom-row="customTableRow"
              :scroll="{ x: 720, y: scrollY }"
              :pagination="{
                current: pagination.current,
                pageSize: pagination.pageSize,
                total: pagination.total,
                size: 'small',
                showSizeChanger: true,
                showTotal: (total: number) => t('equipPage.paginationTotal', { total }),
              }"
              @change="onTableChange"
            >
              <template #bodyCell="{ column, record }">
                <template v-if="column.key === 'name'">
                  <span class="name-cell">
                    <span class="name-cell__thumb">
                      <EquipProductThumb :path="record.productImage" />
                    </span>
                    <span class="name-cell__text">{{ record.name || '—' }}</span>
                  </span>
                </template>
                <template v-else-if="column.key === 'selfCode'">
                  <span v-if="record.selfCode" class="code-cell">{{ record.selfCode }}</span>
                  <span v-else class="text-muted">-</span>
                </template>
                <template v-else-if="column.key === 'gateway'">
                  <a-tag :color="record.config?.gwId ? 'blue' : 'default'" class="status-tag">
                    <ApiOutlined v-if="record.config?.gwId" class="tag-icon" />
                    {{ gatewayLabel(record) }}
                  </a-tag>
                </template>
                <template v-else-if="column.key === 'healthTemplate'">
                  <a-tag :color="record.healthTemplateId ? 'green' : 'default'" class="status-tag">
                    <HeartOutlined v-if="record.healthTemplateId" class="tag-icon" />
                    {{ templateName(record.healthTemplateId) }}
                  </a-tag>
                </template>
              </template>

              <template #emptyText>
                <a-empty :description="hasWorkshop ? t('equipPage.empty') : t('equipPage.emptyWorkshop')">
                  <a-button v-if="hasWorkshop" type="primary" @click="openCreate">
                    <PlusOutlined />
                    {{ t('equipPage.add') }}
                  </a-button>
                </a-empty>
              </template>
            </a-table>
            </template>
          </TableScrollWrap>
          </div>
        </a-card>
      </a-col>

      <a-col :xs="24" :lg="6" class="detail-col">
        <a-card size="small" class="panel-card detail-card">
          <template #title>
            <span class="card-title">{{ t('equipPage.detailTitle') }}</span>
          </template>

          <div v-if="selectedEquip" class="detail-body">
            <div class="detail-hero">
              <div class="detail-hero__icon">
                <EquipProductThumb v-if="selectedEquip.productImage" :path="selectedEquip.productImage" />
                <ClusterOutlined v-else />
              </div>
              <div class="detail-hero__meta">
                <div class="detail-hero__tags">
                  <a-tag v-if="selectedEquip.typeDesc" color="processing" class="type-tag">
                    {{ selectedEquip.typeDesc }}
                  </a-tag>
                  <a-tag v-if="selectedEquip.modelName" color="cyan" class="type-tag">
                    {{ selectedEquip.modelName }}
                  </a-tag>
                  <a-tag :color="selectedEquip.config?.gwId ? 'blue' : 'default'" class="type-tag">
                    <ApiOutlined v-if="selectedEquip.config?.gwId" />
                    {{ gatewayLabel(selectedEquip) }}
                  </a-tag>
                  <a-tag :color="selectedEquip.healthTemplateId ? 'green' : 'default'" class="type-tag">
                    <HeartOutlined v-if="selectedEquip.healthTemplateId" />
                    {{ templateName(selectedEquip.healthTemplateId) }}
                  </a-tag>
                </div>
                <h3 class="detail-name">{{ selectedEquip.name }}</h3>
                <span v-if="selectedEquip.selfCode" class="detail-code">{{ selectedEquip.selfCode }}</span>
                <span v-if="selectedEquip.workshop?.name" class="detail-workshop">
                  {{ selectedEquip.workshop.name }}
                </span>
              </div>
            </div>

            <div class="detail-section">
              <div class="section-label">{{ t('equipPage.actionsManage') }}</div>
              <div class="action-grid">
                <button type="button" class="action-tile" @click="openEdit(selectedEquip)">
                  <span class="action-tile__icon action-tile__icon--primary"><EditOutlined /></span>
                  <span class="action-tile__text">{{ t('equipPage.edit') }}</span>
                </button>
              </div>
            </div>

            <div class="detail-section">
              <div class="section-label">{{ t('equipPage.actionsConfig') }}</div>
              <div class="action-grid action-grid--pair">
                <button type="button" class="action-tile" @click="openHealth(selectedEquip)">
                  <span class="action-tile__icon action-tile__icon--green"><HeartOutlined /></span>
                  <span class="action-tile__text">{{ t('equipPage.health') }}</span>
                </button>
                <button type="button" class="action-tile" @click="openAttrs(selectedEquip)">
                  <span class="action-tile__icon action-tile__icon--cyan"><SettingOutlined /></span>
                  <span class="action-tile__text">{{ t('equipPage.attrs') }}</span>
                </button>
              </div>
            </div>

            <div class="detail-danger">
              <a-button danger block size="large" @click="confirmDelete([selectedEquip.id])">
                <template #icon><DeleteOutlined /></template>
                {{ t('equipPage.delete') }}
              </a-button>
            </div>
          </div>

          <div v-else class="detail-placeholder">
            <div class="placeholder-icon">
              <ClusterOutlined />
            </div>
            <p class="placeholder-text">
              {{ hasWorkshop ? t('equipPage.selectHint') : t('equipPage.emptyWorkshop') }}
            </p>
          </div>
        </a-card>
      </a-col>
    </a-row>

    <EquipFormModal
      v-model:open="formOpen"
      :record="editingRecord"
      :workshop="selectedWorkshop"
      @success="loadTable"
    />
    <HealthTemplateModal
      v-model:open="healthOpen"
      :equip="healthEquip"
      :templates="healthTemplates"
      @success="loadTable"
    />
    <EquipAttrConfigModal
      v-model:open="attrOpen"
      :equip="attrEquip"
      @success="loadTable"
    />
  </div>
</template>

<style scoped>
.equip-page {
  overflow: hidden;
}

.equip-layout {
  height: 100%;
  min-height: 0;
  overflow: hidden;
}

.equip-layout :deep(> .ant-col) {
  height: 100%;
  min-width: 0;
}

.sidebar-col,
.list-col,
.detail-col {
  height: 100%;
  min-width: 0;
  display: flex;
  flex-direction: column;
}

.sidebar-col :deep(> .ant-card),
.list-col :deep(> .ant-card),
.detail-col :deep(> .ant-card) {
  flex: 1;
  min-height: 0;
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

.sidebar-card :deep(.ant-card-body) {
  padding-bottom: 16px;
}

.sidebar-tree-body {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.sidebar-tree-body :deep(.workshop-tree) {
  flex: 1;
  min-height: 0;
  height: 100%;
}

.list-card :deep(.ant-card-body) {
  gap: 0;
  padding-bottom: 12px;
}

.list-card-inner {
  --equip-toolbar-height: 100px;
  display: grid;
  grid-template-rows: var(--equip-toolbar-height) minmax(0, 1fr);
  flex: 1;
  min-height: 0;
  height: 100%;
  overflow: hidden;
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

.title-icon--equip {
  background: #f9f0ff;
  color: var(--omes-color-accent-purple-from);
}

.workshop-tag {
  max-width: 120px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  margin: 0;
  border-radius: 999px;
}

.toolbar-strip {
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-bottom: 10px;
  padding: 8px 10px;
  background: var(--omes-color-bg-elevated);
  border: 1px solid var(--omes-color-border);
  border-radius: var(--omes-radius-lg);
}

/* 查询/工具栏固定高度，表格区域独立 scroll 容器 */
.toolbar-strip--fixed {
  grid-row: 1;
  height: var(--equip-toolbar-height);
  min-height: var(--equip-toolbar-height);
  max-height: var(--equip-toolbar-height);
  margin-bottom: 0;
  overflow: hidden;
  contain: layout style;
  box-sizing: border-box;
}

.toolbar-row--filters {
  display: flex;
  align-items: center;
  flex-wrap: nowrap;
  gap: 8px;
  min-width: 0;
  height: 40px;
  flex: 0 0 40px;
  overflow-x: auto;
  overflow-y: hidden;
}

.toolbar-row--actions {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 8px;
  height: 36px;
  flex: 0 0 36px;
  padding-top: 0;
  border-top: 1px dashed var(--omes-color-border-hover);
  flex-shrink: 0;
}

.search-form {
  flex: 1;
  min-width: 0;
  margin-bottom: 0;
}

.search-form--compact :deep(.ant-form-item) {
  margin-inline-end: 8px;
  margin-bottom: 0;
}

.search-input {
  width: 120px;
}

.search-select {
  width: 108px;
}

.search-select-wide {
  width: 120px;
}

.search-actions {
  margin-inline-end: 0 !important;
}

.list-card-inner :deep(.table-scroll-wrap) {
  grid-row: 2;
  min-height: 0;
  align-self: stretch;
}

.equip-table :deep(.ant-spin-blur) {
  clear: none;
}

.equip-table :deep(.ant-table-content) {
  border-radius: var(--omes-radius-md);
}

.equip-table :deep(.ant-table-thead > tr > th) {
  background: var(--omes-color-bg-elevated);
  font-weight: 600;
}

.equip-table :deep(.ant-table-tbody > tr > td) {
  vertical-align: middle;
  cursor: pointer;
}

.equip-table :deep(.ant-table-tbody > tr:hover > td) {
  background: var(--omes-color-primary-bg-hover);
}

.equip-table :deep(.equip-row--selected > td) {
  background: var(--omes-color-primary-bg) !important;
}

.equip-table :deep(.equip-row--selected > td:first-child) {
  box-shadow: inset 3px 0 0 var(--omes-color-primary);
}

.name-cell {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  max-width: 100%;
  min-width: 0;
  font-weight: 500;
  color: var(--omes-color-text);
}

.name-cell__thumb {
  flex-shrink: 0;
  width: 32px;
  height: 32px;
  border-radius: 6px;
  border: 1px solid #e5e7eb;
  overflow: hidden;
}

.name-cell__text {
  min-width: 0;
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
  background: #f0f5ff;
  padding: 2px 8px;
  border-radius: 4px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  vertical-align: middle;
}

.text-muted {
  color: var(--omes-color-text-quaternary);
}

.status-tag {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  margin: 0;
  max-width: 100%;
}

.tag-icon {
  font-size: 12px;
}

.detail-card :deep(.ant-card-body) {
  padding-top: 12px;
}

.detail-body {
  display: flex;
  flex-direction: column;
  gap: 12px;
  flex: 1;
  min-height: 0;
  overflow-y: auto;
}

.detail-col .action-grid {
  grid-template-columns: 1fr;
}

.detail-col .action-grid--pair {
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px;
}

.detail-hero {
  display: flex;
  gap: 10px;
  padding: 12px;
  border-radius: 12px;
  background: linear-gradient(135deg, #f9f0ff 0%, #fcfbff 48%, #fff 100%);
  border: 1px solid #efdbff;
}

.detail-hero__icon {
  flex-shrink: 0;
  width: 56px;
  height: 56px;
  border-radius: var(--omes-radius-lg);
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  font-size: 18px;
  background: var(--omes-color-bg-container);
  color: var(--omes-color-accent-purple-from);
  box-shadow: 0 4px 12px rgba(114, 46, 209, 0.15);
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

.type-tag {
  margin: 0;
  border-radius: 999px;
  max-width: 100%;
  overflow: hidden;
  text-overflow: ellipsis;
}

.detail-name {
  margin: 0 0 6px;
  font-size: 15px;
  font-weight: 600;
  line-height: 1.35;
  color: var(--omes-color-text);
  word-break: break-word;
}

.detail-code {
  display: inline-block;
  max-width: 100%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  vertical-align: middle;
  font-family: ui-monospace, SFMono-Regular, 'SF Mono', Menlo, Consolas, monospace;
  font-size: 11px;
  color: var(--omes-color-primary);
  background: rgba(255, 255, 255, 0.85);
  padding: 4px 10px;
  border-radius: var(--omes-radius-sm);
  border: 1px solid rgba(22, 119, 255, 0.2);
}

.detail-workshop {
  display: block;
  margin-top: 8px;
  font-size: 13px;
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
  grid-template-columns: repeat(2, 1fr);
  gap: 10px;
}

.action-grid--triple {
  grid-template-columns: repeat(3, 1fr);
}

.action-grid--pair {
  grid-template-columns: repeat(2, 1fr);
}

.detail-col .action-tile {
  min-height: 56px;
  padding: 8px 4px;
}

.detail-col .action-tile__icon {
  width: 30px;
  height: 30px;
  font-size: 15px;
}

.detail-col .action-tile__text {
  font-size: 12px;
}

.action-tile {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
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

.action-tile:hover:not(.action-tile--disabled) {
  border-color: var(--omes-color-primary-border);
  box-shadow: 0 4px 12px rgba(22, 119, 255, 0.1);
  transform: translateY(-1px);
}

.action-tile:active:not(.action-tile--disabled) {
  transform: translateY(0);
}

.action-tile--disabled {
  opacity: 0.45;
  cursor: not-allowed;
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

.action-tile__icon--green {
  background: #f6ffed;
  color: var(--omes-color-success);
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
  font-size: 13px;
  color: var(--omes-color-text-label);
  line-height: 1.2;
  text-align: center;
}

.detail-danger {
  margin-top: auto;
  padding-top: 4px;
}

.detail-col .detail-danger :deep(.ant-btn) {
  height: 36px;
  font-size: 13px;
  border-radius: var(--omes-radius-md);
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
  color: #b37feb;
  background: linear-gradient(135deg, #f9f0ff 0%, #f0f5ff 100%);
}

.placeholder-text {
  margin: 0;
  max-width: 220px;
  font-size: 14px;
  color: var(--omes-color-text-quaternary);
  line-height: 1.6;
}


@media (max-width: 992px) {
  .equip-page {
    height: auto;
    max-height: none;
    overflow: visible;
  }

  .equip-layout {
    flex: none;
    overflow: visible;
  }

  .sidebar-col,
  .list-col,
  .detail-col {
    height: auto;
  }

  .panel-card {
    height: auto;
  }

  .sidebar-card :deep(.ant-card-body) {
    max-height: 480px;
  }

  .table-scroll-wrap {
    overflow: visible;
    min-height: 320px;
  }

  .sidebar-col .panel-card,
  .list-col .panel-card {
    margin-bottom: 16px;
  }

  .toolbar-row--actions {
    justify-content: stretch;
  }

  .toolbar-row--actions .ant-btn {
    flex: 1;
  }

  .search-input,
  .search-select,
  .search-select-wide {
    width: 100%;
  }
}

@media (max-width: 768px) {
  .search-input,
  .search-select,
  .search-select-wide {
    width: 100%;
  }

  .workshop-tag {
    max-width: 80px;
  }
}

@media (max-width: 576px) {
  .action-grid,
  .action-grid--triple,
  .action-grid--pair,
  .detail-col .action-grid--pair {
    grid-template-columns: 1fr;
  }
}
</style>
