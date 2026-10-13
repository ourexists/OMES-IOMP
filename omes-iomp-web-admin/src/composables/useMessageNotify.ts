import { onMounted, onUnmounted, ref, shallowRef } from 'vue'
import { notification } from 'ant-design-vue'
import { OAUTH_CLIENT, STORAGE_KEYS } from '@/config'
import { gatewayApiPath } from '@/config/gateway'
import { i18n } from '@/i18n'
import { fetchMessagePage, fetchUnreadMessageCount, markMessageRead } from '@/api/message'
import type { MessagePushEvent, MessageRecord } from '@/types/message'
import { getItem } from '@/utils/storage'

const RECONNECT_BASE_MS = 2_000
const RECONNECT_MAX_MS = 30_000

function t(key: string, fallback: string) {
  const { te } = i18n.global
  return te(key) ? i18n.global.t(key) : fallback
}

function resolveAccessToken(): string | null {
  const raw = getItem(STORAGE_KEYS.token)?.trim()
  if (!raw) {
    return null
  }
  return raw.replace(/^Bearer\s+/i, '').trim() || null
}

function resolveWebSocketUrl(): string | null {
  const token = resolveAccessToken()
  if (!token) {
    return null
  }
  const httpPath = gatewayApiPath('/message/ws')
  const params = new URLSearchParams({
    access_token: token,
    platform: OAUTH_CLIENT.platform,
    tenant: String(OAUTH_CLIENT.tenantId),
  })
  if (/^https?:\/\//i.test(httpPath)) {
    return `${httpPath.replace(/^http/i, 'ws')}?${params}`
  }
  if (typeof window === 'undefined') {
    return null
  }
  const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:'
  return `${protocol}//${window.location.host}${httpPath}?${params}`
}

export function useMessageNotify() {
  const unreadCount = ref(0)
  const recentMessages = shallowRef<MessageRecord[]>([])
  const connected = ref(false)

  let socket: WebSocket | null = null
  let reconnectTimer: ReturnType<typeof setTimeout> | null = null
  let reconnectDelay = RECONNECT_BASE_MS
  let disposed = false

  async function refreshUnreadCount() {
    try {
      unreadCount.value = (await fetchUnreadMessageCount()) ?? 0
    } catch {
      // ignore polling errors
    }
  }

  async function refreshRecentMessages() {
    try {
      const page = await fetchMessagePage({ page: 1, pageSize: 10 })
      recentMessages.value = page.records ?? []
    } catch {
      // ignore load errors
    }
  }

  function scheduleReconnect() {
    if (disposed || reconnectTimer) {
      return
    }
    reconnectTimer = setTimeout(() => {
      reconnectTimer = null
      connect()
      reconnectDelay = Math.min(reconnectDelay * 2, RECONNECT_MAX_MS)
    }, reconnectDelay)
  }

  function handlePayload(raw: string) {
    let payload: MessagePushEvent
    try {
      payload = JSON.parse(raw) as MessagePushEvent
    } catch {
      return
    }
    if (payload.event !== 'message' || !payload.data?.id) {
      return
    }
    const message = payload.data
    unreadCount.value += 1
    const next = [message, ...recentMessages.value.filter((item) => item.id !== message.id)]
    recentMessages.value = next.slice(0, 20)
    notification.open({
      message: message.title || t('messageNotify.newTitle', '新消息'),
      description: message.context || '',
      placement: 'topRight',
      duration: 5,
    })
  }

  function connect() {
    if (disposed) {
      return
    }
    const url = resolveWebSocketUrl()
    if (!url) {
      return
    }
    if (socket && (socket.readyState === WebSocket.OPEN || socket.readyState === WebSocket.CONNECTING)) {
      return
    }
    socket?.close()
    socket = new WebSocket(url)
    socket.onopen = () => {
      connected.value = true
      reconnectDelay = RECONNECT_BASE_MS
    }
    socket.onmessage = (event) => {
      if (typeof event.data === 'string') {
        handlePayload(event.data)
      }
    }
    socket.onclose = () => {
      connected.value = false
      socket = null
      scheduleReconnect()
    }
    socket.onerror = () => {
      socket?.close()
    }
  }

  function disconnect() {
    disposed = true
    if (reconnectTimer) {
      clearTimeout(reconnectTimer)
      reconnectTimer = null
    }
    socket?.close()
    socket = null
    connected.value = false
  }

  async function readMessage(message: MessageRecord) {
    if (!message.id) {
      return
    }
    await markMessageRead(message.id)
    if (message.readStatus !== 1) {
      unreadCount.value = Math.max(0, unreadCount.value - 1)
      message.readStatus = 1
    }
    recentMessages.value = recentMessages.value.map((item) =>
      item.id === message.id ? { ...item, readStatus: 1 } : item,
    )
  }

  onMounted(() => {
    disposed = false
    void refreshUnreadCount()
    connect()
  })

  onUnmounted(() => {
    disconnect()
  })

  return {
    unreadCount,
    recentMessages,
    connected,
    refreshUnreadCount,
    refreshRecentMessages,
    readMessage,
    reconnect: connect,
  }
}
