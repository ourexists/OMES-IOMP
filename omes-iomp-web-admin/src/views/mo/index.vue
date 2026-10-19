<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import CompactSearchActions from '@/components/admin/CompactSearchActions.vue'
import AdminPanelTitle from '@/components/admin/AdminPanelTitle.vue'
import {
  AppstoreOutlined,
  DeleteOutlined,
  FileTextOutlined,
  InboxOutlined,
  PlusOutlined,
  ReloadOutlined,
  UnorderedListOutlined,
} from '@ant-design/icons-vue'
import { cancelMo, deleteMos, fetchMoPage, fetchMoStatusOptions, previewMoAdjust } from '@/api/mo'
import type { MoRecord } from '@/types/mo'
import { MO_STATUS } from '@/types/mo'
import MoAdjustModal, { type MoAdjustType } from './components/MoAdjustModal.vue'
import MoFormWizard from './components/MoFormWizard.vue'
import MoOrderCard from './components/MoOrderCard.vue'
import MoOrderList from './components/MoOrderList.vue'
import { message, Modal } from 'ant-design-vue'
import type { CheckboxChangeEvent } from 'ant-design-vue/es/checkbox/interface'

const { t } = useI18n()
const router = useRouter()
const route = useRoute()

const VIEW_STORAGE_KEY = 'mo_list_view_mode'
type ViewMode = 'card' | 'list'

const loading = ref(false)
const viewMode = ref<ViewMode>(
  localStorage.getItem(VIEW_STORAGE_KEY) === 'list' ? 'list' : 'card',
)
const dataSource = ref<MoRecord[]>([])
const selectedRowKeys = ref<string[]>([])
const pagination = reactive({ current: 1, pageSize: 10, total: 0 })
const statusOptions = ref<{ value: number; label: string }[]>([])
const formOpen = ref(false)

const adjustOpen = ref(false)
const adjustType = ref<MoAdjustType | null>(null)
const adjustRecord = ref<MoRecord | null>(null)

const searchForm = reactive({
  selfCode: '',
  productName: '',
  productCode: '',
  status: undefined as number | undefined,
})

const pageIds = computed(() => dataSource.value.map((item) => item.id))

const allPageSelected = computed(
  () => pageIds.value.length > 0 && pageIds.value.every((id) => selectedRowKeys.value.includes(id)),
)

const somePageSelected = computed(
  () => pageIds.value.some((id) => selectedRowKeys.value.includes(id)) && !allPageSelected.value,
)

const gridFillRows = computed(() => dataSource.value.length > 0)

function canExec(record: MoRecord): boolean {
  return record.status === MO_STATUS.INIT || record.status === MO_STATUS.PART
}

function canDelete(record: MoRecord): boolean {
  return record.status === MO_STATUS.INIT
}

function canCancel(record: MoRecord): boolean {
  return record.status != null && record.status !== MO_STATUS.CANCEL && record.status !== MO_STATUS.COMPLETE
}

function statusFilterClass(status?: number): string {
  if (status === MO_STATUS.PART) {
    return 'mo-status-pill--warning'
  }
  if (status === MO_STATUS.RUN) {
    return 'mo-status-pill--processing'
  }
  if (status === MO_STATUS.COMPLETE) {
    return 'mo-status-pill--success'
  }
  if (status === MO_STATUS.CANCEL) {
    return 'mo-status-pill--danger'
  }
  if (status === MO_STATUS.INIT) {
    return 'mo-status-pill--default'
  }
  return ''
}

async function loadFilters() {
  try {
    statusOptions.value = await fetchMoStatusOptions()
  } catch {
    statusOptions.value = []
  }
}

