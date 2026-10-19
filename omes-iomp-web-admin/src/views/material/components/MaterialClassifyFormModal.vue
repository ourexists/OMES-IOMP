<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { BarcodeOutlined, FolderOutlined, InfoCircleOutlined } from '@ant-design/icons-vue'
import type { MaterialClassifyRecord } from '@/api/material'
import { saveMaterialClassify } from '@/api/material'
import { message } from 'ant-design-vue'

const props = defineProps<{
  open: boolean
  record: MaterialClassifyRecord | null
}>()

const emit = defineEmits<{
  'update:open': [value: boolean]
  success: []
}>()

const { t } = useI18n()

const saving = ref(false)

const formState = reactive({
  id: '',
  name: '',
  selfCode: '',
})

const initialSnapshot = ref({
  name: '',
  selfCode: '',
})

const isEdit = computed(() => Boolean(props.record?.id))
const title = computed(() =>
  isEdit.value ? t('materialPage.classifyFormEdit') : t('materialPage.classifyFormAdd'),
)
const modeTag = computed(() =>
  isEdit.value ? t('materialPage.formModeEdit') : t('materialPage.formModeAdd'),
)

function snapshotForm() {
  initialSnapshot.value = {
    name: formState.name,
    selfCode: formState.selfCode,
  }
}

function resetForm() {
  formState.name = initialSnapshot.value.name
  formState.selfCode = initialSnapshot.value.selfCode
}

watch(
  () => props.open,
  (open) => {
    if (!open) {
      return
    }
    if (props.record) {
      formState.id = props.record.id || ''
      formState.name = props.record.name || ''
      formState.selfCode = props.record.selfCode || ''
    } else {
      formState.id = ''
      formState.name = ''
      formState.selfCode = ''
    }
    snapshotForm()
  },
)

function closeModal() {
  emit('update:open', false)
}

async function handleSubmit() {
  const name = formState.name.trim()
  const selfCode = formState.selfCode.trim()
  if (!name) {
    message.warning(t('materialPage.classifyNameRequired'))
    return
  }
  if (!selfCode) {
    message.warning(t('materialPage.classifyCodeRequired'))
    return
  }
  saving.value = true
  try {
    await saveMaterialClassify({
      id: formState.id || undefined,
      name,
      selfCode,
    })
    message.success(t('materialPage.saveSuccess'))
    emit('update:open', false)
    emit('success')
  } finally {
    saving.value = false
  }
}
</script>

<template>
  <a-modal
    :open="open"
    width="480px"
    destroy-on-close
    class="material-classify-form-modal"
    :footer="null"
    @cancel="closeModal"
    @update:open="emit('update:open', $event)"
  >
    <template #title>
      <div class="modal-title-wrap">
        <span class="modal-title">{{ title }}</span>
        <a-tag :color="isEdit ? 'processing' : 'success'">{{ modeTag }}</a-tag>
      </div>
    </template>

    <a-form layout="vertical" class="classify-form">
      <div class="form-section">
        <div class="section-title">
          <FolderOutlined />
          {{ t('materialPage.formClassifySection') }}
        </div>

        <a-form-item :label="t('materialPage.classifyName')" required>
          <a-input
            v-model:value="formState.name"
            allow-clear
            :placeholder="t('materialPage.classifyNamePlaceholder')"
            @press-enter="handleSubmit"
          >
            <template #prefix>
              <FolderOutlined class="input-prefix-icon" />
            </template>
          </a-input>
        </a-form-item>

        <a-form-item :label="t('materialPage.classifyCode')" required>
          <a-input
            v-model:value="formState.selfCode"
            allow-clear
            :disabled="isEdit"
            :placeholder="t('materialPage.classifyCodePlaceholder')"
            @press-enter="handleSubmit"
          >
            <template #prefix>
              <BarcodeOutlined class="input-prefix-icon" />
            </template>
          </a-input>
          <div v-if="isEdit" class="field-hint">
            <InfoCircleOutlined />
            {{ t('materialPage.codeEditHint') }}
          </div>
          <div v-else class="field-hint">{{ t('materialPage.classifyCodeHint') }}</div>
        </a-form-item>
      </div>
    </a-form>

    <div class="modal-footer">
      <a-button @click="resetForm">{{ t('materialPage.reset') }}</a-button>
      <a-button @click="closeModal">{{ t('materialPage.cancel') }}</a-button>
      <a-button type="primary" :loading="saving" @click="handleSubmit">
        {{ t('materialPage.save') }}
      </a-button>
    </div>
  </a-modal>
</template>

<style scoped>
.material-classify-form-modal :deep(.ant-modal-body) {
  padding: 16px 24px 0;
}

.modal-title-wrap {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.modal-title {
  font-size: 16px;
  font-weight: 600;
  color: var(--omes-color-text);
}

.classify-form {
  padding-bottom: 4px;
}

.form-section {
  padding: 16px 18px;
  background: var(--omes-color-bg-elevated);
  border: 1px solid var(--omes-color-border);
  border-radius: var(--omes-radius-lg);
}

.section-title {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
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

.classify-form :deep(.ant-form-item) {
  margin-bottom: 16px;
}

.classify-form :deep(.ant-form-item:last-child) {
  margin-bottom: 0;
}

.classify-form :deep(.ant-form-item-label > label) {
  font-weight: 500;
  color: var(--omes-color-text-label);
}

.classify-form :deep(.ant-input-disabled),
.classify-form :deep(.ant-input-affix-wrapper-disabled) {
  background: var(--omes-color-bg-layout);
  color: var(--omes-color-text-secondary);
}

.input-prefix-icon {
  color: var(--omes-color-text-placeholder);
}

.field-hint {
  display: flex;
  align-items: flex-start;
  gap: 6px;
  margin-top: 6px;
  font-size: 12px;
  line-height: 1.5;
  color: var(--omes-color-text-quaternary);
}

.field-hint :deep(.anticon) {
  margin-top: 3px;
  flex-shrink: 0;
}

.modal-footer {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  margin-top: 16px;
  padding: 16px 0 8px;
  border-top: 1px solid var(--omes-color-border);
}
</style>
