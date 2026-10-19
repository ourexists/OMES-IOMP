<script setup lang="ts">
import CompactSearchActions from '@/components/admin/CompactSearchActions.vue'
import AdminPanelTitle from '@/components/admin/AdminPanelTitle.vue'
import { computed, onMounted, onUnmounted, reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import {
  ApiOutlined,
  DeleteOutlined,
  EditOutlined,
  GatewayOutlined,
  InboxOutlined,
  LinkOutlined,
  PauseCircleOutlined,
  PlayCircleOutlined,
  PlusOutlined,
  ReloadOutlined } from '@ant-design/icons-vue'
import {
  deleteGateways,
  fetchGatewayList,
  fetchGatewayProtocols,
  startGateway,
  stopGateway,
  type GatewayRecord,
  type ProtocolOption,
} from '@/api/gateway'
import GatewayBindingModal from './components/GatewayBindingModal.vue'
import GatewayFormModal from './components/GatewayFormModal.vue'
import { useThemeStore } from '@/stores/theme'
import { message, Modal } from 'ant-design-vue'

const { t } = useI18n()
const themeStore = useThemeStore()

// gatewayPage.subtitle: 管理 MQTT、REST 等协议连接，启停后由 Admin 运行时拉起采集；系统管理端说明文案不向普通用户展示。

const loading = ref(false)
const togglingId = ref('')
const togglePercent = ref(0)
let toggleTimer: ReturnType<typeof setInterval> | undefined
let linkTimer: ReturnType<typeof setInterval> | undefined
const gateways = ref<GatewayRecord[]>([])
const protocolOptions = ref<ProtocolOption[]>([])

const searchForm = reactive({
  protocol: undefined as string | undefined,
  serverName: '',
})

const formOpen = ref(false)
const editingRecord = ref<GatewayRecord | null>(null)

const bindingOpen = ref(false)
const bindingGateway = ref<GatewayRecord | null>(null)

const PROTOCOL_COLORS = computed(() => [...themeStore.chartPalette])

function protocolColor(protocol?: string): string {
  const palette = PROTOCOL_COLORS.value
  if (!protocol) {
    return palette[0]
  }
  let hash = 0
  for (let i = 0; i < protocol.length; i += 1) {
    hash = protocol.charCodeAt(i) + hash * 31
  }
  return palette[Math.abs(hash) % palette.length]
}

const emptyText = computed(() => t('gatewayPage.empty'))
const enabledCount = computed(() => gateways.value.filter((g) => g.enabled).length)
const disabledCount = computed(() => gateways.value.length - enabledCount.value)

function linkLabel(state?: string) {
  if (state === 'up') {
    return t('gatewayPage.linkUp')
  }
  if (state === 'down') {
    return t('gatewayPage.linkDown')
  }
  return t('gatewayPage.linkPending')
}

function linkColor(state?: string) {
  if (state === 'up') {
    return 'success'
  }
  if (state === 'down') {
    return 'error'
  }
  return 'processing'
}

function linkTip(item: GatewayRecord) {
  if (item.linkState === 'up') {
    return t('gatewayPage.linkUp')
  }
  if (item.linkState === 'down') {
    return item.linkMessage || t('gatewayPage.linkDown')
  }
  return t('gatewayPage.linkPendingTip')
}

async function loadProtocols() {
  try {
    const list = await fetchGatewayProtocols()
    protocolOptions.value = Array.isArray(list) ? list : []
  } catch {
    protocolOptions.value = []
  }
}

async function loadCards(silent = false) {
  if (!silent) {
    loading.value = true
  }
  try {
    const list = await fetchGatewayList({
      protocol: searchForm.protocol,
      serverName: searchForm.serverName?.trim() || undefined,
    })
    gateways.value = Array.isArray(list) ? list : []
  } catch {
    // 列表请求失败时保留当前卡片，避免启停后的刷新把页面清空
  } finally {
    if (!silent) {
      loading.value = false
    }
  }
}

function onSearch() {
  loadCards()
}

function onReset() {
  searchForm.protocol = undefined
  searchForm.serverName = ''
  loadCards()
}

function openCreate() {
  editingRecord.value = null
  formOpen.value = true
}

function openEdit(record: GatewayRecord) {
  if (record.enabled) {
    message.warning(t('gatewayPage.disableFirst'))
    return
  }
  editingRecord.value = record
  formOpen.value = true
}

function openBinding(record: GatewayRecord) {
  bindingGateway.value = record
  bindingOpen.value = true
}

function applyEnabled(id: string, enabled: boolean) {
  const hit = gateways.value.find((item) => item.id === id)
  if (hit) {
    hit.enabled = enabled
  }
}

function beginToggleWait(id: string) {
  // shortcut: 接口不回报进度，条走到 90% 停住；有真实进度再换成服务端百分比
  togglingId.value = id
  togglePercent.value = 12
  clearInterval(toggleTimer)
  toggleTimer = setInterval(() => {
    if (togglePercent.value < 90) {
      togglePercent.value += 6
    }
  }, 300)
}

function endToggleWait() {
  clearInterval(toggleTimer)
  toggleTimer = undefined
  togglingId.value = ''
  togglePercent.value = 0
}

async function toggleEnabled(record: GatewayRecord) {
  if (!record.id || togglingId.value) {
    return
  }
  const id = record.id
  const next = !record.enabled
  beginToggleWait(id)
  let ok = false
  try {
    if (next) {
      await startGateway(id)
    } else {
      await stopGateway(id)
    }
    ok = true
  } catch {
    // 失败提示由请求拦截器弹出
  }
  try {
    await loadCards(true)
  } finally {
    endToggleWait()
  }
  if (!ok) {
    return
  }
  applyEnabled(id, next)
  const current = gateways.value.find((item) => item.id === id)
  if (next && current?.linkState === 'down') {
    message.warning(current.linkMessage || t('gatewayPage.linkDown'))
    return
  }
  message.success(next ? t('gatewayPage.startSuccess') : t('gatewayPage.stopSuccess'))
}

function confirmDelete(record: GatewayRecord) {
  if (record.enabled) {
    message.warning(t('gatewayPage.disableFirst'))
    return
  }
  if (!record.id) {
    return
  }
  Modal.confirm({
    title: t('gatewayPage.deleteConfirm'),
    content: t('gatewayPage.deleteContent', { name: record.serverName || record.id }),
    onOk: async () => {
      await deleteGateways([record.id!])
      message.success(t('gatewayPage.deleteSuccess'))
      loadCards()
    },
  })
}

onMounted(async () => {
  await loadProtocols()
  await loadCards()
  linkTimer = setInterval(() => {
    if (!togglingId.value && gateways.value.some((item) => item.enabled)) {
      loadCards(true)
    }
  }, 5000)
})

onUnmounted(() => {
  endToggleWait()
  clearInterval(linkTimer)
})
</script>

<template>
  <div class="admin-page admin-page--auto gateway-page">
    <a-card size="small" class="admin-panel-card panel-card">
      <template #title>
        <AdminPanelTitle>
          <template #icon><GatewayOutlined /></template>
          {{ t('gatewayPage.title') }}
        </AdminPanelTitle>
      </template>
      <template #extra>
        <a-space size="small" wrap>
          <a-tag v-if="gateways.length" color="processing">
            {{ t('gatewayPage.total', { count: gateways.length }) }}
          </a-tag>
          <a-tag v-if="enabledCount" color="success">
            {{ t('gatewayPage.enabledCount', { count: enabledCount }) }}
          </a-tag>
        </a-space>
      </template>

      <div class="admin-panel-body">
        <div v-if="gateways.length" class="stats-strip">
          <div class="stat-card">
            <span class="stat-card__value">{{ gateways.length }}</span>
            <span class="stat-card__label">{{ t('gatewayPage.all') }}</span>
          </div>
          <div class="stat-card stat-card--success">
            <span class="stat-card__value">{{ enabledCount }}</span>
            <span class="stat-card__label">{{ t('gatewayPage.enabled') }}</span>
          </div>
          <div class="stat-card stat-card--muted">
            <span class="stat-card__value">{{ disabledCount }}</span>
            <span class="stat-card__label">{{ t('gatewayPage.disabled') }}</span>
          </div>
        </div>

        <div class="search-toolbar search-toolbar--compact">
          <a-form layout="inline" class="search-form" :model="searchForm" @finish="onSearch">
            <a-form-item :label="t('gatewayPage.protocol')" name="protocol">
              <a-select size="small"
                v-model:value="searchForm.protocol"
                allow-clear
                :placeholder="t('gatewayPage.all')"
                class="search-select"
                :options="protocolOptions.map((p) => ({ value: p.id, label: p.name }))"
              />
            </a-form-item>
            <a-form-item :label="t('gatewayPage.serverName')" name="serverName">
              <a-input size="small"
                v-model:value="searchForm.serverName"
                allow-clear
                class="search-input"
                :placeholder="t('gatewayPage.serverName')"
              />
            </a-form-item>
            <CompactSearchActions
              :query-title="t('gatewayPage.query')"
              :reset-title="t('gatewayPage.reset')"
              @reset="onReset"
            />
          </a-form>
        </div>

        <div class="table-toolbar">
          <a-space wrap>
            <a-button type="primary" @click="openCreate">
              <template #icon><PlusOutlined /></template>
              {{ t('gatewayPage.add') }}
            </a-button>
            <a-button :loading="loading" @click="loadCards">
              <template #icon><ReloadOutlined /></template>
              {{ t('gatewayPage.refresh') }}
            </a-button>
          </a-space>
        </div>

        <a-spin :spinning="loading" class="grid-spin">
          <div v-if="!gateways.length && !loading" class="grid-empty">
            <InboxOutlined class="grid-empty__icon" />
            <p class="grid-empty__title">{{ emptyText }}</p>
            <a-button type="primary" @click="openCreate">
              <PlusOutlined />
              {{ t('gatewayPage.add') }}
            </a-button>
          </div>
          <div v-else class="gateway-grid">
            <article
              v-for="item in gateways"
              :key="item.id"
              class="gw-card"
              :class="{
                'gw-card--on': item.enabled && item.linkState === 'up',
                'gw-card--down': item.enabled && item.linkState === 'down',
              }"
            >
              <a-progress
                v-if="togglingId === item.id"
                class="gw-card__wait"
                :percent="togglePercent"
                :show-info="false"
                status="active"
                :stroke-width="3"
              />
              <div v-else class="gw-card__accent" :style="{ background: protocolColor(item.protocol) }" />

              <header class="gw-card__head">
                <div
                  class="gw-card__icon"
                  :style="{
                    color: protocolColor(item.protocol),
                    borderColor: protocolColor(item.protocol),
                  }"
                >
                  <GatewayOutlined />
                </div>
                <div class="gw-card__title-wrap">
                  <div class="gw-card__title-row">
                    <h3 class="gw-card__name" :title="item.serverName">{{ item.serverName || '—' }}</h3>
                    <a-tag
                      class="gw-card__status-tag"
                      :color="item.enabled ? 'success' : 'default'"
                    >
                      {{ item.enabled ? t('gatewayPage.enabled') : t('gatewayPage.disabled') }}
                    </a-tag>
                  </div>
                  <div class="gw-card__meta">
                    <span
                      class="gw-card__protocol"
                      :style="{ color: protocolColor(item.protocol), borderColor: protocolColor(item.protocol) }"
                    >
                      {{ item.protocol || '—' }}
                    </span>
                    <a-tooltip v-if="item.enabled" :title="linkTip(item)">
                      <a-tag class="gw-card__status-tag" :color="linkColor(item.linkState)">
                        {{ linkLabel(item.linkState) }}
                      </a-tag>
                    </a-tooltip>
                  </div>
                </div>
              </header>

              <div class="gw-card__body">
                <div class="gw-card__uri">
                  <ApiOutlined class="uri-icon" />
                  <div class="uri-content">
                    <span class="uri-label">{{ t('gatewayPage.uri') }}</span>
                    <span class="uri-value" :title="item.uri">{{ item.uri || '—' }}</span>
                  </div>
                </div>
                <p
                  v-if="item.enabled && item.linkState === 'down' && item.linkMessage"
                  class="gw-card__link-msg"
                  :title="item.linkMessage"
                >
                  {{ item.linkMessage }}
                </p>
              </div>

              <footer class="gw-card__foot">
                <a-tooltip :title="item.enabled ? t('gatewayPage.stop') : t('gatewayPage.start')">
                  <a-button
                    type="text"
                    size="small"
                    class="action-btn"
                    :class="{ 'action-btn--active': item.enabled }"
                    :loading="togglingId === item.id"
                    :disabled="!!togglingId && togglingId !== item.id"
                    @click="toggleEnabled(item)"
                  >
                    <PauseCircleOutlined v-if="item.enabled" />
                    <PlayCircleOutlined v-else />
                    <span class="action-btn__text">{{ item.enabled ? t('gatewayPage.stop') : t('gatewayPage.start') }}</span>
                  </a-button>
                </a-tooltip>
                <a-tooltip :title="item.enabled ? t('gatewayPage.disableFirst') : t('gatewayPage.edit')">
                  <span class="action-btn-wrap">
                    <a-button type="text" size="small" class="action-btn" :disabled="item.enabled" @click="openEdit(item)">
                      <EditOutlined />
                      <span class="action-btn__text">{{ t('gatewayPage.edit') }}</span>
                    </a-button>
                  </span>
                </a-tooltip>
                <a-tooltip :title="t('gatewayPage.binding')">
                  <a-button type="text" size="small" class="action-btn" @click="openBinding(item)">
                    <LinkOutlined />
                    <span class="action-btn__text">{{ t('gatewayPage.binding') }}</span>
                  </a-button>
                </a-tooltip>
                <a-tooltip :title="item.enabled ? t('gatewayPage.disableFirst') : t('gatewayPage.delete')">
                  <span class="action-btn-wrap">
                    <a-button
                      type="text"
                      size="small"
                      danger
                      class="action-btn"
                      :disabled="item.enabled"
                      @click="confirmDelete(item)"
                    >
                      <DeleteOutlined />
                      <span class="action-btn__text">{{ t('gatewayPage.delete') }}</span>
                    </a-button>
                  </span>
                </a-tooltip>
              </footer>
            </article>
          </div>
        </a-spin>
      </div>
    </a-card>

    <GatewayFormModal v-model:open="formOpen" :record="editingRecord" @success="loadCards" />
    <GatewayBindingModal v-model:open="bindingOpen" :gateway="bindingGateway" />
  </div>
