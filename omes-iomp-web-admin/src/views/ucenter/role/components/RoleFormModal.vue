<script setup lang="ts">
import { computed, reactive, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { TeamOutlined } from '@ant-design/icons-vue'
import type { RoleRecord } from '@/api/ucenter'
import { saveRole } from '@/api/ucenter'
import { message } from 'ant-design-vue'

const props = defineProps<{
  open: boolean
  record: RoleRecord | null
}>()

const emit = defineEmits<{
  'update:open': [value: boolean]
  success: []
}>()

const { t } = useI18n()

const isEdit = computed(() => Boolean(props.record?.id))
const title = computed(() => (isEdit.value ? t('rolePage.formEdit') : t('rolePage.formAdd')))

const formState = reactive({
  name: '',
  code: '',
  description: '',
})

watch(
  () => props.open,
  (open) => {
    if (!open) {
      return
    }
    formState.name = props.record?.name || ''
    formState.code = props.record?.code || ''
    formState.description = props.record?.description || ''
  },
)

async function handleSubmit() {
  await saveRole({
    ...(props.record || { id: '' }),
    name: formState.name,
    code: formState.code,
    description: formState.description,
  })
  message.success(t('rolePage.saveSuccess'))
  emit('update:open', false)
  emit('success')
}
</script>

<template>
  <a-modal
    :open="open"
    width="560px"
    destroy-on-close
    class="admin-form-modal"
    @update:open="emit('update:open', $event)"
    @ok="handleSubmit"
  >
    <template #title>
      <span class="modal-title">
        <span class="modal-title__icon modal-title__icon--purple">
          <TeamOutlined />
        </span>
        {{ title }}
      </span>
    </template>
    <a-form layout="vertical" class="system-form">
      <div class="system-form-section">
        <div class="system-form-section__title">
          <TeamOutlined />
          {{ t('rolePage.formBasic') }}
        </div>
        <a-form-item :label="t('rolePage.colName')" required>
          <a-input v-model:value="formState.name" :placeholder="t('rolePage.namePlaceholder')" />
        </a-form-item>
        <a-form-item :label="t('rolePage.colCode')" required>
          <a-input
            v-model:value="formState.code"
            :disabled="isEdit"
            :placeholder="t('rolePage.codePlaceholder')"
          />
        </a-form-item>
        <a-form-item :label="t('rolePage.colDescription')">
          <a-textarea
            v-model:value="formState.description"
            :rows="4"
            :placeholder="t('rolePage.descriptionPlaceholder')"
          />
        </a-form-item>
      </div>
    </a-form>
  </a-modal>
</template>
