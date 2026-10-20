<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import {
  CheckSquareOutlined,
  DeleteOutlined,
  FieldNumberOutlined,
  FormOutlined,
  InfoCircleOutlined,
  PlusOutlined,
  UnorderedListOutlined,
} from '@ant-design/icons-vue'
import type { InspectItemRecord } from '@/api/inspect-item'
import {
  fetchInspectItemById,
  parseChoiceOptions,
  saveInspectItem,
  serializeChoiceOptions,
} from '@/api/inspect-item'
import { message } from 'ant-design-vue'

const props = defineProps<{
  open: boolean
  record: InspectItemRecord | null
}>()

const emit = defineEmits<{
  'update:open': [value: boolean]
  success: []
}>()

const { t } = useI18n()

const loading = ref(false)
const saving = ref(false)
const choiceOptions = ref<string[]>([''])

const initialSnapshot = ref({
  itemName: '',
  itemType: 2,
  unit: '',
  choiceOptions: [''] as string[],
  minValue: null as number | null,
  maxValue: null as number | null,
  requiredFlag: false,
})

const formState = reactive({
  id: '' as string | null,
  itemName: '',
  itemType: 2,
  unit: '',
  minValue: null as number | null,
  maxValue: null as number | null,
  requiredFlag: false,
})

const isEdit = computed(() => Boolean(props.record?.id))
const title = computed(() =>
  isEdit.value ? t('inspectItemPage.formEdit') : t('inspectItemPage.formAdd'),
)
const isNumericType = computed(() => formState.itemType === 2)
const isChoiceType = computed(() => formState.itemType === 1)

const typeCards = computed(() => [
  {
    value: 1,
    label: t('inspectItemPage.itemType.选择'),
    desc: t('inspectItemPage.typeChoiceDesc'),
    icon: UnorderedListOutlined,
    tone: 'choice',
  },
  {
    value: 2,
    label: t('inspectItemPage.itemType.数值'),
    desc: t('inspectItemPage.typeNumericDesc'),
    icon: FieldNumberOutlined,
    tone: 'numeric',
  },
  {
    value: 3,
    label: t('inspectItemPage.itemType.是否'),
    desc: t('inspectItemPage.typeBoolDesc'),
    icon: CheckSquareOutlined,
    tone: 'bool',
  },
])

function defaultChoiceOptions(): string[] {
  return ['正常', '异常']
}

function normalizeChoiceOptions(options: string[]): string[] {
  const cleaned = options.map((o) => o.trim()).filter(Boolean)
  return cleaned.length > 0 ? cleaned : ['']
}

function setChoiceOptionsFromUnit(unit?: string) {
  const parsed = parseChoiceOptions(unit)
  choiceOptions.value = normalizeChoiceOptions(parsed.length > 0 ? parsed : defaultChoiceOptions())
}

function snapshotForm() {
  initialSnapshot.value = {
    itemName: formState.itemName,
    itemType: formState.itemType,
    unit: formState.unit,
    choiceOptions: [...choiceOptions.value],
    minValue: formState.minValue,
    maxValue: formState.maxValue,
    requiredFlag: formState.requiredFlag,
  }
}

function resetForm() {
  formState.itemName = initialSnapshot.value.itemName
  formState.itemType = initialSnapshot.value.itemType
  formState.unit = initialSnapshot.value.unit
  choiceOptions.value = [...initialSnapshot.value.choiceOptions]
  formState.minValue = initialSnapshot.value.minValue
  formState.maxValue = initialSnapshot.value.maxValue
  formState.requiredFlag = initialSnapshot.value.requiredFlag
}

function clearForm() {
  formState.id = null
  formState.itemName = ''
  formState.itemType = 2
  formState.unit = ''
  formState.minValue = null
  formState.maxValue = null
  formState.requiredFlag = false
  choiceOptions.value = ['']
}

