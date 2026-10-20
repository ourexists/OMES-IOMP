<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import {
  AppstoreOutlined,
  BarcodeOutlined,
  DeleteOutlined,
  InfoCircleOutlined,
  PlusOutlined,
  ShoppingCartOutlined,
} from '@ant-design/icons-vue'
import type { BomDetailRecord, BomRecord, BomTypeOption } from '@/api/bom'
import { fetchBomById, fetchBomTypes, saveBom } from '@/api/bom'
import type { MaterialRecord } from '@/api/material'
import MaterialMultiPickerModal from '@/views/material/components/MaterialMultiPickerModal.vue'
import { message } from 'ant-design-vue'

/** 与后端 BOMTypeEnum 一致：0=定量配比(NUM)，1=定比配比(PER) */
const BOM_TYPE_PERCENT = 1

const props = defineProps<{
  open: boolean
  record: Partial<BomRecord> | null
  defaultClassifyCode?: string
}>()

const emit = defineEmits<{
  'update:open': [value: boolean]
  success: []
}>()

const { t } = useI18n()

const saving = ref(false)
const loading = ref(false)
const typeOptions = ref<BomTypeOption[]>([])
const details = ref<BomDetailRecord[]>([])
const selectedDetailKeys = ref<string[]>([])
const materialPickerOpen = ref(false)

const formState = reactive({
  id: '',
  name: '',
  selfCode: '',
  classifyCode: '' as string | undefined,
  type: 0 as number | undefined,
})

const isEdit = computed(() => Boolean(props.record?.id))
const title = computed(() => (isEdit.value ? t('bomPage.formEdit') : t('bomPage.formAdd')))
const isPercentType = computed(() => Number(formState.type) === BOM_TYPE_PERCENT)

const scaleHint = computed(() =>
  isPercentType.value ? t('bomPage.scaleHintPer') : t('bomPage.scaleHintNum'),
)

const scaleColumnTitle = computed(() =>
  isPercentType.value ? t('bomPage.detailScalePercent') : t('bomPage.detailScaleQty'),
)

const scaleSum = computed(() =>
  details.value.reduce((sum, row) => sum + normalizeScale(row.matScale), 0),
)

const scaleSumDisplay = computed(() => {
  const sum = scaleSum.value
  return isPercentType.value ? sum.toFixed(2) : sum.toFixed(4)
})

const percentFill = computed(() => Math.min(100, Math.max(0, scaleSum.value)))

const detailScroll = computed(() => (details.value.length > 8 ? { y: 320 } : undefined))

const percentSumValid = computed(() => {
  if (!isPercentType.value || !details.value.length) {
    return true
  }
  return Math.abs(scaleSum.value - 100) <= 0.01
})

const existingMatIds = computed(() =>
  details.value.map((row) => row.matId).filter((id): id is string => Boolean(id)),
)

const detailRowKey = (row: BomDetailRecord) => row.tmp_id || row.id || `${row.matCode}-${row.matId}`

const detailColumns = computed(() => [
  { title: '#', key: 'index', width: 52, align: 'center' as const },
  { title: t('bomPage.detailMatName'), dataIndex: 'matName', key: 'matName', ellipsis: true },
  { title: t('bomPage.detailMatCode'), dataIndex: 'matCode', key: 'matCode', width: 140 },
  { title: scaleColumnTitle.value, key: 'matScale', width: 150, align: 'right' as const },
  { title: t('bomPage.colAction'), key: 'action', width: 72, align: 'center' as const, fixed: 'right' as const },
])

const detailRowSelection = computed(() => ({
  selectedRowKeys: selectedDetailKeys.value,
  onChange: (keys: string[]) => {
    selectedDetailKeys.value = keys
  },
}))

function detailKey(row: BomDetailRecord): string {
  return detailRowKey(row)
}

function normalizeScale(value: unknown): number {
  const num = Number(value)
  return Number.isFinite(num) ? num : 0
}

