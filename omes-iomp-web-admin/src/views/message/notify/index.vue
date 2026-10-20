<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import {
  BellOutlined,
  CheckOutlined,
  PlusOutlined,
  PlayCircleOutlined,
  ReloadOutlined } from '@ant-design/icons-vue'
import type { PlatformNode } from '@/api/ucenter'
import { fetchPlatforms } from '@/api/ucenter'
import {
  completeNotify,
  deleteNotifies,
  fetchNotifyPage,
  fetchNotifyStatusMap,
  fetchMessageTypeMap,
  startNotify,
} from '@/api/notify'
import type { NotifyRecord } from '@/types/notify'
import { NOTIFY_STATUS } from '@/types/notify'
import NotifyFormModal from './components/NotifyFormModal.vue'
import AdminPanelTitle from '@/components/admin/AdminPanelTitle.vue'
import TableScrollWrap from '@/components/common/TableScrollWrap.vue'
import { message, Modal } from 'ant-design-vue'
import type { TablePaginationConfig } from 'ant-design-vue/es/table'

const { t } = useI18n()

const loading = ref(false)
const dataSource = ref<NotifyRecord[]>([])
const selectedRowKeys = ref<string[]>([])
const pagination = reactive({ current: 1, pageSize: 10, total: 0 })

const statusOptions = ref<{ value: number; label: string }[]>([])
const typeOptions = ref<{ value: number; label: string }[]>([])
const platformOptions = ref<PlatformNode[]>([])

const searchForm = reactive({
  type: undefined as number | undefined,
  status: undefined as number | undefined,
  platform: undefined as string | undefined,
})

const formOpen = ref(false)
const editingRecord = ref<NotifyRecord | null>(null)

const columns = computed(() => [
  { title: 'ID', dataIndex: 'id', key: 'id', width: 200, ellipsis: true },
  { title: t('notifyPage.colTitle'), dataIndex: 'title', key: 'title', width: 160, ellipsis: true },
  { title: t('notifyPage.colContent'), dataIndex: 'context', key: 'context', ellipsis: true },
  { title: t('notifyPage.colType'), dataIndex: 'typeDesc', key: 'typeDesc', width: 110 },
  { title: t('notifyPage.colStatus'), dataIndex: 'statusDesc', key: 'statusDesc', width: 120 },
  { title: t('notifyPage.colInterval'), dataIndex: 'step', key: 'step', width: 110 },
  { title: t('notifyPage.colAction'), key: 'action', width: 200 },
])

const rowSelection = computed(() => ({
  selectedRowKeys: selectedRowKeys.value,
  onChange: (keys: string[]) => {
    selectedRowKeys.value = keys
  },
}))

function statusColor(status?: number): string {
  if (status === NOTIFY_STATUS.PROGRESS) {
    return 'processing'
  }
  if (status === NOTIFY_STATUS.COMPLETED) {
    return 'success'
  }
  return 'default'
}

function statusLabel(record: NotifyRecord): string {
  const key = record.statusDesc || ''
  if (key && t(`notifyPage.status.${key}`, key) !== key) {
    return t(`notifyPage.status.${key}`)
  }
  return key || '-'
}

function typeLabel(record: NotifyRecord): string {
  const key = record.typeDesc || ''
  if (key && t(`notifyPage.messageType.${key}`, key) !== key) {
    return t(`notifyPage.messageType.${key}`)
  }
  return key || '-'
}

async function loadFilters() {
  const [statusMap, typeMap, platforms] = await Promise.all([
    fetchNotifyStatusMap(),
    fetchMessageTypeMap(),
    fetchPlatforms(),
  ])
  statusOptions.value = Object.entries(statusMap || {}).map(([code, label]) => ({
    value: Number(code),
    label,
  }))
  typeOptions.value = Object.entries(typeMap || {}).map(([code, label]) => ({
    value: Number(code),
    label,
  }))
  platformOptions.value = platforms || []
}