async function loadTable() {
  loading.value = true
  try {
    const result = await fetchMoPage({
      page: pagination.current,
      pageSize: pagination.pageSize,
      selfCode: searchForm.selfCode.trim() || undefined,
      productName: searchForm.productName.trim() || undefined,
      productCode: searchForm.productCode.trim() || undefined,
      status: searchForm.status,
    })
    dataSource.value = result.records || []
    pagination.total = result.total || 0
  } catch {
    dataSource.value = []
    pagination.total = 0
  } finally {
    loading.value = false
  }
}

function onSearch() {
  pagination.current = 1
  selectedRowKeys.value = []
  loadTable()
}

function onReset() {
  searchForm.selfCode = ''
  searchForm.productName = ''
  searchForm.productCode = ''
  searchForm.status = undefined
  onSearch()
}

function onStatusFilter(status: number | undefined) {
  searchForm.status = status
  onSearch()
}

function onPageChange(page: number, pageSize: number) {
  pagination.current = page
  pagination.pageSize = pageSize
  selectedRowKeys.value = []
  loadTable()
}

function toggleSelectAll(checked: boolean) {
  if (checked) {
    selectedRowKeys.value = [...new Set([...selectedRowKeys.value, ...pageIds.value])]
    return
  }
  selectedRowKeys.value = selectedRowKeys.value.filter((id) => !pageIds.value.includes(id))
}

function toggleSelect(id: string, checked: boolean) {
  if (checked) {
    if (!selectedRowKeys.value.includes(id)) {
      selectedRowKeys.value = [...selectedRowKeys.value, id]
    }
    return
  }
  selectedRowKeys.value = selectedRowKeys.value.filter((key) => key !== id)
}

function openCreate() {
  formOpen.value = true
}

function openDetail(record: MoRecord) {
  router.push({ path: '/view/mo_form_edit', query: { page_type: '1', id: record.id } })
}

function openExec(record: MoRecord) {
  router.push({ path: '/view/mo_exec_edit', query: { id: record.id } })
}

function openAdjust(record: MoRecord, type: MoAdjustType) {
  adjustRecord.value = record
  adjustType.value = type
  adjustOpen.value = true
}

function confirmDelete(ids: string[]) {
  Modal.confirm({
    title: t('moPage.deleteConfirm'),
    content: t('moPage.deleteContent', { count: ids.length }),
    onOk: async () => {
      await deleteMos(ids)
      message.success(t('moPage.deleteSuccess'))
      selectedRowKeys.value = []
      loadTable()
    },
  })
}

function confirmBatchCancel(records: MoRecord[], force = false) {
  const targets = records.filter(canCancel)
  if (!targets.length) {
    message.warning(t('moPage.selectOne'))
    return
  }
  Modal.confirm({
    title: t(force ? 'moPage.cancelForce' : 'moPage.cancelConfirm'),
    content: t(force ? 'moPage.cancelForceContent' : 'moPage.cancelContent', { count: targets.length }),
    onOk: async () => {
      let operator: string | undefined
      if (force) {
        operator = window.prompt(t('moPage.forceOperator'), 'admin') || undefined
        if (!operator?.trim()) {
          message.warning(t('moPage.forceOperator'))
          return Promise.reject()
        }
      }
      for (const record of targets) {
        if (!record.selfCode) continue
        const preview = await previewMoAdjust({
          moCode: record.selfCode,
          adjustType: 'CANCEL_MO',
          force,
          operator,
          payload: {},
        })
        if (preview && preview.allowed === false && !force) {
          message.warning(preview.rejectReason || t('moPage.cancelConfirm'))
          if (preview.requiresForce) {
            confirmBatchCancel([record], true)
          }
          return
        }
        await cancelMo(record.selfCode, { force, operator })
      }
      message.success(t('moPage.cancelSuccess'))
      selectedRowKeys.value = []
      loadTable()
    },
  })
}

function onBatchDelete() {
  if (!selectedRowKeys.value.length) {
    message.warning(t('moPage.selectOne'))
    return
  }
  confirmDelete(selectedRowKeys.value)
}