function addDetailRows(items: MaterialRecord[]) {
  const timestamp = Date.now()
  items.forEach((item, index) => {
    details.value.push({
      matName: item.name,
      matCode: item.selfCode,
      matId: item.id,
      tmp_id: `${timestamp}_${index}`,
      matScale: 0,
      attribute: 0,
    })
  })
}

function openMaterialPicker() {
  materialPickerOpen.value = true
}

function onMaterialsPicked(items: MaterialRecord[]) {
  if (!items.length) {
    return
  }
  const existing = new Set(existingMatIds.value)
  const toAdd: MaterialRecord[] = []
  let skipped = 0
  for (const item of items) {
    if (item.id && existing.has(item.id)) {
      skipped += 1
      continue
    }
    toAdd.push(item)
    if (item.id) {
      existing.add(item.id)
    }
  }
  if (toAdd.length) {
    addDetailRows(toAdd)
  }
  if (skipped > 0) {
    message.info(t('bomPage.detailSkipDuplicate', { count: skipped }))
  }
}

function removeDetailRows(rows: BomDetailRecord[]) {
  const keys = new Set(rows.map((row) => detailKey(row)))
  details.value = details.value.filter((row) => !keys.has(detailKey(row)))
  selectedDetailKeys.value = selectedDetailKeys.value.filter((key) => !keys.has(key))
}

function removeSelectedDetails() {
  if (!selectedDetailKeys.value.length) {
    message.warning(t('bomPage.detailSelectOne'))
    return
  }
  const keys = new Set(selectedDetailKeys.value)
  details.value = details.value.filter((row) => !keys.has(detailKey(row)))
  selectedDetailKeys.value = []
}

function removeOneDetail(row: BomDetailRecord) {
  removeDetailRows([row])
}

function validateDetails(): boolean {
  if (!details.value.length) {
    message.warning(t('bomPage.detailRequired'))
    return false
  }
  for (let i = 0; i < details.value.length; i++) {
    const scale = normalizeScale(details.value[i].matScale)
    if (scale <= 0) {
      message.warning(t('bomPage.detailScaleRequired', { row: i + 1 }))
      return false
    }
    if (isPercentType.value && scale > 100) {
      message.warning(t('bomPage.detailScaleRequired', { row: i + 1 }))
      return false
    }
  }
  if (isPercentType.value && !percentSumValid.value) {
    message.warning(t('bomPage.detailPercentMust100', { sum: scaleSumDisplay.value }))
    return false
  }
  return true
}

async function loadTypes() {
  const list = await fetchBomTypes()
  typeOptions.value = Array.isArray(list) ? list : []
}

async function loadRecord(id: string) {
  loading.value = true
  try {
    const result = await fetchBomById(id)
    formState.id = result.id || ''
    formState.name = result.name || ''
    formState.selfCode = result.selfCode || ''
    formState.classifyCode = result.classifyCode
    formState.type = result.type ?? 0
    details.value = (result.details || []).map((row) => ({
      ...row,
      tmp_id: row.id || row.tmp_id,
      matScale: normalizeScale(row.matScale),
    }))
  } finally {
    loading.value = false
  }
}

watch(
  () => props.open,
  async (open) => {
    if (!open) {
      materialPickerOpen.value = false
      return
    }
    selectedDetailKeys.value = []
    if (!typeOptions.value.length) {
      await loadTypes()
    }
    const record = props.record
    if (record?.id) {
      await loadRecord(record.id)
      return
    }
    formState.id = ''
    formState.name = record?.name || ''
    formState.selfCode = record?.selfCode || ''
    formState.classifyCode = record?.classifyCode || props.defaultClassifyCode
    formState.type = record?.type ?? 0
    details.value = []
  },
)

function closeModal() {
  emit('update:open', false)
}

