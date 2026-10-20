<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRoute, useRouter } from 'vue-router'
import {
  CarryOutOutlined,
  DeleteOutlined,
  EyeOutlined,
  RedoOutlined,
  ReloadOutlined,
  SearchOutlined,
  UserAddOutlined,
} from '@ant-design/icons-vue'
import type { InspectTaskRecord } from '@/api/inspect-task'
import {
  deleteInspectTasks,
  fetchInspectTaskPage,
  fetchInspectTaskStatusTypes,
  inspectTaskStatusLabel,
  restartOverdueInspectTasks,
} from '@/api/inspect-task'
import InspectTaskAssignModal from './components/InspectTaskAssignModal.vue'
import TableScrollWrap from '@/components/common/TableScrollWrap.vue'
import AdminPanelTitle from '@/components/admin/AdminPanelTitle.vue'
import { message, Modal } from 'ant-design-vue'
import type { TablePaginationConfig } from 'ant-design-vue/es/table'

const TASK_STATUS_PENDING = 0
const TASK_STATUS_OVERDUE = 3

const { t } = useI18n()
const route = useRoute()
const router = useRouter()

const loading = ref(false)
const dataSource = ref<InspectTaskRecord[]>([])
const selectedRowKeys = ref<string[]>([])
const pagination = reactive({ current: 1, pageSize: 10, total: 0 })
const statusOptions = ref<{ value: number; label: string }[]>([])

const planId = computed(() => {
  const value = route.query.planId
  return typeof value === 'string' && value ? value : undefined
})

const searchForm = reactive({
  planName: '',
  status: undefined as number | undefined,
  unassigned: undefined as boolean | undefined,
})

const assignOpen = ref(false)
const assignTaskIds = ref<string[]>([])

const columns = computed(() => [
  { title: t('inspectTaskPage.colPlanName'), dataIndex: 'planName', key: 'planName', width: 140, ellipsis: true },
  { title: t('inspectTaskPage.colTemplate'), dataIndex: 'templateName', key: 'templateName', width: 130, ellipsis: true },
  { title: t('inspectTaskPage.colScheduledTime'), dataIndex: 'scheduledTime', key: 'scheduledTime', width: 170 },
  { title: t('inspectTaskPage.colWorkshop'), key: 'workshop', width: 120, ellipsis: true },
  { title: t('inspectTaskPage.colStatus'), key: 'status', width: 90, align: 'center' as const },
  { title: t('inspectTaskPage.colExecutor'), dataIndex: 'executorName', key: 'executorName', width: 100, ellipsis: true },
  { title: t('inspectTaskPage.colActualStart'), dataIndex: 'actualStartTime', key: 'actualStartTime', width: 170 },
  { title: t('inspectTaskPage.colActualEnd'), dataIndex: 'actualEndTime', key: 'actualEndTime', width: 170 },
  { title: t('inspectTaskPage.colRemark'), dataIndex: 'remark', key: 'remark', minWidth: 120, ellipsis: true },
  { title: t('inspectTaskPage.colAction'), key: 'action', width: 220, fixed: 'right' as const },
])

const rowSelection = computed(() => ({
  selectedRowKeys: selectedRowKeys.value,
  onChange: (keys: string[]) => {
    selectedRowKeys.value = keys
  },
}))

const hasSelection = computed(() => selectedRowKeys.value.length > 0)

const unassignedFilterOptions = computed(() => [
  { value: true, label: t('inspectTaskPage.unassignedOnly') },
])

function statusLabel(record: InspectTaskRecord): string {
  if (record.statusDesc) {
    return t(`inspectTaskPage.status.${record.statusDesc}`, record.statusDesc)
  }
  return inspectTaskStatusLabel(record.status)
}

function statusColor(status?: number): string {
  if (status === 1) {
    return 'processing'
  }
  if (status === 2) {
    return 'success'
  }
  if (status === 3) {
    return 'error'
  }
  return 'default'
}

function workshopLabel(record: InspectTaskRecord): string {
  if (record.workshopName?.trim()) {
    return record.workshopName
  }
  return record.workshopCode || '-'
}

async function loadStatusOptions() {
  const map = await fetchInspectTaskStatusTypes()
  if (map && typeof map === 'object') {
    statusOptions.value = Object.entries(map)
      .map(([key, label]) => ({
        value: Number(key),
        label: t(`inspectTaskPage.status.${label}`, String(label)),
      }))
      .filter((item) => !Number.isNaN(item.value))
      .sort((a, b) => a.value - b.value)
  }
}

