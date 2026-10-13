import request, { get, postForm } from '@/api/request'
import { gatewayApiPath } from '@/config/gateway'

export interface LocalFileEntry {
  name: string
  path: string
  directory: boolean
  size?: number | null
  lastModified?: number | null
  extension?: string
}

const FILE_URL_PREFIX = '/files/'

/** 统一成存储根下的相对路径（正斜杠，无盘符/无 /files 前缀） */
export function toRelativeStoragePath(path?: string): string {
  if (!path?.trim()) {
    return ''
  }
  let value = path.trim().replace(/\\/g, '/')
  if (/^https?:\/\//i.test(value)) {
    try {
      value = new URL(value).pathname + new URL(value).search
    } catch {
      return ''
    }
  }
  const queryPath = value.match(/[?&]path=([^&]*)/i)
  if (queryPath) {
    try {
      value = decodeURIComponent(queryPath[1] || '')
    } catch {
      value = queryPath[1] || ''
    }
    value = value.replace(/\\/g, '/')
  }
  value = value.replace(/^\/+files\//i, '')
  value = value.replace(/^[a-zA-Z]:\//, '')
  value = value.replace(/^\/+/, '')
  if (!value || value.includes('..')) {
    return ''
  }
  return value
}

export function resolveLocalFileUrl(path?: string): string {
  if (!path?.trim()) {
    return ''
  }
  const trimmed = path.trim()
  if (trimmed.startsWith('http://') || trimmed.startsWith('https://')) {
    return trimmed
  }
  const relative = toRelativeStoragePath(trimmed)
  if (!relative) {
    return trimmed.startsWith('/') ? gatewayApiPath(trimmed) : ''
  }
  return gatewayApiPath(`${FILE_URL_PREFIX}${relative}`)
}

export function getLocalFileDownloadUrl(path: string): string {
  const relative = toRelativeStoragePath(path)
  if (!relative) {
    return ''
  }
  return gatewayApiPath(`/localFile/download?path=${encodeURIComponent(relative)}`)
}

export function fetchLocalFileBrowse(dir?: string) {
  const relative = toRelativeStoragePath(dir)
  return get<LocalFileEntry[]>('/localFile/browse', relative ? { dir: relative } : undefined)
}

export async function uploadLocalFile(file: File, dir?: string) {
  const formData = new FormData()
  formData.append('file', file)
  const relative = toRelativeStoragePath(dir)
  if (relative) {
    formData.append('dir', relative)
  }
  const { data } = await request.post<string>('/localFile/upload', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  })
  return toRelativeStoragePath(typeof data === 'string' ? data : '')
}

export function mkdirLocalFile(name: string, dir?: string) {
  const relative = toRelativeStoragePath(dir)
  return postForm<string>('/localFile/mkdir', {
    name,
    ...(relative ? { dir: relative } : {}),
  })
}

export function renameLocalFile(path: string, newName: string) {
  return postForm<string>('/localFile/rename', {
    path: toRelativeStoragePath(path),
    newName,
  })
}

export function deleteLocalFile(path: string) {
  return postForm<boolean>('/localFile/delete', { path: toRelativeStoragePath(path) })
}

export function formatFileSize(size?: number | null): string {
  if (size == null || size < 0) {
    return '-'
  }
  if (size < 1024) {
    return `${size} B`
  }
  if (size < 1024 * 1024) {
    return `${(size / 1024).toFixed(size < 10 * 1024 ? 1 : 0)} KB`
  }
  if (size < 1024 * 1024 * 1024) {
    return `${(size / (1024 * 1024)).toFixed(size < 10 * 1024 * 1024 ? 1 : 0)} MB`
  }
  return `${(size / (1024 * 1024 * 1024)).toFixed(2)} GB`
}

export function isPreviewableImage(entry: LocalFileEntry): boolean {
  if (entry.directory) {
    return false
  }
  return ['png', 'jpg', 'jpeg', 'gif', 'webp', 'bmp', 'svg'].includes((entry.extension || '').toLowerCase())
}

export function splitDirPath(dir: string): string[] {
  return toRelativeStoragePath(dir).split('/').filter(Boolean)
}

export function joinDirPath(parts: string[]): string {
  return parts.filter(Boolean).join('/')
}