async function handleSubmit() {
  const name = formState.name.trim()
  const selfCode = formState.selfCode.trim()
  if (!name) {
    message.warning(t('bomPage.nameRequired'))
    return
  }
  if (!selfCode) {
    message.warning(t('bomPage.codeRequired'))
    return
  }
  if (!formState.classifyCode) {
    message.warning(t('bomPage.classifyRequired'))
    return
  }
  if (!validateDetails()) {
    return
  }
  saving.value = true
  try {
    await saveBom({
      id: formState.id || undefined,
      name,
      selfCode,
      classifyCode: formState.classifyCode,
      type: formState.type,
      details: details.value.map((row) => ({
        ...row,
        matScale: normalizeScale(row.matScale),
      })),
    })
    message.success(t('bomPage.saveSuccess'))
    emit('update:open', false)
    emit('success')
  } finally {
    saving.value = false
  }
}
</script>

<template>
  <a-modal
    :open="open"
    width="min(1080px, 96vw)"
    destroy-on-close
    class="bom-form-modal"
    :footer="null"
    @cancel="closeModal"
    @update:open="emit('update:open', $event)"
  >
    <template #title>
      <div class="modal-title-wrap">
        <span class="modal-title">{{ title }}</span>
        <a-tag :color="isEdit ? 'processing' : 'success'">
          {{ isEdit ? t('bomPage.formModeEdit') : t('bomPage.formModeAdd') }}
        </a-tag>
      </div>
    </template>

    <a-spin :spinning="loading" class="bom-form-spin">
      <div class="bom-form-scroll">
      <a-form layout="vertical" class="bom-form">
        <div class="form-section">
          <div class="section-title">
            <AppstoreOutlined />
            {{ t('bomPage.sectionBasic') }}
          </div>
          <a-row :gutter="16">
            <a-col :xs="24" :md="8">
              <a-form-item :label="t('bomPage.name')" required>
                <a-input v-model:value="formState.name" allow-clear :placeholder="t('bomPage.namePlaceholder')" />
              </a-form-item>
            </a-col>
            <a-col :xs="24" :md="8">
              <a-form-item :label="t('bomPage.code')" required>
                <a-input
                  v-model:value="formState.selfCode"
                  allow-clear
                  :disabled="isEdit"
                  :placeholder="t('bomPage.codePlaceholder')"
                >
                  <template #prefix>
                    <BarcodeOutlined class="input-prefix-icon" />
                  </template>
                </a-input>
                <div v-if="isEdit" class="field-hint">
                  <InfoCircleOutlined />
                  {{ t('bomPage.codeEditHint') }}
                </div>
              </a-form-item>
            </a-col>
            <a-col :xs="24" :md="8">
              <a-form-item :label="t('bomPage.type')">
                <a-select
                  v-model:value="formState.type"
                  :options="typeOptions.map((item) => ({ value: Number(item.id), label: item.name }))"
                />
              </a-form-item>
            </a-col>
          </a-row>
        </div>

        <div class="form-section form-section--items">
          <div class="items-head">
            <div class="section-title">
              <ShoppingCartOutlined />
              {{ t('bomPage.sectionItems') }}
              <a-tag color="processing">{{ details.length }}</a-tag>
            </div>
            <a-space wrap>
              <a-button type="primary" size="small" @click="openMaterialPicker">
                <PlusOutlined />
                {{ t('bomPage.detailAdd') }}
              </a-button>
              <a-button size="small" danger :disabled="!selectedDetailKeys.length" @click="removeSelectedDetails">
                <DeleteOutlined />
                {{ t('bomPage.detailBatchDelete') }}
              </a-button>
            </a-space>
          </div>

          <p class="scale-hint">
            <InfoCircleOutlined />
            <span>{{ scaleHint }}</span>
          </p>

          <a-empty
            v-if="!details.length"
            class="detail-empty"
            :image-style="{ height: '56px' }"
            :description="t('bomPage.detailEmpty')"
          />

          <template v-else>
            <div class="detail-table-wrap">
              <a-table
                :row-key="detailRowKey"
                size="small"
                bordered
                class="detail-table"
                :columns="detailColumns"
                :data-source="details"
                :pagination="false"
                :row-selection="detailRowSelection"
                :scroll="detailScroll"
              >
                <template #bodyCell="{ column, record, index }">
                  <template v-if="column.key === 'index'">
                    {{ index + 1 }}
                  </template>
                  <template v-else-if="column.key === 'matName'">
                    <span class="mat-name-cell">{{ record.matName || '—' }}</span>
                  </template>
                  <template v-else-if="column.key === 'matCode'">
                    <span v-if="record.matCode" class="code-cell">{{ record.matCode }}</span>
                    <span v-else class="empty-cell">—</span>
                  </template>
                  <template v-else-if="column.key === 'matScale'">
                    <a-input-number
                      v-model:value="record.matScale"
                      :min="0"
                      :max="isPercentType ? 100 : undefined"
                      :precision="isPercentType ? 2 : 4"
                      :addon-after="isPercentType ? '%' : undefined"
                      class="scale-input"
                    />
                  </template>
                  <template v-else-if="column.key === 'action'">
                    <a-button type="link" size="small" danger @click="removeOneDetail(record)">
                      <DeleteOutlined />
                    </a-button>
                  </template>
                </template>
              </a-table>
            </div>

            <div class="detail-summary">
              <div class="detail-summary__row">
                <span class="detail-summary__item">
                  {{ t('bomPage.detailStatCount', { count: details.length }) }}
                </span>
                <span class="detail-summary__item detail-summary__item--emphasis">
                  {{
                    isPercentType
                      ? t('bomPage.detailStatPercentSum', { sum: scaleSumDisplay })
                      : t('bomPage.detailStatQtySum', { sum: scaleSumDisplay })
                  }}
                </span>
                <a-tag v-if="isPercentType" :color="percentSumValid ? 'success' : 'warning'">
                  {{ percentSumValid ? t('bomPage.detailPercentOk') : t('bomPage.detailPercentWarn') }}
                </a-tag>
              </div>
              <div
                v-if="isPercentType"
                class="percent-meter"
                :class="percentSumValid ? 'is-ok' : 'is-warn'"
              >
                <div class="percent-meter__fill" :style="{ width: `${percentFill}%` }" />
              </div>
            </div>
          </template>
        </div>
      </a-form>
      </div>
    </a-spin>

    <div class="modal-footer">
      <a-button @click="closeModal">{{ t('bomPage.cancel') }}</a-button>
      <a-button type="primary" :loading="saving" :disabled="!details.length" @click="handleSubmit">
        {{ t('bomPage.save') }}
      </a-button>
    </div>

    <MaterialMultiPickerModal
      v-model:open="materialPickerOpen"
      :exclude-mat-ids="existingMatIds"
      @confirm="onMaterialsPicked"
    />
  </a-modal>