async function loadTable() {
  loading.value = true
  try {
    const result = await fetchNotifyPage({
      page: pagination.current,
      pageSize: pagination.pageSize,
      type: searchForm.type,
      status: searchForm.status,
      platform: searchForm.platform,
    })
    dataSource.value = result.records || []
    pagination.total = result.total || 0
  } finally {
    loading.value = false
  }
}

function onSearch() {
  pagination.current = 1
  loadTable()
}

function onReset() {
  searchForm.type = undefined
  searchForm.status = undefined
  searchForm.platform = undefined
  onSearch()
}

function onTableChange(page: TablePaginationConfig) {
  pagination.current = page.current || 1
  pagination.pageSize = page.pageSize || 10
  loadTable()
}

function openCreate() {
  editingRecord.value = null
  formOpen.value = true
}

function openEdit(record: NotifyRecord) {
  editingRecord.value = record
  formOpen.value = true
}

function confirmDelete(ids: string[]) {
  Modal.confirm({
    title: t('notifyPage.deleteConfirm'),
    content: t('notifyPage.deleteContent', { count: ids.length }),
    onOk: async () => {
      await deleteNotifies(ids)
      message.success(t('notifyPage.deleteSuccess'))
      selectedRowKeys.value = []
      loadTable()
    },
  })
}

async function handleStart(id: string) {
  await startNotify(id)
  message.success(t('notifyPage.startSuccess'))
  loadTable()
}

async function handleComplete(id: string) {
  await completeNotify(id)
  message.success(t('notifyPage.completeSuccess'))
  loadTable()
}

onMounted(async () => {
  await loadFilters()
  await loadTable()
})
</script>

