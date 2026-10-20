<script setup lang="ts">
import { computed, onMounted, ref, shallowRef, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import {
  DeleteOutlined,
  EditOutlined,
  FolderOutlined,
  PlusOutlined,
  SearchOutlined,
} from '@ant-design/icons-vue'
import type { BomClassifyNode } from '@/api/bom'
import { fetchBomClassifyTree } from '@/api/bom'
import {
  buildBomTreeIndex,
  collectBomExpandableKeys,
  collectBomSearchMatchKeys,
  filterBomTree,
} from '@/utils/bom-tree'

defineProps<{
  readonly?: boolean
}>()

const emit = defineEmits<{
  select: [node: BomClassifyNode | null]
  loaded: [payload: { nodeCount: number }]
  edit: [node: BomClassifyNode]
  addChild: [node: BomClassifyNode]
  delete: [node: BomClassifyNode]
}>()

const { t } = useI18n()

const loading = ref(false)
const treeData = shallowRef<BomClassifyNode[]>([])
const nodeByCode = shallowRef(new Map<string, BomClassifyNode>())

const selectedKeys = ref<string[]>([])
const expandedKeys = ref<string[]>([])
const searchKeyword = ref('')

const displayTreeData = computed(() => {
  const keyword = searchKeyword.value.trim().toLowerCase()
  if (!keyword) {
    return treeData.value
  }
  return filterBomTree(treeData.value, keyword)
})

const searchActive = computed(() => Boolean(searchKeyword.value.trim()))
const searchMatchKeys = computed(() => {
  const keyword = searchKeyword.value.trim().toLowerCase()
  if (!keyword) {
    return null
  }
  return collectBomSearchMatchKeys(treeData.value, keyword)
})

function syncExpandedOnSearch() {
  const keyword = searchKeyword.value.trim()
  if (keyword) {
    expandedKeys.value = collectBomExpandableKeys(displayTreeData.value)
  }
}

function onSelect(keys: string[]) {
  selectedKeys.value = keys
  if (!keys.length) {
    emit('select', null)
    return
  }
  const node = nodeByCode.value.get(keys[0])
  emit('select', node || null)
}

async function loadTree() {
  loading.value = true
  try {
    const list = await fetchBomClassifyTree()
    treeData.value = Array.isArray(list) ? list : []
    const index = buildBomTreeIndex(treeData.value)
    nodeByCode.value = index.nodeByCode
    expandedKeys.value = collectBomExpandableKeys(treeData.value, 1)
    emit('loaded', { nodeCount: index.nodeCount })
    const current = selectedKeys.value[0]
    const node = current ? nodeByCode.value.get(current) : undefined
    if (node) {
      emit('select', node)
    } else if (treeData.value[0]?.selfCode) {
      selectedKeys.value = [treeData.value[0].selfCode]
      emit('select', treeData.value[0])
    } else if (current) {
      selectedKeys.value = []
      emit('select', null)
    }
  } finally {
    loading.value = false
  }
}

defineExpose({ reload: loadTree, getSelectedNode: () => nodeByCode.value.get(selectedKeys.value[0] || '') })

watch(searchKeyword, syncExpandedOnSearch)

onMounted(loadTree)
</script>

<template>
  <div class="bom-classify-panel">
    <a-input
      v-model:value="searchKeyword"
      allow-clear
      class="bom-classify-panel__search"
      :placeholder="t('bomPage.classifySearch')"
    >
      <template #prefix>
        <SearchOutlined class="search-icon" />
      </template>
    </a-input>

    <a-spin :spinning="loading" class="bom-classify-panel__spin tree-spin">
      <div
        class="tree-scroll"
        :class="{ 'is-filtering': searchActive }"
      >
        <a-tree
          v-if="displayTreeData.length"
          v-model:selected-keys="selectedKeys"
          v-model:expanded-keys="expandedKeys"
          block-node
          :tree-data="displayTreeData"
          :field-names="{ title: 'name', key: 'selfCode', children: 'children' }"
          @select="onSelect"
        >
          <template #title="node">
            <div
              class="tree-node-row"
              :class="{ 'tree-node-row--selected': selectedKeys.includes(node.selfCode) }"
            >
              <span
                class="tree-node"
                :class="{
                  'is-leaf': !node.children?.length,
                  'is-match': searchMatchKeys?.has(node.selfCode),
                }"
              >
                <span class="node-icon-wrap" :class="node.children?.length ? 'folder' : 'leaf'">
                  <FolderOutlined />
                </span>
                <span class="node-name" :title="node.name">{{ node.name || node.selfCode }}</span>
                <span v-if="node.selfCode" class="code-tag" :title="node.selfCode">{{ node.selfCode }}</span>
              </span>
              <div v-if="!readonly" class="tree-node-actions" @click.stop>
                <a-tooltip :title="t('bomPage.edit')">
                  <a-button type="text" size="small" class="tree-node-action-btn" @click="emit('edit', node)">
                    <EditOutlined />
                  </a-button>
                </a-tooltip>
                <a-tooltip :title="t('bomPage.classifyAddChild')">
                  <a-button type="text" size="small" class="tree-node-action-btn" @click="emit('addChild', node)">
                    <PlusOutlined />
                  </a-button>
                </a-tooltip>
                <a-tooltip :title="t('bomPage.delete')">
                  <a-button type="text" size="small" danger class="tree-node-action-btn" @click="emit('delete', node)">
                    <DeleteOutlined />
                  </a-button>
                </a-tooltip>
              </div>
            </div>
          </template>
        </a-tree>
        <a-empty v-else :description="t('bomPage.classifyEmpty')" class="tree-empty" />
      </div>
    </a-spin>
  </div>