async function loadDetail() {
  if (!props.record?.id) {
    clearForm()
    snapshotForm()
    return
  }
  loading.value = true
  try {
    const detail = await fetchInspectItemById(props.record.id)
    formState.id = detail?.id || props.record.id
    formState.itemName = detail?.itemName || ''
    formState.itemType = detail?.itemType ?? 2
    formState.unit = detail?.unit || ''
    formState.minValue = detail?.minValue ?? null
    formState.maxValue = detail?.maxValue ?? null
    formState.requiredFlag = Boolean(detail?.requiredFlag)
    if (formState.itemType === 1) {
      setChoiceOptionsFromUnit(formState.unit)
    } else {
      choiceOptions.value = ['']
    }
    snapshotForm()
  } finally {
    loading.value = false
  }
}

watch(
  () => props.open,
  (open) => {
    if (open) {
      loadDetail()
    }
  },
)

function closeModal() {
  emit('update:open', false)
}

function selectType(value: number) {
  formState.itemType = value
  if (value === 1) {
    setChoiceOptionsFromUnit(formState.unit)
    formState.minValue = null
    formState.maxValue = null
    formState.requiredFlag = false
    return
  }
  if (value === 2) {
    choiceOptions.value = ['']
    return
  }
  formState.unit = ''
  choiceOptions.value = ['']
  formState.minValue = null
  formState.maxValue = null
  formState.requiredFlag = false
}

function addOption() {
  choiceOptions.value.push('')
}

function removeOption(index: number) {
  if (choiceOptions.value.length <= 1) {
    return
  }
  choiceOptions.value.splice(index, 1)
}

function validateChoiceOptions(): string[] | null {
  const cleaned = choiceOptions.value.map((o) => o.trim()).filter(Boolean)
  if (cleaned.length === 0) {
    message.warning(t('inspectItemPage.optionsRequired'))
    return null
  }
  const unique = new Set(cleaned)
  if (unique.size !== cleaned.length) {
    message.warning(t('inspectItemPage.optionDuplicate'))
    return null
  }
  return cleaned
}

function resolveUnitForSave(): string | undefined {
  if (isChoiceType.value) {
    const cleaned = validateChoiceOptions()
    return cleaned ? serializeChoiceOptions(cleaned) : undefined
  }
  if (isNumericType.value) {
    return formState.unit.trim() || undefined
  }
  return undefined
}

async function handleSubmit() {
  const itemName = formState.itemName.trim()
  if (!itemName) {
    message.warning(t('inspectItemPage.nameRequired'))
    return
  }
  const unit = resolveUnitForSave()
  if (isChoiceType.value && unit === undefined) {
    return
  }
  saving.value = true
  try {
    await saveInspectItem({
      id: formState.id || undefined,
      templateId: undefined,
      itemName,
      itemType: formState.itemType,
      unit,
      minValue: isNumericType.value ? formState.minValue : undefined,
      maxValue: isNumericType.value ? formState.maxValue : undefined,
      requiredFlag: isNumericType.value ? formState.requiredFlag : false,
    })
    message.success(t('inspectItemPage.saveSuccess'))
    emit('success')
    closeModal()
  } finally {
    saving.value = false
  }
}
</script>

