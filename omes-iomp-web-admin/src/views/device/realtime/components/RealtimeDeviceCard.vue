<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { AlertOutlined } from '@ant-design/icons-vue'
import type { EquipRecord } from '@/api/device'
import EquipProductThumb from '@/components/EquipProductThumb.vue'
import {
  equipCardToneClass,
  equipStatusDotClass,
  resolveEquipCardTone,
  resolveOnlineSignal,
  resolveRunSignal,
  resolveAlarmSignal,
} from '@/utils/equip-status'

const props = defineProps<{
  item: EquipRecord
}>()

const emit = defineEmits<{
  open: [item: EquipRecord]
}>()

const { t } = useI18n()

const cardTone = computed(() => resolveEquipCardTone(props.item))
const cardToneClass = computed(() => equipCardToneClass(cardTone.value))

const sceneLabel = computed(() => props.item.workshop?.name?.trim() || '')
const typeLabel = computed(() => props.item.typeDesc || (props.item.type != null ? String(props.item.type) : ''))

/** 角标优先：有场景用场景，否则类型；都没有时用默认文案 */
const badgeLabel = computed(
  () => sceneLabel.value || typeLabel.value || t('realtimePage.device'),
)

/** 有场景标签时，类型单独展示，避免与 badge 重复 */
const showTypeTag = computed(() => !!sceneLabel.value && !!typeLabel.value)

const onlineSignal = computed(() => resolveOnlineSignal(props.item))
const runSignal = computed(() => resolveRunSignal(props.item))
const alarmSignal = computed(() => resolveAlarmSignal(props.item))

const onlineDotClass = computed(() => equipStatusDotClass(onlineSignal.value))
const runDotClass = computed(() => equipStatusDotClass(runSignal.value))
const alarmDotClass = computed(() => equipStatusDotClass(alarmSignal.value))

const onlineLabel = computed(() =>
  props.item.onlineState === 1 ? t('realtimePage.online') : t('realtimePage.offline'),
)

const runLabel = computed(() => {
  if (props.item.onlineState !== 1) return t('realtimePage.offline')
  if (props.item.runState === 1) return t('realtimePage.run')
  if (props.item.runState === -1) return t('realtimePage.unknown')
  return t('realtimePage.stop')
})

const alarmLabel = computed(() => {
  if (props.item.onlineState !== 1) return t('realtimePage.offline')
  if (props.item.alarmState === 1) return t('realtimePage.alarm')
  if (props.item.alarmState === -1) return t('realtimePage.unknown')
  return t('realtimePage.normal')
})

const displayAttrs = computed(() => (props.item.attrs ?? []).slice(0, 4))
const hasTelemetry = computed(() => displayAttrs.value.length > 0)
const hasMoreAttrs = computed(() => (props.item.attrs?.length ?? 0) > 4)

function attrDisplayValue(value: unknown): string {
  if (value == null || value === '') {
    return t('realtimePage.unknown')
  }
  return String(value)
}
</script>

<template>
  <article class="rt-card equip-card-tone" :class="cardToneClass" @click="emit('open', item)">
    <header class="rt-card__head">
      <span class="rt-card__thumb">
        <EquipProductThumb :path="item.productImage" />
      </span>
      <div class="rt-card__head-body">
        <div class="rt-card__head-top">
          <span class="rt-card__badge" :title="badgeLabel">{{ badgeLabel }}</span>
          <div class="rt-card__dots" aria-hidden="true">
            <i :class="onlineDotClass" :title="onlineLabel" />
            <i :class="runDotClass" :title="runLabel" />
            <i :class="alarmDotClass" :title="alarmLabel" />
          </div>
        </div>

        <h3 class="rt-card__name" :title="item.name">{{ item.name }}</h3>

        <div class="rt-card__meta">
          <span class="rt-card__sn">{{ item.selfCode || '—' }}</span>
          <span v-if="showTypeTag" class="rt-card__type">{{ typeLabel }}</span>
        </div>
      </div>
    </header>

    <div v-if="item.alarmTexts?.length" class="rt-card__alarm">
      <AlertOutlined class="rt-card__alarm-icon" />
      <div class="rt-card__alarm-texts">
        <p v-for="(text, idx) in item.alarmTexts" :key="idx" class="rt-card__alarm-text">
          {{ text }}
        </p>
      </div>
    </div>

    <section v-if="hasTelemetry" class="rt-card__telemetry">
      <div class="telemetry-head">
        <span>{{ t('realtimePage.telemetry') }}</span>
        <span v-if="hasMoreAttrs" class="telemetry-more">+{{ (item.attrs?.length ?? 0) - 4 }}</span>
      </div>
      <ul class="telemetry-list">
        <li v-for="(attr, idx) in displayAttrs" :key="idx" class="telemetry-row">
          <span class="telemetry-label">{{ attr.name }}</span>
          <span class="telemetry-value">
            {{ attrDisplayValue(attr.value) }}<small v-if="attr.unit">{{ attr.unit }}</small>
          </span>
        </li>
      </ul>
    </section>
  </article>
</template>

