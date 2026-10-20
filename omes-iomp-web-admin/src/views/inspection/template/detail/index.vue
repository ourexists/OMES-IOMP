<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import {
  AppstoreOutlined,
  CheckSquareOutlined,
  FieldNumberOutlined,
  UnorderedListOutlined,
} from '@ant-design/icons-vue'
import { inspectItemTypeLabel } from '@/api/inspect-item'
import type { InspectRuleRecord, InspectTemplateRecord, TemplateItemRow, TemplateProductBlock } from '@/api/inspect-template'
import {
  fetchInspectTemplateWithItems,
  formatRuleCondition,
  parseRuleConfig,
  sumBlockWeight,
  templateItemsToProductBlocks,
} from '@/api/inspect-template'
import { fetchProductListAll, type ProductOption } from '@/api/device'
import InspectTemplateFormModal from '../components/InspectTemplateFormModal.vue'
import DetailHeader from './components/DetailHeader.vue'
import { message } from 'ant-design-vue'

const route = useRoute()
const router = useRouter()
const { t } = useI18n()

const templateId = computed(() => String(route.query.id || ''))
const loading = ref(false)
const template = ref<InspectTemplateRecord | null>(null)
const productBlocks = ref<TemplateProductBlock[]>([])
const productOptions = ref<ProductOption[]>([])
const formOpen = ref(false)

const productNameMap = computed(() => {
  const map = new Map<string, string>()
  for (const p of productOptions.value) {
    if (p.code) {
      map.set(p.code, p.name || p.code)
    }
  }
  return map
})

const itemCount = computed(() =>
  productBlocks.value.reduce((sum, block) => sum + block.rows.length, 0),
)

const typeTone = (itemType: number) => {
  if (itemType === 3) {
    return 'bool'
  }
  if (itemType === 1) {
    return 'choice'
  }
  return 'numeric'
}

const typeIcon = (itemType: number) => {
  if (itemType === 3) {
    return CheckSquareOutlined
  }
  if (itemType === 1) {
    return UnorderedListOutlined
  }
  return FieldNumberOutlined
}

function resolveProductLabel(code: string) {
  if (!code) {
    return t('inspectTemplateDetailPage.unassignedProduct')
  }
  return productNameMap.value.get(code) || code
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

async function loadProducts() {
  if (productOptions.value.length === 0) {
    productOptions.value = (await fetchProductListAll()) || []
  }
}

async function loadDetail() {
  if (!templateId.value) {
    message.error(t('inspectTemplateDetailPage.missingId'))
    return
  }
  loading.value = true
  try {
    await loadProducts()
    const detail = await fetchInspectTemplateWithItems(templateId.value)
    template.value = detail
    productBlocks.value = templateItemsToProductBlocks(detail?.items || [])
  } finally {
    loading.value = false
  }
}

function goBack() {
  if (window.history.length > 1) {
    router.back()
    return
  }
  router.push('/view/inspect_template_tables')
}

function openEdit() {
  formOpen.value = true
}

function onEditSuccess() {
  void loadDetail()
}

watch(templateId, () => {
  void loadDetail()
})

onMounted(() => {
  void loadDetail()
})
</script>

<template>
  <div class="admin-page inspect-template-detail-page">
    <DetailHeader
      :template="template"
      :product-block-count="productBlocks.length"
      :item-count="itemCount"
      @refresh="loadDetail"
      @back="goBack"
      @edit="openEdit"
    />

    <div class="detail-body">
        <div v-if="productBlocks.length === 0" class="empty-wrap">
          <a-empty :description="t('inspectTemplateDetailPage.emptyItems')" />
        </div>

        <div v-else class="product-blocks">
        <div
          v-for="(block, blockIndex) in productBlocks"
          :key="block.key"
          class="product-block"
        >
          <div class="product-block__head">
            <div class="product-block__head-left">
              <span class="product-block__badge">{{ blockIndex + 1 }}</span>
              <AppstoreOutlined class="product-block__icon" />
              <div class="product-block__title">
                <span class="product-block__name">{{ resolveProductLabel(block.productCode) }}</span>
                <span v-if="block.productCode" class="product-block__code">{{ block.productCode }}</span>
              </div>
            </div>
            <div class="product-block__stats">
              <a-tag color="blue">
                {{ t('inspectTemplateDetailPage.blockItemCount', { count: block.rows.length }) }}
              </a-tag>
              <a-tag color="processing">
                {{ t('inspectTemplatePage.weightTotal') }}: {{ sumBlockWeight(block.rows) }}
              </a-tag>
            </div>
          </div>

          <div v-if="block.rows.length === 0" class="product-block__empty">
            {{ t('inspectTemplatePage.noItemsInBlock') }}
          </div>

          <div v-else class="item-list">
            <div
              v-for="(row, rowIndex) in block.rows"
              :key="row.key"
              class="item-card"
            >
              <div class="item-card__main">
                <span class="item-card__no">{{ rowIndex + 1 }}</span>
                <div class="item-card__info">
                  <div class="item-card__name-row">
                    <span class="item-card__name">{{ row.itemName }}</span>
                    <span class="type-tag" :class="`type-tag--${typeTone(row.itemType)}`">
                      <component :is="typeIcon(row.itemType)" />
                      {{ inspectItemTypeLabel(row.itemType) }}
                    </span>
                  </div>
                  <div class="item-card__meta">
                    <span v-if="row.unit">{{ t('inspectTemplatePage.colUnit') }}: {{ row.unit }}</span>
                    <span>{{ t('inspectTemplatePage.colWeight') }}: {{ row.weight ?? 0 }}</span>
                    <span>
                      {{ t('inspectTemplatePage.colWeightRate') }}:
                      {{ row.weightRate != null && row.weightRate > 0 ? row.weightRate : '-' }}
                    </span>
                  </div>
                </div>
              </div>

              <div class="item-card__rules">
                <div class="rules-title">{{ t('inspectTemplateDetailPage.rulesTitle') }}</div>
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
                    <span class="rule-chip__arrow">-&gt;</span>
                    <span class="rule-chip__score">
                      {{ t('inspectTemplateDetailPage.ruleScoreValue', { score: rule.weight ?? 0 }) }}
                    </span>
                  </div>
                </div>
                <p v-if="getRules(row).length === 0" class="rules-hint">
                  {{ t('inspectTemplateDetailPage.ruleDefaultHint') }}
                </p>
              </div>
            </div>
          </div>
        </div>
        </div>
    </div>

    <InspectTemplateFormModal
      v-model:open="formOpen"
      :record="template"
      @success="onEditSuccess"
    />
  </div>
</template>

<style scoped>
.inspect-template-detail-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
  min-height: 0;
  height: 100%;
}