<template>
  <a-modal
    :open="open"
    :title="title"
    width="600px"
    destroy-on-close
    class="inspect-item-form-modal"
    :footer="null"
    @cancel="closeModal"
    @update:open="emit('update:open', $event)"
  >
    <template #title>
      <span class="modal-title">
        <FormOutlined />
        {{ title }}
      </span>
    </template>

    <a-spin :spinning="loading">
      <a-form layout="vertical" class="inspect-item-form">
        <div class="form-section">
          <div class="section-title">
            <FormOutlined />
            {{ t('inspectItemPage.formBasic') }}
          </div>

          <a-form-item :label="t('inspectItemPage.colName')" required>
            <a-input
              v-model:value="formState.itemName"
              :placeholder="t('inspectItemPage.namePlaceholder')"
              allow-clear
              size="large"
            />
          </a-form-item>

          <a-form-item :label="t('inspectItemPage.colType')" class="type-form-item">
            <div class="type-cards">
              <button
                v-for="card in typeCards"
                :key="card.value"
                type="button"
                class="type-card"
                :class="[
                  `type-card--${card.tone}`,
                  { 'type-card--active': formState.itemType === card.value },
                ]"
                @click="selectType(card.value)"
              >
                <span class="type-card__icon">
                  <component :is="card.icon" />
                </span>
                <span class="type-card__label">{{ card.label }}</span>
                <span class="type-card__desc">{{ card.desc }}</span>
              </button>
            </div>
          </a-form-item>
        </div>

        <div class="extended-section-wrap">
          <Transition name="section-fade" mode="out-in">
            <div v-if="isChoiceType" key="choice" class="form-section form-section--choice">
              <div class="section-title">
                <UnorderedListOutlined />
                {{ t('inspectItemPage.formChoice') }}
              </div>

              <div class="option-list">
                <div
                  v-for="(_, index) in choiceOptions"
                  :key="index"
                  class="option-row"
                >
                  <span class="option-row__no">{{ index + 1 }}</span>
                  <a-input
                    v-model:value="choiceOptions[index]"
                    :placeholder="t('inspectItemPage.optionPlaceholder')"
                    allow-clear
                  />
                  <a-button
                    type="text"
                    danger
                    class="option-row__delete"
                    :disabled="choiceOptions.length <= 1"
                    @click="removeOption(index)"
                  >
                    <DeleteOutlined />
                  </a-button>
                </div>
              </div>

              <a-button type="dashed" block class="option-add" @click="addOption">
                <PlusOutlined />
                {{ t('inspectItemPage.addOption') }}
              </a-button>

              <p class="field-hint">
                <InfoCircleOutlined />
                {{ t('inspectItemPage.unitHintChoice') }}
              </p>
            </div>

            <div v-else-if="isNumericType" key="numeric" class="form-section form-section--numeric">
              <div class="section-title">
                <FieldNumberOutlined />
                {{ t('inspectItemPage.formNumeric') }}
              </div>

              <a-form-item :label="t('inspectItemPage.colUnit')">
                <a-input
                  v-model:value="formState.unit"
                  :placeholder="t('inspectItemPage.unitPlaceholderNumeric')"
                  allow-clear
                />
                <p class="field-hint">
                  <InfoCircleOutlined />
                  {{ t('inspectItemPage.unitHintNumeric') }}
                </p>
              </a-form-item>

              <a-row :gutter="16">
                <a-col :span="12">
                  <a-form-item :label="t('inspectItemPage.colMin')">
                    <a-input-number
                      v-model:value="formState.minValue"
                      :placeholder="t('inspectItemPage.minPlaceholder')"
                      style="width: 100%"
                    />
                  </a-form-item>
                </a-col>
                <a-col :span="12">
                  <a-form-item :label="t('inspectItemPage.colMax')">
                    <a-input-number
                      v-model:value="formState.maxValue"
                      :placeholder="t('inspectItemPage.maxPlaceholder')"
                      style="width: 100%"
                    />
                  </a-form-item>
                </a-col>
              </a-row>

              <div class="required-row">
                <div class="required-row__text">
                  <span class="required-row__label">{{ t('inspectItemPage.colRequired') }}</span>
                  <span class="required-row__hint">{{ t('inspectItemPage.requiredHint') }}</span>
                </div>
                <a-switch v-model:checked="formState.requiredFlag" />
              </div>
            </div>
          </Transition>
        </div>
      </a-form>
    </a-spin>

    <div class="modal-footer">
      <a-button @click="resetForm">{{ t('inspectItemPage.reset') }}</a-button>
      <a-button @click="closeModal">{{ t('inspectItemPage.cancel') }}</a-button>
      <a-button type="primary" :loading="saving" @click="handleSubmit">
        {{ t('inspectItemPage.save') }}
      </a-button>
    </div>
  </a-modal>
</template>

<style scoped>
.inspect-item-form-modal :deep(.ant-modal-body) {
  padding: 16px 24px 0;
}

