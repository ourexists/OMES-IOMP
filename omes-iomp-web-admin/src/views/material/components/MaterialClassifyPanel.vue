<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import {
  AppstoreOutlined,
  DeleteOutlined,
  EditOutlined,
  FolderOutlined,
  SearchOutlined,
} from '@ant-design/icons-vue'
import type { MaterialClassifyRecord } from '@/api/material'
import { fetchMaterialClassifyList } from '@/api/material'

export type MaterialClassifySelection = MaterialClassifyRecord | null | 'all'

const props = defineProps<{
  modelValue?: MaterialClassifySelection
}>()

const emit = defineEmits<{
  'update:modelValue': [value: MaterialClassifySelection]
  change: [value: MaterialClassifySelection]
  loaded: [list: MaterialClassifyRecord[]]
  edit: []
  delete: []
}>()

const { t } = useI18n()

const loading = ref(false)
const keyword = ref('')
const classifies = ref<MaterialClassifyRecord[]>([])
const selectedKey = ref<string>('all')

const filteredList = computed(() => {
  const q = keyword.value.trim().toLowerCase()
  if (!q) {
    return classifies.value
  }
  return classifies.value.filter((item) => {
    const name = (item.name || '').toLowerCase()
    const code = (item.selfCode || '').toLowerCase()
    return name.includes(q) || code.includes(q)
  })
})

const showEmpty = computed(() => !loading.value && classifies.value.length === 0)

function itemKey(item: MaterialClassifyRecord): string {
  return item.selfCode || item.id
}

function isItemActive(item: MaterialClassifyRecord): boolean {
  return selectedKey.value === itemKey(item)
}

function emitSelection(value: MaterialClassifySelection) {
  emit('update:modelValue', value)
  emit('change', value)
}

function syncSelectedKey() {
  const value = props.modelValue
  if (value === 'all' || value == null) {
    selectedKey.value = 'all'
    return
  }
  selectedKey.value = value.selfCode || value.id || 'all'
}

function onSelect(key: string) {
  selectedKey.value = key
  if (key === 'all') {
    emitSelection('all')
    return
  }
  const item = classifies.value.find((row) => itemKey(row) === key)
  emitSelection(item || 'all')
}

function onEditClick(event: Event) {
  event.stopPropagation()
  emit('edit')
}

function onDeleteClick(event: Event) {
  event.stopPropagation()
  emit('delete')
}

async function loadList() {
  loading.value = true
  try {
    const list = await fetchMaterialClassifyList()
    classifies.value = Array.isArray(list) ? list : []
    emit('loaded', classifies.value)
    syncSelectedKey()
    if (props.modelValue == null && classifies.value.length > 0) {
      emitSelection('all')
    }
  } finally {
    loading.value = false
  }
}

defineExpose({ reload: loadList })

onMounted(() => {
  syncSelectedKey()
  loadList()
})
</script>

<template>
  <div class="classify-panel">
    <a-input
      v-model:value="keyword"
      allow-clear
      class="classify-panel__search"
      :placeholder="t('materialPage.classifySearch')"
    >
      <template #prefix>
        <SearchOutlined class="input-prefix-icon" />
      </template>
    </a-input>

    <a-spin :spinning="loading" class="classify-panel__spin">
      <div v-if="showEmpty" class="classify-panel__empty-wrap">
        <a-empty :description="t('materialPage.classifyNone')" :image-style="{ height: '56px' }" />
      </div>
      <div v-else class="classify-panel__list">
        <div
          class="classify-item classify-item--all"
          :class="{ 'classify-item--active': selectedKey === 'all' }"
          @click="onSelect('all')"
        >
          <span class="classify-item__icon-wrap classify-item__icon-wrap--all">
            <AppstoreOutlined />
          </span>
          <span class="classify-item__label">{{ t('materialPage.classifyAll') }}</span>
        </div>
        <div
          v-for="item in filteredList"
          :key="item.id"
          class="classify-item"
          :class="{
            'classify-item--active': isItemActive(item),
            'classify-item--with-actions': isItemActive(item),
          }"
          @click="onSelect(itemKey(item))"
        >
          <span class="classify-item__icon-wrap">
            <FolderOutlined />
          </span>
          <div class="classify-item__body">
            <span class="classify-item__label">{{ item.name || '—' }}</span>
            <span v-if="item.selfCode" class="classify-item__code">{{ item.selfCode }}</span>
          </div>
          <div v-if="isItemActive(item)" class="classify-item__actions" @click.stop>
            <a-tooltip :title="t('materialPage.edit')">
              <a-button
                type="text"
                size="small"
                class="classify-item__action-btn"
                @click="onEditClick"
              >
                <EditOutlined />
              </a-button>
            </a-tooltip>
            <a-tooltip :title="t('materialPage.delete')">
              <a-button
                type="text"
                size="small"
                danger
                class="classify-item__action-btn"
                @click="onDeleteClick"
              >
                <DeleteOutlined />
              </a-button>
            </a-tooltip>
          </div>
        </div>
        <div
          v-if="!loading && !filteredList.length && keyword.trim()"
          class="classify-panel__filter-empty"
        >
          {{ t('materialPage.classifyEmpty') }}
        </div>
      </div>
    </a-spin>
  </div>
