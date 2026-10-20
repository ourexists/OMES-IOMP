<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import {
  CarryOutOutlined,
  RedoOutlined,
  ReloadOutlined,
  RollbackOutlined,
  UserAddOutlined,
} from '@ant-design/icons-vue'
import type { InspectTaskRecord } from '@/api/inspect-task'
import { inspectTaskStatusLabel } from '@/api/inspect-task'

const TASK_STATUS_PENDING = 0
const TASK_STATUS_OVERDUE = 3

const props = withDefaults(
  defineProps<{
    task: InspectTaskRecord | null
    loading?: boolean
    recordCount?: number
    compact?: boolean
  }>(),
  { compact: false },
)

const emit = defineEmits<{
  refresh: []
  back: []
  assign: []
  restart: []
}>()

const { t } = useI18n()

const statusLabel = computed(() => {
  const task = props.task
  if (!task) {
    return '-'
  }
  if (task.statusDesc) {
    return t(`inspectTaskPage.status.${task.statusDesc}`, task.statusDesc)
  }
  return inspectTaskStatusLabel(task.status)
})

const statusColor = computed(() => {
  const status = props.task?.status
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
})

const titleText = computed(() => props.task?.planName || props.task?.id || '-')

const metaLine = computed(() => {
  const parts: string[] = []
  if (props.task?.templateName) {
    parts.push(`${t('inspectTaskPage.colTemplate')}: ${props.task.templateName}`)
  }
  if (props.task?.scheduledTime) {
    parts.push(`${t('inspectTaskPage.colScheduledTime')}: ${props.task.scheduledTime}`)
  }
  if (props.compact) {
    parts.push(`${t('inspectTaskPage.colWorkshop')}: ${workshopText.value}`)
    parts.push(
      `${t('inspectTaskPage.colExecutor')}: ${props.task?.executorName || t('inspectTaskPage.unassigned')}`,
    )
  }
  if (props.recordCount != null) {
    parts.push(t('inspectTaskDetailPage.recordCount', { count: props.recordCount }))
  }
  return parts.join(' · ')
})

const workshopText = computed(() => {
  const task = props.task
  if (!task) {
    return '-'
  }
  if (task.workshopName?.trim()) {
    return task.workshopName
  }
  return task.workshopCode || '-'
})

const canAssign = computed(() => props.task?.status === TASK_STATUS_PENDING)
const canRestart = computed(() => props.task?.status === TASK_STATUS_OVERDUE)
</script>

<template>
  <div class="detail-header" :class="{ 'detail-header--compact': compact }">
    <div class="detail-overview">
      <div class="detail-icon">
        <CarryOutOutlined />
      </div>
      <div class="detail-main">
        <div class="detail-name-row">
          <h1 class="detail-name">{{ titleText }}</h1>
          <a-tag :color="statusColor">{{ statusLabel }}</a-tag>
        </div>
        <p class="detail-meta">{{ metaLine || '-' }}</p>
        <p v-if="!compact" class="detail-sub">
          <span>{{ t('inspectTaskPage.colWorkshop') }}: {{ workshopText }}</span>
          <span class="detail-sub__sep">|</span>
          <span>
            {{ t('inspectTaskPage.colExecutor') }}:
            {{ task?.executorName || t('inspectTaskPage.unassigned') }}
          </span>
        </p>
      </div>
    </div>
    <a-space wrap>
      <a-button v-if="canAssign" type="primary" @click="emit('assign')">
        <UserAddOutlined />
        {{ t('inspectTaskPage.assign') }}
      </a-button>
      <a-button v-if="canRestart" @click="emit('restart')">
        <RedoOutlined />
        {{ t('inspectTaskPage.restart') }}
      </a-button>
      <a-button :loading="loading" @click="emit('refresh')">
        <ReloadOutlined />
        {{ t('inspectTaskDetailPage.refresh') }}
      </a-button>
      <a-button type="primary" ghost @click="emit('back')">
        <RollbackOutlined />
        {{ t('inspectTaskDetailPage.backList') }}
      </a-button>
    </a-space>
  </div>
</template>

<style scoped>
.detail-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 16px 20px;
  background: var(--omes-color-bg-container);
  border: 1px solid var(--omes-color-primary-border);
  border-radius: 14px;
  box-shadow: 0 4px 16px color-mix(in srgb, var(--omes-color-text) 6%, transparent);
}

.detail-overview {
  display: flex;
  align-items: center;
  gap: 16px;
  min-width: 0;
}

.detail-icon {
  width: 64px;
  height: 64px;
  border-radius: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 28px;
  flex-shrink: 0;
  color: var(--omes-color-accent-cyan-from);
  background: linear-gradient(
    135deg,
    color-mix(in srgb, var(--omes-color-accent-cyan-from) 14%, var(--omes-color-bg-container)) 0%,
    color-mix(in srgb, var(--omes-color-accent-cyan-to) 8%, var(--omes-color-bg-container)) 100%
  );
  border: 1px solid color-mix(in srgb, var(--omes-color-accent-cyan-border) 35%, var(--omes-color-border));
}

.detail-main {
  min-width: 0;
}

.detail-name-row {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.detail-name {
  margin: 0;
  font-size: 20px;
  font-weight: 700;
  color: var(--omes-color-text-heading);
}

.detail-meta {
  margin: 6px 0 0;
  font-size: 13px;
  color: var(--omes-color-text-quaternary);
}

.detail-sub {
  margin: 4px 0 0;
  font-size: 13px;
  color: var(--omes-color-text-tertiary);
}

.detail-sub__sep {
  margin: 0 8px;
  color: rgba(0, 0, 0, 0.2);
}

.detail-header--compact {
  padding: 10px 14px;
  border-radius: var(--omes-radius-lg);
  box-shadow: none;
}

.detail-header--compact .detail-overview {
  gap: 10px;
}

.detail-header--compact .detail-icon {
  width: 40px;
  height: 40px;
  border-radius: var(--omes-radius-lg);
  font-size: 18px;
}

.detail-header--compact .detail-name {
  font-size: 16px;
}

.detail-header--compact .detail-meta {
  margin-top: 2px;
  font-size: 12px;
  line-height: 1.4;
}

.detail-header--compact :deep(.ant-btn) {
  height: 28px;
  padding-inline: 10px;
  font-size: 13px;
}

@media (max-width: 768px) {
  .detail-header {
    flex-direction: column;
    align-items: stretch;
  }
}
</style>