</template>

<style scoped>
.bom-form-modal :deep(.ant-modal-body) {
  display: flex;
  flex-direction: column;
  padding: 0;
  max-height: min(78vh, 760px);
  overflow: hidden;
}

.bom-form-spin {
  display: flex;
  flex-direction: column;
  min-height: 0;
}

.bom-form-spin :deep(.ant-spin-container) {
  display: flex;
  flex-direction: column;
  min-height: 0;
}

.bom-form-scroll {
  min-height: 0;
  overflow-y: auto;
  padding: 16px 24px 12px;
}

.modal-title-wrap {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.modal-title {
  font-size: 16px;
  font-weight: 600;
  color: var(--omes-color-text);
}

.bom-form :deep(.ant-form-item) {
  margin-bottom: 0;
}

.bom-form :deep(.ant-form-item-label > label) {
  font-weight: 500;
  color: var(--omes-color-text-label);
}

.bom-form :deep(.ant-input-affix-wrapper-disabled) {
  background: var(--omes-color-bg-layout);
  color: var(--omes-color-text-secondary);
}

.form-section {
  padding: 16px 18px;
  margin-bottom: 14px;
  background: var(--omes-color-bg-elevated);
  border: 1px solid var(--omes-color-border);
  border-radius: var(--omes-radius-lg);
}

.form-section--items {
  margin-bottom: 0;
}

.section-title {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 14px;
  font-size: 14px;
  font-weight: 600;
  color: var(--omes-color-text-heading);
}

.section-title::before {
  content: '';
  width: 3px;
  height: 16px;
  margin-right: 4px;
  background: var(--omes-color-primary);
  border-radius: 2px;
  flex-shrink: 0;
}

.section-title :deep(.anticon) {
  font-size: 15px;
  color: var(--omes-color-primary);
}

.items-head {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 8px;
}

.items-head .section-title {
  margin-bottom: 0;
}

.scale-hint {
  display: flex;
  align-items: flex-start;
  gap: 6px;
  margin: 0 0 12px;
  font-size: 12px;
  line-height: 1.5;
  color: var(--omes-color-text-tertiary);
}

.scale-hint :deep(.anticon) {
  margin-top: 2px;
  flex-shrink: 0;
  color: var(--omes-color-primary);
}

.detail-empty {
  margin: 4px 0 0;
  padding: 20px 16px;
  border: 1px dashed var(--omes-color-border-hover);
  border-radius: var(--omes-radius-lg);
  background: var(--omes-color-bg-container);
}

.detail-table-wrap {
  border: 1px solid var(--omes-color-border-secondary);
  border-radius: var(--omes-radius-lg);
  background: var(--omes-color-bg-container);
  overflow: hidden;
}

.detail-table :deep(.ant-table-container) {
  border: none !important;
  border-radius: 0 !important;
}

.detail-table :deep(.ant-table-thead > tr > th) {
  background: var(--omes-color-bg-table-head);
  font-weight: 600;
  font-size: 13px;
  border-bottom: 1px solid var(--omes-color-border-secondary);
}

.detail-table :deep(.ant-table-tbody > tr:nth-child(even) > td) {
  background: var(--omes-color-bg-muted);
}

.detail-table :deep(.ant-table-tbody > tr:hover > td) {
  background: var(--omes-color-bg-table-hover) !important;
}

.detail-summary {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-top: 12px;
  padding: 10px 14px;
  background: var(--omes-color-bg-muted);
  border: 1px solid var(--omes-color-border-secondary);
  border-radius: var(--omes-radius-md);
  font-size: 13px;
}

.detail-summary__row {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 12px 16px;
}

.detail-summary__item {
  color: var(--omes-color-text-secondary);
}

.detail-summary__item--emphasis {
  font-weight: 600;
  color: var(--omes-color-text);
  font-variant-numeric: tabular-nums;
}

.percent-meter {
  height: 6px;
  border-radius: 999px;
  background: var(--omes-color-border-secondary);
  overflow: hidden;
}

.percent-meter__fill {
  height: 100%;
  border-radius: inherit;
  background: var(--omes-color-warning);
  transition: width 0.2s ease;
}

.percent-meter.is-ok .percent-meter__fill {
  background: var(--omes-color-success);
}

.mat-name-cell {
  font-weight: 500;
  color: var(--omes-color-text);
}

.code-cell {
  font-family: ui-monospace, SFMono-Regular, Menlo, Consolas, monospace;
  font-size: 12px;
  color: var(--omes-color-primary);
  background: var(--omes-color-primary-bg);
  border: 1px solid var(--omes-color-primary-border);
  padding: 2px 8px;
  border-radius: var(--omes-radius-sm);
}

.empty-cell {
  color: var(--omes-color-text-placeholder);
}

.scale-input {
  width: 100%;
}

.scale-input :deep(.ant-input-number-input) {
  text-align: right;
  font-weight: 600;
  font-variant-numeric: tabular-nums;
}

.input-prefix-icon {
  color: var(--omes-color-text-placeholder);
}

.field-hint {
  display: flex;
  align-items: flex-start;
  gap: 6px;
  margin-top: 6px;
  font-size: 12px;
  line-height: 1.5;
  color: var(--omes-color-text-quaternary);
}

.field-hint :deep(.anticon) {
  margin-top: 3px;
  flex-shrink: 0;
}

.modal-footer {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  flex-shrink: 0;
  margin: 0;
  padding: 12px 24px 16px;
  border-top: 1px solid var(--omes-color-border);
  background: var(--omes-color-bg-container);
}
</style>
