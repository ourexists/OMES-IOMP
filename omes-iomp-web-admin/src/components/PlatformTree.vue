<script setup lang="ts">
import { onMounted, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import type { PlatformNode } from '@/api/ucenter'
import { fetchPlatforms } from '@/api/ucenter'

const { t } = useI18n()

const props = defineProps<{
  modelValue?: PlatformNode | null
}>()

const emit = defineEmits<{
  'update:modelValue': [value: PlatformNode | null]
  change: [value: PlatformNode | null]
}>()

const loading = ref(false)
const treeData = ref<PlatformNode[]>([])
const selectedKeys = ref<string[]>([])

async function loadTree() {
  loading.value = true
  try {
    const data = await fetchPlatforms()
    treeData.value = Array.isArray(data) ? data : []
    if (props.modelValue?.code) {
      selectedKeys.value = [props.modelValue.code]
    } else if (treeData.value.length > 0) {
      const first = treeData.value[0]
      selectedKeys.value = [first.code]
      emit('update:modelValue', first)
      emit('change', first)
    }
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
  const code = keys[0]
  const node = findNode(treeData.value, code)
  emit('update:modelValue', node)
  emit('change', node)
}

function findNode(nodes: PlatformNode[], code: string): PlatformNode | null {
  for (const node of nodes) {
    if (node.code === code) {
      return node
    }
    if (node.children?.length) {
      const found = findNode(node.children, code)
      if (found) {
        return found
      }
    }
  }
  return null
}

watch(
  () => props.modelValue?.code,
  (code) => {
    selectedKeys.value = code ? [code] : []
  },
)

onMounted(loadTree)
</script>

<template>
  <div class="platform-tree">
    <a-spin :spinning="loading">
      <a-tree
        v-if="treeData.length"
        class="platform-tree__list"
        :tree-data="treeData"
        :selected-keys="selectedKeys"
        :field-names="{ title: 'name', key: 'code', children: 'children' }"
        default-expand-all
        block-node
        @select="onSelect"
      >
        <template #title="node">
          <span class="platform-node">
            <span class="platform-node__name">{{ node.name }}</span>
            <span class="platform-node__code">{{ node.code }}</span>
          </span>
        </template>
      </a-tree>
      <a-empty v-else :description="t('platformTree.empty')" />
    </a-spin>
  </div>
</template>

<style scoped>
.platform-tree {
  display: flex;
  flex-direction: column;
  height: 100%;
  min-height: 0;
}

.platform-tree :deep(.ant-spin-nested-loading),
.platform-tree :deep(.ant-spin-container) {
  height: 100%;
}

.platform-tree__list {
  height: 100%;
  background: transparent;
}

.platform-tree :deep(.ant-tree-list-holder-inner) {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.platform-tree :deep(.ant-tree-treenode) {
  width: 100%;
  padding: 0;
  align-items: stretch;
}

.platform-tree :deep(.ant-tree-switcher-noop) {
  width: 0;
}

.platform-tree :deep(.ant-tree-node-content-wrapper) {
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

.platform-tree :deep(.ant-tree-node-content-wrapper:hover) {
  border-color: var(--omes-color-primary-border);
  background: var(--omes-color-primary-bg-hover);
}

.platform-tree :deep(.ant-tree-node-content-wrapper.ant-tree-node-selected) {
  border-color: var(--omes-color-primary);
  background: var(--omes-color-primary-bg);
  box-shadow: 0 0 0 1px color-mix(in srgb, var(--omes-color-primary) 18%, transparent);
}

.platform-node {
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
}

.platform-node__name {
  font-weight: 600;
  color: var(--omes-color-text);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.platform-tree :deep(.ant-tree-node-selected) .platform-node__name {
  color: var(--omes-color-primary);
}

.platform-node__code {
  font-size: 12px;
  color: var(--omes-color-text-secondary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
</style>
