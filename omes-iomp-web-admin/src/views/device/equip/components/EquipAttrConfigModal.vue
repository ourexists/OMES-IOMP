<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { AppstoreOutlined } from '@ant-design/icons-vue'
import type { EquipRecord } from '@/api/device'
import { fetchEquipConfig, saveEquipConfig } from '@/api/device'
import { fetchGatewayById, isDgwProtocol, type GatewayRecord } from '@/api/gateway'
import { fetchProductByCode } from '@/api/product'
import { fetchProductModelById, saveProductModel, type ProductModelRecord } from '@/api/product-model'
import GatewayPickerCard from '@/components/device/GatewayPickerCard.vue'
import type { EquipAlarmRow, EquipAttrRow, EquipControlRow } from '@/types/equip-config'
import { mergeProductAttrConfig } from '@/utils/product-attr-map'
import { message } from 'ant-design-vue'

const props = defineProps<{
  open: boolean
  equip: EquipRecord | null
}>()

const emit = defineEmits<{
  'update:open': [value: boolean]
  success: []
}>()

const { t } = useI18n()

const loading = ref(false)
const saving = ref(false)
const selectedGateway = ref<GatewayRecord | null>(null)
const modelDetail = ref<ProductModelRecord | null>(null)
const runMap = ref('')
const attrs = ref<EquipAttrRow[]>([])
const alarms = ref<EquipAlarmRow[]>([])
const controls = ref<EquipControlRow[]>([])

const hasProduct = computed(() => Boolean(String(props.equip?.type || '').trim()))
const hasModel = computed(() => Boolean(String(props.equip?.modelId || '').trim()))
const isDgw = computed(() => isDgwProtocol(selectedGateway.value?.protocol))

const productHint = computed(() => {
  if (!hasProduct.value) {
    return t('productPage.noProductHint')
  }
  if (!hasModel.value) {
    return t('productPage.noModelHint')
  }
  return t('productPage.fromModelHint', {
    name: props.equip?.modelName || props.equip?.modelId,
    product: props.equip?.typeDesc || props.equip?.type,
  })
})

const title = computed(() =>
  props.equip?.name
    ? t('equipAttrPage.title', { name: props.equip.name })
    : t('equipAttrPage.titleDefault'),
)

const namedAttrs = computed(() => attrs.value.filter((row) => String(row.name || '').trim()))
const namedAlarms = computed(() =>
  alarms.value.filter((row) => String(row.name || '').trim() || String(row.text || '').trim()),
)
const namedControls = computed(() => controls.value.filter((row) => String(row.name || '').trim()))

async function loadConfig() {
  if (!props.equip?.id) {
    return
  }
  loading.value = true
  try {
    const binding = await fetchEquipConfig(props.equip.id)
    const gwId = binding?.gwId
    if (gwId) {
      try {
        selectedGateway.value = await fetchGatewayById(gwId)
      } catch {
        selectedGateway.value = { id: gwId }
      }
    } else {
      selectedGateway.value = null
    }
    await loadModelMaps()
  } finally {
    loading.value = false
  }
}

async function loadModelMaps() {
  modelDetail.value = null
  runMap.value = ''
  attrs.value = []
  alarms.value = []
  controls.value = []
  if (!props.equip?.modelId) {
    return
  }
  const productCode = String(props.equip.type || '').trim()
  const [model, product] = await Promise.all([
    fetchProductModelById(props.equip.modelId).catch(() => null),
    productCode ? fetchProductByCode(productCode).catch(() => null) : Promise.resolve(null),
  ])
  modelDetail.value = model
  const merged = mergeProductAttrConfig(product?.attrConfig, model?.attrConfig)
  runMap.value = merged.runMap || ''
  attrs.value = merged.attrs || []
  alarms.value = merged.alarms || []
  controls.value = merged.controls || []
}

function persistRows<T extends { name?: string; text?: string; map?: string; fromProduct?: boolean }>(rows: T[]) {
  return rows
    .filter((row) => String(row.name || '').trim() || String(row.text || '').trim())
    .map(({ fromProduct: _fromProduct, ...rest }) => ({
      ...rest,
      map: String(rest.map || '').trim(),
    }))
}

function onGatewaySelect(gw: GatewayRecord) {
  selectedGateway.value = gw
}

function handleClose() {
  emit('update:open', false)
}

async function handleSave() {
  if (!props.equip?.id) {
    return
  }
  if (!selectedGateway.value?.id) {
    message.warning(t('equipAttrPage.gatewayRequired'))
    return
  }
  saving.value = true
  try {
    if (isDgw.value && modelDetail.value?.id) {
      await saveProductModel({
        id: modelDetail.value.id,
        productCode: modelDetail.value.productCode,
        name: modelDetail.value.name,
        code: modelDetail.value.code,
        attrConfig: {
          ...modelDetail.value.attrConfig,
          runMap: runMap.value.trim(),
          attrs: persistRows(attrs.value),
          alarms: persistRows(alarms.value),
          controls: persistRows(controls.value),
        },
      })
    }
    await saveEquipConfig({
      equipId: props.equip.id,
      gwId: selectedGateway.value.id,
    })
    message.success(t('equipAttrPage.saveSuccess'))
    emit('update:open', false)
    emit('success')
  } finally {
    saving.value = false
  }
}

