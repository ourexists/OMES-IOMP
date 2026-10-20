<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import {
  CheckOutlined,
  CloseOutlined,
  DeleteOutlined,
  EditOutlined,
  FormOutlined,
  PlusOutlined,
  ReloadOutlined,
  SearchOutlined,
} from '@ant-design/icons-vue'
import type { InspectItemRecord } from '@/api/inspect-item'
import { deleteInspectItems, fetchInspectItemPage, inspectItemTypeLabel } from '@/api/inspect-item'
import InspectItemFormModal from './components/InspectItemFormModal.vue'
import TableScrollWrap from '@/components/common/TableScrollWrap.vue'
import AdminPanelTitle from '@/components/admin/AdminPanelTitle.vue'
import { message, Modal } from 'ant-design-vue'
import type { TablePaginationConfig } from 'ant-design-vue/es/table'

const { t } = useI18n()

const loading = ref(false)
const dataSource = ref<InspectItemRecord[]>([])
const selectedRowKeys = ref<string[]>([])
const pagination = reactive({ current: 1, pageSize: 10, total: 0 })

const searchForm = reactive({
  itemName: '',
})

const formOpen = ref(false)
const editingRecord = ref<InspectItemRecord | null>(null)

const columns = computed(() => [
  { title: t('inspectItemPage.colName'), dataIndex: 'itemName', key: 'itemName', ellipsis: true, width: 220 },
  { title: t('inspectItemPage.colType'), key: 'itemType', width: 110, align: 'center' as const },
  { title: t('inspectItemPage.colUnit'), dataIndex: 'unit', key: 'unit', width: 180, ellipsis: true },
  { title: t('inspectItemPage.colMin'), dataIndex: 'minValue', key: 'minValue', width: 100 },
  { title: t('inspectItemPage.colMax'), dataIndex: 'maxValue', key: 'maxValue', width: 100 },
  { title: t('inspectItemPage.colRequired'), key: 'requiredFlag', width: 90, align: 'center' as const },
  { title: t('inspectItemPage.colAction'), key: 'action', width: 150, fixed: 'right' as const },
])

const rowSelection = computed(() => ({
  selectedRowKeys: selectedRowKeys.value,
  onChange: (keys: string[]) => {
    selectedRowKeys.value = keys
  },
}))

const hasSelection = computed(() => selectedRowKeys.value.length > 0)

function typeTone(type?: number): string {
  if (type === 1) {
    return 'choice'
  }
  if (type === 3) {
    return 'bool'
  }
  return 'numeric'
}

function typeLabel(record: InspectItemRecord): string {
  if (record.itemTypeDesc) {
    const key = record.itemTypeDesc
    return t(`inspectItemPage.itemType.${key}`, key)
  }
  return inspectItemTypeLabel(record.itemType)
}

