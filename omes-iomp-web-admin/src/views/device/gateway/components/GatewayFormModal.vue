<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import {
  CloudOutlined,
  GatewayOutlined,
  LinkOutlined,
  LockOutlined,
  SettingOutlined,
} from '@ant-design/icons-vue'
import {
  fetchGatewayById,
  fetchGatewayProtocols,
  isDgwProtocol,
  isMqttProtocol,
  needsGatewayAuth,
  saveGateway,
  type GatewayRecord,
  type ProtocolOption,
} from '@/api/gateway'
import { message } from 'ant-design-vue'

const props = defineProps<{
  open: boolean
  record: GatewayRecord | null
}>()

const emit = defineEmits<{
  'update:open': [value: boolean]
  success: []
}>()

const { t } = useI18n()

const loading = ref(false)
const saving = ref(false)
const protocolOptions = ref<ProtocolOption[]>([])

const isEdit = computed(() => Boolean(props.record?.id))
const title = computed(() => (isEdit.value ? t('gatewayPage.formEdit') : t('gatewayPage.formAdd')))

const formState = reactive({
  serverName: '',
  protocol: undefined as string | undefined,
  uri: '',
  topic: '',
  username: '',
  password: '',
  collectCron: '',
})

const showMqttFields = computed(() => isMqttProtocol(formState.protocol))
const showDgwFields = computed(() => isDgwProtocol(formState.protocol))
const showAuthFields = computed(() => needsGatewayAuth(formState.protocol))
const topicRequired = computed(() => showMqttFields.value)
const uriPlaceholder = computed(() =>
  showDgwFields.value ? t('gatewayPage.dgwUriPlaceholder') : t('gatewayPage.uriPlaceholder'),
)

const protocolLabel = computed(() => {
  const hit = protocolOptions.value.find((p) => p.id === formState.protocol)
  return hit?.name || formState.protocol || '-'
})

async function loadProtocols() {
  const list = await fetchGatewayProtocols()
  protocolOptions.value = Array.isArray(list) ? list : []
}

async function loadDetail(id: string) {
  loading.value = true
  try {
    const data = await fetchGatewayById(id)
    formState.serverName = data.serverName || ''
    formState.protocol = data.protocol
    formState.uri = data.uri || ''
    formState.topic = data.topic || ''
    formState.username = data.username || ''
    formState.password = data.password || ''
    formState.collectCron = data.collectCron || ''
  } finally {
    loading.value = false
  }
}

function resetForm() {
  formState.serverName = ''
  formState.protocol = protocolOptions.value[0]?.id
  formState.uri = ''
  formState.topic = ''
  formState.username = ''
  formState.password = ''
  formState.collectCron = ''
}

function closeModal() {
  emit('update:open', false)
}

watch(
  () => formState.protocol,
  (protocol) => {
    if (isDgwProtocol(protocol) && !formState.collectCron.trim()) {
      formState.collectCron = '0/5 * * * * ?'
    }
  },
)

watch(
  () => props.open,
  async (open) => {
    if (!open) {
      return
    }
    await loadProtocols()
    if (props.record?.id) {
      await loadDetail(props.record.id)
    } else {
      resetForm()
    }
  },
)