</template>

<style scoped>
.gateway-page {
  /* layout via .admin-page--auto */
}

.panel-card {
  border-radius: var(--omes-radius-md);
  box-shadow: var(--omes-shadow-card-sm);
}

.panel-card :deep(.ant-card-head) {
  min-height: 48px;
  border-bottom: 1px solid var(--omes-color-border);
  background: var(--omes-gradient-card-head);
}

.card-title {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  font-weight: 600;
}

.card-title :deep(.anticon) {
  color: var(--omes-color-primary);
}

.stats-strip {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-bottom: 14px;
}

.stat-card {
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 120px;
  padding: 10px 16px;
  background: var(--omes-color-bg-elevated);
  border: 1px solid var(--omes-color-border);
  border-radius: var(--omes-radius-lg);
}

.stat-card--success {
  background: linear-gradient(135deg, #f6ffed 0%, var(--omes-color-bg-elevated) 100%);
  border-color: #d9f7be;
}

.stat-card--success .stat-card__value {
  color: var(--omes-color-success);
}

.stat-card--muted .stat-card__value {
  color: var(--omes-color-text-secondary);
}

.stat-card__value {
  font-size: 22px;
  font-weight: 700;
  line-height: 1.2;
  color: var(--omes-color-primary);
}

.stat-card__label {
  font-size: 12px;
  color: var(--omes-color-text-quaternary);
}

.search-toolbar {
  margin-bottom: 12px;
  padding: 12px 16px;
  background: var(--omes-gradient-toolbar);
  border: 1px solid var(--omes-color-border);
  border-radius: var(--omes-radius-md);
}

.search-form {
  margin-bottom: 0;
}

.search-select {
  width: 168px;
}

.search-input {
  width: 200px;
}

.table-toolbar {
  margin-bottom: 16px;
}

.grid-spin {
  min-height: 200px;
}

.grid-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 56px 16px;
  text-align: center;
  border: 1px dashed var(--omes-color-border-secondary);
  border-radius: var(--omes-radius-lg);
  background: var(--omes-color-bg-elevated);
}

