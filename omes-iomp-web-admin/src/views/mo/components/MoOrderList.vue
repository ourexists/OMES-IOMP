<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import {
  CheckCircleOutlined,
  DeleteOutlined,
  ExperimentOutlined,
  EyeOutlined,
  MoreOutlined,
  PlayCircleOutlined,
} from '@ant-design/icons-vue'
import type { MoRecord } from '@/types/mo'
import { MO_STATUS } from '@/types/mo'
import type { MoAdjustType } from './MoAdjustModal.vue'

const props = defineProps<{
  records: MoRecord[]
  selectedRowKeys: string[]
  loading?: boolean
}>()

const emit = defineEmits<{
  'update:selectedRowKeys': [keys: string[]]
  exec: [record: MoRecord]
  detail: [record: MoRecord]
  delete: [record: MoRecord]
  adjust: [record: MoRecord, type: MoAdjustType]
}>()

const { t } = useI18n()

const columns = computed(() => [
  {
    title: t('moPage.moCode'),
    dataIndex: 'selfCode',
    key: 'selfCode',
    width: 148,
    ellipsis: true,
  },
  {
    title: t('moPage.status'),
    dataIndex: 'statusDesc',
    key: 'statusDesc',
    width: 96,
    align: 'center' as const,
  },
  {
    title: t('moPage.progressLabel'),
    key: 'progress',
    width: 136,
  },
  {
    title: t('moPage.bomName'),
    key: 'product',
    ellipsis: true,
    minWidth: 180,
  },
  {
    title: t('moPage.lineCode'),
    dataIndex: 'lineCode',
    key: 'lineCode',
    width: 128,
    ellipsis: true,
  },
  {
    title: t('moPage.createTime'),
    dataIndex: 'createdTime',
    key: 'createdTime',
    width: 164,
    ellipsis: true,
  },
  {
    title: t('moPage.execTime'),
    dataIndex: 'execTime',
    key: 'execTime',
    width: 164,
    ellipsis: true,
  },
  {
    title: t('moPage.colAction'),
    key: 'action',
    width: 200,
    align: 'center' as const,
    fixed: 'right' as const,
  },
])

const rowSelection = computed(() => ({
  selectedRowKeys: props.selectedRowKeys,
  onChange: (keys: string[]) => emit('update:selectedRowKeys', keys as string[]),
}))

const tableScroll = computed(() => ({
  x: 1120,
}))

function statusTone(status?: number): string {
  if (status === MO_STATUS.PART) {
    return 'warning'
  }
  if (status === MO_STATUS.RUN) {
    return 'processing'
  }
  if (status === MO_STATUS.COMPLETE) {
    return 'success'
  }
  if (status === MO_STATUS.CANCEL) {
    return 'error'
  }
  return 'default'
}

function getProgress(record: MoRecord) {
  const total = record.num
  const remaining = record.surplus
  if (total == null || total <= 0 || remaining == null) {
    return null
  }
  const done = Math.max(0, total - remaining)
  const percent = Math.min(100, Math.round((done / total) * 100))
  return { total, done, percent }
}

function recipeDisplay(record: MoRecord): string {
  const name = record.productName?.trim()
  const code = record.productCode?.trim()
  if (name && code) {
    return `${name} · ${code}`
  }
  return name || code || t('moPage.bomNotSelected')
}

function canExec(record: MoRecord): boolean {
  return record.status === MO_STATUS.INIT || record.status === MO_STATUS.PART
}

function canDelete(record: MoRecord): boolean {
  return record.status === MO_STATUS.INIT
}

function canCancel(record: MoRecord): boolean {
  return record.status != null && record.status !== MO_STATUS.CANCEL && record.status !== MO_STATUS.COMPLETE
}

function rowClassName(record: MoRecord): string {
  return `mo-list-table__row mo-list-table__row--${statusTone(record.status)}`
}

function onAdjustMenu(record: MoRecord, { key }: { key: string | number }) {
  emit('adjust', record, String(key) as MoAdjustType)
}
</script>