.inspect-template-detail-page > :first-child {
  flex-shrink: 0;
}

.detail-body {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  overflow-x: hidden;
  padding-right: 2px;
}

.detail-body :deep(.ant-spin-nested-loading),
.detail-body :deep(.ant-spin-container) {
  min-height: min-content;
}

.empty-wrap {
  padding: 48px 24px;
  background: var(--omes-color-bg-container);
  border: 1px solid var(--omes-color-primary-border);
  border-radius: 14px;
}

.product-blocks {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.product-block {
  background: var(--omes-color-bg-container);
  border: 1px solid var(--omes-color-primary-border);
  border-radius: 14px;
  overflow: hidden;
  box-shadow: 0 4px 16px rgba(15, 23, 42, 0.04);
}

.product-block__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
  padding: 14px 18px;
  background: linear-gradient(180deg, var(--omes-color-bg-table-hover-alt) 0%, var(--omes-color-primary-bg-hover) 100%);
  border-bottom: 1px solid var(--omes-color-primary-border);
}

.product-block__head-left {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
}

.product-block__badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 24px;
  height: 24px;
  border-radius: 50%;
  font-size: 12px;
  font-weight: 700;
  color: var(--omes-color-primary);
  background: var(--omes-color-bg-container);
  border: 1px solid var(--omes-color-primary-border);
  flex-shrink: 0;
}

.product-block__icon {
  font-size: 18px;
  color: var(--omes-color-primary);
  flex-shrink: 0;
}

.product-block__title {
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
}

.product-block__name {
  font-size: 15px;
  font-weight: 600;
  color: var(--omes-color-text);
}

.product-block__code {
  font-size: 12px;
  color: var(--omes-color-text-quaternary);
}

.product-block__stats {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.product-block__empty {
  padding: 32px 18px;
  text-align: center;
  font-size: 13px;
  color: var(--omes-color-text-quaternary);
}

.item-list {
  display: flex;
  flex-direction: column;
  gap: 0;
}

.item-card {
  display: grid;
  grid-template-columns: minmax(220px, 1fr) minmax(280px, 1.2fr);
  gap: 16px;
  padding: 16px 18px;
  border-bottom: 1px solid var(--omes-color-border);
}

.item-card:last-child {
  border-bottom: none;
}

.item-card__main {
  display: flex;
  gap: 12px;
  min-width: 0;
}

.item-card__no {
  flex: 0 0 24px;
  font-size: 12px;
  font-weight: 700;
  color: var(--omes-color-text-placeholder);
  text-align: center;
  padding-top: 2px;
}

.item-card__info {
  min-width: 0;
}

.item-card__name-row {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
  margin-bottom: 6px;
}

.item-card__name {
  font-size: 14px;
  font-weight: 600;
  color: var(--omes-color-text);
}

.type-tag {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 2px 8px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 500;
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

.item-card__meta {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  font-size: 12px;
  color: var(--omes-color-text-quaternary);
}

.item-card__rules {
  min-width: 0;
}

.rules-title {
  margin-bottom: 8px;
  font-size: 12px;
  font-weight: 600;
  color: var(--omes-color-text-quaternary);
}

.rules-empty {
  font-size: 13px;
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
  gap: 8px;
  align-self: flex-start;
  padding: 6px 12px;
  border-radius: var(--omes-radius-md);
  border: 1px solid transparent;
  font-size: 13px;
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

.rules-hint {
  margin: 8px 0 0;
  font-size: 12px;
  color: var(--omes-color-text-placeholder);
  line-height: 1.5;
}

@media (max-width: 900px) {
  .item-card {
    grid-template-columns: 1fr;
  }
}
</style>