async function loadTable() {
  loading.value = true
  try {
    const result = await fetchInspectTaskPage({
      page: pagination.current,
      pageSize: pagination.pageSize,
      planId: planId.value,
      planName: searchForm.planName.trim() || undefined,
      status: searchForm.status,
      unassigned: searchForm.unassigned,
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
  searchForm.planName = ''
  searchForm.status = undefined
  searchForm.unassigned = undefined
  onSearch()
}

function onTableChange(page: TablePaginationConfig) {
  pagination.current = page.current || 1
  pagination.pageSize = page.pageSize || 10
  loadTable()
}

function getSelectedRecords(): InspectTaskRecord[] {
  const idSet = new Set(selectedRowKeys.value)
  return dataSource.value.filter((row) => idSet.has(row.id))
}

function openAssign(taskIds: string[]) {
  if (!taskIds.length) {
    message.warning(t('inspectTaskPage.assignSelectRequired'))
    return
  }
  assignTaskIds.value = taskIds
  assignOpen.value = true
}

function batchAssign() {
  const pending = getSelectedRecords().filter((row) => row.status === TASK_STATUS_PENDING)
  if (!pending.length) {
    message.warning(t('inspectTaskPage.assignPendingOnly'))
    return
  }
  openAssign(pending.map((row) => row.id))
}

function assignOne(record: InspectTaskRecord) {
  if (record.status !== TASK_STATUS_PENDING) {
    message.warning(t('inspectTaskPage.assignPendingOnly'))
    return
  }
  openAssign([record.id])
}

async function restartTasks(ids: string[]) {
  if (!ids.length) {
    message.warning(t('inspectTaskPage.restartSelectRequired'))
    return
  }
  await restartOverdueInspectTasks(ids)
  message.success(t('inspectTaskPage.restartSuccess'))
  selectedRowKeys.value = []
  loadTable()
}

function batchRestart() {
  const overdue = getSelectedRecords().filter((row) => row.status === TASK_STATUS_OVERDUE)
  if (!overdue.length) {
    message.warning(t('inspectTaskPage.restartOverdueOnly'))
    return
  }
  restartTasks(overdue.map((row) => row.id))
}

function restartOne(record: InspectTaskRecord) {
  if (record.status !== TASK_STATUS_OVERDUE) {
    message.warning(t('inspectTaskPage.restartOverdueOnly'))
    return
  }
  restartTasks([record.id])
}

function openDetail(record: InspectTaskRecord) {
  router.push({ path: '/view/inspect_task_detail', query: { id: record.id } })
}

function confirmDelete(ids: string[]) {
  Modal.confirm({
    title: t('inspectTaskPage.deleteConfirm'),
    content: t('inspectTaskPage.deleteContent', { count: ids.length }),
    onOk: async () => {
      await deleteInspectTasks(ids)
      message.success(t('inspectTaskPage.deleteSuccess'))
      selectedRowKeys.value = []
      loadTable()
    },
  })
}

onMounted(async () => {
  await loadStatusOptions()
  await loadTable()
})
</script>

<template>
  <div class="admin-page inspect-module-page inspect-module-page--task">
    <a-card size="small" class="admin-panel-card panel-card">
      <template #title>
        <AdminPanelTitle :subtitle="t('inspectTaskPage.subtitle')">
          <template #icon><CarryOutOutlined /></template>
          {{ t('inspectTaskPage.title') }}
        </AdminPanelTitle>
      </template>
      <template #extra>
        <a-space :size="8" class="extra-tags">
          <a-tag v-if="planId" color="blue">
            {{ t('inspectTaskPage.planFilter', { id: planId }) }}
          </a-tag>
          <a-tag v-if="hasSelection" color="blue">
            {{ t('inspectTaskPage.selectedCount', { count: selectedRowKeys.length }) }}
          </a-tag>
          <a-tag v-if="pagination.total" color="processing">
            {{ t('inspectTaskPage.total', { count: pagination.total }) }}
          </a-tag>
        </a-space>
      </template>

      <div class="admin-panel-body">
        <div class="search-toolbar search-toolbar--compact">
          <a-form layout="inline" class="search-form" :model="searchForm" @finish="onSearch">
            <a-form-item :label="t('inspectTaskPage.colPlanName')" name="planName">
              <a-input size="small"
                v-model:value="searchForm.planName"
                allow-clear
                :placeholder="t('inspectTaskPage.searchPlanName')"
                class="search-input"
              >
                <template #prefix>
                  <SearchOutlined class="input-prefix-icon" />
                </template>
              </a-input>
            </a-form-item>
            <a-form-item :label="t('inspectTaskPage.colStatus')" name="status">
              <a-select size="small"
                v-model:value="searchForm.status"
                allow-clear
                class="search-select-sm"
                :placeholder="t('inspectTaskPage.searchStatus')"
                :options="statusOptions"
              />
            </a-form-item>
            <a-form-item :label="t('inspectTaskPage.colAssign')" name="unassigned">
              <a-select size="small"
                v-model:value="searchForm.unassigned"
                allow-clear
                class="search-select-sm"
                :placeholder="t('inspectTaskPage.searchAssign')"
                :options="unassignedFilterOptions"
              />
            </a-form-item>
            <CompactSearchActions
              :query-title="t('inspectTaskPage.query')"
              :reset-title="t('inspectTaskPage.reset')"
              @reset="onReset"
            />
          </a-form>
        </div>

        <div class="table-toolbar">
          <a-space wrap>
            <a-button type="primary" @click="batchAssign">
              <template #icon><UserAddOutlined /></template>
              {{ t('inspectTaskPage.batchAssign') }}
            </a-button>
            <a-button @click="batchRestart">
              <template #icon><RedoOutlined /></template>
              {{ t('inspectTaskPage.batchRestart') }}
            </a-button>
            <a-button danger :disabled="!hasSelection" @click="confirmDelete(selectedRowKeys)">
              <template #icon><DeleteOutlined /></template>
              {{ t('inspectTaskPage.batchDelete') }}
            </a-button>
            <a-button :loading="loading" @click="loadTable">
              <template #icon><ReloadOutlined /></template>
              {{ t('inspectTaskPage.refresh') }}
            </a-button>
          </a-space>
        </div>

        <TableScrollWrap :refresh-keys="[dataSource.length, pagination.total]">
          <template #default="{ scrollY }">
            <a-table
              row-key="id"
              size="middle"
              bordered
              class="scroll-table inspect-module-table"
              :columns="columns"
              :data-source="dataSource"
              :row-selection="rowSelection"
              :scroll="{ x: 1400, y: scrollY }"
              :pagination="{
                current: pagination.current,
                pageSize: pagination.pageSize,
                total: pagination.total,
                showSizeChanger: true,
                showTotal: (total: number) => t('inspectTaskPage.paginationTotal', { total }),
              }"
              @change="onTableChange"
            >
              <template #bodyCell="{ column, record }">
                <template v-if="column.key === 'planName'">
                  <span class="name-cell">{{ record.planName || '—' }}</span>
                </template>
                <template v-else-if="column.key === 'templateName'">
                  <span class="muted-cell">{{ record.templateName || '—' }}</span>
                </template>
                <template v-else-if="column.key === 'scheduledTime'">
                  <span class="time-cell">{{ record.scheduledTime || '—' }}</span>
                </template>
                <template v-else-if="column.key === 'workshop'">
                  <span class="muted-cell">{{ workshopLabel(record) }}</span>
                </template>
                <template v-else-if="column.key === 'status'">
                  <a-tag :color="statusColor(record.status)">
                    {{ statusLabel(record) }}
                  </a-tag>
                </template>
                <template v-else-if="column.key === 'executorName'">
                  <span :class="record.executorName ? 'name-cell' : 'muted-cell'">
                    {{ record.executorName || t('inspectTaskPage.unassigned') }}
                  </span>
                </template>
                <template v-else-if="column.key === 'actualStartTime'">
                  <span class="time-cell">{{ record.actualStartTime || '—' }}</span>
                </template>
                <template v-else-if="column.key === 'actualEndTime'">
                  <span class="time-cell">{{ record.actualEndTime || '—' }}</span>
                </template>
                <template v-else-if="column.key === 'action'">
                  <a-space size="small" class="action-group">
                    <a-button
                      v-if="record.status === TASK_STATUS_PENDING"
                      type="link"
                      size="small"
                      class="action-link--accent"
                      @click="assignOne(record)"
                    >
                      <UserAddOutlined />
                      {{ t('inspectTaskPage.assign') }}
                    </a-button>
                    <a-button
                      v-if="record.status === TASK_STATUS_OVERDUE"
                      type="link"
                      size="small"
                      class="action-link--warn"
                      @click="restartOne(record)"
                    >
                      <RedoOutlined />
                      {{ t('inspectTaskPage.restart') }}
                    </a-button>
                    <a-button type="link" size="small" @click="openDetail(record)">
                      <EyeOutlined />
                      {{ t('inspectTaskPage.detail') }}
                    </a-button>
                    <a-button type="link" size="small" danger @click="confirmDelete([record.id])">
                      <DeleteOutlined />
                      {{ t('inspectTaskPage.delete') }}
                    </a-button>
                  </a-space>
                </template>
              </template>

              <template #emptyText>
                <a-empty :description="t('inspectTaskPage.empty')" />
              </template>
            </a-table>
          </template>
        </TableScrollWrap>
      </div>
    </a-card>

    <InspectTaskAssignModal v-model:open="assignOpen" :task-ids="assignTaskIds" @success="loadTable" />

  </div>
</template>

