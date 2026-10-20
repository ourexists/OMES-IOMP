<script setup lang="ts">
import { computed, nextTick, onMounted, onUnmounted, reactive, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import {
  AppstoreOutlined,
  CheckSquareOutlined,
  DeleteOutlined,
  FieldNumberOutlined,
  FileTextOutlined,
  HolderOutlined,
  PlusOutlined,
  SettingOutlined,
  UnorderedListOutlined,
} from '@ant-design/icons-vue'
import type { InspectItemRecord } from '@/api/inspect-item'
import { inspectItemTypeLabel } from '@/api/inspect-item'
import type { InspectRuleRecord, InspectTemplateRecord, TemplateItemRow, TemplateProductBlock } from '@/api/inspect-template'
import {
  fetchInspectTemplateWithItems,
  formatRuleCondition,
  parseRuleConfig,
  productBlocksToTemplateItems,
  roundWeightRate,
  saveInspectTemplate,
  templateItemsToProductBlocks,
} from '@/api/inspect-template'
import { fetchProductListAll, type ProductOption } from '@/api/device'
import InspectRuleConfigModal from './InspectRuleConfigModal.vue'
import LoadInspectItemsModal from './LoadInspectItemsModal.vue'
import { message } from 'ant-design-vue'

const props = defineProps<{
  open: boolean
  record: InspectTemplateRecord | null
}>()

const emit = defineEmits<{
  'update:open': [value: boolean]
  success: []
}>()

const { t } = useI18n()

const loading = ref(false)
const saving = ref(false)
const productOptions = ref<ProductOption[]>([])
const productBlocks = ref<TemplateProductBlock[]>([])

const initialSnapshot = ref({
  name: '',
  remark: '',
  blocks: [] as TemplateProductBlock[],
})

const formState = reactive({
  id: '' as string | null,
  name: '',
  remark: '',
})

const loadItemsOpen = ref(false)
const loadItemsBlockKey = ref('')

const ruleConfigOpen = ref(false)
const ruleConfigRow = ref<TemplateItemRow | null>(null)
const activeBlockKey = ref('')

/** 弹窗最大高度、列表区滚动最小高度 */
const MODAL_MAX_HEIGHT = 900
const MODAL_VIEWPORT_GAP = 24
const ITEMS_SCROLL_MIN = 320

/** 巡检项列表滚动最大高度（px），按视口计算，保证内容在内部滚动 */
const itemsScrollMaxPx = ref(400)

function recalcItemsScrollMax() {
  const modal = document.querySelector(
    '.inspect-template-form-modal-wrap .inspect-template-form-modal .ant-modal-content',
  )
  if (!modal) {
    const modalH = Math.min(MODAL_MAX_HEIGHT, window.innerHeight - MODAL_VIEWPORT_GAP)
    itemsScrollMaxPx.value = Math.max(ITEMS_SCROLL_MIN, Math.floor(modalH - 340))
    return
  }

  const body = modal.querySelector('.ant-modal-body')
  const head = modal.querySelector('.product-tabs .ant-tabs-tabpane-active .product-block__head')
  const sectionBasic = modal.querySelector('.form-section:not(.form-section--items)')
  const sectionItemsHead = modal.querySelector('.form-section--items .section-head')
  const tabsNav = modal.querySelector('.product-tabs .ant-tabs-nav')
  const itemsSection = modal.querySelector('.form-section--items')

  const sumHeights = (nodes: (Element | null | undefined)[]) =>
    nodes.reduce((sum, el) => sum + (el?.getBoundingClientRect().height ?? 0), 0)

  if (body && head) {
    const bodyH = body.getBoundingClientRect().height
    const bodyStyle = getComputedStyle(body)
    const bodyPaddingY =
      (parseFloat(bodyStyle.paddingTop) || 0) + (parseFloat(bodyStyle.paddingBottom) || 0)
    const itemsStyle = itemsSection ? getComputedStyle(itemsSection) : null
    const itemsPaddingY = itemsSection
      ? (parseFloat(itemsStyle!.paddingTop) || 0) + (parseFloat(itemsStyle!.paddingBottom) || 0)
      : 32

    const aboveScroll =
      sumHeights([sectionBasic, sectionItemsHead, tabsNav, head]) +
      bodyPaddingY +
      itemsPaddingY +
      12

    itemsScrollMaxPx.value = Math.max(ITEMS_SCROLL_MIN, Math.floor(bodyH - aboveScroll))
    return
  }

  const modalRect = modal.getBoundingClientRect()
  const modalFooter = modal.querySelector('.ant-modal-footer')
  const chrome =
    sumHeights([
      modal.querySelector('.ant-modal-header'),
      sectionBasic,
      sectionItemsHead,
      tabsNav,
      modalFooter,
    ]) + 40

  itemsScrollMaxPx.value = Math.max(ITEMS_SCROLL_MIN, Math.floor(modalRect.height - chrome))
}

const isEdit = computed(() => Boolean(props.record?.id))
const title = computed(() =>
  isEdit.value ? t('inspectTemplatePage.formEdit') : t('inspectTemplatePage.formAdd'),
)

function cloneBlocks(blocks: TemplateProductBlock[]): TemplateProductBlock[] {
  return blocks.map((block) => ({
    ...block,
    rows: block.rows.map((row) => ({ ...row })),
  }))
}

function createBlock(): TemplateProductBlock {
  return {
    key: `block-${Date.now()}-${Math.random().toString(36).slice(2, 7)}`,
    productCode: '',
    rows: [],
  }
}

function createRow(partial?: Partial<TemplateItemRow>): TemplateItemRow {
  return {
    key: `row-${Date.now()}-${Math.random().toString(36).slice(2, 7)}`,
    itemName: '',
    itemType: 2,
    unit: '',
    weight: null,
    weightRate: null,
    ...partial,
  }
}

function blockWeightTotal(block: TemplateProductBlock): number {
  return block.rows.reduce((sum, row) => {
    const w = Number(row.weight)
    return sum + (Number.isFinite(w) && w > 0 ? w : 0)
  }, 0)
}

function refreshBlockWeightRates(block: TemplateProductBlock) {
  const total = blockWeightTotal(block)
  for (const row of block.rows) {
    const w = Number(row.weight)
    if (Number.isFinite(w) && w > 0 && total > 0) {
      row.weightRate = roundWeightRate(w / total)
    } else {
      row.weightRate = null
    }
  }
}

function onWeightChange(block: TemplateProductBlock) {
  refreshBlockWeightRates(block)
}

function syncActiveBlockKey() {
  if (productBlocks.value.length === 0) {
    activeBlockKey.value = ''
    return
  }
  if (!productBlocks.value.some((b) => b.key === activeBlockKey.value)) {
    activeBlockKey.value = productBlocks.value[0].key
  }
}

function addProductBlock() {
  const block = createBlock()
  productBlocks.value.push(block)
  activeBlockKey.value = block.key
}

function removeProductBlock(blockKey: string) {
  const index = productBlocks.value.findIndex((b) => b.key === blockKey)
  productBlocks.value = productBlocks.value.filter((b) => b.key !== blockKey)
  if (productBlocks.value.length === 0) {
    addProductBlock()
    return
  }
  if (activeBlockKey.value === blockKey) {
    const nextIndex = Math.min(index, productBlocks.value.length - 1)
    activeBlockKey.value = productBlocks.value[nextIndex].key
  }
}

function onTabEdit(targetKey: string | MouseEvent, action: 'add' | 'remove') {
  if (action === 'add') {
    addProductBlock()
    return
  }
  removeProductBlock(String(targetKey))
}

function addManualRow(block: TemplateProductBlock) {
  block.rows.push(createRow())
}

function removeRow(block: TemplateProductBlock, rowKey: string) {
  block.rows = block.rows.filter((r) => r.key !== rowKey)
  refreshBlockWeightRates(block)
}

function openLoadItems(block: TemplateProductBlock) {
  if (!block.productCode.trim()) {
    message.warning(t('inspectTemplatePage.selectProductFirst'))
    return
  }
  loadItemsBlockKey.value = block.key
  loadItemsOpen.value = true
}

function onLoadItemsConfirm(items: InspectItemRecord[]) {
  const block = productBlocks.value.find((b) => b.key === loadItemsBlockKey.value)
  if (!block) {
    return
  }
  for (const item of items) {
    block.rows.push(
      createRow({
        referenceItemId: item.id,
        itemName: item.itemName || '',
        itemType: item.itemType ?? 2,
        unit: item.unit || '',
        weight: null,
      }),
    )
  }
  refreshBlockWeightRates(block)
}

function openRuleConfig(row: TemplateItemRow) {
  ruleConfigRow.value = row
  ruleConfigOpen.value = true
}

function onRuleConfigSave(rules: InspectRuleRecord[]) {
  if (ruleConfigRow.value) {
    ruleConfigRow.value.ruleConfig = JSON.stringify(rules)
  }
}

function moveRow(block: TemplateProductBlock, fromIndex: number, toIndex: number) {
  if (fromIndex === toIndex || fromIndex < 0 || toIndex < 0 || fromIndex >= block.rows.length || toIndex >= block.rows.length) {
    return
  }
  const rows = [...block.rows]
  const [moved] = rows.splice(fromIndex, 1)
  rows.splice(toIndex, 0, moved)
  block.rows = rows
}

let dragFromIndex = -1
let dragBlockKey = ''

function onDragStart(blockKey: string, index: number) {
  dragBlockKey = blockKey
  dragFromIndex = index
}

function onDrop(blockKey: string, toIndex: number) {
  if (dragBlockKey !== blockKey || dragFromIndex < 0) {
    return
  }
  const block = productBlocks.value.find((b) => b.key === blockKey)
  if (block) {
    moveRow(block, dragFromIndex, toIndex)
  }
  dragFromIndex = -1
  dragBlockKey = ''
}

function resetFormState() {
  formState.id = null
  formState.name = ''
  formState.remark = ''
  productBlocks.value = [createBlock()]
  activeBlockKey.value = productBlocks.value[0].key
}

function snapshotForm() {
  initialSnapshot.value = {
    name: formState.name,
    remark: formState.remark,
    blocks: cloneBlocks(productBlocks.value),
  }
}

function resetForm() {
  formState.name = initialSnapshot.value.name
  formState.remark = initialSnapshot.value.remark
  productBlocks.value = cloneBlocks(initialSnapshot.value.blocks)
  syncActiveBlockKey()
}

async function loadProducts() {
  if (productOptions.value.length === 0) {
    productOptions.value = (await fetchProductListAll()) || []
  }
}

async function loadDetail() {
  loading.value = true
  try {
    await loadProducts()
    if (!props.record?.id) {
      resetFormState()
      snapshotForm()
      return
    }
    const detail = await fetchInspectTemplateWithItems(props.record.id)
    formState.id = detail?.id || props.record.id
    formState.name = detail?.name || ''
    formState.remark = detail?.remark || ''
    productBlocks.value = templateItemsToProductBlocks(detail?.items || [])
    for (const block of productBlocks.value) {
      refreshBlockWeightRates(block)
    }
    syncActiveBlockKey()
    snapshotForm()
  } finally {
    loading.value = false
    await nextTick()
    recalcItemsScrollMax()
  }
}

const BODY_LOCK_CLASS = 'inspect-template-modal-open'

function setBodyScrollLock(locked: boolean) {
  if (locked) {
    document.body.classList.add(BODY_LOCK_CLASS)
  } else {
    document.body.classList.remove(BODY_LOCK_CLASS)
  }
}

watch(
  () => activeBlockKey.value,
  () => {
    if (props.open) {
      nextTick(recalcItemsScrollMax)
    }
  },
)

watch(
  () => props.open,
  (open) => {
    setBodyScrollLock(open)
    if (open) {
      loadDetail()
      nextTick(recalcItemsScrollMax)
    }
  },
)

onMounted(() => {
  window.addEventListener('resize', recalcItemsScrollMax)
})

onUnmounted(() => {
  window.removeEventListener('resize', recalcItemsScrollMax)
  setBodyScrollLock(false)
})

function closeModal() {
  emit('update:open', false)
}

async function handleSubmit() {
  const name = formState.name.trim()
  if (!name) {
    message.warning(t('inspectTemplatePage.nameRequired'))
    return
  }
  saving.value = true
  try {
    await saveInspectTemplate({
      id: formState.id || undefined,
      name,
      remark: formState.remark.trim() || undefined,
      items: productBlocksToTemplateItems(productBlocks.value, formState.id || undefined),
    })
    message.success(t('inspectTemplatePage.saveSuccess'))
    emit('success')
    closeModal()
  } finally {
    saving.value = false
  }
}

function productSelectOptions() {
  return productOptions.value.map((p) => ({
    value: p.code || '',
    label: p.name || p.code || '-',
  }))
}

function resolveBlockTabLabel(block: TemplateProductBlock, index: number) {
  const code = block.productCode?.trim()
  if (!code) {
    return t('inspectTemplatePage.productTabUnnamed', { index: index + 1 })
  }
  const option = productOptions.value.find((p) => p.code === code)
  const name = option?.name || code
  const count = block.rows.length
  return count > 0 ? `${name} (${count})` : name
}

function typeTone(itemType: number) {
  if (itemType === 3) {
    return 'bool'
  }
  if (itemType === 1) {
    return 'choice'
  }
  return 'numeric'
}

function typeIcon(itemType: number) {
  if (itemType === 3) {
    return CheckSquareOutlined
  }
  if (itemType === 1) {
    return UnorderedListOutlined
  }
  return FieldNumberOutlined
}

function ruleLabels() {
  return {
    yes: t('inspectTemplatePage.yes'),
    no: t('inspectTemplatePage.no'),
    any: t('inspectTemplateDetailPage.ruleAnyValue'),
  }
}

function getRules(row: TemplateItemRow): InspectRuleRecord[] {
  return parseRuleConfig(row.ruleConfig)
}

function formatCondition(rule: InspectRuleRecord, itemType: number) {
  return formatRuleCondition(rule, itemType, ruleLabels())
}
</script>

<template>
  <a-modal
    :open="open"
    width="1080px"
    destroy-on-close
    class="inspect-template-form-modal"
    wrap-class-name="inspect-template-form-modal-wrap"
    :mask-closable="false"
    @cancel="closeModal"
    @update:open="emit('update:open', $event)"
  >
    <template #title>
      <span class="modal-title">
        <FileTextOutlined />
        {{ title }}
      </span>
    </template>

    <template #footer>
      <div class="modal-footer">
        <a-button @click="resetForm">{{ t('inspectTemplatePage.reset') }}</a-button>
        <a-button @click="closeModal">{{ t('inspectTemplatePage.cancel') }}</a-button>
        <a-button type="primary" :loading="saving" @click="handleSubmit">
          {{ t('inspectTemplatePage.save') }}
        </a-button>
      </div>
    </template>

    <div class="modal-body-shell">
      <a-spin :spinning="loading" class="modal-body-spin">
        <div class="template-form">
        <div class="form-section">
          <div class="section-title">
            <FileTextOutlined />
            {{ t('inspectTemplatePage.formBasic') }}
          </div>
          <a-form layout="vertical" class="basic-form">
            <a-row :gutter="16">
              <a-col :span="12">
                <a-form-item :label="t('inspectTemplatePage.colName')" required>
                  <a-input
                    v-model:value="formState.name"
                    :placeholder="t('inspectTemplatePage.namePlaceholder')"
                    allow-clear
                    size="large"
                  />
                </a-form-item>
              </a-col>
              <a-col :span="12">
                <a-form-item :label="t('inspectTemplatePage.colRemark')">
                  <a-input
                    v-model:value="formState.remark"
                    :placeholder="t('inspectTemplatePage.remarkPlaceholder')"
                    allow-clear
                    size="large"
                  />
                </a-form-item>
              </a-col>
            </a-row>
          </a-form>
        </div>

        <div class="form-section form-section--items">
          <div class="section-head">
            <div class="section-title section-title--inline">
              <UnorderedListOutlined />
              <span>{{ t('inspectTemplatePage.formItems') }}</span>
              <span class="section-sub">{{ t('inspectTemplatePage.formItemsTabHint') }}</span>
            </div>
          </div>

          <a-tabs
            v-model:active-key="activeBlockKey"
            type="editable-card"
            class="product-tabs"
            @edit="onTabEdit"
          >
            <a-tab-pane
              v-for="(block, blockIndex) in productBlocks"
              :key="block.key"
              :tab="resolveBlockTabLabel(block, blockIndex)"
              :closable="productBlocks.length > 1"
            >
              <div class="product-block">
                <div class="product-block__head">
                  <div class="product-block__head-left">
                    <AppstoreOutlined class="product-block__icon" />
                    <a-select
                      v-model:value="block.productCode"
                      :options="productSelectOptions()"
                      :placeholder="t('inspectTemplatePage.selectProduct')"
                      class="product-select"
                      allow-clear
                      show-search
                      option-filter-prop="label"
                    />
                  </div>
                  <div class="product-block__head-right">
                    <a-tag color="blue">
                      {{ t('inspectTemplateDetailPage.blockItemCount', { count: block.rows.length }) }}
                    </a-tag>
                    <a-tag color="processing">
                      {{ t('inspectTemplatePage.weightTotal') }}: {{ blockWeightTotal(block) }}
                    </a-tag>
                    <a-button size="small" type="primary" ghost @click="openLoadItems(block)">
                      {{ t('inspectTemplatePage.loadFromPool') }}
                    </a-button>
                  </div>
                </div>

                <div
                  class="product-block__scroll"
                  :style="{ maxHeight: `${itemsScrollMaxPx}px` }"
                >
                  <div v-if="block.rows.length === 0" class="product-block__empty">
                    <a-empty :image="false" :description="t('inspectTemplatePage.noItemsInBlock')">
                      <a-space>
                        <a-button size="small" type="primary" ghost @click="openLoadItems(block)">
                          {{ t('inspectTemplatePage.loadFromPool') }}
                        </a-button>
                        <a-button size="small" type="dashed" @click="addManualRow(block)">
                          <PlusOutlined />
                          {{ t('inspectTemplatePage.addManualRow') }}
                        </a-button>
                      </a-space>
                    </a-empty>
                  </div>

                  <div v-else class="item-list">
                    <div
                      v-for="(row, rowIndex) in block.rows"
                      :key="row.key"
                      class="item-card"
                      :class="row.referenceItemId ? 'item-card--ref' : 'item-card--manual'"
                      @dragover.prevent
                      @drop.prevent="onDrop(block.key, rowIndex)"
                    >
                      <div class="item-card__main">
                        <span
                          class="drag-handle"
                          draggable="true"
                          @dragstart="onDragStart(block.key, rowIndex)"
                        >
                          <HolderOutlined />
                          <span class="item-card__no">{{ rowIndex + 1 }}</span>
                        </span>

                        <div class="item-card__fields">
                          <div class="field-row field-row--name">
                            <span v-if="!row.referenceItemId" class="item-card__badge">
                              {{ t('inspectTemplatePage.manualRowBadge') }}
                            </span>
                            <a-input
                              v-if="!row.referenceItemId"
                              v-model:value="row.itemName"
                              :placeholder="t('inspectTemplatePage.itemNamePlaceholder')"
                              allow-clear
                            />
                            <span v-else class="item-card__name">{{ row.itemName }}</span>

                            <a-select
                              v-if="!row.referenceItemId"
                              v-model:value="row.itemType"
                              class="type-select"
                              :options="[
                                { value: 1, label: inspectItemTypeLabel(1) },
                                { value: 2, label: inspectItemTypeLabel(2) },
                                { value: 3, label: inspectItemTypeLabel(3) },
                              ]"
                            />
                            <span v-else class="type-tag" :class="`type-tag--${typeTone(row.itemType)}`">
                              <component :is="typeIcon(row.itemType)" />
                              {{ inspectItemTypeLabel(row.itemType) }}
                            </span>
                          </div>

                          <div class="field-row field-row--meta">
                            <div class="field-item">
                              <span class="field-label">{{ t('inspectTemplatePage.colUnit') }}</span>
                              <a-input
                                v-if="!row.referenceItemId"
                                v-model:value="row.unit"
                                allow-clear
                                size="small"
                              />
                              <span v-else class="field-value">{{ row.unit || '-' }}</span>
                            </div>
                            <div class="field-item">
                              <span class="field-label">{{ t('inspectTemplatePage.colWeight') }}</span>
                              <a-input-number
                                v-model:value="row.weight"
                                :min="0"
                                size="small"
                                class="field-input-num"
                                @change="onWeightChange(block)"
                              />
                            </div>
                            <div class="field-item">
                              <span class="field-label">{{ t('inspectTemplatePage.colWeightRate') }}</span>
                              <span class="field-value field-value--rate">
                                {{ row.weightRate != null && row.weightRate > 0 ? row.weightRate : '-' }}
                              </span>
                            </div>
                          </div>
                        </div>
                      </div>

                      <div class="item-card__side">
                        <div class="item-card__side-head">
                          <div class="rules-title">{{ t('inspectTemplateDetailPage.rulesTitle') }}</div>
                          <div class="item-card__actions">
                            <a-button type="primary" ghost size="small" @click="openRuleConfig(row)">
                              <SettingOutlined />
                              {{ t('inspectTemplatePage.configRule') }}
                            </a-button>
                            <a-button type="text" danger size="small" @click="removeRow(block, row.key)">
                              <DeleteOutlined />
                              {{ t('inspectTemplatePage.delete') }}
                            </a-button>
                          </div>
                        </div>
                        <div class="rules-panel">
                          <div v-if="getRules(row).length === 0" class="rules-empty">
                            {{ t('inspectTemplateDetailPage.ruleNotConfigured') }}
                          </div>
                          <div v-else class="rules-list">
                            <div
                              v-for="(rule, ruleIndex) in getRules(row)"
                              :key="ruleIndex"
                              class="rule-chip"
                              :class="`rule-chip--${typeTone(row.itemType)}`"
                            >
                              <span class="rule-chip__condition">{{ formatCondition(rule, row.itemType) }}</span>
                              <span class="rule-chip__arrow">→</span>
                              <span class="rule-chip__score">
                                {{ t('inspectTemplateDetailPage.ruleScoreValue', { score: rule.weight ?? 0 }) }}
                              </span>
                            </div>
                          </div>
                        </div>
                      </div>
                    </div>
                  </div>

                  <div v-if="block.rows.length > 0" class="product-block__foot">
                    <a-button size="small" type="dashed" block @click="addManualRow(block)">
                      <PlusOutlined />
                      {{ t('inspectTemplatePage.addManualRow') }}
                    </a-button>
                  </div>
                </div>
              </div>
            </a-tab-pane>
          </a-tabs>
        </div>
        </div>
      </a-spin>
    </div>

    <LoadInspectItemsModal v-model:open="loadItemsOpen" @confirm="onLoadItemsConfirm" />

    <InspectRuleConfigModal
      v-if="ruleConfigRow"
      v-model:open="ruleConfigOpen"
      :item-type="ruleConfigRow.itemType"
      :unit="ruleConfigRow.unit"
      :rules="parseRuleConfig(ruleConfigRow.ruleConfig)"
      @save="onRuleConfigSave"
    />
  </a-modal>
