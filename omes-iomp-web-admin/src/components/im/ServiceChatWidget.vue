<script setup lang="ts">
import { nextTick, onUnmounted, ref } from 'vue'
import { CloseOutlined, CustomerServiceOutlined } from '@ant-design/icons-vue'
import { useI18n } from 'vue-i18n'
import dayjs from 'dayjs'
import { closeImSession, fetchImMessages, openImSession, sendImMessage, type ImMessage } from '@/api/im'

// shortcut: 3s polling instead of the wasm sdk, move to a subdomain sdk when COOP/COEP is acceptable
const POLL_MS = 3000

const { t } = useI18n()
const open = ref(false)
const loading = ref(false)
const sending = ref(false)
const failed = ref(false)
const draft = ref('')
const messages = ref<ImMessage[]>([])
const listRef = ref<HTMLElement | null>(null)
let timer = 0
let ticket = 0
let held = false

function formatTime(value: number) {
  if (!value) {
    return ''
  }
  return dayjs(value).format('MM-DD HH:mm')
}

async function scrollDown() {
  await nextTick()
  const el = listRef.value
  if (el) {
    el.scrollTop = el.scrollHeight
  }
}

function stop() {
  if (timer) {
    window.clearInterval(timer)
    timer = 0
  }
}

async function release() {
  ticket += 1
  stop()
  if (!held) {
    return
  }
  held = false
  try {
    await closeImSession()
  } catch {
    // window is already closed
  }
}

async function refresh() {
  messages.value = (await fetchImMessages()) || []
  await scrollDown()
}

async function toggle() {
  if (open.value) {
    open.value = false
    await release()
    return
  }
  const mine = ++ticket
  open.value = true
  loading.value = true
  failed.value = false
  try {
    await openImSession()
    if (mine !== ticket) {
      await closeImSession().catch(() => {})
      return
    }
    held = true
    await refresh()
    if (mine !== ticket) {
      return
    }
    stop()
    timer = window.setInterval(() => {
      refresh().catch(() => {
        failed.value = true
        stop()
      })
    }, POLL_MS)
  } catch {
    if (mine === ticket) {
      failed.value = true
    }
  } finally {
    if (mine === ticket) {
      loading.value = false
    }
  }
}

async function onSend() {
  const text = draft.value.trim()
  if (!text || sending.value) {
    return
  }
  sending.value = true
  try {
    await sendImMessage(text)
    draft.value = ''
    await refresh()
  } catch {
    failed.value = true
  } finally {
    sending.value = false
  }
}

function onKeydown(event: KeyboardEvent) {
  if (event.key === 'Enter' && !event.shiftKey) {
    event.preventDefault()
    void onSend()
  }
}

onUnmounted(() => {
  void release()
})
</script>

<template>
  <div class="service-chat" :class="{ 'service-chat--open': open }">
    <section v-if="open" class="service-chat__panel">
      <header class="service-chat__head">
        <span class="service-chat__badge" aria-hidden="true">
          <CustomerServiceOutlined />
        </span>
        <strong class="service-chat__title">{{ t('imChat.title') }}</strong>
        <button type="button" class="service-chat__close" :aria-label="t('imChat.close')" @click="toggle">
          <CloseOutlined />
        </button>
      </header>
      <div ref="listRef" class="service-chat__list">
        <p v-if="failed && messages.length" class="service-chat__hint">{{ t('imChat.unavailable') }}</p>
        <div v-else-if="!loading && messages.length === 0" class="service-chat__empty">
          <CustomerServiceOutlined />
          <p>{{ failed ? t('imChat.unavailable') : t('imChat.empty') }}</p>
        </div>
        <div
          v-for="(item, index) in messages"
          :key="`${item.sendTime}-${index}`"
          class="service-chat__row"
          :class="{ 'service-chat__row--mine': !item.fromService }"
        >
          <div class="service-chat__bubble">
            <div class="service-chat__meta">
              <span>{{ item.fromService ? t('imChat.peer') : item.senderName }}</span>
              <span>{{ formatTime(item.sendTime) }}</span>
            </div>
            <div class="service-chat__text">{{ item.text }}</div>
          </div>
        </div>
      </div>
      <footer class="service-chat__composer">
        <a-textarea
          v-model:value="draft"
          :rows="2"
          :maxlength="2000"
          :placeholder="t('imChat.placeholder')"
          @keydown="onKeydown"
        />
        <a-button type="primary" :loading="sending" @click="onSend">{{ t('imChat.send') }}</a-button>
      </footer>
    </section>
    <button v-show="!open" type="button" class="service-chat__launcher" @click="toggle">
      <CustomerServiceOutlined />
      <span>{{ t('imChat.title') }}</span>
    </button>
  </div>