</template>

<style scoped>
.classify-panel {
  display: flex;
  flex-direction: column;
  flex: 1;
  min-height: 0;
  height: 100%;
}

.classify-panel__search {
  flex-shrink: 0;
  margin-bottom: 12px;
}

.classify-panel__spin {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
}

.classify-panel__spin :deep(.ant-spin-container) {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
}

.classify-panel__list {
  flex: 1;
  min-height: 0;
  overflow: auto;
  padding: 4px;
  border: 1px solid var(--omes-color-border-secondary);
  border-radius: var(--omes-radius-lg);
  background: #f8f9fc;
  scrollbar-width: thin;
  scrollbar-color: #d9d9d9 transparent;
}

.classify-panel__list::-webkit-scrollbar {
  width: 6px;
}

.classify-panel__list::-webkit-scrollbar-thumb {
  background: #d9d9d9;
  border-radius: 3px;
}

.classify-panel__empty-wrap {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 200px;
  border: 1px dashed var(--omes-color-border-hover);
  border-radius: var(--omes-radius-lg);
  background: var(--omes-color-bg-elevated);
}

.classify-panel__filter-empty {
  padding: 24px 12px;
  text-align: center;
  font-size: 13px;
  color: var(--omes-color-text-quaternary);
}

.classify-item {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  margin-bottom: 4px;
  padding: 10px 12px;
  cursor: pointer;
  border-radius: var(--omes-radius-md);
  border: 1px solid transparent;
  background: var(--omes-color-bg-container);
  transition:
    background 0.15s,
    border-color 0.15s,
    box-shadow 0.15s;
}

.classify-item:last-child {
  margin-bottom: 0;
}

.classify-item:hover {
  border-color: var(--omes-color-primary-border);
  box-shadow: 0 1px 4px rgba(22, 119, 255, 0.08);
}

.classify-item--active {
  border-color: #91caff;
  background: var(--omes-color-bg-container);
  box-shadow: 0 2px 8px rgba(22, 119, 255, 0.12);
}

.classify-item--active .classify-item__label {
  color: var(--omes-color-primary);
  font-weight: 600;
}

.classify-item--all .classify-item__label {
  font-weight: 500;
}

.classify-item--with-actions {
  align-items: flex-start;
  padding-right: 8px;
}

.classify-item__icon-wrap {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  flex-shrink: 0;
  border-radius: var(--omes-radius-sm);
  font-size: 14px;
  color: #8c8c8c;
  background: var(--omes-color-bg-layout);
}

.classify-item__icon-wrap--all {
  color: var(--omes-color-primary);
  background: var(--omes-color-primary-bg);
}

.classify-item--active .classify-item__icon-wrap {
  color: var(--omes-color-primary);
  background: var(--omes-color-primary-bg);
}

.classify-item__body {
  min-width: 0;
  flex: 1;
}

.classify-item__label {
  display: block;
  line-height: 1.45;
  color: var(--omes-color-text);
  word-break: break-word;
}

.classify-item__code {
  display: block;
  margin-top: 4px;
  font-size: 12px;
  line-height: 1.3;
  color: var(--omes-color-text-quaternary);
  font-family: ui-monospace, SFMono-Regular, Menlo, Consolas, monospace;
}

.classify-item__actions {
  display: flex;
  align-items: center;
  gap: 2px;
  flex-shrink: 0;
  margin-top: -2px;
  margin-inline-start: 4px;
}

.classify-item__action-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  padding: 0;
  border-radius: var(--omes-radius-sm);
}

.classify-item__action-btn:hover {
  background: rgba(0, 0, 0, 0.04);
}

.classify-item__action-btn.ant-btn-dangerous:hover {
  background: #fff1f0;
}

.classify-item__action-btn :deep(.anticon) {
  font-size: 14px;
}

.input-prefix-icon {
  color: var(--omes-color-text-placeholder);
}
</style>
