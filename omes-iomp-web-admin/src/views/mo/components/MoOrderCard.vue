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
  record: MoRecord
  selected?: boolean
  canExec?: boolean
  canDelete?: boolean
  canCancel?: boolean
}>()

const emit = defineEmits<{
  select: [checked: boolean]
  exec: []
  detail: []
  delete: []
  adjust: [type: MoAdjustType]
}>()

const { t } = useI18n()

const statusTone = computed(() => {
  const status = props.record.status
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
})

const progress = computed(() => {
  const total = props.record.num
  const remaining = props.record.surplus
  if (total == null || total <= 0 || remaining == null) {
    return null
  }
  const done = Math.max(0, total - remaining)
  const percent = Math.min(100, Math.round((done / total) * 100))
  return { total, remaining, done, percent }
})

const recipeDisplay = computed(() => {
  const name = props.record.productName?.trim()
  const code = props.record.productCode?.trim()
  if (name && code) {
    return `${name} · ${code}`
  }
  return name || code || t('moPage.bomNotSelected')
})

const recipeTitle = computed(() => {
  const name = props.record.productName?.trim()
  const code = props.record.productCode?.trim()
  if (name && code) {
    return `${t('moPage.bomName')}：${name}  ${t('moPage.bomCode')}：${code}`
  }
  return recipeDisplay.value
})

function onAdjustMenu({ key }: { key: string | number }) {
  emit('adjust', String(key) as MoAdjustType)
}
</script>

<template>
  <article
    class="mo-order-card"
    :class="[`mo-order-card--${statusTone}`, { 'mo-order-card--selected': selected }]"
  >
    <div class="mo-order-card__accent" aria-hidden="true" />

    <header class="mo-order-card__head">
      <label class="mo-order-card__check">
        <input
          type="checkbox"
          class="mo-order-card__check-input"
          :checked="selected"
          @change="emit('select', ($event.target as HTMLInputElement).checked)"
        />
      </label>
      <div class="mo-order-card__head-main">
        <code class="mo-order-card__code" :title="record.selfCode">{{ record.selfCode || '—' }}</code>
        <span class="mo-status-tag" :class="`mo-status-tag--${statusTone}`">
          <CheckCircleOutlined v-if="statusTone === 'success'" class="mo-status-tag__icon" />
          {{ record.statusDesc || '—' }}
        </span>
      </div>
    </header>

    <div class="mo-order-card__body">
      <div v-if="progress" class="mo-order-card__progress">
        <div class="mo-order-card__progress-head">
          <span class="mo-order-card__progress-label">{{ t('moPage.progressLabel') }}</span>
          <span class="mo-order-card__progress-value">
            {{ t('moPage.progressDone', { done: progress.done, total: progress.total }) }}
          </span>
        </div>
        <a-progress
          :percent="progress.percent"
          :show-info="false"
          :stroke-width="5"
          size="small"
        />
      </div>

      <div class="mo-order-card__recipe" :title="recipeTitle">
        <ExperimentOutlined class="mo-order-card__recipe-icon" />
        <span class="mo-order-card__recipe-text">{{ recipeDisplay }}</span>
      </div>

      <dl class="mo-order-card__info">
        <div class="info-cell">
          <dt>{{ t('moPage.lineCode') }}</dt>
          <dd :title="record.lineCode">{{ record.lineCode || '—' }}</dd>
        </div>
        <div class="info-cell">
          <dt>{{ t('moPage.execTime') }}</dt>
          <dd :title="record.execTime">{{ record.execTime || '—' }}</dd>
        </div>
        <div class="info-cell info-cell--wide">
          <dt>{{ t('moPage.createTime') }}</dt>
          <dd :title="record.createdTime">{{ record.createdTime || '—' }}</dd>
        </div>
      </dl>
    </div>

    <footer class="mo-order-card__foot">
      <a-button
        v-if="canExec"
        type="primary"
        size="small"
        ghost
        class="mo-order-card__exec-btn"
        @click="emit('exec')"
      >
        <PlayCircleOutlined />
        {{ t('moPage.exec') }}
      </a-button>
      <div class="mo-order-card__foot-actions">
        <a-tooltip :title="t('moPage.detail')">
          <a-button type="text" size="small" class="mo-order-card__icon-btn" @click="emit('detail')">
            <EyeOutlined />
          </a-button>
        </a-tooltip>
        <a-dropdown v-if="canCancel" :trigger="['click']">
          <a-button type="text" size="small" class="mo-order-card__icon-btn mo-order-card__menu-btn">
            <MoreOutlined />
            {{ t('moPage.adjust') }}
          </a-button>
          <template #overlay>
            <a-menu @click="onAdjustMenu">
              <a-menu-item key="RESCHEDULE">{{ t('moPage.reschedule') }}</a-menu-item>
              <a-menu-item key="CHANGE_LINE">{{ t('moPage.changeLine') }}</a-menu-item>
              <a-menu-item key="QTY_UP">{{ t('moPage.qtyUp') }}</a-menu-item>
              <a-menu-item key="QTY_DOWN">{{ t('moPage.qtyDown') }}</a-menu-item>
              <a-menu-divider />
              <a-menu-item key="CANCEL_MO" danger>{{ t('moPage.cancelOrder') }}</a-menu-item>
            </a-menu>
          </template>
        </a-dropdown>
        <a-tooltip v-if="canDelete" :title="t('moPage.delete')">
          <a-button type="text" size="small" danger class="mo-order-card__icon-btn" @click="emit('delete')">
            <DeleteOutlined />
          </a-button>
        </a-tooltip>
      </div>
    </footer>
  </article>
</template>