</template>

<style scoped>
.inspect-template-form-modal {
  --itfm-h: min(900px, calc(100vh - 24px));
}

.inspect-template-form-modal :deep(.ant-modal-content) {
  display: flex;
  flex-direction: column;
  width: 1080px !important;
  height: var(--itfm-h);
  min-height: var(--itfm-h);
  max-height: var(--itfm-h);
  overflow: hidden;
}

.inspect-template-form-modal :deep(.ant-modal-header) {
  flex: 0 0 auto;
}

.inspect-template-form-modal :deep(.ant-modal-body) {
  flex: 1 1 auto;
  min-height: 0;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  padding: 16px 24px;
}

.inspect-template-form-modal :deep(.ant-modal-footer) {
  flex: 0 0 auto;
  margin-top: 0;
  padding: 0;
  border-top: 1px solid var(--omes-color-border);
}

.modal-body-shell {
  flex: 1 1 auto;
  min-height: 0;
  height: 100%;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.modal-body-spin {
  flex: 1 1 auto;
  min-height: 0;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.modal-body-spin :deep(.ant-spin-nested-loading),
.modal-body-spin :deep(.ant-spin-container) {
  flex: 1 1 auto;
  min-height: 0;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.modal-title {
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

.template-form {
  display: flex;
  flex-direction: column;
  gap: 14px;
  flex: 1 1 auto;
  min-height: 0;
  overflow: hidden;
}

.form-section {
  padding: 16px 18px;
  background: var(--omes-color-bg-elevated);
  border: 1px solid var(--omes-color-border);
  border-radius: var(--omes-radius-lg);
  flex: 0 0 auto;
}

.form-section--items {
  background: linear-gradient(180deg, var(--omes-color-bg-container) 0%, var(--omes-color-primary-bg-hover) 100%);
  border-color: var(--omes-color-primary-border);
  flex: 1 1 auto;
  min-height: 0;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.section-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 14px;
  flex-wrap: wrap;
  flex-shrink: 0;
}

.section-title {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 16px;
  font-size: 14px;
  font-weight: 600;
  color: var(--omes-color-text);
}

.section-title--inline {
  margin-bottom: 0;
  flex-wrap: wrap;
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

.form-section--items .section-title::before {
  background: var(--omes-color-primary-hover);
}

.section-sub {
  margin-left: 4px;
  font-size: 12px;
  font-weight: 400;
  color: var(--omes-color-text-quaternary);
}

.basic-form :deep(.ant-form-item) {
  margin-bottom: 0;
}

.basic-form :deep(.ant-form-item-label > label) {
  font-weight: 500;
  color: var(--omes-color-text-label);
}

.product-tabs {
  margin-top: 4px;
  flex: 1 1 auto;
  min-height: 0;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.product-tabs :deep(.ant-tabs-nav) {
  margin-bottom: 0;
  flex: 0 0 auto;
}

.product-tabs :deep(.ant-tabs-content-holder) {
  flex: 1 1 auto;
  min-height: 0;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.product-tabs :deep(.ant-tabs-content) {
  margin-top: 0;
  flex: 1 1 auto;
  min-height: 0;
  height: 100%;
}

.product-tabs :deep(.ant-tabs-tabpane) {
  height: 100%;
  overflow: hidden;
}

.product-tabs :deep(.ant-tabs-tabpane-active) {
  display: flex !important;
  flex-direction: column;
  height: 100%;
  overflow: hidden;
}

.product-block {
  background: var(--omes-color-bg-container);
  border: 1px solid var(--omes-color-primary-border);
  border-top: none;
  border-radius: 0 0 12px 12px;
  overflow: hidden;
  flex: 1 1 auto;
  min-height: 0;
  display: flex;
  flex-direction: column;
}

.product-block__scroll {
  flex: 1 1 auto;
  width: 100%;
  min-height: 0;
  box-sizing: border-box;
  overflow-x: hidden;
  overflow-y: auto;
  scrollbar-gutter: stable;
  background: var(--omes-color-bg-container);
  padding-bottom: 12px;
}

.product-block__scroll::-webkit-scrollbar {
  width: 6px;
}

.product-block__scroll::-webkit-scrollbar-thumb {
  background: rgba(0, 0, 0, 0.15);
  border-radius: 3px;
}

.product-block__scroll::-webkit-scrollbar-thumb:hover {
  background: rgba(0, 0, 0, 0.25);
}

.product-block__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 12px;
  padding: 14px 16px;
  background: linear-gradient(180deg, var(--omes-color-bg-table-hover-alt) 0%, var(--omes-color-primary-bg-hover) 100%);
  border-bottom: 1px solid var(--omes-color-primary-border);
  flex-shrink: 0;
}

.product-block__head-left {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 10px;
  min-width: 0;
}

.product-block__head-right {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
}

.product-block__badge {
  display: none;
}

.product-block__icon {
  color: var(--omes-color-primary);
  font-size: 18px;
  flex-shrink: 0;
}

.product-select {
  min-width: 200px;
  max-width: 280px;
}

.product-block__empty {
  padding: 28px 16px;
  min-height: 120px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.item-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 8px 0 4px;
}

.item-card {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(260px, 340px);
  gap: 16px;
  padding: 14px 16px;
  background: var(--omes-color-bg-container);
  border: 1px solid var(--omes-color-border);
  border-radius: var(--omes-radius-md);
  transition: border-color 0.15s, box-shadow 0.15s;
}

.item-card:hover {
  border-color: var(--omes-color-border-secondary);
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.06);
}

.item-card--manual {
  border-style: dashed;
  border-color: var(--omes-color-border-secondary);
  background: var(--omes-color-bg-elevated);
}

.item-card--manual:hover {
  border-color: var(--omes-color-border-hover);
  background: var(--omes-color-bg-container);
}

.item-card__badge {
  flex-shrink: 0;
  padding: 0 6px;
  font-size: 12px;
  line-height: 20px;
  color: var(--omes-color-text-tertiary);
  background: var(--omes-color-bg-layout);
  border: 1px solid var(--omes-color-border-hover);
  border-radius: 4px;
}

.item-card__main {
  display: flex;
  gap: 10px;
  min-width: 0;
}

.drag-handle {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  padding-top: 6px;
  cursor: grab;
  color: rgba(0, 0, 0, 0.25);
  user-select: none;
  flex-shrink: 0;
}

.drag-handle:active {
  cursor: grabbing;
  color: var(--omes-color-text-quaternary);
}

.item-card__no {
  font-size: 12px;
  font-weight: 600;
  color: var(--omes-color-text-quaternary);
  font-variant-numeric: tabular-nums;
}

.item-card__fields {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.field-row {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 10px;
}

.field-row--name {
  gap: 12px;
}

.field-row--name :deep(.ant-input) {
  flex: 1;
  min-width: 140px;
}

.item-card__name {
  flex: 1;
  font-size: 14px;
  font-weight: 600;
  color: var(--omes-color-text);
}

.type-select {
  width: 110px;
  flex-shrink: 0;
}

.type-tag {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 2px 10px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 500;
  border: 1px solid transparent;
  flex-shrink: 0;
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

.field-row--meta {
  gap: 16px;
}

.field-item {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
}

.field-label {
  font-size: 12px;
  color: var(--omes-color-text-quaternary);
  white-space: nowrap;
}

.field-value {
  font-size: 13px;
  color: var(--omes-color-text-label);
}

.field-value--rate {
  font-variant-numeric: tabular-nums;
  font-weight: 500;
  color: var(--omes-color-text-label);
}

.field-input-num {
  width: 88px;
}

.item-card__side {
  display: flex;
  flex-direction: column;
  gap: 8px;
  min-width: 0;
  padding-left: 16px;
  border-left: 1px solid var(--omes-color-border);
}

.item-card__side-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  min-width: 0;
}

.rules-panel {
  flex: 1;
  min-width: 0;
}

.rules-title {
  margin-bottom: 0;
  font-size: 12px;
  font-weight: 600;
  color: var(--omes-color-text-quaternary);
  flex-shrink: 0;
}

.rules-empty {
  font-size: 12px;
  color: var(--omes-color-text-placeholder);
  font-style: italic;
}

.rules-list {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.rule-chip {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  align-self: flex-start;
  padding: 4px 10px;
  border-radius: var(--omes-radius-md);
  border: 1px solid transparent;
  font-size: 12px;
}

.rule-chip--choice {
  background: var(--omes-color-accent-purple-bg);
  border-color: var(--omes-color-accent-purple-border);
}

.rule-chip--numeric {
  background: var(--omes-color-bg-table-hover-alt);
  border-color: var(--omes-color-primary-border);
}

.rule-chip--bool {
  background: var(--omes-color-success-bg);
  border-color: var(--omes-color-success-border);
}

.rule-chip__condition {
  font-weight: 500;
  color: var(--omes-color-text-label);
}

.rule-chip__arrow {
  color: rgba(0, 0, 0, 0.25);
}

.rule-chip__score {
  font-weight: 600;
  color: var(--omes-color-primary);
}

.item-card__actions {
  display: flex;
  flex-shrink: 0;
  flex-wrap: nowrap;
  align-items: center;
  justify-content: flex-end;
  gap: 6px;
  margin-left: auto;
}

.product-block__foot {
  padding: 10px 16px 14px;
  border-top: 1px solid var(--omes-color-border);
  background: var(--omes-color-bg-elevated);
  flex: 0 0 auto;
}

.modal-footer {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  width: 100%;
  padding: 12px 24px;
  background: var(--omes-color-bg-container);
}

@media (max-width: 900px) {
  .item-card {
    grid-template-columns: 1fr;
  }

  .item-card__side {
    padding-left: 0;
    padding-top: 12px;
    border-left: none;
    border-top: 1px solid var(--omes-color-border);
  }

  .product-select {
    min-width: 160px;
    max-width: 100%;
  }

  .product-block__head {
    flex-direction: column;
    align-items: stretch;
  }
}
</style>

<style>
/* 遮罩层：防止背景滚动，弹窗固定居中 */
.inspect-template-form-modal-wrap {
  overflow: hidden !important;
  display: flex !important;
  align-items: center !important;
  justify-content: center !important;
  padding: 16px 16px !important;
}

.inspect-template-form-modal-wrap .ant-modal {
  top: auto !important;
  padding-bottom: 0 !important;
  margin: 0 auto !important;
  max-width: calc(100vw - 32px);
}

/* 固定弹窗高度（非 scoped，确保 teleport 仍生效） */
.inspect-template-form-modal-wrap .inspect-template-form-modal.ant-modal .ant-modal-content {
  display: flex;
  flex-direction: column;
  width: 1080px !important;
  height: min(900px, calc(100vh - 24px));
  min-height: min(900px, calc(100vh - 24px));
  max-height: min(900px, calc(100vh - 24px));
  overflow: hidden;
}

.inspect-template-form-modal-wrap .inspect-template-form-modal.ant-modal .ant-modal-body {
  flex: 1 1 auto;
  min-height: 0;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.inspect-template-form-modal-wrap .inspect-template-form-modal.ant-modal .ant-modal-footer {
  flex: 0 0 auto;
}

body.inspect-template-modal-open {
  overflow: hidden !important;
}

/* Tabs 与内容区高度约束（全局），配合列表 max-height */
.inspect-template-form-modal-wrap .product-tabs.ant-tabs {
  display: flex;
  flex-direction: column;
  min-height: 0;
}

.inspect-template-form-modal-wrap .product-tabs .ant-tabs-content-holder {
  min-height: 0;
  overflow: hidden;
}
</style>
