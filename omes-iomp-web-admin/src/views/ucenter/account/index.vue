<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import {
  ApartmentOutlined,
  PlusOutlined,
  ReloadOutlined, UserOutlined,
} from '@ant-design/icons-vue'
import type { AccountRecord, PlatformNode } from '@/api/ucenter'
import { deleteAccounts, fetchAccountPage, frozenAccount, invokeAccount } from '@/api/ucenter'
import PlatformTree from '@/components/PlatformTree.vue'
import AccountFormModal from './components/AccountFormModal.vue'
import AccountRoleModal from './components/AccountRoleModal.vue'
import TableScrollWrap from '@/components/common/TableScrollWrap.vue'
import AdminPanelTitle from '@/components/admin/AdminPanelTitle.vue'
import { message, Modal } from 'ant-design-vue'
import type { TablePaginationConfig } from 'ant-design-vue/es/table'

const { t } = useI18n()

const selectedPlatform = ref<PlatformNode | null>(null)
const loading = ref(false)
const dataSource = ref<AccountRecord[]>([])
const selectedRowKeys = ref<string[]>([])
const pagination = reactive({ current: 1, pageSize: 10, total: 0 })

const searchForm = reactive({
  accName: '',
  mobile: '',
})

const formOpen = ref(false)
const roleOpen = ref(false)
const editingRecord = ref<AccountRecord | null>(null)
const roleAccountId = ref('')

const columns = computed(() => [
  { title: t('accountPage.colAccount'), dataIndex: 'accName', key: 'accName', width: 120, ellipsis: true },
  { title: t('accountPage.colNickname'), dataIndex: 'nickName', key: 'nickName', width: 100, ellipsis: true },
  { title: t('accountPage.colStatus'), dataIndex: 'statusDesc', key: 'statusDesc', width: 88, align: 'center' as const },
  { title: t('accountPage.colMobile'), dataIndex: 'mobile', key: 'mobile', width: 120, ellipsis: true },
  { title: t('accountPage.colSettledTime'), dataIndex: 'settledTime', key: 'settledTime', width: 156, ellipsis: true },
  { title: t('accountPage.colExpireTime'), dataIndex: 'expireTime', key: 'expireTime', width: 156, ellipsis: true },
  { title: t('accountPage.colSource'), dataIndex: 'source', key: 'source', width: 88, ellipsis: true },
  { title: t('accountPage.colAction'), key: 'action', width: 220 },
])

const rowSelection = computed(() => ({
  selectedRowKeys: selectedRowKeys.value,
  onChange: (keys: string[]) => {
    selectedRowKeys.value = keys
  },
}))

function statusTagColor(status?: number) {
  return status === 0 ? 'success' : 'default'
}

async function loadTable() {
  if (!selectedPlatform.value?.code) {
    dataSource.value = []
    pagination.total = 0
    selectedRowKeys.value = []
    return
  }
  loading.value = true
  try {
    const result = await fetchAccountPage({
      page: pagination.current,
      pageSize: pagination.pageSize,
      platform: selectedPlatform.value.code,
      accRole: 'COMMON',
      accName: searchForm.accName || undefined,
      mobile: searchForm.mobile || undefined,
    })
    dataSource.value = result.records || []
    pagination.total = result.total || 0
  } finally {
    loading.value = false
  }
}

function onPlatformChange() {
  pagination.current = 1
  selectedRowKeys.value = []
  loadTable()
}

function onSearch() {
  pagination.current = 1
  loadTable()
}

function onReset() {
  searchForm.accName = ''
  searchForm.mobile = ''
  onSearch()
}

function onTableChange(page: TablePaginationConfig) {
  pagination.current = page.current || 1
  pagination.pageSize = page.pageSize || 10
  loadTable()
}

function openCreate() {
  if (!selectedPlatform.value) {
    message.warning(t('accountPage.selectPlatform'))
    return
  }
  editingRecord.value = null
  formOpen.value = true
}

function openEdit(record: AccountRecord) {
  editingRecord.value = record
  formOpen.value = true
}

function openAssign(record: AccountRecord) {
  roleAccountId.value = record.id
  roleOpen.value = true
}

function confirmDelete(ids: string[]) {
  Modal.confirm({
    title: t('accountPage.deleteConfirm'),
    content: t('accountPage.deleteContent', { count: ids.length }),
    onOk: async () => {
      await deleteAccounts(ids)
      message.success(t('accountPage.deleteSuccess'))
      selectedRowKeys.value = []
      loadTable()
    },
  })
}

async function toggleStatus(record: AccountRecord) {
  if (record.status === 0) {
    await frozenAccount(record.id)
    message.success(t('accountPage.frozen'))
  } else {
    await invokeAccount(record.id)
    message.success(t('accountPage.enabled'))
  }
  loadTable()
}

watch(selectedPlatform, () => {
  if (selectedPlatform.value) {
    loadTable()
  }
})
</script>