.modal-title {
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

.inspect-item-form {
  display: flex;
  flex-direction: column;
  gap: 14px;
  padding-bottom: 8px;
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

.section-title {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 16px;
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

.inspect-item-form :deep(.ant-form-item) {
  margin-bottom: 16px;
}

.inspect-item-form :deep(.ant-form-item:last-child) {
  margin-bottom: 0;
}

.inspect-item-form :deep(.ant-form-item-label > label) {
  font-weight: 500;
  color: var(--omes-color-text-label);
}

.type-form-item :deep(.ant-form-item-control-input) {
  min-height: auto;
}

.type-cards {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
}

.type-card {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 6px;
  padding: 12px 12px 10px;
  border: 1px solid var(--omes-color-border-hover);
  border-radius: var(--omes-radius-lg);
  background: var(--omes-color-bg-container);
  cursor: pointer;
  text-align: left;
  transition: border-color 0.2s, box-shadow 0.2s, background 0.2s;
}

.type-card:hover {
  border-color: var(--omes-color-primary-border);
  box-shadow: 0 2px 8px rgba(22, 119, 255, 0.08);
}

.type-card--active {
  border-color: var(--omes-color-primary);
  background: var(--omes-color-primary-bg);
  box-shadow: 0 0 0 2px color-mix(in srgb, var(--omes-color-primary) 16%, transparent);
}

.type-card__icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  border-radius: var(--omes-radius-md);
  font-size: 16px;
}

.type-card--choice .type-card__icon {
  color: var(--omes-color-accent-purple-from);
  background: var(--omes-color-accent-purple-bg);
}

.type-card--numeric .type-card__icon {
  color: var(--omes-color-primary);
  background: var(--omes-color-primary-bg);
}

.type-card--bool .type-card__icon {
  color: var(--omes-color-success);
  background: var(--omes-color-success-bg);
}

.type-card--active.type-card--choice .type-card__icon {
  color: var(--omes-color-accent-purple-from);
  background: var(--omes-color-accent-purple-bg-strong);
}

.type-card--active.type-card--numeric .type-card__icon {
  color: var(--omes-color-primary-active);
  background: var(--omes-color-primary-border);
}

.type-card--active.type-card--bool .type-card__icon {
  color: var(--omes-color-success);
  background: var(--omes-color-success-border);
}

.type-card__label {
  font-size: 14px;
  font-weight: 600;
  color: var(--omes-color-text);
  line-height: 1.3;
}

.type-card__desc {
  font-size: 12px;
  line-height: 1.45;
  color: var(--omes-color-text-quaternary);
}

.option-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-bottom: 10px;
}

.option-row {
  display: flex;
  align-items: center;
  gap: 8px;
}

.option-row__no {
  flex: 0 0 24px;
  font-size: 12px;
  font-weight: 600;
  color: var(--omes-color-text-quaternary);
  text-align: center;
}

.option-row :deep(.ant-input) {
  flex: 1;
}

.option-row__delete {
  flex-shrink: 0;
}

.option-add {
  margin-bottom: 4px;
}

.field-hint {
  display: flex;
  align-items: flex-start;
  gap: 6px;
  margin: 8px 0 0;
  font-size: 12px;
  line-height: 1.5;
  color: var(--omes-color-text-quaternary);
}

.field-hint :deep(.anticon) {
  margin-top: 3px;
  flex-shrink: 0;
}

.form-section--choice .field-hint :deep(.anticon) {
  color: var(--omes-color-accent-purple-from);
}

.form-section--numeric .field-hint :deep(.anticon) {
  color: var(--omes-color-primary);
}

.required-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 12px 14px;
  background: var(--omes-color-bg-container);
  border: 1px solid var(--omes-color-primary-bg);
  border-radius: var(--omes-radius-md);
}

.required-row__text {
  display: flex;
  flex-direction: column;
  gap: 4px;
  min-width: 0;
}

.required-row__label {
  font-size: 14px;
  font-weight: 500;
  color: var(--omes-color-text);
}

.required-row__hint {
  font-size: 12px;
  line-height: 1.45;
  color: var(--omes-color-text-quaternary);
}

.modal-footer {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  margin-top: 16px;
  padding: 16px 0 8px;
  border-top: 1px solid var(--omes-color-border);
}

.extended-section-wrap {
  min-height: 0;
}

.section-fade-enter-active,
.section-fade-leave-active {
  transition: opacity 0.12s ease;
}

.section-fade-enter-from,
.section-fade-leave-to {
  opacity: 0;
}

@media (max-width: 576px) {
  .type-cards {
    grid-template-columns: 1fr;
  }
}
</style>
