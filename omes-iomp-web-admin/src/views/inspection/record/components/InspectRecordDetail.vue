<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import {
  ToolOutlined,
  UnorderedListOutlined,
} from '@ant-design/icons-vue'
import type { InspectRecordItemRecord, InspectRecordRecord } from '@/api/inspect-record'
import { fetchInspectRecordById } from '@/api/inspect-record'

const props = defineProps<{
  recordId?: string | null
  /** 弹窗内嵌时略收紧外边距 */
  compact?: boolean
}>()

const { t } = useI18n()

const loading = ref(false)
const record = ref<InspectRecordRecord | null>(null)

const items = computed(() => record.value?.items || [])
const itemCount = computed(() => items.value.length)

const itemColumns = computed(() => [
  { title: '#', key: 'index', width: 48, align: 'center' as const },
  { title: t('inspectRecordPage.colItemName'), dataIndex: 'itemName', key: 'itemName', ellipsis: true, minWidth: 120 },
  { title: t('inspectRecordPage.colContent'), dataIndex: 'content', key: 'content', ellipsis: true, minWidth: 140 },
  { title: t('inspectRecordPage.colRuleScore'), key: 'ruleScore', width: 96, align: 'center' as const },
  { title: t('inspectRecordPage.colItemScore'), key: 'score', width: 88, align: 'center' as const },
  { title: t('inspectRecordPage.colItemRemark'), dataIndex: 'remark', key: 'remark', ellipsis: true, minWidth: 100 },
])

const summaryMeta = computed(() => {
  const parts: string[] = []
  if (record.value?.recordTime) {
    parts.push(`${t('inspectRecordPage.colRecordTime')}: ${record.value.recordTime}`)
  }
  if (record.value?.createTime) {
    parts.push(`${t('inspectRecordPage.colCreateTime')}: ${record.value.createTime}`)
  }
  if (record.value?.taskId) {
    parts.push(t('inspectRecordPage.taskIdLabel', { id: record.value.taskId }))
  }
  return parts.join(' · ')
})

function itemRowKey(row: InspectRecordItemRecord): string {
  if (row.id) return row.id
  return [row.recordId, row.itemId, row.itemName, row.content].map((v) => v ?? '').join('|')
}

function displayScore(value?: number | null): string {
  return value != null ? String(value) : '-'
}

async function loadRecord(id: string) {
  loading.value = true
  try {
    record.value = await fetchInspectRecordById(id)
  } finally {
    loading.value = false
  }
}

watch(
  () => props.recordId,
  (id) => {
    record.value = null
    if (id) {
      loadRecord(id)
    }
  },
  { immediate: true },
)
</script>

<template>
  <a-spin :spinning="loading">
    <div v-if="record" class="record-detail" :class="{ 'record-detail--compact': compact }">
      <section class="record-summary">
        <div class="record-summary__overview">
          <div class="record-summary__icon">
            <ToolOutlined />
          </div>
          <div class="record-summary__main">
            <h2 class="record-summary__equip">{{ record.equipName || '—' }}</h2>
            <p v-if="record.equipSelfCode" class="record-summary__code">
              {{ t('inspectRecordPage.colEquipCode') }}: {{ record.equipSelfCode }}
            </p>
            <p class="record-summary__meta">{{ summaryMeta || '—' }}</p>
          </div>
        </div>
        <div class="record-summary__stats">
          <div class="stat-card stat-card--score">
            <span class="stat-card__value">{{ displayScore(record.score) }}</span>
            <span class="stat-card__label">{{ t('inspectRecordPage.colScore') }}</span>
          </div>
          <div class="stat-card">
            <span class="stat-card__value">{{ itemCount }}</span>
            <span class="stat-card__label">{{ t('inspectRecordPage.colItemCount') }}</span>
          </div>
        </div>
      </section>

      <section class="detail-panel">
        <div class="detail-panel__head">
          <div class="detail-panel__title">
            <UnorderedListOutlined />
            {{ t('inspectRecordPage.detailItems') }}
            <span class="detail-panel__hint">{{ t('inspectRecordPage.detailItemsHint') }}</span>
          </div>
          <a-tag color="processing">{{ t('inspectRecordPage.itemCountTag', { count: itemCount }) }}</a-tag>
        </div>

        <div v-if="items.length === 0" class="items-empty">
          <a-empty :image="false" :description="t('inspectRecordPage.detailItemsEmpty')" />
        </div>

        <a-table
          v-else
          :row-key="itemRowKey"
          size="middle"
          bordered
          class="items-table"
          :columns="itemColumns"
          :data-source="items"
          :pagination="false"
          :scroll="{ x: 640 }"
        >
          <template #bodyCell="{ column, record: row, index }">
            <template v-if="column.key === 'index'">
              {{ index + 1 }}
            </template>
            <template v-else-if="column.key === 'itemName'">
              {{ row.itemName || '—' }}
            </template>
            <template v-else-if="column.key === 'content'">
              {{ row.content || '-' }}
            </template>
            <template v-else-if="column.key === 'ruleScore'">
              {{ displayScore(row.ruleScore) }}
            </template>
            <template v-else-if="column.key === 'score'">
              <span class="score-cell">{{ displayScore(row.score) }}</span>
            </template>
            <template v-else-if="column.key === 'remark'">
              {{ row.remark || '-' }}
            </template>
          </template>
        </a-table>
      </section>
    </div>
    <a-empty v-else-if="!loading" class="record-empty" :description="t('inspectRecordPage.missingId')" />
  </a-spin>
