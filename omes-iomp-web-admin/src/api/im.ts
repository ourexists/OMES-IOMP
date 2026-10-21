import { get, post } from '@/api/request'

export interface ImSession {
  peerName: string
}

export interface ImMessage {
  sendId: string
  senderName: string
  text: string
  sendTime: number
  fromService: boolean
}

export interface ImDeskSession {
  userAccId: string
  nickName: string
  preview: string
  sendTime: number
}

export function openImSession() {
  return post<ImSession>('/im/session')
}

export function closeImSession() {
  return post<boolean>('/im/disconnect')
}

export function fetchImMessages() {
  return get<ImMessage[]>('/im/messages')
}

export function sendImMessage(text: string) {
  return post<boolean>('/im/send', { text })
}

export function fetchDeskSessions() {
  return get<ImDeskSession[]>('/im/desk/sessions')
}

export function fetchDeskMessages(userAccId: string) {
  return get<ImMessage[]>('/im/desk/messages', { userAccId })
}

export function sendDeskMessage(userAccId: string, text: string) {
  return post<boolean>('/im/desk/send', { userAccId, text })
}
