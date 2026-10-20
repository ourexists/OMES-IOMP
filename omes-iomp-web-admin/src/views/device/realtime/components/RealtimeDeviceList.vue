<script setup lang="ts">
import { useI18n } from 'vue-i18n'
import type { EquipRecord } from '@/api/device'
import EquipProductThumb from '@/components/EquipProductThumb.vue'
import {
  equipStatusDotClass,
  formatAlarmSignalTitle,
  formatRunSignalTitle,
  resolveAlarmSignal,
  resolveOnlineSignal,
  resolveRunSignal,
} from '@/utils/equip-status'

defineProps<{
  items: EquipRecord[]
}>()

const emit = defineEmits<{
  open: [item: EquipRecord]
}>()

const { t } = useI18n()

function workshopLabel(item: EquipRecord): string {
  return item.workshop?.name || item.typeDesc || String(item.type || t('realtimePage.device'))
}

function rowClass(item: EquipRecord): Record<string, boolean> {
  return {
    'equip-list-row--offline': item.onlineState !== 1,
    'equip-list-row--alarm': item.onlineState === 1 && item.alarmState === 1,
  }
}
</script>

<template>
  <div class="device-list-table">
    <div class="device-list-header">
      <span class="col-name">{{ t('realtimePage.colName') }}</span>
      <span class="col-code">{{ t('realtimePage.colCode') }}</span>
      <span class="col-workshop">{{ t('realtimePage.colWorkshop') }}</span>
      <span class="col-status">{{ t('realtimePage.colStatus') }}</span>
    </div>

    <div
      v-for="item in items"
      :key="item.id"
      v-memo="[item]"
      class="device-list-row device-card"
      :class="rowClass(item)"
      @click="emit('open', item)"
    >
      <span class="col-name">
        <span class="list-thumb">
          <EquipProductThumb :path="item.productImage" />
        </span>
        <span class="card-vendor-tag">{{ workshopLabel(item) }}</span>
        <span class="card-device-name">{{ item.name }}</span>
        <span
          v-if="item.alarmTexts?.length"
          class="equip-list-alarm-badge"
          :title="item.alarmTexts.join('; ')"
        >
          !
        </span>
      </span>
      <span class="col-code">{{ item.selfCode || '—' }}</span>
      <span class="col-workshop">{{ item.workshop?.name || '—' }}</span>
      <span class="col-status">
        <span
          :class="equipStatusDotClass(resolveOnlineSignal(item))"
          :title="t('realtimePage.online')"
        />
        <span
          :class="equipStatusDotClass(resolveRunSignal(item))"
          :title="formatRunSignalTitle(t, item)"
        />
        <span
          :class="equipStatusDotClass(resolveAlarmSignal(item))"
          :title="formatAlarmSignalTitle(t, item)"
        />
      </span>
    </div>

    <a-empty v-if="!items.length" class="list-empty" :description="t('realtimePage.empty')" />
  </div>
</template>

<style scoped>
.device-list-table {
  display: flex;
  flex-direction: column;
  gap: 8px;
  width: 100%;
  min-width: 0;
  padding: 8px;
  box-sizing: border-box;
}

.device-list-header {
  display: grid;
  grid-template-columns: 2fr 1.2fr 1.2fr 120px;
  gap: 12px;
  padding: 10px 16px;
  font-size: 12px;
  font-weight: 600;
  color: var(--equip-status-offline-fg);
  background: var(--omes-color-bg-toolbar-from);
  border-radius: var(--omes-radius-md);
  flex-shrink: 0;
}

.device-list-row {
  display: grid;
  grid-template-columns: 2fr 1.2fr 1.2fr 120px;
  gap: 12px;
  padding: 12px 16px;
  align-items: center;
  margin: 0;
  box-sizing: border-box;
  overflow: hidden;
  background: var(--omes-color-bg-container);
  border: 1px solid #e5e7eb;
  border-radius: 12px;
  box-shadow: 0 1px 3px rgba(15, 23, 42, 0.06);
  color: #0f172a;
  cursor: pointer;
  transition:
    box-shadow 0.2s ease,
    border-color 0.2s ease;
}

.device-list-row:hover {
  box-shadow: 0 4px 12px rgba(15, 23, 42, 0.08);
  border-color: #cbd5e1;
}

.device-list-row .col-name {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
}

.list-thumb {
  flex-shrink: 0;
  width: 36px;
  height: 36px;
  border-radius: 8px;
  border: 1px solid #e5e7eb;
  overflow: hidden;
}

.card-vendor-tag {
  display: inline-block;
  padding: 2px 10px;
  font-size: 12px;
  font-weight: 500;
  color: #374151;
  background: #e5e7eb;
  border-radius: 999px;
  white-space: nowrap;
  flex-shrink: 0;
  max-width: 120px;
  overflow: hidden;
  text-overflow: ellipsis;
}

.card-device-name {
  flex: 1;
  min-width: 0;
  font-size: 15px;
  font-weight: 600;
  color: #0f172a;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.col-code,
.col-workshop {
  font-size: 12px;
  color: var(--equip-status-offline-fg);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.col-status {
  display: flex;
  gap: 4px;
}

.list-empty {
  padding: 48px 0;
}
</style>