watch(
  () => [props.open, props.equip?.id] as const,
  async ([open, equipId]) => {
    if (!open || !equipId) {
      return
    }
    await loadConfig()
  },
)
</script>

<template>
  <a-modal
    :open="open"
    :title="title"
    :width="isDgw ? 760 : 520"
    centered
    destroy-on-close
    class="equip-gateway-modal"
    :mask-closable="false"
    :confirm-loading="saving"
    @update:open="emit('update:open', $event)"
    @cancel="handleClose"
    @ok="handleSave"
  >
    <a-spin :spinning="loading">
      <a-alert
        class="gateway-hint"
        :type="hasModel ? 'info' : 'warning'"
        show-icon
        :message="productHint"
      />
      <div v-if="equip?.typeDesc || equip?.modelName" class="gateway-meta">
        <a-tag v-if="equip?.typeDesc" color="processing">
          <AppstoreOutlined />
          {{ equip.typeDesc }}
        </a-tag>
        <a-tag v-if="equip?.modelName" color="cyan">{{ equip.modelName }}</a-tag>
      </div>
      <GatewayPickerCard :gateway="selectedGateway" @select="onGatewaySelect" />

      <template v-if="isDgw">
        <a-alert
          v-if="!hasModel"
          class="gateway-hint"
          type="warning"
          show-icon
          :message="t('equipAttrPage.dgwNoModel')"
        />
        <template v-else>
          <a-alert class="gateway-hint" type="info" show-icon :message="t('equipAttrPage.dgwMapHint')" />

          <section class="dgw-section">
            <div class="dgw-section__title">{{ t('equipAttrPage.runMap') }}</div>
            <a-input
              v-model:value="runMap"
              allow-clear
              class="dgw-input"
              :placeholder="t('equipAttrPage.dgwPointPlaceholder')"
            />
          </section>

          <section v-if="namedAttrs.length || namedAlarms.length" class="dgw-section">
            <div class="dgw-section__title">{{ t('equipAttrPage.dgwRead') }}</div>
            <div v-for="(row, idx) in namedAttrs" :key="`attr-${idx}`" class="dgw-row">
              <span class="dgw-row__name" :title="row.name">{{ row.name }}</span>
              <a-input
                v-model:value="row.map"
                allow-clear
                :placeholder="t('equipAttrPage.dgwPointPlaceholder')"
              />
            </div>
            <div v-for="(row, idx) in namedAlarms" :key="`alarm-${idx}`" class="dgw-row">
              <span class="dgw-row__name" :title="row.name || row.text">{{ row.name || row.text }}</span>
              <a-input
                v-model:value="row.map"
                allow-clear
                :placeholder="t('equipAttrPage.dgwPointPlaceholder')"
              />
            </div>
          </section>

          <section v-if="namedControls.length" class="dgw-section">
            <div class="dgw-section__title">{{ t('equipAttrPage.dgwControl') }}</div>
            <div v-for="(row, idx) in namedControls" :key="`ctrl-${idx}`" class="dgw-row">
              <span class="dgw-row__name" :title="row.name">{{ row.name }}</span>
              <a-input
                v-model:value="row.map"
                allow-clear
                :placeholder="t('equipAttrPage.dgwPointPlaceholder')"
              />
            </div>
          </section>
        </template>
      </template>
    </a-spin>
  </a-modal>
</template>

<style scoped>
.equip-gateway-modal :deep(.ant-modal-content) {
  border-radius: 12px;
}

.equip-gateway-modal :deep(.ant-modal-header) {
  padding: 16px 24px;
}

.equip-gateway-modal :deep(.ant-modal-title) {
  font-size: 17px;
}

.equip-gateway-modal :deep(.ant-modal-body) {
  max-height: min(72vh, 680px);
  overflow-y: auto;
  padding: 16px 24px 8px;
}

.equip-gateway-modal :deep(.ant-modal-footer) {
  padding: 12px 24px 16px;
}

.equip-gateway-modal :deep(.ant-modal-footer .ant-btn) {
  min-width: 80px;
  height: 36px;
}

.gateway-hint {
  margin-bottom: 12px;
}

.gateway-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 12px;
}

.gateway-meta :deep(.ant-tag) {
  margin: 0;
  border-radius: 999px;
}

.dgw-section {
  margin-top: 12px;
  padding: 12px 14px;
  background: var(--omes-color-bg-container);
  border: 1px solid var(--omes-color-border-secondary);
  border-radius: var(--omes-radius-md);
}

.dgw-section__title {
  margin-bottom: 8px;
  font-size: 13px;
  font-weight: 600;
}

.dgw-row {
  display: grid;
  grid-template-columns: 140px 1fr;
  gap: 8px;
  align-items: center;
  margin-top: 8px;
}

.dgw-row__name {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.dgw-input {
  width: 100%;
}
</style>
