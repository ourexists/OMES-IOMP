<script setup lang="ts">
import { onMounted, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import type { PlatformNode } from '@/api/ucenter'
import { fetchPlatforms } from '@/api/ucenter'
import PlatformFormModal from './PlatformFormModal.vue'

const props = defineProps<{
  modelValue?: PlatformNode | null
}>()

const emit = defineEmits<{
  'update:modelValue': [value: PlatformNode | null]
  change: [value: PlatformNode | null]
}>()

const { t } = useI18n()

const loading = ref(false)
const platformList = ref<PlatformNode[]>([])
const selectedKeys = ref<string[]>([])

const formOpen = ref(false)
const editingRecord = ref<PlatformNode | null>(null)

async function loadPlatforms(preferredCode?: string) {
  loading.value = true
  try {
    const data = await fetchPlatforms()
    platformList.value = Array.isArray(data) ? data : []

    const preferred = preferredCode || props.modelValue?.code
    let next: PlatformNode | null = null

    if (preferred) {
      next = platformList.value.find((item) => item.code === preferred) || null
    }
    if (!next && platformList.value.length > 0) {
      next = platformList.value[0]
    }

    selectedKeys.value = next?.code ? [next.code] : []
    emit('update:modelValue', next)
    emit('change', next)
  } finally {
    loading.value = false
  }
}

function onSelect(keys: string[]) {
  selectedKeys.value = keys
  if (!keys.length) {
    emit('update:modelValue', null)
    emit('change', null)
    return
  }
  const node = platformList.value.find((item) => item.code === keys[0]) || null
  emit('update:modelValue', node)
  emit('change', node)
}

function openEdit(record: PlatformNode) {
  editingRecord.value = record
  formOpen.value = true
}

function onFormSuccess(code: string) {
  loadPlatforms(code || selectedKeys.value[0])
}

watch(
  () => props.modelValue?.code,
  (code) => {
    selectedKeys.value = code ? [code] : []
  },
)

onMounted(() => loadPlatforms())
</script>

<template>
  <div class="platform-manage">
    <a-spin :spinning="loading">
      <div class="platform-list">
        <a-tree
          v-if="platformList.length"
          class="platform-list__tree"
          :tree-data="platformList"
          :selected-keys="selectedKeys"
          :field-names="{ title: 'name', key: 'code', children: 'children' }"
          block-node
          @select="onSelect"
        >
          <template #title="record">
            <div class="platform-node">
              <div class="node-info">
                <span class="node-name">{{ record.name }}</span>
                <span class="node-code">{{ record.code }}</span>
              </div>
              <span class="node-actions" @click.stop>
                <a-button type="link" size="small" @click="openEdit(record)">
                  {{ t('permissionPage.edit') }}
                </a-button>
              </span>
            </div>
          </template>
        </a-tree>
        <a-empty v-else :description="t('platformTree.empty')" />
      </div>
    </a-spin>

    <PlatformFormModal v-model:open="formOpen" :record="editingRecord" @success="onFormSuccess" />
  </div>
</template>

<style scoped>
.platform-manage {
  display: flex;
  flex-direction: column;
  height: 100%;
  min-height: 0;
}

.platform-manage :deep(.ant-spin-nested-loading),
.platform-manage :deep(.ant-spin-container) {
  height: 100%;
}

.platform-list {
  flex: 1;
  min-height: 0;
  overflow: auto;
}

.platform-list__tree {
  background: transparent;
}

.platform-list :deep(.ant-tree-list-holder-inner) {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.platform-list :deep(.ant-tree-treenode) {
  width: 100%;
  padding: 0;
  align-items: stretch;
}

.platform-list :deep(.ant-tree-switcher-noop) {
  width: 0;
}

.platform-list :deep(.ant-tree-node-content-wrapper) {
  flex: 1;
  min-width: 0;
  min-height: 44px;
  padding: 8px 12px;
  line-height: 1.3;
  border: 1px solid var(--omes-color-border);
  border-radius: var(--omes-radius-md);
  background: var(--omes-color-bg-container);
  transition: background 0.2s, border-color 0.2s, box-shadow 0.2s;
}

.platform-list :deep(.ant-tree-node-content-wrapper:hover) {
  border-color: var(--omes-color-primary-border);
  background: var(--omes-color-primary-bg-hover);
}

.platform-list :deep(.ant-tree-node-content-wrapper.ant-tree-node-selected) {
  border-color: var(--omes-color-primary);
  background: var(--omes-color-primary-bg);
  box-shadow: 0 0 0 1px color-mix(in srgb, var(--omes-color-primary) 18%, transparent);
}

.platform-list :deep(.ant-tree-title) {
  flex: 1;
  min-width: 0;
}

.platform-node {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  min-width: 0;
}

.node-info {
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
}

.node-name {
  font-weight: 600;
  color: var(--omes-color-text);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.platform-list :deep(.ant-tree-node-selected) .node-name {
  color: var(--omes-color-primary);
}

.node-code {
  font-size: 12px;
  color: var(--omes-color-text-secondary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.node-actions {
  display: flex;
  align-items: center;
  flex-shrink: 0;
}

.node-actions :deep(.ant-btn-link) {
  padding-inline: 4px;
  height: 24px;
}
</style>