<template>
  <div class="admin-page notify-page">
    <a-card size="small" class="admin-panel-card panel-card">
      <template #title>
        <AdminPanelTitle>
          <template #icon><BellOutlined /></template>
          {{ t('notifyPage.title') }}
        </AdminPanelTitle>
      </template>
      <template #extra>
        <a-tag v-if="pagination.total" color="processing">
          {{ t('notifyPage.total', { count: pagination.total }) }}
        </a-tag>
      </template>

      <div class="admin-panel-body">
      <div class="search-toolbar search-toolbar--compact">
        <a-form layout="inline" class="search-form" :model="searchForm" @finish="onSearch">
          <a-form-item :label="t('notifyPage.colType')" name="type">
            <a-select size="small"
              v-model:value="searchForm.type"
              allow-clear
              :placeholder="t('notifyPage.colType')"
              :options="typeOptions"
              class="search-select"
            />
          </a-form-item>
          <a-form-item :label="t('notifyPage.colStatus')" name="status">
            <a-select size="small"
              v-model:value="searchForm.status"
              allow-clear
              :placeholder="t('notifyPage.colStatus')"
              :options="statusOptions"
              class="search-select"
            />
          </a-form-item>
          <a-form-item :label="t('notifyPage.colPlatforms')" name="platform">
            <a-select size="small"
              v-model:value="searchForm.platform"
              allow-clear
              :placeholder="t('notifyPage.colPlatforms')"
              class="search-select"
            >
              <a-select-option
                v-for="item in platformOptions"
                :key="item.code"
                :value="item.code"
              >
                {{ item.name }}
              </a-select-option>
            </a-select>
          </a-form-item>
          <CompactSearchActions
              :query-title="t('notifyPage.query')"
              :reset-title="t('notifyPage.reset')"
              @reset="onReset"
            />
        </a-form>
      </div>

      <div class="table-toolbar">
        <a-space wrap>
          <a-button type="primary" @click="openCreate">
            <template #icon><PlusOutlined /></template>
            {{ t('notifyPage.add') }}
          </a-button>
          <a-button danger :disabled="!selectedRowKeys.length" @click="confirmDelete(selectedRowKeys)">
            {{ t('notifyPage.batchDelete') }}
          </a-button>
          <a-button :loading="loading" @click="loadTable">
            <template #icon><ReloadOutlined /></template>
            {{ t('notifyPage.refresh') }}
          </a-button>
        </a-space>
      </div>

      <TableScrollWrap :refresh-keys="[dataSource.length, pagination.total]">
        <template #default="{ scrollY }">
      <a-table
        row-key="id"
        size="middle"
        bordered
        class="scroll-table notify-table"
        :columns="columns"
        :data-source="dataSource"
        :row-selection="rowSelection"
        :scroll="{ y: scrollY }"
        :pagination="{
          current: pagination.current,
          pageSize: pagination.pageSize,
          total: pagination.total,
          showSizeChanger: true,
          showTotal: (total: number) => t('notifyPage.paginationTotal', { total }),
        }"
        @change="onTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'title'">
            <span class="title-cell">{{ record.title || '—' }}</span>
          </template>
          <template v-else-if="column.key === 'context'">
            <span class="context-cell">{{ record.context || '—' }}</span>
          </template>
          <template v-else-if="column.key === 'typeDesc'">
            <a-tag>{{ typeLabel(record) }}</a-tag>
          </template>
          <template v-else-if="column.key === 'statusDesc'">
            <a-tag :color="statusColor(record.status)">{{ statusLabel(record) }}</a-tag>
          </template>
          <template v-else-if="column.key === 'step'">
            {{ record.step ?? 0 }}
          </template>
          <template v-else-if="column.key === 'action'">
            <a-space size="small" class="action-group">
              <a-button
                v-if="record.status === NOTIFY_STATUS.READY"
                type="link"
                size="small"
                @click="openEdit(record)"
              >
                {{ t('notifyPage.edit') }}
              </a-button>
              <a-button
                v-if="record.status === NOTIFY_STATUS.READY"
                type="link"
                size="small"
                @click="handleStart(record.id)"
              >
                <PlayCircleOutlined />
                {{ t('notifyPage.start') }}
              </a-button>
              <a-button
                v-if="record.status === NOTIFY_STATUS.PROGRESS"
                type="link"
                size="small"
                @click="handleComplete(record.id)"
              >
                <CheckOutlined />
                {{ t('notifyPage.complete') }}
              </a-button>
              <a-button type="link" size="small" danger @click="confirmDelete([record.id])">
                {{ t('notifyPage.delete') }}
              </a-button>
            </a-space>
          </template>
        </template>

        <template #emptyText>
          <a-empty :description="t('notifyPage.empty')" />
        </template>
      </a-table>
        </template>
      </TableScrollWrap>
      </div>
    </a-card>

    <NotifyFormModal v-model:open="formOpen" :record="editingRecord" @success="loadTable" />
  </div>
</template>

<style scoped>
.notify-page {
  /* layout via .admin-page */
}

.panel-card {
  border-radius: var(--omes-radius-md);
  box-shadow: var(--omes-shadow-card-sm);
}

.panel-card :deep(.ant-card-head) {
  min-height: 48px;
  border-bottom: 1px solid var(--omes-color-border);
}

.card-title {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  font-weight: 600;
}

.search-toolbar {
  margin-bottom: 12px;
  padding: 12px 16px;
  background: var(--omes-color-bg-elevated);
  border: 1px solid var(--omes-color-border);
  border-radius: var(--omes-radius-md);
}

.search-form {
  margin-bottom: 0;
}

.search-select {
  width: 160px;
}

.table-toolbar {
  margin-bottom: 16px;
}

.notify-table :deep(.ant-table) {
  border-radius: var(--omes-radius-md);
  overflow: hidden;
}

.notify-table :deep(.ant-table-thead > tr > th) {
  background: var(--omes-color-bg-elevated);
  font-weight: 600;
}

.notify-table :deep(.ant-table-tbody > tr:hover > td) {
  background: var(--omes-color-primary-bg-hover);
}

.title-cell {
  font-weight: 500;
  color: var(--omes-color-text);
}

.context-cell {
  color: var(--omes-color-text-secondary);
}

.action-group :deep(.ant-btn-link) {
  padding-inline: 4px;
}

@media (max-width: 768px) {
  .search-select {
    width: 100%;
  }
}
</style>
