<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import {
  CheckSquareOutlined,
  DeleteOutlined,
  FieldNumberOutlined,
  InfoCircleOutlined,
  PlusOutlined,
  SettingOutlined,
  UnorderedListOutlined,
} from '@ant-design/icons-vue'
import type { InspectRuleRecord } from '@/api/inspect-template'
import { inspectItemTypeLabel } from '@/api/inspect-item'
import { message } from 'ant-design-vue'

const props = defineProps<{
  open: boolean
  itemType: number
  unit?: string
  rules: InspectRuleRecord[]
}>()

const emit = defineEmits<{
  'update:open': [value: boolean]
  save: [rules: InspectRuleRecord[]]
}>()

const { t } = useI18n()

const rows = ref<InspectRuleRecord[]>([])

const typeName = computed(() => inspectItemTypeLabel(props.itemType))

const typeMeta = computed(() => {
  if (props.itemType === 3) {
    return { tone: 'bool', icon: CheckSquareOutlined }
  }
  if (props.itemType === 2) {
    return { tone: 'numeric', icon: FieldNumberOutlined }
  }
  return { tone: 'choice', icon: UnorderedListOutlined }
})

const sectionClass = computed(() => `form-section--${typeMeta.value.tone}`)

const optionList = computed(() => {
  if (props.itemType !== 1 || !props.unit?.trim()) {
    return [] as string[]
  }
  return props.unit.split(/[,，、\/／|｜]/).map((s) => s.trim()).filter(Boolean)
})

const typeHint = computed(() => {
  if (props.itemType === 3) {
    return t('inspectTemplatePage.ruleBoolHint')
  }
  if (props.itemType === 1) {
    return t('inspectTemplatePage.ruleOptionHint')
  }
  return t('inspectTemplatePage.ruleNumericHint')
})

const canAddRow = computed(() => {
  if (props.itemType === 3) {
    const hasYes = rows.value.some((r) => r.boolValue === 1)
    const hasNo = rows.value.some((r) => r.boolValue === 0)
    return !(hasYes && hasNo)
  }
  return true
})

function defaultRows(): InspectRuleRecord[] {
  if (props.itemType === 3) {
    return [
      { ruleType: 1, boolValue: 1, weight: 0 },
      { ruleType: 1, boolValue: 0, weight: 0 },
    ]
  }
  if (props.itemType === 2) {
    return [{ ruleType: 2, minValue: null, maxValue: null, weight: 0 }]
  }
  return [{ ruleType: 3, optionValue: '', weight: 0 }]
}

watch(
  () => props.open,
  (open) => {
    if (!open) {
      return
    }
    rows.value = props.rules.length > 0 ? props.rules.map((r) => ({ ...r })) : defaultRows()
  },
)

function closeModal() {
  emit('update:open', false)
}

function addRow() {
  if (props.itemType === 3) {
    const hasYes = rows.value.some((r) => r.boolValue === 1)
    const hasNo = rows.value.some((r) => r.boolValue === 0)
    if (hasYes && hasNo) {
      message.warning(t('inspectTemplatePage.ruleBoolLimit'))
      return
    }
    rows.value.push({
      ruleType: 1,
      boolValue: !hasYes ? 1 : 0,
      weight: 0,
    })
    return
  }
  if (props.itemType === 2) {
    rows.value.push({ ruleType: 2, minValue: null, maxValue: null, weight: 0 })
    return
  }
  rows.value.push({ ruleType: 3, optionValue: '', weight: 0 })
}

function removeRow(index: number) {
  rows.value.splice(index, 1)
}

function onSave() {
  const result: InspectRuleRecord[] = []
  for (const row of rows.value) {
    const weight = Number(row.weight)
    if (Number.isNaN(weight)) {
      continue
    }
    if (props.itemType === 3) {
      result.push({ ruleType: 1, boolValue: row.boolValue ?? 1, weight })
    } else if (props.itemType === 2) {
      result.push({
        ruleType: 2,
        minValue: row.minValue ?? null,
        maxValue: row.maxValue ?? null,
        weight,
      })
    } else {
      const opt = (row.optionValue || '').trim()
      if (!opt) {
        continue
      }
      result.push({ ruleType: 3, optionValue: opt, weight })
    }
  }
  emit('save', result)
  closeModal()
}
</script>

