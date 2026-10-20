<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { PlusOutlined, ReloadOutlined, TeamOutlined } from '@ant-design/icons-vue'
import type { RoleRecord } from '@/api/ucenter'
import { deleteRoles, fetchRolePage } from '@/api/ucenter'
import RoleFormModal from './components/RoleFormModal.vue'
import RoleAssignModal from './components/RoleAssignModal.vue'
import TableScrollWrap from '@/components/common/TableScrollWrap.vue'
import AdminPanelTitle from '@/components/admin/AdminPanelTitle.vue'
import { message, Modal } from 'ant-design-vue'
import type { TablePaginationConfig } from 'ant-design-vue/es/table'

const { t } = useI18n()

const loading = ref(false)
const dataSource = ref<RoleRecord[]>([])
const selectedRowKeys = ref<string[]>([])
const pagination = reactive({ current: 1, pageSize: 10, total: 0 })

const searchForm = reactive({
  name: '',
  code: '',
})

const formOpen = ref(false)
const assignOpen = ref(false)
const editingRecord = ref<RoleRecord | null>(null)
const assignRoleId = ref('')

const columns = computed(() => [
  { title: t('rolePage.colCode'), dataIndex: 'code', key: 'code', width: 160 },
  { title: t('rolePage.colName'), dataIndex: 'name', key: 'name', width: 180, ellipsis: true },
  { title: t('rolePage.colDescription'), dataIndex: 'description', key: 'description', ellipsis: true },
  { title: t('rolePage.colAction'), key: 'action', width: 260, fixed: 'right' as const },
])

const rowSelection = computed(() => ({
  selectedRowKeys: selectedRowKeys.value,
  onChange: (keys: string[]) => {
    selectedRowKeys.value = keys
  },
}))

async function loadTable() {
  loading.value = true
  try {
    const result = await fetchRolePage({
      page: pagination.current,
      pageSize: pagination.pageSize,
      name: searchForm.name || undefined,
      code: searchForm.code || undefined,
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
  searchForm.name = ''
  searchForm.code = ''
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

function openEdit(record: RoleRecord) {
  editingRecord.value = record
  formOpen.value = true
}

function openAssign(record: RoleRecord) {
  assignRoleId.value = record.id
  assignOpen.value = true
}

function confirmDelete(ids: string[]) {
  Modal.confirm({
    title: t('rolePage.deleteConfirm'),
    content: t('rolePage.deleteContent', { count: ids.length }),
    onOk: async () => {
      await deleteRoles(ids)
      message.success(t('rolePage.deleteSuccess'))
      selectedRowKeys.value = []
      loadTable()
    },
  })
}

loadTable()
</script>

<template>
  <div class="admin-page role-page">
    <a-card size="small" class="admin-panel-card panel-card">
      <template #title>
        <AdminPanelTitle>
          <template #icon><TeamOutlined /></template>
          {{ t('rolePage.title') }}
        </AdminPanelTitle>
      </template>
      <template #extra>
        <a-tag v-if="pagination.total" color="processing">
          {{ t('rolePage.total', { count: pagination.total }) }}
        </a-tag>
      </template>

      <div class="admin-panel-body">
      <div class="search-toolbar search-toolbar--compact">
        <a-form layout="inline" class="search-form" :model="searchForm" @finish="onSearch">
          <a-form-item :label="t('rolePage.name')" name="name">
            <a-input size="small"
              v-model:value="searchForm.name"
              allow-clear
              :placeholder="t('rolePage.searchName')"
              class="search-input"
            />
          </a-form-item>
          <a-form-item :label="t('rolePage.code')" name="code">
            <a-input size="small"
              v-model:value="searchForm.code"
              allow-clear
              :placeholder="t('rolePage.searchCode')"
              class="search-input"
            />
          </a-form-item>
          <CompactSearchActions
              :query-title="t('rolePage.query')"
              :reset-title="t('rolePage.reset')"
              @reset="onReset"
            />
        </a-form>
      </div>

      <div class="table-toolbar">
        <a-space wrap>
          <a-button type="primary" @click="openCreate">
            <template #icon><PlusOutlined /></template>
            {{ t('rolePage.add') }}
          </a-button>
          <a-button danger :disabled="!selectedRowKeys.length" @click="confirmDelete(selectedRowKeys)">
            {{ t('rolePage.batchDelete') }}
          </a-button>
          <a-button :loading="loading" @click="loadTable">
            <template #icon><ReloadOutlined /></template>
            {{ t('rolePage.refresh') }}
          </a-button>
        </a-space>
      </div>

      <TableScrollWrap :refresh-keys="[dataSource.length, pagination.total]">
        <template #default="{ scrollY }">
      <a-table
        row-key="id"
        size="middle"
        bordered
        class="scroll-table role-table"
        :columns="columns"
        :data-source="dataSource"
        :row-selection="rowSelection"
        :scroll="{ x: 900, y: scrollY }"
        :pagination="{
          current: pagination.current,
          pageSize: pagination.pageSize,
          total: pagination.total,
          showSizeChanger: true,
          showTotal: (total: number) => t('rolePage.paginationTotal', { total }),
        }"
        @change="onTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'code'">
            <span class="code-cell">{{ record.code }}</span>
          </template>
          <template v-else-if="column.key === 'name'">
            <span class="name-cell">{{ record.name }}</span>
          </template>
          <template v-else-if="column.key === 'description'">
            <span class="desc-cell">{{ record.description || '—' }}</span>
          </template>
          <template v-else-if="column.key === 'action'">
            <a-space wrap size="small" class="action-group">
              <a-button type="link" size="small" @click="openEdit(record)">
                {{ t('rolePage.edit') }}
              </a-button>
              <a-button type="link" size="small" @click="openAssign(record)">
                {{ t('rolePage.assign') }}
              </a-button>
              <a-button type="link" size="small" danger @click="confirmDelete([record.id])">
                {{ t('rolePage.delete') }}
              </a-button>
            </a-space>
          </template>
        </template>

        <template #emptyText>
          <a-empty :description="t('rolePage.empty')" />
        </template>
      </a-table>
        </template>
      </TableScrollWrap>
      </div>
    </a-card>

    <RoleFormModal v-model:open="formOpen" :record="editingRecord" @success="loadTable" />
    <RoleAssignModal v-model:open="assignOpen" :role-id="assignRoleId" />
  </div>
</template>

<style scoped>
.role-page {
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

.search-input {
  width: 160px;
}

.table-toolbar {
  margin-bottom: 16px;
}

.role-table :deep(.ant-table) {
  border-radius: var(--omes-radius-md);
  overflow: hidden;
}

.role-table :deep(.ant-table-thead > tr > th) {
  background: var(--omes-color-bg-elevated);
  font-weight: 600;
}

.role-table :deep(.ant-table-tbody > tr:hover > td) {
  background: var(--omes-color-primary-bg-hover);
}

.code-cell {
  display: inline-block;
  font-family: ui-monospace, SFMono-Regular, 'SF Mono', Menlo, Consolas, monospace;
  font-size: 12px;
  line-height: 20px;
  color: var(--omes-color-primary);
  background: var(--omes-color-primary-bg);
  border: 1px solid var(--omes-color-primary-border);
  padding: 0 8px;
  border-radius: 4px;
  white-space: nowrap;
}

.name-cell {
  font-weight: 500;
  color: var(--omes-color-text);
}

.desc-cell {
  color: var(--omes-color-text-secondary);
}

.action-group :deep(.ant-btn-link) {
  padding-inline: 4px;
}

@media (max-width: 768px) {
  .search-input {
    width: 100%;
  }
}
</style>