async function loadTable() {
  loading.value = true
  try {
    const result = await fetchInspectItemPage({
      page: pagination.current,
      pageSize: pagination.pageSize,
      itemName: searchForm.itemName.trim() || undefined,
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
  searchForm.itemName = ''
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

function openEdit(record: InspectItemRecord) {
  editingRecord.value = record
  formOpen.value = true
}

function confirmDelete(ids: string[]) {
  Modal.confirm({
    title: t('inspectItemPage.deleteConfirm'),
    content: t('inspectItemPage.deleteContent', { count: ids.length }),
    onOk: async () => {
      await deleteInspectItems(ids)
      message.success(t('inspectItemPage.deleteSuccess'))
      selectedRowKeys.value = []
      loadTable()
    },
  })
}

onMounted(loadTable)
</script>

<template>
  <div class="admin-page inspect-item-page">
    <a-card size="small" class="admin-panel-card panel-card">
      <template #title>
        <AdminPanelTitle>
          <template #icon><FormOutlined /></template>
          {{ t('inspectItemPage.title') }}
        </AdminPanelTitle>
      </template>
      <template #extra>
        <a-space :size="8">
          <a-tag v-if="hasSelection" color="blue">
            {{ t('inspectItemPage.selectedCount', { count: selectedRowKeys.length }) }}
          </a-tag>
          <a-tag v-if="pagination.total" color="processing">
            {{ t('inspectItemPage.total', { count: pagination.total }) }}
          </a-tag>
        </a-space>
      </template>

      <div class="admin-panel-body">
        <div class="search-toolbar search-toolbar--compact">
          <a-form layout="inline" class="search-form" :model="searchForm" @finish="onSearch">
            <a-form-item :label="t('inspectItemPage.colName')" name="itemName">
              <a-input size="small"
                v-model:value="searchForm.itemName"
                allow-clear
                :placeholder="t('inspectItemPage.searchName')"
                class="search-input"
              >
                <template #prefix>
                  <SearchOutlined class="input-prefix-icon" />
                </template>
              </a-input>
            </a-form-item>
            <CompactSearchActions
              :query-title="t('inspectItemPage.query')"
              :reset-title="t('inspectItemPage.reset')"
              @reset="onReset"
            />
          </a-form>
        </div>

        <div class="table-toolbar">
          <a-space wrap>
            <a-button type="primary" @click="openCreate">
              <template #icon><PlusOutlined /></template>
              {{ t('inspectItemPage.add') }}
            </a-button>
            <a-button danger :disabled="!hasSelection" @click="confirmDelete(selectedRowKeys)">
              <template #icon><DeleteOutlined /></template>
              {{ t('inspectItemPage.batchDelete') }}
            </a-button>
            <a-button :loading="loading" @click="loadTable">
              <template #icon><ReloadOutlined /></template>
              {{ t('inspectItemPage.refresh') }}
            </a-button>
          </a-space>
        </div>

        <TableScrollWrap :refresh-keys="[dataSource.length, pagination.total]">
          <template #default="{ scrollY }">
            <a-table
              row-key="id"
              size="middle"
              bordered
              class="scroll-table inspect-item-table"
              :columns="columns"
              :data-source="dataSource"
              :row-selection="rowSelection"
              :scroll="{ x: 1040, y: scrollY }"
              :pagination="{
                current: pagination.current,
                pageSize: pagination.pageSize,
                total: pagination.total,
                showSizeChanger: true,
                showTotal: (total: number) => t('inspectItemPage.paginationTotal', { total }),
              }"
              @change="onTableChange"
            >
              <template #bodyCell="{ column, record }">
                <template v-if="column.key === 'itemName'">
                  <span class="item-name">{{ record.itemName || '—' }}</span>
                </template>
                <template v-else-if="column.key === 'itemType'">
                  <span class="type-tag" :class="`type-tag--${typeTone(record.itemType)}`">
                    {{ typeLabel(record) }}
                  </span>
                </template>
                <template v-else-if="column.key === 'minValue'">
                  {{ record.itemType === 2 && record.minValue != null ? record.minValue : '-' }}
                </template>
                <template v-else-if="column.key === 'maxValue'">
                  {{ record.itemType === 2 && record.maxValue != null ? record.maxValue : '-' }}
                </template>
                <template v-else-if="column.key === 'requiredFlag'">
                  <template v-if="record.itemType === 2">
                    <CheckOutlined v-if="record.requiredFlag" class="flag-yes" />
                    <CloseOutlined v-else class="flag-no" />
                  </template>
                  <span v-else class="text-muted">-</span>
                </template>
                <template v-else-if="column.key === 'action'">
                  <a-space wrap size="small" class="action-group">
                    <a-button type="link" size="small" @click="openEdit(record)">
                      <EditOutlined />
                      {{ t('inspectItemPage.edit') }}
                    </a-button>
                    <a-button type="link" size="small" danger @click="confirmDelete([record.id])">
                      <DeleteOutlined />
                      {{ t('inspectItemPage.delete') }}
                    </a-button>
                  </a-space>
                </template>
              </template>

              <template #emptyText>
                <a-empty :description="t('inspectItemPage.empty')">
                  <a-button type="primary" @click="openCreate">
                    <PlusOutlined />
                    {{ t('inspectItemPage.add') }}
                  </a-button>
                </a-empty>
              </template>
            </a-table>
          </template>
        </TableScrollWrap>
      </div>
    </a-card>

    <InspectItemFormModal v-model:open="formOpen" :record="editingRecord" @success="loadTable" />
  </div>
</template>

<style scoped>
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

.search-input {
  width: 220px;
}

.input-prefix-icon {
  color: var(--omes-color-text-placeholder);
}

.table-toolbar {
  margin-bottom: 16px;
}

.inspect-item-table :deep(.ant-table-thead > tr > th) {
  background: var(--omes-color-bg-elevated);
  font-weight: 600;
}

.item-name {
  font-weight: 600;
}

.type-tag {
  display: inline-flex;
  align-items: center;
  padding: 1px 8px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 500;
  line-height: 20px;
  border: 1px solid transparent;
}

.type-tag--choice {
  color: var(--omes-color-accent-purple-from);
  background: var(--omes-color-accent-purple-bg);
  border-color: var(--omes-color-accent-purple-border);
}

.type-tag--numeric {
  color: var(--omes-color-primary);
  background: var(--omes-color-primary-bg);
  border-color: var(--omes-color-primary-border);
}

.type-tag--bool {
  color: var(--omes-color-success);
  background: var(--omes-color-success-bg);
  border-color: var(--omes-color-success-border);
}

.flag-yes {
  color: var(--omes-color-success);
}

.flag-no {
  color: rgba(0, 0, 0, 0.25);
}

.text-muted {
  color: var(--omes-color-text-quaternary);
}

.action-group :deep(.ant-btn-link) {
  padding-inline: 4px;
}

.action-group :deep(.ant-btn-link .anticon) {
  margin-inline-end: 4px;
  font-size: 13px;
}
</style>