.grid-empty__icon {
  font-size: 52px;
  color: var(--omes-color-text-placeholder);
}

.grid-empty__title {
  margin: 4px 0 8px;
  font-size: 15px;
  font-weight: 500;
  color: var(--omes-color-text-secondary);
}

.gateway-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 16px;
}

.gw-card {
  position: relative;
  display: flex;
  flex-direction: column;
  min-height: 196px;
  padding: 0;
  background: var(--omes-color-bg-container);
  border: 1px solid var(--omes-color-border);
  border-radius: var(--omes-radius-lg);
  overflow: hidden;
  transition:
    box-shadow 0.2s ease,
    border-color 0.2s ease,
    transform 0.2s ease;
}

.gw-card:hover {
  border-color: var(--omes-color-primary-border);
  box-shadow: 0 6px 16px rgba(22, 119, 255, 0.1);
  transform: translateY(-1px);
}

.gw-card--on {
  background: linear-gradient(180deg, #f6ffed 0%, var(--omes-color-bg-container) 52%);
  border-color: #b7eb8f;
}

.gw-card--down {
  background: color-mix(in srgb, var(--omes-color-error) 8%, var(--omes-color-bg-container));
  border-color: color-mix(in srgb, var(--omes-color-error) 45%, var(--omes-color-border));
}

.gw-card__accent {
  height: 3px;
  flex-shrink: 0;
}

.gw-card__wait {
  margin: 0;
  line-height: 0;
}

.gw-card__wait :deep(.ant-progress-outer),
.gw-card__wait :deep(.ant-progress-inner) {
  display: block;
}

.gw-card__head {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 14px 16px 10px;
}

.gw-card__icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 44px;
  height: 44px;
  flex-shrink: 0;
  font-size: 22px;
  border: 1px solid;
  border-radius: var(--omes-radius-lg);
  background: var(--omes-color-primary-bg-hover);
}

