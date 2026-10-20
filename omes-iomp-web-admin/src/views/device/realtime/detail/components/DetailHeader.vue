<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { ClusterOutlined, ReloadOutlined, RollbackOutlined } from '@ant-design/icons-vue'
import type { EquipRecord } from '@/api/device'
import EquipProductThumb from '@/components/EquipProductThumb.vue'
import { resolveEquipStatusView } from '@/utils/equip-status'

const props = defineProps<{
  equip: EquipRecord | null
  loading?: boolean
}>()

const emit = defineEmits<{
  refresh: []
  back: []
}>()

const { t } = useI18n()

const statusView = computed(() => (props.equip ? resolveEquipStatusView(props.equip) : null))

const statusLabel = computed(() => {
  if (!statusView.value) {
    return '-'
  }
  return t(statusView.value.labelKey)
})
</script>

<template>
  <div class="detail-header">
    <div class="detail-overview">
      <div
        class="detail-icon equip-status-icon"
        :class="statusView ? `equip-status-icon--${statusView.tone}` : ''"
      >
        <EquipProductThumb v-if="equip?.productImage" :path="equip.productImage" />
        <ClusterOutlined v-else />
      </div>
      <div class="detail-main">
        <div class="detail-name-row">
          <h1 class="detail-name">{{ equip?.name || '-' }}</h1>
          <span
            v-if="statusView"
            class="equip-status-tag"
            :class="`equip-status-tag--${statusView.tone}`"
          >
            {{ statusLabel }}
          </span>
        </div>
        <p class="detail-meta">
          {{ t('equipDetailPage.code') }}: {{ equip?.selfCode || '-' }}
          &nbsp;·&nbsp;
          {{ t('equipDetailPage.workshop') }}: {{ equip?.workshop?.name || '-' }}
        </p>
      </div>
    </div>
    <a-space>
      <a-button :loading="loading" @click="emit('refresh')">
        <ReloadOutlined />
        {{ t('equipDetailPage.syncStatus') }}
      </a-button>
      <a-button type="primary" ghost @click="emit('back')">
        <RollbackOutlined />
        {{ t('equipDetailPage.backList') }}
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
  border: 1px solid #e8eef5;
  border-radius: 14px;
  box-shadow: 0 4px 16px rgba(15, 23, 42, 0.04);
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
  overflow: hidden;
  font-size: 28px;
  flex-shrink: 0;
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
  color: #0f172a;
}

.detail-meta {
  margin: 6px 0 0;
  font-size: 13px;
  color: var(--equip-status-offline-fg);
}

@media (max-width: 768px) {
  .detail-header {
    flex-direction: column;
    align-items: stretch;
  }
}
</style>