<template>
  <div class="admin-page account-page">
    <a-row :gutter="16" class="admin-page-row page-body">
      <a-col :xs="24" :lg="5" class="admin-page-col">
        <a-card size="small" class="admin-panel-card panel-card platform-panel">
          <template #title>
            <AdminPanelTitle>
              <template #icon><ApartmentOutlined /></template>
              {{ t('permissionPage.platform') }}
            </AdminPanelTitle>
          </template>
          <PlatformTree v-model="selectedPlatform" @change="onPlatformChange" />
        </a-card>
      </a-col>

      <a-col :xs="24" :lg="19" class="admin-page-col">
        <a-card size="small" class="admin-panel-card panel-card account-panel">
          <template #title>
            <AdminPanelTitle>
              <template #icon><UserOutlined /></template>
              {{ t('accountPage.title') }}
            </AdminPanelTitle>
          </template>
          <template #extra>
            <a-tag v-if="selectedPlatform" color="processing">
              {{ selectedPlatform.name }}
            </a-tag>
            <a-tag v-if="selectedPlatform && pagination.total">
              {{ t('accountPage.total', { count: pagination.total }) }}
            </a-tag>
          </template>

          <template v-if="selectedPlatform">
            <div class="admin-panel-body">
            <div class="search-toolbar search-toolbar--compact">
              <a-form layout="inline" class="search-form" :model="searchForm" @finish="onSearch">
                <a-form-item :label="t('accountPage.account')" name="accName">
                  <a-input size="small"
                    v-model:value="searchForm.accName"
                    allow-clear
                    :placeholder="t('accountPage.accountPlaceholder')"
                    class="search-input"
                  />
                </a-form-item>
                <a-form-item :label="t('accountPage.mobile')" name="mobile">
                  <a-input size="small"
                    v-model:value="searchForm.mobile"
                    allow-clear
                    :placeholder="t('accountPage.mobilePlaceholder')"
                    class="search-input"
                  />
                </a-form-item>
                <CompactSearchActions
              :query-title="t('accountPage.query')"
              :reset-title="t('accountPage.reset')"
              @reset="onReset"
            />
              </a-form>
            </div>

            <div class="table-toolbar">
              <a-space wrap>
                <a-button type="primary" @click="openCreate">
                  <template #icon><PlusOutlined /></template>
                  {{ t('accountPage.add') }}
                </a-button>
                <a-button
                  danger
                  :disabled="!selectedRowKeys.length"
                  @click="confirmDelete(selectedRowKeys)"
                >
                  {{ t('accountPage.batchDelete') }}
                </a-button>
                <a-button :loading="loading" @click="loadTable">
                  <template #icon><ReloadOutlined /></template>
                  {{ t('accountPage.refresh') }}
                </a-button>
              </a-space>
            </div>

            <TableScrollWrap :refresh-keys="[dataSource.length, pagination.total]">
              <template #default="{ scrollY }">
            <a-table
              row-key="id"
              size="middle"
              bordered
              class="scroll-table account-table"
              :columns="columns"
              :data-source="dataSource"
              :row-selection="rowSelection"
              :scroll="{ y: scrollY }"
              :pagination="{
                current: pagination.current,
                pageSize: pagination.pageSize,
                total: pagination.total,
                showSizeChanger: true,
                showTotal: (total: number) => t('accountPage.paginationTotal', { total }),
              }"
              @change="onTableChange"
            >
              <template #bodyCell="{ column, record }">
                <template v-if="column.key === 'accName'">
                  <span class="account-cell">{{ record.accName || '—' }}</span>
                </template>
                <template v-else-if="column.key === 'statusDesc'">
                  <a-tag :color="statusTagColor(record.status)">
                    {{ record.statusDesc || (record.status === 0 ? t('accountPage.statusActive') : t('accountPage.statusFrozen')) }}
                  </a-tag>
                </template>
                <template v-else-if="column.key === 'source'">
                  {{ record.source || '—' }}
                </template>
                <template v-else-if="column.key === 'action'">
                  <a-space size="small" class="action-group">
                    <a-button type="link" size="small" @click="openEdit(record)">
                      {{ t('accountPage.edit') }}
                    </a-button>
                    <a-button type="link" size="small" @click="toggleStatus(record)">
                      {{ record.status === 0 ? t('accountPage.freeze') : t('accountPage.enable') }}
                    </a-button>
                    <a-button type="link" size="small" @click="openAssign(record)">
                      {{ t('accountPage.assignRole') }}
                    </a-button>
                    <a-button type="link" size="small" danger @click="confirmDelete([record.id])">
                      {{ t('accountPage.delete') }}
                    </a-button>
                  </a-space>
                </template>
              </template>

              <template #emptyText>
                <a-empty :description="t('accountPage.empty')" />
              </template>
            </a-table>
              </template>
            </TableScrollWrap>
            </div>
          </template>

          <div v-else class="empty-panel">
            <a-empty :description="t('accountPage.selectPlatformHint')" />
          </div>
        </a-card>
      </a-col>
    </a-row>

    <AccountFormModal
      v-model:open="formOpen"
      :record="editingRecord"
      :platform="selectedPlatform"
      @success="loadTable"
    />
    <AccountRoleModal v-model:open="roleOpen" :account-id="roleAccountId" />
  </div>
</template>

<style scoped>
.account-page {
  /* layout via .admin-page */
}

.page-body {
  flex: 1;
  min-height: 0;
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

.account-panel :deep(.ant-card-body) {
  display: flex;
  flex-direction: column;
  min-height: calc(100vh - 280px);
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

.account-table {
  flex: 1;
}

.account-table :deep(.ant-table) {
  border-radius: var(--omes-radius-md);
  overflow: hidden;
}

.account-table :deep(.ant-table-thead > tr > th) {
  background: var(--omes-color-bg-elevated);
  font-weight: 600;
}

.account-table :deep(.ant-table-tbody > tr:hover > td) {
  background: var(--omes-color-primary-bg-hover);
}

.account-cell {
  font-weight: 500;
  color: var(--omes-color-text);
}

.action-group {
  white-space: nowrap;
}

.action-group :deep(.ant-btn-link) {
  padding-inline: 2px;
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
  .account-panel :deep(.ant-card-body) {
    min-height: auto;
  }

  .search-input {
    width: 100%;
  }
}
</style>