.gw-card--on .gw-card__icon {
  background: #f6ffed;
}

.gw-card__title-wrap {
  min-width: 0;
  flex: 1;
}

.gw-card__title-row {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 6px;
}

.gw-card__name {
  margin: 0;
  flex: 1;
  min-width: 0;
  font-size: 16px;
  font-weight: 600;
  line-height: 1.4;
  color: var(--omes-color-text);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.gw-card__status-tag {
  flex-shrink: 0;
  margin: 0;
  line-height: 20px;
  font-size: 12px;
}

.gw-card__meta {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 6px;
}

.gw-card__protocol {
  display: inline-block;
  padding: 0 8px;
  font-size: 12px;
  font-weight: 600;
  line-height: 22px;
  border: 1px solid;
  border-radius: 999px;
  background: var(--omes-color-bg-container);
}

.gw-card__body {
  flex: 1;
  padding: 0 16px 12px;
}

.gw-card__uri {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  padding: 10px 12px;
  background: var(--omes-color-bg-elevated);
  border: 1px solid var(--omes-color-border-secondary);
  border-radius: var(--omes-radius-md);
}

.uri-icon {
  flex-shrink: 0;
  margin-top: 2px;
  font-size: 15px;
  color: var(--omes-color-text-placeholder);
}

.uri-content {
  min-width: 0;
  flex: 1;
}

.uri-label {
  display: block;
  margin-bottom: 2px;
  font-size: 12px;
  color: var(--omes-color-text-quaternary);
}

.gw-card__link-msg {
  margin: 8px 0 0;
  font-size: 12px;
  line-height: 1.4;
  color: var(--omes-color-error);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.uri-value {
  display: block;
  font-size: 13px;
  font-weight: 500;
  font-family: ui-monospace, SFMono-Regular, Menlo, Consolas, monospace;
  color: var(--omes-color-text-label);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.gw-card__foot {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 0;
  margin-top: auto;
  border-top: 1px solid var(--omes-color-border);
  background: var(--omes-color-bg-elevated);
}

.gw-card--on .gw-card__foot {
  background: rgba(246, 255, 237, 0.55);
}

.action-btn-wrap {
  display: flex;
  min-width: 0;
}

.action-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 4px;
  width: 100%;
  height: 38px;
  padding-inline: 4px;
  font-size: 13px;
  border-radius: 0;
}

.action-btn--active {
  color: var(--omes-color-success);
}

.action-btn:not(:disabled):hover {
  background: var(--omes-color-primary-bg-hover);
}

@media (max-width: 768px) {
  .search-select,
  .search-input {
    width: 100%;
  }

  .stats-strip {
    display: grid;
    grid-template-columns: repeat(3, 1fr);
  }

  .stat-card {
    min-width: 0;
  }

  .gateway-grid {
    grid-template-columns: 1fr;
  }

  .gw-card__foot .action-btn__text {
    display: none;
  }
}
</style>