</template>

<style scoped>
.bom-classify-panel {
  display: flex;
  flex-direction: column;
  flex: 1;
  min-height: 0;
  height: 100%;
}

.bom-classify-panel__search {
  flex-shrink: 0;
  margin-bottom: 12px;
}

.search-icon {
  color: var(--omes-color-text-placeholder);
}

.bom-classify-panel__spin {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
}

.bom-classify-panel__spin :deep(.ant-spin-container) {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
}

.tree-scroll {
  flex: 1;
  min-height: 200px;
  box-sizing: border-box;
  overflow-x: hidden;
  overflow-y: auto;
  padding: 6px 4px 6px 6px;
  border: 1px solid var(--omes-color-border-secondary);
  border-radius: var(--omes-radius-lg);
  background: linear-gradient(180deg, #f8faff 0%, #fff 24%);
  transition: border-color 0.2s ease;
}

.tree-scroll::-webkit-scrollbar {
  width: 6px;
}

.tree-scroll::-webkit-scrollbar-thumb {
  background: rgba(22, 119, 255, 0.35);
  border-radius: 3px;
}

.tree-scroll.is-filtering {
  border-color: #91caff;
  background: linear-gradient(180deg, #f0f7ff 0%, #fff 28%);
}

.tree-scroll :deep(.ant-tree) {
  background: transparent;
  font-size: 13px;
}

.tree-scroll :deep(.ant-tree-treenode) {
  align-items: center;
  padding: 2px 0;
  width: 100%;
}

.tree-scroll :deep(.ant-tree-node-content-wrapper) {
  flex: 1;
  min-width: 0;
  display: flex !important;
  align-items: center;
  min-height: 36px;
  line-height: 36px;
  padding: 0 8px !important;
  border-radius: var(--omes-radius-md);
  transition: background 0.15s ease;
}

.tree-scroll :deep(.ant-tree-node-content-wrapper:hover) {
  background: rgba(22, 119, 255, 0.06);
}

.tree-scroll :deep(.ant-tree-treenode-selected .ant-tree-node-content-wrapper) {
  background: var(--omes-color-primary-bg) !important;
  border-left: 3px solid var(--omes-color-primary);
  padding-left: 5px !important;
}

.tree-scroll :deep(.ant-tree-switcher) {
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--omes-color-text-placeholder);
}

.tree-scroll :deep(.ant-tree-title) {
  flex: 1;
  min-width: 0;
  overflow: visible;
}

.tree-node-row {
  display: flex;
  align-items: center;
  gap: 4px;
  width: 100%;
  min-width: 0;
  padding-right: 2px;
}

.tree-node {
  display: flex;
  align-items: center;
  gap: 8px;
  flex: 1;
  min-width: 0;
}

.tree-node-actions {
  display: inline-flex;
  align-items: center;
  flex-shrink: 0;
  gap: 0;
}

.tree-node-action-btn {
  display: inline-flex !important;
  align-items: center;
  justify-content: center;
  width: 26px !important;
  height: 26px !important;
  padding: 0 !important;
  border-radius: var(--omes-radius-sm);
}

.tree-node-action-btn:hover {
  background: rgba(0, 0, 0, 0.04);
}

.tree-node-action-btn.ant-btn-dangerous:hover {
  background: #fff1f0;
}

.tree-node-action-btn :deep(.anticon) {
  font-size: 13px;
}

.tree-node.is-match .node-name {
  color: var(--omes-color-primary);
  font-weight: 600;
}

.tree-node.is-match .code-tag {
  background: #fff7e6;
  color: #d46b08;
  border: 1px solid #ffd591;
}

.node-icon-wrap {
  flex-shrink: 0;
  width: 26px;
  height: 26px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 7px;
  font-size: 14px;
}

.node-icon-wrap.folder {
  background: var(--omes-color-primary-bg);
  color: var(--omes-color-primary);
}

.node-icon-wrap.leaf {
  background: #f6ffed;
  color: var(--omes-color-success);
}

.node-name {
  flex: 1;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  color: var(--omes-color-text);
}

.code-tag {
  flex-shrink: 0;
  max-width: 88px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 11px;
  font-family: ui-monospace, SFMono-Regular, Menlo, Consolas, monospace;
  color: var(--omes-color-text-quaternary);
  background: var(--omes-color-bg-layout);
  border: 1px solid var(--omes-color-border);
  padding: 0 6px;
  border-radius: 4px;
  line-height: 20px;
}

.tree-empty {
  margin: 48px 0;
}
</style>