<template>
  <a-table
    row-key="id"
    size="middle"
    table-layout="fixed"
    class="mo-list-table"
    :columns="columns"
    :data-source="records"
    :loading="loading"
    :row-selection="rowSelection"
    :row-class-name="(_record: MoRecord) => rowClassName(_record)"
    :scroll="tableScroll"
    :pagination="false"
  >
    <template #bodyCell="{ column, record }">
      <template v-if="column.key === 'selfCode'">
        <code class="mo-list-table__code">{{ record.selfCode || '—' }}</code>
      </template>
      <template v-else-if="column.key === 'statusDesc'">
        <span class="mo-status-tag" :class="`mo-status-tag--${statusTone(record.status)}`">
          <CheckCircleOutlined
            v-if="statusTone(record.status) === 'success'"
            class="mo-status-tag__icon"
          />
          {{ record.statusDesc || '—' }}
        </span>
      </template>
      <template v-else-if="column.key === 'progress'">
        <div
          v-if="getProgress(record)"
          class="mo-list-table__progress"
          :class="`mo-list-table__progress--${statusTone(record.status)}`"
        >
          <span class="mo-list-table__progress-text">
            {{
              t('moPage.progressDone', {
                done: getProgress(record)!.done,
                total: getProgress(record)!.total,
              })
            }}
          </span>
          <a-progress
            :percent="getProgress(record)!.percent"
            :show-info="false"
            :stroke-width="4"
            size="small"
          />
        </div>
        <span v-else class="mo-list-table__empty">—</span>
      </template>
      <template v-else-if="column.key === 'product'">
        <div class="mo-list-table__product" :title="recipeDisplay(record)">
          <ExperimentOutlined class="mo-list-table__product-icon" />
          <span>{{ recipeDisplay(record) }}</span>
        </div>
      </template>
      <template v-else-if="column.key === 'lineCode'">
        <span class="mo-list-table__muted">{{ record.lineCode || '—' }}</span>
      </template>
      <template v-else-if="column.key === 'createdTime'">
        <span class="mo-list-table__muted">{{ record.createdTime || '—' }}</span>
      </template>
      <template v-else-if="column.key === 'execTime'">
        <span class="mo-list-table__muted">{{ record.execTime || '—' }}</span>
      </template>
      <template v-else-if="column.key === 'action'">
        <div class="mo-list-table__actions">
          <a-button
            v-if="canExec(record)"
            type="link"
            size="small"
            class="mo-list-table__action-link mo-list-table__action-link--exec"
            @click="emit('exec', record)"
          >
            <PlayCircleOutlined />
            {{ t('moPage.exec') }}
          </a-button>
          <a-button type="link" size="small" class="mo-list-table__action-link" @click="emit('detail', record)">
            <EyeOutlined />
            {{ t('moPage.detail') }}
          </a-button>
          <a-dropdown v-if="canCancel(record)" :trigger="['click']">
            <a-button type="link" size="small" class="mo-list-table__action-link">
              <MoreOutlined />
              {{ t('moPage.adjust') }}
            </a-button>
            <template #overlay>
              <a-menu @click="(info) => onAdjustMenu(record, info)">
                <a-menu-item key="RESCHEDULE">{{ t('moPage.reschedule') }}</a-menu-item>
                <a-menu-item key="CHANGE_LINE">{{ t('moPage.changeLine') }}</a-menu-item>
                <a-menu-item key="QTY_UP">{{ t('moPage.qtyUp') }}</a-menu-item>
                <a-menu-item key="QTY_DOWN">{{ t('moPage.qtyDown') }}</a-menu-item>
                <a-menu-divider />
                <a-menu-item key="CANCEL_MO" danger>{{ t('moPage.cancelOrder') }}</a-menu-item>
              </a-menu>
            </template>
          </a-dropdown>
          <a-button
            v-if="canDelete(record)"
            type="link"
            size="small"
            danger
            class="mo-list-table__action-link"
            @click="emit('delete', record)"
          >
            <DeleteOutlined />
          </a-button>
        </div>
      </template>
    </template>
  </a-table>
</template>