</template>

<style scoped>
.service-chat {
  position: fixed;
  right: 20px;
  /* 收起时抬高，避开列表底部分页 */
  bottom: 108px;
  z-index: 30;
  display: flex;
  flex-direction: column;
  align-items: flex-end;
}

.service-chat--open {
  bottom: 20px;
}

.service-chat__panel {
  width: min(360px, calc(100vw - 32px));
  height: min(520px, calc(100vh - 96px));
  display: flex;
  flex-direction: column;
  background: var(--omes-color-bg-container);
  border: 1px solid var(--omes-color-border-secondary);
  border-radius: 12px;
  box-shadow: var(--omes-shadow-card), 0 12px 28px rgba(0, 0, 0, 0.12);
  overflow: hidden;
}

.service-chat__head {
  display: flex;
  align-items: center;
  gap: 10px;
  min-height: 52px;
  padding: 10px 12px 10px 14px;
  background: var(--omes-gradient-card-head);
  border-bottom: 1px solid var(--omes-color-border-secondary);
}

.service-chat__badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  flex-shrink: 0;
  border-radius: 8px;
  background: var(--omes-gradient-primary);
  color: var(--omes-color-white);
  font-size: 15px;
  box-shadow: var(--omes-shadow-primary-icon);
}

.service-chat__title {
  flex: 1;
  min-width: 0;
  font-size: 15px;
  font-weight: var(--omes-font-weight-semibold);
  color: var(--omes-color-text-heading);
}

.service-chat__close {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  padding: 0;
  border: 0;
  border-radius: var(--omes-radius-sm);
  background: transparent;
  color: var(--omes-color-text-tertiary);
  cursor: pointer;
}

.service-chat__close:hover {
  background: var(--omes-color-bg-muted);
  color: var(--omes-color-text);
}

.service-chat__list {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
  overflow: auto;
  padding: 14px;
  background: var(--omes-color-bg-muted);
}

.service-chat__empty {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
  color: var(--omes-color-text-quaternary);
  text-align: center;
}

.service-chat__empty .anticon {
  font-size: 28px;
  color: var(--omes-color-primary);
  opacity: 0.55;
}

.service-chat__empty p,
.service-chat__hint {
  margin: 0;
  font-size: 13px;
  color: var(--omes-color-text-tertiary);
}

.service-chat__hint {
  margin-bottom: 10px;
  text-align: center;
}

.service-chat__row {
  display: flex;
  margin-bottom: 12px;
}

.service-chat__row:last-child {
  margin-bottom: 0;
}

.service-chat__row--mine {
  justify-content: flex-end;
}

.service-chat__bubble {
  max-width: 82%;
  padding: 8px 12px;
  border-radius: 4px 12px 12px 12px;
  background: var(--omes-color-bg-container);
  border: 1px solid var(--omes-color-border-secondary);
  box-shadow: var(--omes-shadow-card-sm);
}

.service-chat__row--mine .service-chat__bubble {
  border-radius: 12px 4px 12px 12px;
  background: var(--omes-color-primary-bg);
  border-color: var(--omes-color-primary-border);
}

.service-chat__meta {
  display: flex;
  gap: 8px;
  margin-bottom: 2px;
  font-size: 12px;
  line-height: 18px;
  color: var(--omes-color-text-quaternary);
}

.service-chat__text {
  color: var(--omes-color-text);
  line-height: 20px;
  white-space: pre-wrap;
  word-break: break-word;
}

.service-chat__composer {
  display: flex;
  gap: 8px;
  align-items: flex-end;
  padding: 12px;
  background: var(--omes-color-bg-container);
  border-top: 1px solid var(--omes-color-border-secondary);
}

.service-chat__composer :deep(textarea.ant-input) {
  border-radius: var(--omes-radius-md);
  resize: none;
}

.service-chat__launcher {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  height: 40px;
  padding: 0 16px 0 14px;
  border: 0;
  border-radius: 20px;
  background: var(--omes-gradient-primary);
  color: var(--omes-color-white);
  font-size: 14px;
  font-weight: var(--omes-font-weight-semibold);
  line-height: 1;
  cursor: pointer;
  box-shadow: var(--omes-shadow-primary-icon), 0 8px 18px rgba(0, 120, 212, 0.28);
}

.service-chat__launcher:hover {
  background: var(--omes-color-primary-hover);
}

.service-chat__launcher:active {
  background: var(--omes-color-primary-active);
}
</style>
