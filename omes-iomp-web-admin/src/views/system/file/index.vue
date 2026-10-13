<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import dayjs from 'dayjs'
import {
  DeleteOutlined,
  DownloadOutlined,
  EditOutlined,
  EyeOutlined,
  FileOutlined,
  FolderOpenOutlined,
  FolderOutlined,
  PlusOutlined,
  ReloadOutlined,
  UploadOutlined,
} from '@ant-design/icons-vue'
import { message, Modal } from 'ant-design-vue'
import request from '@/api/request'
import {
  deleteLocalFile,
  fetchLocalFileBrowse,
  formatFileSize,
  isPreviewableImage,
  joinDirPath,
  mkdirLocalFile,
  renameLocalFile,
  splitDirPath,
  toRelativeStoragePath,
  uploadLocalFile,
  type LocalFileEntry,
} from '@/api/local-file'
import AdminPanelTitle from '@/components/admin/AdminPanelTitle.vue'
import TableScrollWrap from '@/components/common/TableScrollWrap.vue'

const { t } = useI18n()

const loading = ref(false)
const uploading = ref(false)
const dragging = ref(false)
const currentDir = ref('')
const entries = ref<LocalFileEntry[]>([])
const fileInput = ref<HTMLInputElement | null>(null)

const folderOpen = ref(false)
const folderName = ref('')
const renameOpen = ref(false)
const renameName = ref('')
const renameTarget = ref<LocalFileEntry | null>(null)
const previewOpen = ref(false)
const previewName = ref('')
const previewUrl = ref('')

const crumbs = computed(() => splitDirPath(currentDir.value))

const columns = computed(() => [
  { title: t('filePage.colName'), key: 'name', dataIndex: 'name', ellipsis: true },
  { title: t('filePage.colSize'), key: 'size', width: 120 },
  { title: t('filePage.colModified'), key: 'modified', width: 180 },
  { title: t('filePage.colAction'), key: 'action', width: 280, fixed: 'right' as const },
])

function formatModified(value?: number | null): string {
  if (!value) {
    return '-'
  }
  const parsed = dayjs(value)
  return parsed.isValid() ? parsed.format('YYYY-MM-DD HH:mm') : '-'
}

async function load() {
  loading.value = true
  try {
    const list = await fetchLocalFileBrowse(currentDir.value || undefined)
    entries.value = (list || [])
      .map((entry) => ({ ...entry, path: toRelativeStoragePath(entry.path) }))
      .filter((entry) => entry.path)
      .sort((a, b) => {
        if (a.directory !== b.directory) {
          return a.directory ? -1 : 1
        }
        return a.name.localeCompare(b.name)
      })
  } finally {
    loading.value = false
  }
}

function openDir(path: string) {
  currentDir.value = toRelativeStoragePath(path)
  load()
}

function openCrumb(index: number) {
  openDir(index < 0 ? '' : joinDirPath(crumbs.value.slice(0, index + 1)))
}

async function uploadFiles(files: File[]) {
  if (!files.length) {
    return
  }
  uploading.value = true
  try {
    for (const file of files) {
      await uploadLocalFile(file, currentDir.value || undefined)
    }
    message.success(t('filePage.uploadSuccess', { count: files.length }))
    await load()
  } finally {
    uploading.value = false
  }
}

function pickFiles() {
  fileInput.value?.click()
}

function onPick(event: Event) {
  const input = event.target as HTMLInputElement
  const files = Array.from(input.files || [])
  input.value = ''
  uploadFiles(files)
}

function onDrop(event: DragEvent) {
  dragging.value = false
  uploadFiles(Array.from(event.dataTransfer?.files || []))
}

function openFolderModal() {
  folderName.value = ''
  folderOpen.value = true
}

async function submitFolder() {
  const name = folderName.value.trim()
  if (!name) {
    message.warning(t('filePage.folderNameRequired'))
    return Promise.reject()
  }
  await mkdirLocalFile(name, currentDir.value || undefined)
  message.success(t('filePage.mkdirSuccess'))
  folderOpen.value = false
  folderName.value = ''
  await load()
}