<template>
  <a-modal
    :open="open"
    width="580px"
    destroy-on-close
    class="inspect-rule-config-modal"
    :footer="null"
    @update:open="emit('update:open', $event)"
    @cancel="closeModal"
  >
    <template #title>
      <span class="modal-title">
        <SettingOutlined />
        {{ t('inspectTemplatePage.ruleConfigTitle') }}
      </span>
    </template>

    <div class="rule-config">
      <div class="type-badge" :class="`type-badge--${typeMeta.tone}`">
        <span class="type-badge__icon">
          <component :is="typeMeta.icon" />
        </span>
        <span class="type-badge__label">{{ t('inspectTemplatePage.ruleCurrentType', { type: typeName }) }}</span>
      </div>

      <div class="hint-panel">
        <p class="field-hint">
          <InfoCircleOutlined />
          {{ t('inspectTemplatePage.ruleScoreHint') }}
        </p>
        <p class="field-hint field-hint--secondary">
          <InfoCircleOutlined />
          {{ typeHint }}
        </p>
      </div>

      <div class="form-section" :class="sectionClass">
        <div class="section-title">
          <SettingOutlined />
          {{ t('inspectTemplatePage.ruleListTitle') }}
        </div>

        <!-- 是否型 -->
        <template v-if="itemType === 3">
          <div class="rule-header rule-header--bool">
            <span>{{ t('inspectTemplatePage.ruleOption') }}</span>
            <span>{{ t('inspectTemplatePage.ruleScore') }}</span>
            <span />
          </div>
          <div v-if="rows.length === 0" class="rule-empty">
            {{ t('inspectTemplatePage.ruleEmpty') }}
          </div>
          <div v-else class="rule-list">
            <div v-for="(row, index) in rows" :key="index" class="rule-row rule-row--bool">
              <span class="rule-row__no">{{ index + 1 }}</span>
              <a-select v-model:value="row.boolValue" class="rule-row__field">
                <a-select-option :value="1">{{ t('inspectTemplatePage.yes') }}</a-select-option>
                <a-select-option :value="0">{{ t('inspectTemplatePage.no') }}</a-select-option>
              </a-select>
              <a-input-number
                v-model:value="row.weight"
                :min="0"
                class="rule-row__score"
                :placeholder="t('inspectTemplatePage.ruleScorePlaceholder')"
              />
              <a-button
                type="text"
                danger
                class="rule-row__delete"
                :disabled="rows.length <= 1"
                @click="removeRow(index)"
              >
                <DeleteOutlined />
              </a-button>
            </div>
          </div>
        </template>

        <!-- 数值型 -->
        <template v-else-if="itemType === 2">
          <div class="rule-header rule-header--numeric">
            <span>{{ t('inspectTemplatePage.ruleMin') }}</span>
            <span>{{ t('inspectTemplatePage.ruleMax') }}</span>
            <span>{{ t('inspectTemplatePage.ruleScore') }}</span>
            <span />
          </div>
          <div v-if="rows.length === 0" class="rule-empty">
            {{ t('inspectTemplatePage.ruleEmpty') }}
          </div>
          <div v-else class="rule-list">
            <div v-for="(row, index) in rows" :key="index" class="rule-row rule-row--numeric">
              <span class="rule-row__no">{{ index + 1 }}</span>
              <a-input-number
                v-model:value="row.minValue"
                class="rule-row__field"
                :placeholder="t('inspectTemplatePage.ruleMinPlaceholder')"
              />
              <a-input-number
                v-model:value="row.maxValue"
                class="rule-row__field"
                :placeholder="t('inspectTemplatePage.ruleMaxPlaceholder')"
              />
              <a-input-number
                v-model:value="row.weight"
                :min="0"
                class="rule-row__score"
                :placeholder="t('inspectTemplatePage.ruleScorePlaceholder')"
              />
              <a-button
                type="text"
                danger
                class="rule-row__delete"
                :disabled="rows.length <= 1"
                @click="removeRow(index)"
              >
                <DeleteOutlined />
              </a-button>
            </div>
          </div>
        </template>

        <!-- 选择型 -->
        <template v-else>
          <div class="rule-header rule-header--choice">
            <span>{{ t('inspectTemplatePage.ruleOptionValue') }}</span>
            <span>{{ t('inspectTemplatePage.ruleScore') }}</span>
            <span />
          </div>
          <div v-if="rows.length === 0" class="rule-empty">
            {{ t('inspectTemplatePage.ruleEmpty') }}
          </div>
          <div v-else class="rule-list">
            <div v-for="(row, index) in rows" :key="index" class="rule-row rule-row--choice">
              <span class="rule-row__no">{{ index + 1 }}</span>
              <a-select
                v-if="optionList.length > 0"
                v-model:value="row.optionValue"
                :options="optionList.map((o) => ({ value: o, label: o }))"
                class="rule-row__field"
                allow-clear
                :placeholder="t('inspectTemplatePage.ruleOptionPlaceholder')"
              />
              <a-input
                v-else
                v-model:value="row.optionValue"
                class="rule-row__field"
                :placeholder="t('inspectTemplatePage.ruleOptionPlaceholder')"
                allow-clear
              />
              <a-input-number
                v-model:value="row.weight"
                :min="0"
                class="rule-row__score"
                :placeholder="t('inspectTemplatePage.ruleScorePlaceholder')"
              />
              <a-button
                type="text"
                danger
                class="rule-row__delete"
                :disabled="rows.length <= 1"
                @click="removeRow(index)"
              >
                <DeleteOutlined />
              </a-button>
            </div>
          </div>
        </template>

        <a-button
          type="dashed"
          block
          class="rule-add"
          :disabled="!canAddRow"
          @click="addRow"
        >
          <PlusOutlined />
          {{ t('inspectTemplatePage.ruleAddRow') }}
        </a-button>
      </div>
    </div>

    <div class="modal-footer">
      <a-button @click="closeModal">{{ t('inspectTemplatePage.cancel') }}</a-button>
      <a-button type="primary" @click="onSave">
        {{ t('inspectTemplatePage.save') }}
      </a-button>
    </div>
  </a-modal>
