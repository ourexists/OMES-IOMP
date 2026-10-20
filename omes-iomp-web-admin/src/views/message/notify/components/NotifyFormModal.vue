<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { BellOutlined, SendOutlined } from '@ant-design/icons-vue'
import type { PlatformNode } from '@/api/ucenter'
import { fetchPlatforms } from '@/api/ucenter'
import { fetchMessageSourceMap, fetchMessageTypeMap, saveNotify } from '@/api/notify'
import type { NotifyFormPayload, NotifyRecord } from '@/types/notify'
import { message } from 'ant-design-vue'

const props = defineProps<{
  open: boolean
  record: NotifyRecord | null
}>()

const emit = defineEmits<{
  'update:open': [value: boolean]
  success: []
}>()

const { t } = useI18n()

const isEdit = computed(() => Boolean(props.record?.id))
const title = computed(() => (isEdit.value ? t('notifyPage.formEdit') : t('notifyPage.formAdd')))

const typeOptions = ref<{ value: number; label: string }[]>([])
const sourceOptions = ref<{ value: string; label: string }[]>([])
const platformOptions = ref<PlatformNode[]>([])

const formState = reactive({
  title: '',
  context: '',
  type: undefined as number | undefined,
  step: 0,
  source: undefined as string | undefined,
  sourceId: '',
  platforms: [] as string[],
})

async function loadOptions() {
  const [types, sources, platforms] = await Promise.all([
    fetchMessageTypeMap(),
    fetchMessageSourceMap(),
    fetchPlatforms(),
  ])
  typeOptions.value = Object.entries(types || {}).map(([code, label]) => ({
    value: Number(code),
    label,
  }))
  sourceOptions.value = Object.entries(sources || {}).map(([code, label]) => ({
    value: code,
    label,
  }))
  platformOptions.value = platforms || []
}

watch(
  () => props.open,
  async (open) => {
    if (!open) {
      return
    }
    await loadOptions()
    formState.title = props.record?.title || ''
    formState.context = props.record?.context || ''
    formState.type = props.record?.type
    formState.step = props.record?.step ?? 0
    formState.source = props.record?.source
    formState.sourceId = props.record?.sourceId || ''
    formState.platforms = [...(props.record?.platforms || [])]
  },
)

async function handleSubmit() {
  if (!formState.title?.trim()) {
    message.warning(t('notifyPage.titleRequired'))
    return
  }
  if (formState.type == null) {
    message.warning(t('notifyPage.typeRequired'))
    return
  }
  if (!formState.platforms.length) {
    message.warning(t('notifyPage.platformsRequired'))
    return
  }

  const payload: NotifyFormPayload = {
    id: props.record?.id,
    title: formState.title.trim(),
    context: formState.context,
    type: formState.type,
    step: formState.step ?? 0,
    source: formState.source,
    sourceId: formState.sourceId || undefined,
    platforms: formState.platforms,
  }
  await saveNotify(payload)
  message.success(t('notifyPage.saveSuccess'))
  emit('update:open', false)
  emit('success')
}
</script>

<template>
  <a-modal
    :open="open"
    width="720px"
    destroy-on-close
    class="admin-form-modal"
    @update:open="emit('update:open', $event)"
    @ok="handleSubmit"
  >
    <template #title>
      <span class="modal-title">
        <span class="modal-title__icon modal-title__icon--primary">
          <BellOutlined />
        </span>
        {{ title }}
      </span>
    </template>
    <a-form layout="vertical" class="system-form">
      <div class="system-form-section">
        <div class="system-form-section__title">
          <BellOutlined />
          {{ t('notifyPage.formBasic') }}
        </div>
        <a-form-item :label="t('notifyPage.colTitle')" required>
          <a-input v-model:value="formState.title" :placeholder="t('notifyPage.titlePlaceholder')" />
        </a-form-item>
        <a-form-item :label="t('notifyPage.colContent')">
          <a-textarea
            v-model:value="formState.context"
            :rows="4"
            :placeholder="t('notifyPage.contentPlaceholder')"
          />
        </a-form-item>
      </div>

      <div class="system-form-section">
        <div class="system-form-section__title">
          <SendOutlined />
          {{ t('notifyPage.formRule') }}
        </div>
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item :label="t('notifyPage.colType')" required>
              <a-select
                v-model:value="formState.type"
                :options="typeOptions"
                :placeholder="t('notifyPage.typePlaceholder')"
                allow-clear
              />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item :label="t('notifyPage.colInterval')" required>
              <a-input-number
                v-model:value="formState.step"
                :min="0"
                :max="100"
                :step="1"
                style="width: 100%"
              />
            </a-form-item>
          </a-col>
        </a-row>
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item :label="t('notifyPage.colSource')">
              <a-select
                v-model:value="formState.source"
                :options="sourceOptions"
                :placeholder="t('notifyPage.sourcePlaceholder')"
                allow-clear
              />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item :label="t('notifyPage.colSourceId')">
              <a-input
                v-model:value="formState.sourceId"
                :placeholder="t('notifyPage.sourceIdPlaceholder')"
              />
            </a-form-item>
          </a-col>
        </a-row>
        <a-form-item :label="t('notifyPage.colPlatforms')" required>
          <a-checkbox-group v-model:value="formState.platforms" class="platform-group">
            <a-checkbox
              v-for="item in platformOptions"
              :key="item.code"
              :value="item.code"
            >
              {{ item.name }}
            </a-checkbox>
          </a-checkbox-group>
        </a-form-item>
      </div>
    </a-form>
  </a-modal>
</template>

<style scoped>
.platform-group {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.platform-group :deep(.ant-checkbox-wrapper) {
  margin-inline-start: 0;
  padding: 6px 10px;
  border: 1px solid var(--omes-color-border);
  border-radius: var(--omes-radius-md);
  background: var(--omes-color-bg-container);
}

.platform-group :deep(.ant-checkbox-wrapper-checked) {
  border-color: var(--omes-color-primary);
  background: var(--omes-color-primary-bg);
}
</style>