function openRename(entry: LocalFileEntry) {
  renameTarget.value = entry
  renameName.value = entry.name
  renameOpen.value = true
}

async function submitRename() {
  const name = renameName.value.trim()
  const target = renameTarget.value
  if (!target) {
    return
  }
  if (!name) {
    message.warning(t('filePage.renameRequired'))
    return Promise.reject()
  }
  await renameLocalFile(target.path, name)
  message.success(t('filePage.renameSuccess'))
  renameOpen.value = false
  await load()
}

function confirmDelete(entry: LocalFileEntry) {
  Modal.confirm({
    title: t('filePage.deleteConfirm'),
    content: entry.directory
      ? t('filePage.deleteFolderContent', { name: entry.name })
      : t('filePage.deleteFileContent', { name: entry.name }),
    okText: t('filePage.confirm'),
    cancelText: t('filePage.cancel'),
    onOk: async () => {
      await deleteLocalFile(entry.path)
      message.success(t('filePage.deleteSuccess'))
      await load()
    },
  })
}

async function fetchBlob(path: string): Promise<Blob> {
  const response = await request.get<Blob>('/localFile/download', {
    params: { path: toRelativeStoragePath(path) },
    responseType: 'blob',
  })
  return response.data
}

async function downloadEntry(entry: LocalFileEntry) {
  const blob = await fetchBlob(entry.path)
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = entry.name
  link.click()
  URL.revokeObjectURL(url)
}

async function previewEntry(entry: LocalFileEntry) {
  if (previewUrl.value) {
    URL.revokeObjectURL(previewUrl.value)
  }
  const blob = await fetchBlob(entry.path)
  previewName.value = entry.name
  previewUrl.value = URL.createObjectURL(blob)
  previewOpen.value = true
}

function closePreview() {
  previewOpen.value = false
  if (previewUrl.value) {
    URL.revokeObjectURL(previewUrl.value)
    previewUrl.value = ''
  }
}

onMounted(load)
</script>