</template>

<style scoped>
.inspect-rule-config-modal :deep(.ant-modal-body) {
  padding: 16px 24px 0;
}

.modal-title {
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

.rule-config {
  display: flex;
  flex-direction: column;
  gap: 14px;
  padding-bottom: 8px;
}

.type-badge {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  align-self: flex-start;
  padding: 8px 14px;
  border-radius: 999px;
  border: 1px solid transparent;
}

.type-badge__icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  border-radius: 50%;
  font-size: 14px;
}

.type-badge__label {
  font-size: 13px;
  font-weight: 600;
  color: var(--omes-color-text);
}

.type-badge--choice {
  background: var(--omes-color-accent-purple-bg);
  border-color: var(--omes-color-accent-purple-border);
}

.type-badge--choice .type-badge__icon {
  color: var(--omes-color-accent-purple-from);
  background: var(--omes-color-bg-container);
}

.type-badge--numeric {
  background: var(--omes-color-primary-bg);
  border-color: var(--omes-color-primary-border);
}

.type-badge--numeric .type-badge__icon {
  color: var(--omes-color-primary);
  background: var(--omes-color-bg-container);
}

.type-badge--bool {
  background: var(--omes-color-success-bg);
  border-color: var(--omes-color-success-border);
}

.type-badge--bool .type-badge__icon {
  color: var(--omes-color-success);
  background: var(--omes-color-bg-container);
}

