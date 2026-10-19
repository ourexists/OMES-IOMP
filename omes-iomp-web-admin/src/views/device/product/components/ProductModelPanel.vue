<script setup lang="ts">
import { computed, nextTick, reactive, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import {
  AppstoreOutlined,
  ArrowLeftOutlined,
  DeleteOutlined,
  EditOutlined,
  PlusOutlined,
  ReloadOutlined,
  SettingOutlined,
} from '@ant-design/icons-vue'
import { resolveProductImageUrl, type ProductRecord } from '@/api/product'
import type { ProductModelRecord } from '@/api/product-model'
import { deleteProductModels, fetchProductModels, saveProductModel } from '@/api/product-model'
import ProductModelMapPanel from './ProductModelMapModal.vue'
import TableScrollWrap from '@/components/common/TableScrollWrap.vue'
import { message, Modal } from 'ant-design-vue'

const props = defineProps<{
  product: ProductRecord | null
}>()

const { t } = useI18n()

const loading = ref(false)
const saving = ref(false)
const dataSource = ref<ProductModelRecord[]>([])
const formOpen = ref(false)
const view = ref<'list' | 'map'>('list')
const editing = ref<ProductModelRecord | null>(null)
const mappingRecord = ref<ProductModelRecord | null>(null)
const nameInputRef = ref<{ focus?: () => void } | null>(null)
const brokenThumb = ref(false)

const selectedThumb = computed(() => resolveProductImageUrl(props.product?.imageUrl))

const formState = reactive({
  id: '',
  name: '',
  code: '',
})

const modelCount = computed(() => dataSource.value.length)

const mapTitle = computed(() =>
  mappingRecord.value?.name
    ? t('productPage.modelMapTitle', { name: mappingRecord.value.name })
    : t('productPage.modelMapTitleDefault'),
)

const columns = computed(() => [
  {
    title: t('productPage.modelName'),
    dataIndex: 'name',
    key: 'name',
    width: 180,
    ellipsis: true,
  },
  {
    title: t('productPage.modelCode'),
    dataIndex: 'code',
    key: 'code',
    width: 132,
    align: 'center' as const,
    ellipsis: true,
  },
  {
    title: t('productPage.modelMapStatus'),
    key: 'mapStatus',
    width: 96,
    align: 'center' as const,
  },
  {
    title: t('productPage.colAction'),
    key: 'action',
    width: 268,
    align: 'center' as const,
  },
])

function hasMapping(record: ProductModelRecord) {
  const config = record.attrConfig
  if (!config) {
    return false
  }
  if (String(config.runMap || '').trim()) {
    return true
  }
  const rowsHaveMap = (rows?: { map?: string }[]) =>
    Boolean(rows?.some((row) => String(row.map || '').trim()))
  return rowsHaveMap(config.attrs) || rowsHaveMap(config.alarms) || rowsHaveMap(config.controls)
}

async function loadTable() {
  const productCode = String(props.product?.code || '').trim()
  if (!productCode) {
    dataSource.value = []
    return
  }
  loading.value = true
  try {
    dataSource.value = (await fetchProductModels(productCode)) || []
  } finally {
    loading.value = false
  }
}

function resetForm() {
  formState.id = ''
  formState.name = ''
  formState.code = ''
}

async function focusName() {
  await nextTick()
  nameInputRef.value?.focus?.()
}

function openCreate() {
  editing.value = null
  resetForm()
  formOpen.value = true
  void focusName()
}

function openEdit(record: ProductModelRecord) {
  editing.value = record
  formState.id = record.id
  formState.name = record.name || ''
  formState.code = record.code || ''
  formOpen.value = true
  void focusName()
}

function cancelForm() {
  formOpen.value = false
  editing.value = null
  resetForm()
}

function openMap(record: ProductModelRecord) {
  mappingRecord.value = record
  cancelForm()
  view.value = 'map'
}

function backToList() {
  view.value = 'list'
  mappingRecord.value = null
}

function confirmDelete(record: ProductModelRecord) {
  Modal.confirm({
    title: t('productPage.modelDeleteConfirm'),
    content: t('productPage.modelDeleteContent', { name: record.name || record.code }),
    onOk: async () => {
      await deleteProductModels([record.id])
      message.success(t('productPage.deleteSuccess'))
      if (editing.value?.id === record.id) {
        cancelForm()
      }
      await loadTable()
    },
  })
}

function rowClassName(record: ProductModelRecord) {
  return editing.value?.id === record.id ? 'model-table__row--editing' : ''
}

async function handleSave() {
  if (saving.value) {
    return Promise.reject()
  }
  if (!formState.name.trim()) {
    message.warning(t('productPage.modelNameRequired'))
    return Promise.reject()
  }
  if (!formState.code.trim()) {
    message.warning(t('productPage.modelCodeRequired'))
    return Promise.reject()
  }
  if (!props.product?.code) {
    return Promise.reject()
  }
  saving.value = true
  try {
    await saveProductModel({
      id: formState.id || undefined,
      productCode: props.product.code,
      name: formState.name.trim(),
      code: formState.code.trim(),
    })
    message.success(t('productPage.saveSuccess'))
    formOpen.value = false
    editing.value = null
    resetForm()
    await loadTable()
  } catch (error) {
    return Promise.reject(error)
  } finally {
    saving.value = false
  }
}

async function onMapSuccess() {
  backToList()
  await loadTable()
}

function resetView() {
  view.value = 'list'
  formOpen.value = false
  mappingRecord.value = null
  cancelForm()
}

watch(
  () => props.product?.code,
  async (code) => {
    brokenThumb.value = false
    resetView()
    if (!code) {
      dataSource.value = []
      return
    }
    await loadTable()
  },
  { immediate: true },
)

defineExpose({
  reload: loadTable,
})
</script>

<template>
  <div class="model-panel">
    <template v-if="!product">
      <div class="model-panel__empty">
        <a-empty :description="t('productPage.selectProductHint')" :image-style="{ height: '72px' }" />
      </div>
    </template>

    <template v-else>
      <div v-show="view === 'list'" class="model-list-view">
        <div class="model-head">
          <div class="product-summary">
            <div class="product-summary__thumb">
              <img
                v-if="selectedThumb && !brokenThumb"
                :src="selectedThumb"
                alt=""
                class="product-summary__img"
                @error="brokenThumb = true"
              />
              <AppstoreOutlined v-else class="product-summary__placeholder" />
            </div>
            <div class="product-summary__meta">
              <div class="product-summary__name">{{ product.name || '—' }}</div>
              <div class="product-summary__desc">{{ t('productPage.modelPanelHint') }}</div>
            </div>
          </div>
          <div class="model-head__actions">
            <a-button type="primary" @click="openCreate">
              <template #icon><PlusOutlined /></template>
              {{ t('productPage.modelAdd') }}
            </a-button>
            <a-button :loading="loading" @click="loadTable">
              <template #icon><ReloadOutlined /></template>
              {{ t('productPage.refresh') }}
            </a-button>
            <a-tag v-if="modelCount" color="processing" class="model-count-tag">
              {{ t('productPage.modelCount', { count: modelCount }) }}
            </a-tag>
          </div>
        </div>

        <TableScrollWrap class="model-table-scroll" :refresh-keys="[dataSource.length, product.code]">
          <template #default="{ scrollY, scrollReady }">
          <a-table
            row-key="id"
            size="small"
            table-layout="fixed"
            bordered
            class="model-table scroll-table"
            :class="{ 'scroll-table--pending': !scrollReady }"
            :loading="loading"
            :columns="columns"
            :data-source="dataSource"
            :pagination="false"
            :scroll="scrollReady ? { x: 680, y: scrollY } : { x: 680 }"
            :row-class-name="rowClassName"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'name'">
                <div class="name-cell">
                  <span class="name-cell__dot" />
                  <span class="name-cell__text">{{ record.name || '—' }}</span>
                </div>
              </template>
              <template v-else-if="column.key === 'code'">
                <span v-if="record.code" class="code-cell">{{ record.code }}</span>
                <span v-else class="empty-cell">—</span>
              </template>
              <template v-else-if="column.key === 'mapStatus'">
                <a-tag
                  :color="hasMapping(record) ? 'success' : 'default'"
                  class="map-status-tag"
                  :bordered="false"
                >
                  {{ hasMapping(record) ? t('productPage.modelMapped') : t('productPage.modelUnmapped') }}
                </a-tag>
              </template>
              <template v-else-if="column.key === 'action'">
                <div class="model-actions">
                  <a-button type="link" size="small" class="model-action-btn" @click="openEdit(record)">
                    <EditOutlined />
                    {{ t('productPage.edit') }}
                  </a-button>
                  <span class="model-actions__divider" />
                  <a-button
                    type="link"
                    size="small"
                    class="model-action-btn model-action-btn--accent"
                    @click="openMap(record)"
                  >
                    <SettingOutlined />
                    {{ t('productPage.modelMap') }}
                  </a-button>
                  <span class="model-actions__divider" />
                  <a-button
                    type="link"
                    size="small"
                    danger
                    class="model-action-btn"
                    @click="confirmDelete(record)"
                  >
                    <DeleteOutlined />
                    {{ t('productPage.delete') }}
                  </a-button>
                </div>
              </template>
            </template>
            <template #emptyText>
              <a-empty :description="t('productPage.modelEmpty')">
                <a-button type="primary" @click="openCreate">
                  <PlusOutlined />
                  {{ t('productPage.modelAdd') }}
                </a-button>
              </a-empty>
            </template>
          </a-table>
          </template>
        </TableScrollWrap>
      </div>

      <div v-if="view === 'map'" class="model-map-view">
        <div class="model-map-view__head">
          <a-button type="text" size="small" class="model-map-view__back" @click="backToList">
            <ArrowLeftOutlined />
            {{ t('productPage.modelBack') }}
          </a-button>
          <span class="model-map-view__divider" />
          <span class="model-map-view__title">{{ mapTitle }}</span>
        </div>
        <ProductModelMapPanel
          :product="product"
          :record="mappingRecord"
          @success="onMapSuccess"
          @cancel="backToList"
        />
      </div>
    </template>

    <a-modal
      v-model:open="formOpen"
      :title="formState.id ? t('productPage.modelFormEdit') : t('productPage.modelFormAdd')"
      width="460px"
      centered
      destroy-on-close
      class="model-form-modal"
      :confirm-loading="saving"
      :ok-text="t('productPage.save')"
      :cancel-text="t('productPage.cancel')"
      @ok="handleSave"
      @cancel="cancelForm"
    >
      <a-form layout="vertical" class="model-form" @finish="handleSave">
        <a-form-item :label="t('productPage.modelName')" required>
          <a-input
            ref="nameInputRef"
            v-model:value="formState.name"
            allow-clear
            :placeholder="t('productPage.modelNamePlaceholder')"
            @pressEnter="handleSave"
          />
        </a-form-item>
        <a-form-item :label="t('productPage.modelCode')" required>
          <a-input
            v-model:value="formState.code"
            allow-clear
            :disabled="Boolean(formState.id)"
            :placeholder="t('productPage.modelCodePlaceholder')"
            @pressEnter="handleSave"
          />
          <div v-if="formState.id" class="field-hint">{{ t('productPage.modelCodeEditHint') }}</div>
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<style scoped>
.model-panel {
  display: flex;
  flex-direction: column;
  flex: 1;
  min-height: 0;
  height: 100%;
}

.model-panel__empty {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 240px;
  border: 1px dashed var(--omes-color-border-hover);
  border-radius: var(--omes-radius-lg);
  background: var(--omes-color-bg-elevated);
}

.model-list-view {
  display: flex;
  flex-direction: column;
  flex: 1;
  min-height: 0;
}

.model-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  flex-shrink: 0;
  margin-bottom: 12px;
  padding: 10px 12px;
  border: 1px solid var(--omes-color-primary-border);
  border-radius: var(--omes-radius-md);
  background: linear-gradient(120deg, var(--omes-color-primary-bg) 0%, #ffffff 62%);
}

.product-summary {
  display: flex;
  align-items: center;
  gap: 12px;
  min-width: 0;
  flex: 1;
}

.product-summary__thumb {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 40px;
  height: 40px;
  flex-shrink: 0;
  overflow: hidden;
  border-radius: 10px;
  border: 1px solid var(--omes-color-primary-border);
  background: var(--omes-color-bg-container);
}

.product-summary__img {
  width: 100%;
  height: 100%;
  object-fit: contain;
}

.product-summary__placeholder {
  font-size: 18px;
  color: var(--omes-color-primary);
}

.product-summary__meta {
  min-width: 0;
}

.product-summary__name {
  font-size: 15px;
  font-weight: 600;
  color: var(--omes-color-text);
  line-height: 1.35;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.product-summary__desc {
  margin-top: 2px;
  font-size: 12px;
  color: var(--omes-color-text-tertiary);
  line-height: 1.4;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.model-head__actions {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
}

@media (max-width: 720px) {
  .model-head {
    flex-wrap: wrap;
  }

  .model-head__actions {
    width: 100%;
    justify-content: flex-end;
  }
}

.model-count-tag {
  margin-inline-end: 0;
  font-variant-numeric: tabular-nums;
}

.model-form :deep(.ant-form-item) {
  margin-bottom: 16px;
}

.model-form :deep(.ant-form-item:last-child) {
  margin-bottom: 4px;
}

.field-hint {
  margin-top: 6px;
  font-size: 12px;
  color: var(--omes-color-text-tertiary);
}

.model-table-scroll {
  flex: 1;
  min-height: 0;
}

.model-table :deep(.ant-table-thead > tr > th) {
  background: linear-gradient(180deg, var(--omes-color-bg-elevated) 0%, var(--omes-color-bg-layout) 100%);
  font-weight: 600;
  font-size: 13px;
  padding: 10px 12px !important;
}

.model-table :deep(.ant-table-tbody > tr > td) {
  padding: 10px 12px !important;
  vertical-align: middle;
  font-size: 13px;
}

.model-table :deep(.ant-table-tbody > tr:nth-child(even) > td) {
  background: var(--omes-color-bg-muted);
}

.model-table :deep(.ant-table-tbody > tr:hover > td) {
  background: var(--omes-color-bg-table-hover) !important;
}

.model-table :deep(.model-table__row--editing > td) {
  background: var(--omes-color-primary-bg-hover) !important;
}

.model-table :deep(.ant-empty) {
  margin: 36px 0;
}

.name-cell {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
}

.name-cell__dot {
  width: 6px;
  height: 6px;
  flex-shrink: 0;
  border-radius: 50%;
  background: var(--omes-color-primary);
  opacity: 0.55;
}

.name-cell__text {
  min-width: 0;
  font-weight: 500;
  color: var(--omes-color-text);
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
  border: 1px solid var(--omes-color-primary-border);
  padding: 2px 10px;
  border-radius: var(--omes-radius-sm);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  vertical-align: middle;
}

.empty-cell {
  display: inline-block;
  min-width: 28px;
  padding: 2px 10px;
  font-size: 12px;
  color: rgba(0, 0, 0, 0.25);
  background: var(--omes-color-bg-layout);
  border-radius: var(--omes-radius-sm);
  line-height: 20px;
}

.map-status-tag {
  margin-inline-end: 0;
  border-radius: 999px;
  padding-inline: 10px;
}

.model-actions {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex-wrap: nowrap;
  gap: 2px;
  white-space: nowrap;
}

.model-actions__divider {
  display: inline-block;
  width: 1px;
  height: 14px;
  margin: 0 2px;
  background: var(--omes-color-border-hover);
  flex-shrink: 0;
}

.model-action-btn {
  display: inline-flex !important;
  align-items: center;
  gap: 4px;
  height: 26px !important;
  padding: 0 6px !important;
  font-size: 13px !important;
  line-height: 1 !important;
}

.model-action-btn :deep(.anticon) {
  font-size: 13px;
}

.model-action-btn--accent {
  color: var(--omes-color-primary);
}

.model-action-btn--accent:hover {
  color: var(--omes-color-primary-active) !important;
  background: var(--omes-color-primary-bg);
  border-radius: 4px;
}

.model-map-view {
  display: flex;
  flex-direction: column;
  flex: 1;
  min-height: 0;
  overflow: auto;
}

.model-map-view__head {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 14px;
  padding: 8px 12px;
  background: var(--omes-color-bg-elevated);
  border: 1px solid var(--omes-color-border);
  border-radius: var(--omes-radius-md);
}

.model-map-view__back {
  padding-inline: 6px;
  color: var(--omes-color-text-secondary);
}

.model-map-view__back:hover {
  color: var(--omes-color-primary);
}

.model-map-view__divider {
  width: 1px;
  height: 14px;
  background: var(--omes-color-border-hover);
}

.model-map-view__title {
  min-width: 0;
  font-weight: 600;
  color: var(--omes-color-text);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
</style>
