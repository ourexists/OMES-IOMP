<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { ApartmentOutlined, MenuOutlined } from '@ant-design/icons-vue'
import type { PermissionRecord, PlatformNode } from '@/api/ucenter'
import {
  addPermission,
  fetchPermissionStrategies,
  fetchPermissionTypes,
  mapOptions,
  modifyPermission,
} from '@/api/ucenter'
import { message } from 'ant-design-vue'

const props = defineProps<{
  open: boolean
  record: PermissionRecord | null
  platform: PlatformNode | null
  parentCode?: string
}>()

const emit = defineEmits<{
  'update:open': [value: boolean]
  success: []
}>()

const { t } = useI18n()

const isEdit = computed(() => Boolean(props.record?.id))
const title = computed(() => {
  if (isEdit.value) {
    return t('permissionPage.formEdit')
  }
  return props.parentCode ? t('permissionPage.formAddChild') : t('permissionPage.formAdd')
})

const typeOptions = ref<{ value: string | number; label: string }[]>([])
const strategyOptions = ref<{ value: string | number; label: string }[]>([])

const formState = reactive({
  code: '',
  name: '',
  i18n: '',
  type: 0,
  strategy: 0,
  icon: '',
  component: '',
  sortNo: 0,
  url: '',
})

async function loadOptions() {
  const [types, strategies] = await Promise.all([fetchPermissionTypes(), fetchPermissionStrategies()])
  typeOptions.value = mapOptions(types)
  strategyOptions.value = mapOptions(strategies)
}

watch(
  () => props.open,
  async (open) => {
    if (!open) {
      return
    }
    await loadOptions()
    formState.code = props.record?.code || ''
    formState.name = props.record?.name || ''
    formState.i18n = props.record?.i18n || ''
    formState.type = props.record?.type ?? 0
    formState.strategy = props.record?.strategy ?? 0
    formState.icon = props.record?.icon || ''
    formState.component = props.record?.component || ''
    formState.sortNo = props.record?.sortNo ?? 0
    formState.url = props.record?.url || ''
  },
)

async function handleSubmit() {
  if (!props.platform?.code) {
    message.warning(t('permissionPage.selectPlatformWarning'))
    return
  }

  const payload: PermissionRecord = {
    ...(props.record || { id: '' }),
    code: formState.code,
    name: formState.name,
    i18n: formState.i18n,
    type: formState.type,
    strategy: formState.strategy,
    icon: formState.icon,
    component: formState.component,
    sortNo: formState.sortNo,
    url: formState.url,
    platform: props.platform.code,
  }

  if (!isEdit.value && props.parentCode) {
    payload.pcode = props.parentCode
  }

  if (isEdit.value) {
    await modifyPermission(payload)
  } else {
    await addPermission(payload)
  }

  message.success(t('permissionPage.saveSuccess'))
  emit('update:open', false)
  emit('success')
}
</script>

<template>
  <a-modal
    :open="open"
    width="760px"
    destroy-on-close
    class="admin-form-modal"
    @update:open="emit('update:open', $event)"
    @ok="handleSubmit"
  >
    <template #title>
      <span class="modal-title">
        <span class="modal-title__icon modal-title__icon--purple">
          <ApartmentOutlined />
        </span>
        {{ title }}
      </span>
    </template>
    <a-form layout="vertical" class="system-form">
      <div class="system-form-section">
        <div class="system-form-section__title">
          <ApartmentOutlined />
          {{ t('permissionPage.formBasic') }}
        </div>
        <a-form-item :label="t('permissionPage.formCode')" required>
          <a-input
            v-model:value="formState.code"
            :disabled="isEdit"
            :placeholder="t('permissionPage.codePlaceholder')"
          />
        </a-form-item>
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item :label="t('permissionPage.formName')" required>
              <a-input v-model:value="formState.name" :placeholder="t('permissionPage.namePlaceholder')" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item :label="t('permissionPage.formI18n')" required>
              <a-input v-model:value="formState.i18n" :placeholder="t('permissionPage.i18nPlaceholder')" />
            </a-form-item>
          </a-col>
        </a-row>
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item :label="t('permissionPage.formType')" required>
              <a-select v-model:value="formState.type" :options="typeOptions" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item :label="t('permissionPage.formStrategy')" required>
              <a-select v-model:value="formState.strategy" :options="strategyOptions" />
            </a-form-item>
          </a-col>
        </a-row>
      </div>

      <div class="system-form-section">
        <div class="system-form-section__title">
          <MenuOutlined />
          {{ t('permissionPage.formMenu') }}
        </div>
        <a-row :gutter="16">
          <a-col :span="8">
            <a-form-item :label="t('permissionPage.formIcon')">
              <a-input v-model:value="formState.icon" :placeholder="t('permissionPage.iconPlaceholder')" />
            </a-form-item>
          </a-col>
          <a-col :span="8">
            <a-form-item :label="t('permissionPage.formComponent')">
              <a-input v-model:value="formState.component" :placeholder="t('permissionPage.componentPlaceholder')" />
            </a-form-item>
          </a-col>
          <a-col :span="8">
            <a-form-item :label="t('permissionPage.formSort')">
              <a-input-number v-model:value="formState.sortNo" style="width: 100%" :min="0" />
            </a-form-item>
          </a-col>
        </a-row>
        <a-form-item :label="t('permissionPage.formUrl')">
          <a-input v-model:value="formState.url" :placeholder="t('permissionPage.urlPlaceholder')" />
        </a-form-item>
      </div>
    </a-form>
  </a-modal>
</template>