.hint-panel {
  display: flex;
  flex-direction: column;
  gap: 6px;
  padding: 12px 14px;
  background: var(--omes-color-bg-elevated);
  border: 1px solid var(--omes-color-border);
  border-radius: var(--omes-radius-lg);
}

.field-hint {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  margin: 0;
  font-size: 12px;
  line-height: 1.55;
  color: var(--omes-color-text-tertiary);
}

.field-hint :deep(.anticon) {
  margin-top: 3px;
  flex-shrink: 0;
  color: var(--omes-color-primary);
}

.field-hint--secondary {
  color: var(--omes-color-text-quaternary);
}

.form-section {
  padding: 16px 18px;
  background: var(--omes-color-bg-elevated);
  border: 1px solid var(--omes-color-border);
  border-radius: var(--omes-radius-lg);
}

.form-section--numeric {
  background: linear-gradient(180deg, var(--omes-color-bg-table-hover-alt) 0%, var(--omes-color-primary-bg-hover) 100%);
  border-color: var(--omes-color-primary-border);
}

.form-section--choice {
  background: linear-gradient(180deg, var(--omes-color-bg-container) 0%, var(--omes-color-accent-purple-bg) 100%);
  border-color: var(--omes-color-accent-purple-border);
}

.form-section--bool {
  background: linear-gradient(180deg, var(--omes-color-bg-container) 0%, var(--omes-color-success-bg) 100%);
  border-color: var(--omes-color-success-border);
}

.section-title {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 14px;
  font-size: 14px;
  font-weight: 600;
  color: var(--omes-color-text);
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

.form-section--numeric .section-title::before {
  background: var(--omes-color-primary-hover);
}

.form-section--choice .section-title::before {
  background: var(--omes-color-accent-purple-from);
}

.form-section--choice .section-title :deep(.anticon) {
  color: var(--omes-color-accent-purple-from);
}

.form-section--bool .section-title::before {
  background: var(--omes-color-success);
}

.form-section--bool .section-title :deep(.anticon) {
  color: var(--omes-color-success);
}

.rule-header {
  display: grid;
  gap: 8px;
  margin-bottom: 8px;
  padding: 0 4px 0 32px;
  font-size: 12px;
  font-weight: 600;
  color: var(--omes-color-text-quaternary);
}

.rule-header--bool,
.rule-header--choice {
  grid-template-columns: 1fr 110px 32px;
}

.rule-header--numeric {
  grid-template-columns: 1fr 1fr 110px 32px;
}

.rule-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-bottom: 12px;
}

.rule-row {
  display: grid;
  align-items: center;
  gap: 8px;
  padding: 8px 10px;
  background: rgba(255, 255, 255, 0.85);
  border: 1px solid rgba(0, 0, 0, 0.06);
  border-radius: var(--omes-radius-md);
  transition: border-color 0.2s, box-shadow 0.2s;
}

.rule-row:hover {
  border-color: rgba(22, 119, 255, 0.25);
  box-shadow: 0 2px 6px rgba(22, 119, 255, 0.06);
}

.rule-row--bool,
.rule-row--choice {
  grid-template-columns: 24px 1fr 110px 32px;
}

.rule-row--numeric {
  grid-template-columns: 24px 1fr 1fr 110px 32px;
}

.rule-row__no {
  font-size: 12px;
  font-weight: 600;
  color: var(--omes-color-text-placeholder);
  text-align: center;
}

.rule-row__field {
  width: 100%;
  min-width: 0;
}

.rule-row__score {
  width: 100%;
}

.rule-row__delete {
  flex-shrink: 0;
}

.rule-empty {
  margin-bottom: 12px;
  padding: 24px 12px;
  text-align: center;
  font-size: 13px;
  color: var(--omes-color-text-placeholder);
  background: rgba(255, 255, 255, 0.6);
  border: 1px dashed var(--omes-color-border-secondary);
  border-radius: var(--omes-radius-md);
}

.rule-add {
  margin-top: 2px;
}

.modal-footer {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  padding: 16px 0 4px;
  border-top: 1px solid var(--omes-color-border);
  margin-top: 4px;
}
</style>
