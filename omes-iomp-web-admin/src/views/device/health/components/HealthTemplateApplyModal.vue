<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import type { EquipRecord, ProductOption } from '@/api/device'
import { fetchEquipById, fetchEquipPage, fetchProductListAll, saveEquip } from '@/api/device'
import type { HealthRuleTemplate } from '@/api/equip-health'
import type { ProductModelRecord } from '@/api/product-model'
import { fetchProductModels } from '@/api/product-model'
import { message } from 'ant-design-vue'

const props = defineProps<{
  open: boolean
  template: HealthRuleTemplate | null
}>()

const emit = defineEmits<{
  'update:open': [value: boolean]
}>()

const { t } = useI18n()

const loading = ref(false)
const saving = ref(false)
const products = ref<ProductOption[]>([])
const models = ref<ProductModelRecord[]>([])
const devices = ref<EquipRecord[]>([])
const productCode = ref<string>()
const modelId = ref<string>()
const equipId = ref<string>()

const productOptions = computed(() =>
  products.value
    .filter((item) => item.code)
    .map((item) => ({ value: item.code as string, label: item.name || item.code })),
)

const modelOptions = computed(() =>
  models.value.map((item) => ({ value: item.id, label: item.name || item.code || item.id })),
)

const scopedDevices = computed(() => {
  if (!modelId.value) {
    return devices.value
  }
  return devices.value.filter((item) => item.modelId === modelId.value)
})

const deviceOptions = computed(() =>
  scopedDevices.value.map((item) => ({
    value: item.id,
    label: [item.name, item.selfCode].filter(Boolean).join(' / ') || item.id,
  })),
)

const targets = computed(() => {
  if (equipId.value) {
    return devices.value.filter((item) => item.id === equipId.value)
  }
  return scopedDevices.value
})

const scopeText = computed(() => {
  if (!productCode.value) {
    return ''
  }
  if (equipId.value) {
    return t('healthPage.applyScopeEquip')
  }
  if (modelId.value) {
    return t('healthPage.applyScopeModel', { count: targets.value.length })
  }
  return t('healthPage.applyScopeProduct', { count: targets.value.length })
})

watch(
  () => props.open,
  async (open) => {
    if (!open) {
      return
    }
    productCode.value = undefined
    modelId.value = undefined
    equipId.value = undefined
    models.value = []
    devices.value = []
    loading.value = true
    try {
      const list = await fetchProductListAll()
      products.value = Array.isArray(list) ? list : []
    } finally {
      loading.value = false
    }
  },
)

async function onProductChange(code?: string) {
  modelId.value = undefined
  equipId.value = undefined
  models.value = []
  devices.value = []
  if (!code) {
    return
  }
  loading.value = true
  try {
    const [modelList, page] = await Promise.all([
      fetchProductModels(code),
      fetchEquipPage({ page: 1, pageSize: 1000, type: code }),
    ])
    models.value = Array.isArray(modelList) ? modelList : []
    devices.value = page?.records ?? []
  } finally {
    loading.value = false
  }
}

function onModelChange() {
  equipId.value = undefined
}

async function handleSubmit() {
  if (!props.template?.id || !productCode.value) {
    message.warning(t('healthPage.applyProductRequired'))
    return
  }
  const list = targets.value
  if (!list.length) {
    message.warning(t('healthPage.applyEmpty'))
    return
  }
  saving.value = true
  let done = 0
  try {
    for (const item of list) {
      const detail = await fetchEquipById(item.id)
      if (!detail?.id) {
        continue
      }
      detail.healthTemplateId = props.template.id
      // shortcut: 一次最多 1000 台且逐台整行保存，设备更多时改成按产品/型号只更新 healthTemplateId
      await saveEquip(detail)
      done += 1
    }
    message.success(t('healthPage.applyDone', { count: done }))
    emit('update:open', false)
  } catch {
    if (done > 0) {
      message.warning(t('healthPage.applyPartial', { count: done }))
    }
  } finally {
    saving.value = false
  }
}
</script>

<template>
  <a-modal
    :open="open"
    :title="t('healthPage.applyTitle')"
    width="520px"
    :confirm-loading="saving"
    :ok-text="t('healthPage.applyOk', { count: targets.length })"
    :cancel-text="t('healthPage.cancel')"
    destroy-on-close
    @update:open="emit('update:open', $event)"
    @ok="handleSubmit"
  >
    <a-spin :spinning="loading">
      <p v-if="template?.name" class="apply-template">{{ template.name }}</p>
      <a-form layout="vertical">
        <a-form-item :label="t('healthPage.applyProduct')" required>
          <a-select
            v-model:value="productCode"
            show-search
            allow-clear
            option-filter-prop="label"
            :placeholder="t('healthPage.applyProductPlaceholder')"
            :options="productOptions"
            @change="onProductChange"
          />
        </a-form-item>
        <a-form-item :label="t('healthPage.applyModel')">
          <a-select
            v-model:value="modelId"
            show-search
            allow-clear
            option-filter-prop="label"
            :disabled="!productCode"
            :placeholder="t('healthPage.applyModelPlaceholder')"
            :options="modelOptions"
            @change="onModelChange"
          />
        </a-form-item>
        <a-form-item :label="t('healthPage.applyEquip')">
          <a-select
            v-model:value="equipId"
            show-search
            allow-clear
            option-filter-prop="label"
            :disabled="!productCode"
            :placeholder="t('healthPage.applyEquipPlaceholder')"
            :options="deviceOptions"
          />
        </a-form-item>
      </a-form>
      <p v-if="scopeText" class="apply-scope">{{ scopeText }}</p>
    </a-spin>
  </a-modal>
</template>

<style scoped>
.apply-template {
  margin: 0 0 12px;
  font-weight: 600;
}

.apply-scope {
  margin: 0;
  color: var(--omes-color-text-secondary);
}
</style>
