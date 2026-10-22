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
  isConfigurableMqtt,
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
  parseMode: 'flat' as 'flat' | 'grouped',
  snField: 'SN',
  timeField: 'time',
  codeField: 'code',
  groups: [] as { array: string; prefix: string }[],
})

const rawParams = ref('')

const showMqttFields = computed(() => isMqttProtocol(formState.protocol))
const showParseConfig = computed(() => isConfigurableMqtt(formState.protocol))
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

function paramsText(params?: GatewayRecord['params']): string {
  if (!params) {
    return ''
  }
  if (typeof params === 'string') {
    return params
  }
  try {
    return JSON.stringify(params)
  } catch {
    return ''
  }
}

function resetParse() {
  formState.parseMode = 'flat'
  formState.snField = 'SN'
  formState.timeField = 'time'
  formState.codeField = 'code'
  formState.groups = []
}

function applyParams(params?: GatewayRecord['params']) {
  rawParams.value = paramsText(params)
  let mode: 'flat' | 'grouped' = 'flat'
  let snField = 'SN'
  let timeField = 'time'
  let codeField = 'code'
  let groups: { array: string; prefix: string }[] = []
  if (rawParams.value) {
    try {
      const root = JSON.parse(rawParams.value) as { parse?: Record<string, unknown> }
      const parse = root?.parse
      if (parse && typeof parse === 'object') {
        mode = parse.mode === 'grouped' ? 'grouped' : 'flat'
        snField = String(parse.snField || 'SN')
        timeField = String(parse.timeField || 'time')
        codeField = String(parse.codeField || 'code')
        if (Array.isArray(parse.groups)) {
          groups = parse.groups
            .filter((item) => item && typeof item === 'object')
            .map((item) => {
              const group = item as { array?: unknown; prefix?: unknown }
              return {
                array: String(group.array || ''),
                prefix: String(group.prefix || ''),
              }
            })
        }
      }
    } catch {
      // 保留原始 params，表单按平铺显示
    }
  }
  formState.snField = snField
  formState.timeField = timeField
  formState.codeField = codeField
  formState.groups = groups
  formState.parseMode = mode
}

function buildParams(): string | undefined {
  if (!isConfigurableMqtt(formState.protocol)) {
    return rawParams.value || undefined
  }
  let root: Record<string, unknown> = {}
  if (rawParams.value) {
    try {
      const parsed = JSON.parse(rawParams.value) as unknown
      if (parsed && typeof parsed === 'object' && !Array.isArray(parsed)) {
        root = parsed as Record<string, unknown>
      }
    } catch {
      root = {}
    }
  }
  const parse: Record<string, unknown> = { mode: formState.parseMode }
  const snField = formState.snField.trim()
  const timeField = formState.timeField.trim()
  const codeField = formState.codeField.trim()
  if (snField && snField !== 'SN') {
    parse.snField = snField
  }
  if (timeField && timeField !== 'time') {
    parse.timeField = timeField
  }
  if (codeField && codeField !== 'code') {
    parse.codeField = codeField
  }
  const groups = formState.groups
    .map((group) => ({ array: group.array.trim(), prefix: group.prefix.trim() }))
    .filter((group) => group.array)
  if (groups.length) {
    parse.groups = groups
  }
  if (formState.parseMode === 'flat' && !groups.length && Object.keys(parse).length === 1) {
    delete root.parse
  } else {
    root.parse = parse
  }
  // 始终写回字符串。更新策略会跳过 null，分组配置切回平铺时否则清不掉。
  return JSON.stringify(root)
}

function addParseGroup() {
  formState.groups.push({ array: '', prefix: '' })
}

function removeParseGroup(index: number) {
  formState.groups.splice(index, 1)
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
    applyParams(data.params)
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
  rawParams.value = ''
  resetParse()
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
  () => formState.parseMode,
  (mode, prev) => {
    if (mode === 'grouped' && prev !== 'grouped' && formState.groups.length === 0) {
      formState.groups = [
        { array: 'dev', prefix: 'dev' },
        { array: 'ai', prefix: 'yw' },
      ]
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
  if (
    showParseConfig.value
    && formState.parseMode === 'grouped'
    && !formState.groups.some((group) => group.array.trim())
  ) {
    message.warning(t('gatewayPage.parseGroupRequired'))
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
    params: buildParams(),
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
          <template v-if="showParseConfig">
            <a-form-item :label="t('gatewayPage.parseMode')">
              <a-select
                v-model:value="formState.parseMode"
                :options="[
                  { value: 'flat', label: t('gatewayPage.parseFlat') },
                  { value: 'grouped', label: t('gatewayPage.parseGrouped') },
                ]"
              />
            </a-form-item>
            <div class="form-hint">
              {{ formState.parseMode === 'grouped' ? t('gatewayPage.parseGroupedHint') : t('gatewayPage.parseFlatHint') }}
            </div>
            <template v-if="formState.parseMode === 'grouped'">
              <a-row :gutter="16">
                <a-col :span="8">
                  <a-form-item :label="t('gatewayPage.snField')">
                    <a-input v-model:value="formState.snField" allow-clear placeholder="SN" />
                  </a-form-item>
                </a-col>
                <a-col :span="8">
                  <a-form-item :label="t('gatewayPage.timeField')">
                    <a-input v-model:value="formState.timeField" allow-clear placeholder="time" />
                  </a-form-item>
                </a-col>
                <a-col :span="8">
                  <a-form-item :label="t('gatewayPage.codeField')">
                    <a-input v-model:value="formState.codeField" allow-clear placeholder="code" />
                  </a-form-item>
                </a-col>
              </a-row>
              <div class="parse-groups-label">{{ t('gatewayPage.parseGroups') }}</div>
              <div v-for="(group, index) in formState.groups" :key="index" class="parse-group-row">
                <a-input
                  v-model:value="group.array"
                  allow-clear
                  :addon-before="t('gatewayPage.parseArray')"
                  :placeholder="t('gatewayPage.parseArrayPlaceholder')"
                />
                <a-input
                  v-model:value="group.prefix"
                  allow-clear
                  :addon-before="t('gatewayPage.parsePrefix')"
                  :placeholder="t('gatewayPage.parsePrefixPlaceholder')"
                />
                <a-button type="link" danger @click="removeParseGroup(index)">
                  {{ t('gatewayPage.delete') }}
                </a-button>
              </div>
              <a-button type="dashed" block @click="addParseGroup">
                {{ t('gatewayPage.addParseGroup') }}
              </a-button>
            </template>
          </template>
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
  margin-bottom: 12px;
  font-size: 12px;
  line-height: 1.5;
  color: var(--omes-color-text-tertiary);
}

.parse-groups-label {
  margin-bottom: 8px;
  font-size: 14px;
  font-weight: 500;
  color: var(--omes-color-text-label);
}

.parse-group-row {
  display: flex;
  gap: 8px;
  margin-bottom: 8px;
}

.parse-group-row > :deep(.ant-input-group-wrapper) {
  flex: 1;
  min-width: 0;
}

.parse-group-row :deep(.ant-btn) {
  flex: 0 0 auto;
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
