<script setup lang="ts">
import { nextTick, onUnmounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import dayjs from 'dayjs'
import {
  fetchDeskMessages,
  fetchDeskSessions,
  sendDeskMessage,
  type ImDeskSession,
  type ImMessage,
} from '@/api/im'

const POLL_MS = 3000

const { t } = useI18n()
const sessions = ref<ImDeskSession[]>([])
const messages = ref<ImMessage[]>([])
const activeId = ref('')
const draft = ref('')
const failed = ref(false)
const sending = ref(false)
const listRef = ref<HTMLElement | null>(null)
let timer = 0

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

async function loadSessions() {
  sessions.value = (await fetchDeskSessions()) || []
  if (!activeId.value && sessions.value.length > 0) {
    activeId.value = sessions.value[0].userAccId
  }
  if (activeId.value && !sessions.value.some((item) => item.userAccId === activeId.value)) {
    activeId.value = sessions.value[0]?.userAccId || ''
  }
}

async function loadMessages() {
  if (!activeId.value) {
    messages.value = []
    return
  }
  messages.value = (await fetchDeskMessages(activeId.value)) || []
  await scrollDown()
}

async function refresh() {
  await loadSessions()
  await loadMessages()
}

async function choose(userAccId: string) {
  activeId.value = userAccId
  draft.value = ''
  await loadMessages()
}

async function onSend() {
  const text = draft.value.trim()
  if (!text || !activeId.value || sending.value) {
    return
  }
  sending.value = true
  try {
    await sendDeskMessage(activeId.value, text)
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

refresh()
  .then(() => {
    timer = window.setInterval(() => {
      refresh().catch(() => {
        failed.value = true
        window.clearInterval(timer)
      })
    }, POLL_MS)
  })
  .catch(() => {
    failed.value = true
  })

onUnmounted(() => {
  if (timer) {
    window.clearInterval(timer)
  }
})
</script>

<template>
  <div class="im-desk">
    <aside class="im-desk__sessions">
      <div class="im-desk__title">{{ t('imChat.sessions') }}</div>
      <p v-if="sessions.length === 0" class="im-desk__hint">{{ t('imChat.noSession') }}</p>
      <button
        v-for="item in sessions"
        :key="item.userAccId"
        type="button"
        class="im-desk__session"
        :class="{ 'im-desk__session--active': item.userAccId === activeId }"
        @click="choose(item.userAccId)"
      >
        <strong>{{ item.nickName }}</strong>
        <span>{{ item.preview }}</span>
        <em>{{ formatTime(item.sendTime) }}</em>
      </button>
    </aside>
    <section class="im-desk__chat">
      <p v-if="failed" class="im-desk__hint">{{ t('imChat.unavailable') }}</p>
      <div ref="listRef" class="im-desk__list">
        <p v-if="!failed && messages.length === 0" class="im-desk__hint">{{ t('imChat.empty') }}</p>
        <div
          v-for="(item, index) in messages"
          :key="`${item.sendTime}-${index}`"
          class="im-desk__row"
          :class="{ 'im-desk__row--mine': item.fromService }"
        >
          <div class="im-desk__bubble">
            <div class="im-desk__meta">
              <span>{{ item.fromService ? t('imChat.peer') : item.senderName }}</span>
              <span>{{ formatTime(item.sendTime) }}</span>
            </div>
            <div>{{ item.text }}</div>
          </div>
        </div>
      </div>
      <footer class="im-desk__composer">
        <a-textarea
          v-model:value="draft"
          :rows="2"
          :maxlength="2000"
          :disabled="!activeId"
          :placeholder="t('imChat.placeholder')"
          @keydown="onKeydown"
        />
        <a-button type="primary" :disabled="!activeId" :loading="sending" @click="onSend">
          {{ t('imChat.send') }}
        </a-button>
      </footer>
    </section>
  </div>
</template>

<style scoped>
.im-desk {
  height: 100%;
  min-height: 0;
  display: flex;
  background: var(--omes-color-bg-container, #fff);
  border: 1px solid var(--omes-color-border, #f0f0f0);
  border-radius: 8px;
  overflow: hidden;
}

.im-desk__sessions {
  width: 280px;
  flex-shrink: 0;
  overflow: auto;
  border-right: 1px solid var(--omes-color-border, #f0f0f0);
}

.im-desk__title {
  padding: 16px;
  font-weight: 600;
}

.im-desk__session {
  width: 100%;
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding: 12px 16px;
  text-align: left;
  border: 0;
  border-top: 1px solid var(--omes-color-border, #f0f0f0);
  background: transparent;
  cursor: pointer;
}

.im-desk__session--active {
  background: #e6f4ff;
}

.im-desk__session span,
.im-desk__session em {
  color: var(--omes-color-text-secondary, rgba(0, 0, 0, 0.45));
  font-style: normal;
  font-size: 12px;
}

.im-desk__session span {
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
}

.im-desk__chat {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
}

.im-desk__list {
  flex: 1;
  overflow: auto;
  padding: 16px;
}

.im-desk__hint {
  padding: 16px;
  color: var(--omes-color-text-secondary, rgba(0, 0, 0, 0.45));
  text-align: center;
}

.im-desk__row {
  display: flex;
  margin-bottom: 12px;
}

.im-desk__row--mine {
  justify-content: flex-end;
}

.im-desk__bubble {
  max-width: 70%;
  padding: 8px 10px;
  border-radius: 8px;
  background: var(--omes-color-fill-quaternary, #f5f5f5);
  white-space: pre-wrap;
  word-break: break-word;
}

.im-desk__row--mine .im-desk__bubble {
  background: #e6f4ff;
}

.im-desk__meta {
  display: flex;
  gap: 8px;
  margin-bottom: 4px;
  font-size: 12px;
  color: var(--omes-color-text-secondary, rgba(0, 0, 0, 0.45));
}

.im-desk__composer {
  display: flex;
  gap: 8px;
  align-items: flex-end;
  padding: 12px 16px;
  border-top: 1px solid var(--omes-color-border, #f0f0f0);
}
</style>