</template>

<style scoped>
.record-detail {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.record-detail--compact {
  gap: 12px;
}

.record-summary {
  display: flex;
  align-items: stretch;
  justify-content: space-between;
  gap: 16px;
  padding: 18px 20px;
  background: var(--omes-color-bg-container);
  border: 1px solid var(--omes-color-primary-border);
  border-radius: 14px;
  box-shadow: 0 4px 16px color-mix(in srgb, var(--omes-color-text) 6%, transparent);
}

.record-summary__overview {
  display: flex;
  align-items: center;
  gap: 16px;
  min-width: 0;
  flex: 1;
}

.record-summary__icon {
  width: 56px;
  height: 56px;
  border-radius: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  flex-shrink: 0;
  color: var(--omes-color-primary);
  background: linear-gradient(135deg, var(--omes-color-primary-bg) 0%, var(--omes-color-primary-bg-hover) 100%);
  border: 1px solid var(--omes-color-primary-border);
}

.record-summary__main {
  min-width: 0;
}

.record-summary__equip {
  margin: 0;
  font-size: 18px;
  font-weight: 700;
  color: var(--omes-color-text-heading);
  line-height: 1.35;
}

.record-summary__code {
  margin: 4px 0 0;
  font-size: 13px;
  color: var(--omes-color-text-tertiary);
}

.record-summary__meta {
  margin: 6px 0 0;
  font-size: 12px;
  color: var(--omes-color-text-quaternary);
  line-height: 1.5;
}

.record-summary__stats {
  display: flex;
  gap: 12px;
  flex-shrink: 0;
}

.stat-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-width: 88px;
  padding: 12px 16px;
  border-radius: 12px;
  background: var(--omes-color-bg-toolbar-from);
  border: 1px solid var(--omes-color-primary-border);
}

.stat-card--score {
  background: linear-gradient(180deg, var(--omes-color-primary-bg-hover) 0%, var(--omes-color-primary-bg) 100%);
  border-color: var(--omes-color-primary-border);
}

.stat-card__value {
  font-size: 22px;
  font-weight: 700;
  color: var(--omes-color-primary);
  line-height: 1.2;
}

.stat-card__label {
  margin-top: 4px;
  font-size: 12px;
  color: var(--omes-color-text-quaternary);
  white-space: nowrap;
}

.detail-panel {
  background: var(--omes-color-bg-container);
  border: 1px solid var(--omes-color-primary-border);
  border-radius: 14px;
  overflow: hidden;
  box-shadow: 0 4px 16px color-mix(in srgb, var(--omes-color-text) 6%, transparent);
}

.detail-panel__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
  padding: 14px 18px;
  background: linear-gradient(180deg, var(--omes-color-bg-table-hover-alt) 0%, var(--omes-color-primary-bg-hover) 100%);
  border-bottom: 1px solid var(--omes-color-primary-border);
}

.detail-panel__title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 15px;
  font-weight: 600;
  color: var(--omes-color-text);
}

.detail-panel__hint {
  font-weight: 400;
  font-size: 12px;
  color: var(--omes-color-text-quaternary);
}

.items-empty {
  padding: 32px 18px;
}

.items-table :deep(.ant-table) {
  margin: 0 !important;
  border-radius: 0;
}

.items-table :deep(.ant-table-thead > tr > th) {
  background: var(--omes-color-bg-elevated);
  font-weight: 600;
  font-size: 13px;
}

.items-table :deep(.ant-table-tbody > tr > td) {
  font-size: 13px;
}

.items-table :deep(.ant-table-tbody > tr:hover > td) {
  background: var(--omes-color-bg-spotlight);
}

.score-cell {
  font-weight: 600;
  color: var(--omes-color-primary);
}

.record-empty {
  padding: 48px 24px;
}

@media (max-width: 900px) {
  .record-summary {
    flex-direction: column;
  }

  .record-summary__stats {
    justify-content: flex-start;
  }
}
</style>