function onBatchCancel() {
  const records = dataSource.value.filter((item) => selectedRowKeys.value.includes(item.id))
  if (records.length === 1) {
    openAdjust(records[0], 'CANCEL_MO')
    return
  }
  confirmBatchCancel(records)
}

watch(viewMode, (mode) => {
  localStorage.setItem(VIEW_STORAGE_KEY, mode)
})

onMounted(async () => {
  try {
    await loadFilters()
    await loadTable()
    if (route.path.endsWith('mo_form_add')) {
      formOpen.value = true
    }
  } catch {
    // 错误已由 request 拦截器提示，避免 mounted 未捕获告警
  }
})

watch(
  () => route.path,
  (path) => {
    if (path.endsWith('mo_form_add')) {
      formOpen.value = true
    }
  },
)

defineExpose({ reloadTable: loadTable })
</script>

<template>
  <div class="admin-page production-module-page production-module-page--mo mo-list-page">
    <a-card size="small" class="admin-panel-card panel-card">
      <template #title>
        <AdminPanelTitle :subtitle="t('moPage.subtitle')" subtitle-class="mo-list-page__subtitle">
          <template #icon><FileTextOutlined /></template>
          {{ t('moPage.title') }}
        </AdminPanelTitle>
      </template>
      <template #extra>
        <a-space :size="8" class="extra-tags">
          <a-tag v-if="pagination.total" color="processing">
            {{ t('moPage.total', { count: pagination.total }) }}
          </a-tag>
          <a-tag v-if="selectedRowKeys.length" color="orange">
            {{ t('moPage.selectedCount', { count: selectedRowKeys.length }) }}
          </a-tag>
        </a-space>
      </template>

      <div class="admin-panel-body">
        <div class="search-toolbar search-toolbar--compact">
          <a-form layout="inline" class="search-form" :model="searchForm" @finish="onSearch">
            <a-form-item name="selfCode">
              <a-input
                v-model:value="searchForm.selfCode"
                allow-clear
                size="small"
                class="search-input"
                :placeholder="t('moPage.moCode')"
              />
            </a-form-item>
            <a-form-item name="productName">
              <a-input
                v-model:value="searchForm.productName"
                allow-clear
                size="small"
                class="search-input"
                :placeholder="t('moPage.bomName')"
              />
            </a-form-item>
            <a-form-item name="productCode">
              <a-input
                v-model:value="searchForm.productCode"
                allow-clear
                size="small"
                class="search-input"
                :placeholder="t('moPage.bomCode')"
              />
            </a-form-item>
            <CompactSearchActions
              :query-title="t('moPage.query')"
              :reset-title="t('moPage.reset')"
              @reset="onReset"
            />
          </a-form>
        </div>

        <div class="mo-list-control-bar">
          <div v-if="statusOptions.length" class="mo-status-filter">
            <span class="mo-status-filter__label">{{ t('moPage.status') }}</span>
            <div class="mo-status-filter__pills">
              <button
                type="button"
                class="mo-status-pill"
                :class="{ 'mo-status-pill--active': searchForm.status == null }"
                @click="onStatusFilter(undefined)"
              >
                {{ t('moPage.filterAll') }}
              </button>
              <button
                v-for="option in statusOptions"
                :key="option.value"
                type="button"
                class="mo-status-pill"
                :class="[
                  statusFilterClass(option.value),
                  { 'mo-status-pill--active': searchForm.status === option.value },
                ]"
                @click="onStatusFilter(option.value)"
              >
                {{ option.label }}
              </button>
            </div>
          </div>

          <div class="mo-list-toolbar">
            <div class="mo-list-toolbar__actions">
              <a-tooltip :title="t('moPage.add')">
                <a-button type="primary" size="small" @click="openCreate">
                  <PlusOutlined />
                </a-button>
              </a-tooltip>
              <a-tooltip :title="t('moPage.cancelOrder')">
                <a-button
                  size="small"
                  danger
                  ghost
                  class="mo-list-toolbar__label-btn"
                  :disabled="!selectedRowKeys.length"
                  @click="onBatchCancel"
                >
                  {{ t('moPage.cancelOrder') }}
                </a-button>
              </a-tooltip>
              <a-tooltip :title="t('moPage.batchDelete')">
                <a-button
                  size="small"
                  danger
                  :disabled="!selectedRowKeys.length"
                  @click="onBatchDelete"
                >
                  <DeleteOutlined />
                </a-button>
              </a-tooltip>
              <a-tooltip :title="t('moPage.refresh')">
                <a-button size="small" :loading="loading" @click="loadTable">
                  <ReloadOutlined />
                </a-button>
              </a-tooltip>
            </div>
            <a-radio-group
              v-model:value="viewMode"
              class="mo-view-toggle"
              button-style="solid"
              size="small"
            >
              <a-radio-button value="card">
                <AppstoreOutlined />
              </a-radio-button>
              <a-radio-button value="list">
                <UnorderedListOutlined />
              </a-radio-button>
            </a-radio-group>
            <a-tooltip v-if="viewMode === 'card' && dataSource.length" :title="t('moPage.selectAllPage')">
              <a-checkbox
                class="mo-list-toolbar__select-all"
                :checked="allPageSelected"
                :indeterminate="somePageSelected"
                @change="(e: CheckboxChangeEvent) => toggleSelectAll(!!e.target.checked)"
              />
            </a-tooltip>
          </div>
        </div>

        <div class="mo-list-stage">
          <div
            class="mo-list-scroll"
            :class="{
              'mo-list-scroll--page': dataSource.length > 0,
              'mo-list-scroll--table': viewMode === 'list' && dataSource.length > 0,
            }"
          >
          <a-spin :spinning="loading" class="mo-list-spin">
            <div v-if="!loading && !dataSource.length" class="mo-empty">
              <InboxOutlined />
              <p class="mo-empty__title">{{ t('moPage.emptyList') }}</p>
            </div>

            <div
              v-else-if="viewMode === 'card'"
              class="mo-card-grid mo-card-grid--cards"
              :class="{ 'mo-card-grid--fill': gridFillRows }"
            >
              <MoOrderCard
                v-for="record in dataSource"
                :key="record.id"
                :record="record"
                :selected="selectedRowKeys.includes(record.id)"
                :can-exec="canExec(record)"
                :can-delete="canDelete(record)"
                :can-cancel="canCancel(record)"
                @select="(checked) => toggleSelect(record.id, checked)"
                @exec="openExec(record)"
                @detail="openDetail(record)"
                @delete="confirmDelete([record.id])"
                @adjust="(type) => openAdjust(record, type)"
              />
            </div>

            <MoOrderList
              v-else
              :records="dataSource"
              :selected-row-keys="selectedRowKeys"
              :loading="loading"
              @update:selected-row-keys="selectedRowKeys = $event"
              @exec="openExec"
              @detail="openDetail"
              @delete="(record) => confirmDelete([record.id])"
              @adjust="(record, type) => openAdjust(record, type)"
            />
          </a-spin>
          </div>
        </div>

        <div v-if="pagination.total > 0" class="mo-pagination">
          <a-pagination
            :current="pagination.current"
            :page-size="pagination.pageSize"
            :total="pagination.total"
            :show-size-changer="true"
            :page-size-options="['10', '20', '50']"
            :show-total="(total: number) => t('moPage.paginationTotal', { total })"
            @change="onPageChange"
          />
        </div>
      </div>
    </a-card>

    <MoFormWizard v-model:open="formOpen" @success="loadTable" />
    <MoAdjustModal
      v-model:open="adjustOpen"
      :adjust-type="adjustType"
      :mo-record="adjustRecord"
      @success="loadTable"
    />
  </div>
</template>