<style scoped>
.rt-card {
  --rt-accent: var(--equip-card-accent, #94a3b8);
  position: relative;
  display: flex;
  flex-direction: column;
  gap: 0;
  min-height: 132px;
  padding: 0;
  border-radius: var(--omes-radius-lg);
  background: var(--omes-color-bg-container);
  border: 1px solid #e5e7eb;
  box-shadow: 0 1px 2px rgba(15, 23, 42, 0.04);
  cursor: pointer;
  overflow: hidden;
  transition:
    transform 0.16s ease,
    box-shadow 0.16s ease,
    border-color 0.16s ease;
  content-visibility: auto;
  contain-intrinsic-size: 280px 168px;
}

.rt-card::before {
  content: '';
  position: absolute;
  left: 0;
  top: 0;
  bottom: 0;
  width: 3px;
  background: var(--rt-accent);
}

.rt-card:hover {
  transform: translateY(-1px);
  border-color: #cbd5e1;
  box-shadow: 0 4px 12px rgba(15, 23, 42, 0.06);
}

.rt-card:active {
  transform: translateY(0);
}

.rt-card__head {
  position: relative;
  z-index: 1;
  display: flex;
  align-items: flex-start;
  gap: 10px;
  padding: 12px 14px 11px 15px;
  border-bottom: 1px solid #f1f5f9;
}

.rt-card__thumb {
  flex-shrink: 0;
  width: 48px;
  height: 48px;
  border-radius: 8px;
  border: 1px solid #eef2f6;
  overflow: hidden;
}

.rt-card__head-body {
  min-width: 0;
  flex: 1;
}

.rt-card__head-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  margin-bottom: 7px;
}

.rt-card__badge {
  min-width: 0;
  max-width: calc(100% - 44px);
  padding: 2px 8px;
  font-size: 11px;
  font-weight: 500;
  color: #64748b;
  background: var(--omes-color-bg-toolbar-from);
  border: 1px solid #eef2f6;
  border-radius: 4px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.rt-card__dots {
  display: flex;
  align-items: center;
  gap: 4px;
  flex-shrink: 0;
}

.rt-card__dots i {
  display: block;
}

/* 卡片内状态点：缩小尺寸，去掉外发光 */
.rt-card__dots :deep(.equip-status-dot) {
  width: 7px;
  height: 7px;
  box-shadow: none;
}

.rt-card__name {
  margin: 0;
  font-size: 15px;
  font-weight: 600;
  line-height: 1.35;
  color: #111827;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.rt-card__meta {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 4px;
  min-width: 0;
}

.rt-card__sn {
  font-family: ui-monospace, SFMono-Regular, Menlo, Consolas, monospace;
  font-size: 11px;
  letter-spacing: 0.02em;
  color: #94a3b8;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.rt-card__type {
  flex-shrink: 0;
  padding: 1px 6px;
  font-size: 10px;
  font-weight: 500;
  color: #64748b;
  background: var(--omes-color-bg-toolbar-from);
  border: 1px solid #eef2f6;
  border-radius: 4px;
}

.rt-card__alarm {
  position: relative;
  z-index: 1;
  display: flex;
  gap: 8px;
  margin: 0 14px 10px 15px;
  padding: 8px 10px;
  border-radius: var(--omes-radius-sm);
  background: #fef2f2;
  border: 1px solid #fecaca;
  border-left: 3px solid #ef4444;
}

.rt-card__alarm-icon {
  color: #dc2626;
  font-size: 14px;
  margin-top: 1px;
  flex-shrink: 0;
}

.rt-card__alarm-texts {
  min-width: 0;
}

.rt-card__alarm-text {
  margin: 0;
  font-size: 12px;
  font-weight: 600;
  line-height: 1.45;
  color: #991b1b;
}

.rt-card__alarm-text + .rt-card__alarm-text {
  margin-top: 3px;
}

.rt-card__telemetry {
  position: relative;
  z-index: 1;
  margin-top: auto;
  padding: 9px 14px 11px 15px;
  border-top: 1px solid #f1f5f9;
}

.telemetry-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 5px;
  font-size: 10px;
  font-weight: 600;
  letter-spacing: 0.04em;
  color: #94a3b8;
}

.telemetry-more {
  font-size: 10px;
  font-weight: 500;
  color: #64748b;
  padding: 0 5px;
  border-radius: 4px;
  background: var(--omes-color-bg-toolbar-from);
  border: 1px solid #eef2f6;
}

.telemetry-list {
  margin: 0;
  padding: 0;
  list-style: none;
}

.telemetry-row {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 10px;
  padding: 3px 0;
}

.telemetry-row + .telemetry-row {
  border-top: 1px solid #f1f5f9;
}

.telemetry-label {
  flex-shrink: 0;
  max-width: 46%;
  font-size: 11px;
  color: #64748b;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.telemetry-value {
  min-width: 0;
  font-family: ui-monospace, SFMono-Regular, Menlo, Consolas, monospace;
  font-size: 12px;
  font-weight: 600;
  color: #1e293b;
  text-align: right;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.telemetry-value small {
  margin-left: 2px;
  font-size: 10px;
  font-weight: 500;
  color: #94a3b8;
}
</style>