<template>
  <div class="admin-page system-module-page">
    <a-card size="small" class="admin-panel-card panel-card">
      <template #title>
        <AdminPanelTitle>
          <template #icon><FolderOpenOutlined /></template>
          {{ t('filePage.title') }}
        </AdminPanelTitle>
      </template>
      <template #extra>
        <a-tag color="processing">{{ t('filePage.itemCount', { count: entries.length }) }}</a-tag>
      </template>

      <div
        class="admin-panel-body file-drop"
        :class="{ 'file-drop--active': dragging }"
        @dragenter.prevent="dragging = true"
        @dragover.prevent="dragging = true"
        @dragleave.prevent="dragging = false"
        @drop.prevent="onDrop"
      >
        <div v-if="dragging" class="file-drop__hint">{{ t('filePage.dropHint') }}</div>

        <div class="table-toolbar">
          <a-space wrap>
            <a-breadcrumb>
              <a-breadcrumb-item>
                <a @click="openCrumb(-1)">{{ t('filePage.root') }}</a>
              </a-breadcrumb-item>
              <a-breadcrumb-item v-for="(part, index) in crumbs" :key="`${part}-${index}`">
                <a @click="openCrumb(index)">{{ part }}</a>
              </a-breadcrumb-item>
            </a-breadcrumb>
          </a-space>
          <a-space wrap>
            <a-button type="primary" :loading="uploading" @click="pickFiles">
              <template #icon><UploadOutlined /></template>
              {{ t('filePage.upload') }}
            </a-button>
            <a-button @click="openFolderModal">
              <template #icon><PlusOutlined /></template>
              {{ t('filePage.newFolder') }}
            </a-button>
            <a-button :loading="loading" @click="load">
              <template #icon><ReloadOutlined /></template>
              {{ t('filePage.refresh') }}
            </a-button>
            <input ref="fileInput" type="file" multiple hidden @change="onPick" />
          </a-space>
        </div>

        <TableScrollWrap :refresh-keys="[entries.length, currentDir]">
          <template #default="{ scrollY, scrollReady }">
            <a-table
              row-key="path"
              size="middle"
              bordered
              class="scroll-table system-module-table"
              :class="{ 'scroll-table--pending': !scrollReady }"
              :columns="columns"
              :data-source="entries"
              :loading="loading"
              :pagination="false"
              :scroll="{ x: 860, ...(scrollReady ? { y: scrollY } : {}) }"
            >
              <template #bodyCell="{ column, record }">
                <template v-if="column.key === 'name'">
                  <a v-if="record.directory" class="name-link" @click="openDir(record.path)">
                    <FolderOutlined />
                    {{ record.name }}
                  </a>
                  <span v-else class="name-link">
                    <FileOutlined />
                    {{ record.name }}
                  </span>
                </template>
                <template v-else-if="column.key === 'size'">
                  {{ record.directory ? t('filePage.folder') : formatFileSize(record.size) }}
                </template>
                <template v-else-if="column.key === 'modified'">
                  {{ formatModified(record.lastModified) }}
                </template>
                <template v-else-if="column.key === 'action'">
                  <a-space wrap size="small">
                    <a-button
                      v-if="!record.directory"
                      type="link"
                      size="small"
                      @click="downloadEntry(record)"
                    >
                      <DownloadOutlined />
                      {{ t('filePage.download') }}
                    </a-button>
                    <a-button
                      v-if="isPreviewableImage(record)"
                      type="link"
                      size="small"
                      @click="previewEntry(record)"
                    >
                      <EyeOutlined />
                      {{ t('filePage.preview') }}
                    </a-button>
                    <a-button type="link" size="small" @click="openRename(record)">
                      <EditOutlined />
                      {{ t('filePage.rename') }}
                    </a-button>
                    <a-button type="link" size="small" danger @click="confirmDelete(record)">
                      <DeleteOutlined />
                      {{ t('filePage.delete') }}
                    </a-button>
                  </a-space>
                </template>
              </template>
              <template #emptyText>
                <a-empty :description="currentDir ? t('filePage.emptyFolder') : t('filePage.emptyRoot')" />
              </template>
            </a-table>
          </template>
        </TableScrollWrap>
      </div>
    </a-card>

    <a-modal
      v-model:open="folderOpen"
      :title="t('filePage.newFolder')"
      :ok-text="t('filePage.confirm')"
      :cancel-text="t('filePage.cancel')"
      @ok="submitFolder"
    >
      <a-input
        v-model:value="folderName"
        :placeholder="t('filePage.folderNamePlaceholder')"
        @press-enter="submitFolder"
      />
    </a-modal>

    <a-modal
      v-model:open="renameOpen"
      :title="t('filePage.rename')"
      :ok-text="t('filePage.confirm')"
      :cancel-text="t('filePage.cancel')"
      @ok="submitRename"
    >
      <a-input
        v-model:value="renameName"
        :placeholder="t('filePage.renamePlaceholder')"
        @press-enter="submitRename"
      />
    </a-modal>

    <a-modal
      :open="previewOpen"
      :title="previewName || t('filePage.preview')"
      :footer="null"
      @cancel="closePreview"
    >
      <img v-if="previewUrl" class="preview-image" :src="previewUrl" :alt="previewName" />
    </a-modal>
  </div>
</template>

<style scoped>
.file-drop {
  position: relative;
}

.file-drop--active {
  outline: 2px dashed var(--omes-color-primary, #1677ff);
  outline-offset: -8px;
}

.file-drop__hint {
  position: absolute;
  inset: 0;
  z-index: 2;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(255, 255, 255, 0.86);
  font-size: 16px;
}

.name-link {
  display: inline-flex;
  gap: 6px;
  align-items: center;
}

.preview-image {
  display: block;
  max-width: 100%;
  margin: 0 auto;
}
</style>