async function handleSubmit() {
  if (!formState.serverName?.trim()) {
    message.warning(t('gatewayPage.serverNameRequired'))
    return
  }
  if (!formState.protocol) {
    message.warning(t('gatewayPage.protocolRequired'))
    return
  }
  if (!formState.uri?.trim()) {
    message.warning(t('gatewayPage.uriRequired'))
    return
  }
  if (topicRequired.value && !formState.topic?.trim()) {
    message.warning(t('gatewayPage.topicRequired'))
    return
  }
  if (showDgwFields.value && (!formState.username?.trim() || !formState.password)) {
    message.warning(t('gatewayPage.dgwAuthRequired'))
    return
  }
  if (showDgwFields.value && !formState.collectCron?.trim()) {
    message.warning(t('gatewayPage.collectCronRequired'))
    return
  }

  const payload: GatewayRecord = {
    id: props.record?.id,
    serverName: formState.serverName.trim(),
    protocol: formState.protocol,
    uri: formState.uri.trim(),
    topic: formState.topic?.trim() || undefined,
    username: formState.username?.trim() || undefined,
    password: formState.password || undefined,
    collectCron: formState.collectCron?.trim() || undefined,
  }

  saving.value = true
  try {
    await saveGateway(payload)
    message.success(t('gatewayPage.saveSuccess'))
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
    :title="title"
    width="720px"
    destroy-on-close
    class="gateway-form-modal"
    :footer="null"
    @cancel="closeModal"
    @update:open="emit('update:open', $event)"
  >
    <a-spin :spinning="loading">
      <a-form layout="vertical" class="gateway-form">
        <div class="form-section">
          <div class="section-title">
            <GatewayOutlined />
            {{ t('gatewayPage.formBasic') }}
          </div>
          <a-row :gutter="16">
            <a-col :span="12">
              <a-form-item :label="t('gatewayPage.serverName')" required>
                <a-input
                  v-model:value="formState.serverName"
                  allow-clear
                  :placeholder="t('gatewayPage.serverName')"
                />
              </a-form-item>
            </a-col>
            <a-col :span="12">
              <a-form-item :label="t('gatewayPage.protocol')" required>
                <a-select
                  v-model:value="formState.protocol"
                  :placeholder="t('gatewayPage.protocol')"
                  :options="protocolOptions.map((p) => ({ value: p.id, label: p.name }))"
                />
              </a-form-item>
            </a-col>
          </a-row>
          <a-form-item :label="t('gatewayPage.uri')" required>
            <a-input
              v-model:value="formState.uri"
              allow-clear
              :placeholder="uriPlaceholder"
            >
              <template #prefix>
                <LinkOutlined class="input-prefix-icon" />
              </template>
            </a-input>
          </a-form-item>
          <div v-if="formState.protocol" class="protocol-badge">
            {{ t('gatewayPage.currentProtocol', { name: protocolLabel }) }}
          </div>
        </div>

        <div v-if="showAuthFields" class="form-section">
          <div class="section-title">
            <LockOutlined />
            {{ t('gatewayPage.formAuth') }}
          </div>
          <a-row :gutter="16">
            <a-col :span="12">
              <a-form-item :label="t('gatewayPage.username')">
                <a-input
                  v-model:value="formState.username"
                  allow-clear
                  autocomplete="off"
                  :placeholder="t('gatewayPage.username')"
                />
              </a-form-item>
            </a-col>
            <a-col :span="12">
              <a-form-item :label="t('gatewayPage.password')">
                <a-input-password
                  v-model:value="formState.password"
                  autocomplete="new-password"
                  :placeholder="t('gatewayPage.password')"
                />
              </a-form-item>
            </a-col>
          </a-row>
        </div>

        <div v-if="showDgwFields" class="form-section">
          <div class="section-title">
            <SettingOutlined />
            {{ t('gatewayPage.formDgw') }}
          </div>
          <a-form-item :label="t('gatewayPage.collectCron')" required>
            <a-input
              v-model:value="formState.collectCron"
              allow-clear
              :placeholder="t('gatewayPage.collectCronPlaceholder')"
            />
          </a-form-item>
          <div class="form-hint">{{ t('gatewayPage.dgwHint') }}</div>
        </div>

        <div v-if="showMqttFields" class="form-section form-section--mqtt">
          <div class="section-title">
            <CloudOutlined />
            {{ t('gatewayPage.formMqtt') }}
          </div>
          <a-form-item :label="t('gatewayPage.topic')" :required="topicRequired">
            <a-input
              v-model:value="formState.topic"
              allow-clear
              :placeholder="t('gatewayPage.topicPlaceholder')"
            />
          </a-form-item>
        </div>

        <div v-if="!formState.protocol" class="form-hint-empty">
          <SettingOutlined />
          <span>{{ t('gatewayPage.selectProtocolHint') }}</span>
        </div>
      </a-form>
    </a-spin>

    <div class="modal-footer">
      <a-button @click="closeModal">{{ t('gatewayPage.cancel') }}</a-button>
      <a-button type="primary" :loading="saving" @click="handleSubmit">
        {{ t('gatewayPage.save') }}
      </a-button>
    </div>
  </a-modal>
</template>

<style scoped>
.gateway-form-modal :deep(.ant-modal-body) {
  padding: 16px 24px 0;
  max-height: min(72vh, 640px);
  overflow-y: auto;
}

.gateway-form {
  padding-bottom: 8px;
}

.form-section {
  padding: 16px 18px;
  background: var(--omes-color-bg-elevated);
  border: 1px solid var(--omes-color-border);
  border-radius: var(--omes-radius-lg);
}

.form-section + .form-section {
  margin-top: 14px;
}

.form-section--mqtt {
  background: linear-gradient(180deg, #f6ffed 0%, var(--omes-color-bg-elevated) 40%);
  border-color: #d9f7be;
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

.gateway-form :deep(.ant-form-item) {
  margin-bottom: 16px;
}

.gateway-form :deep(.ant-form-item:last-child) {
  margin-bottom: 0;
}

.gateway-form :deep(.ant-form-item-label > label) {
  font-weight: 500;
  color: var(--omes-color-text-label);
}

.input-prefix-icon {
  color: var(--omes-color-text-placeholder);
}

.form-hint {
  margin-top: 10px;
  font-size: 12px;
  line-height: 1.5;
  color: var(--omes-color-text-tertiary);
}

.protocol-badge {
  margin-top: 4px;
  padding: 8px 12px;
  font-size: 12px;
  color: var(--omes-color-primary);
  background: var(--omes-color-primary-bg);
  border-radius: var(--omes-radius-sm);
}

.form-hint-empty {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  margin-top: 14px;
  padding: 20px;
  font-size: 13px;
  color: var(--omes-color-text-quaternary);
  background: var(--omes-color-bg-elevated);
  border: 1px dashed #d9d9d9;
  border-radius: var(--omes-radius-lg);
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
