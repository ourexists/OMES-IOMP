<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import type { PermissionRecord, PlatformNode } from '@/api/ucenter'
import {
  assignRolePermissions,
  fetchPlatforms,
  fetchRolePermissions,
  fetchTenantPermissionTree,
} from '@/api/ucenter'
import { OAUTH_CLIENT } from '@/config'
import { resolvePermissionLabel } from '@/utils/i18n-helper'
import { message } from 'ant-design-vue'

interface TreeCheckNode {
  key: string
  title: string
  children?: TreeCheckNode[]
}

const props = defineProps<{
  open: boolean
  roleId: string
}>()

const emit = defineEmits<{
  'update:open': [value: boolean]
}>()

const { t } = useI18n()

const loading = ref(false)
const treeLoading = ref(false)
const platforms = ref<PlatformNode[]>([])
const selectedPlatformCode = ref<string>()
const currentTreeData = ref<TreeCheckNode[]>([])
const permissionIdsByPlatform = ref<Record<string, string[]>>({})
const permissionTreeCache = ref<Record<string, TreeCheckNode[]>>({})
const checkedPermissionIds = ref<string[]>([])

function collectPermissionIds(nodes: PermissionRecord[] = []): string[] {
  const ids: string[] = []
  const walk = (list: PermissionRecord[]) => {
    for (const node of list) {
      ids.push(node.id)
      if (node.children?.length) {
        walk(node.children)
      }
    }
  }
  walk(nodes)
  return ids
}

function toTreeNodes(nodes: PermissionRecord[] = []): TreeCheckNode[] {
  return nodes.map((node) => ({
    key: node.id,
    title: resolvePermissionLabel(node),
    children: node.children?.length ? toTreeNodes(node.children) : undefined,
  }))
}

const currentPlatformCheckedKeys = computed(() => {
  const platformCode = selectedPlatformCode.value
  if (!platformCode) {
    return { checked: [] as string[], halfChecked: [] as string[] }
  }
  const platformIds = permissionIdsByPlatform.value[platformCode] || []
  return {
    checked: checkedPermissionIds.value.filter((id) => platformIds.includes(id)),
    halfChecked: [] as string[],
  }
})

function syncPlatformCheckedKeys(keys: string[]) {
  const platformCode = selectedPlatformCode.value
  if (!platformCode) {
    return
  }
  const platformIds = permissionIdsByPlatform.value[platformCode] || []
  const others = checkedPermissionIds.value.filter((id) => !platformIds.includes(id))
  checkedPermissionIds.value = [...others, ...keys]
}

function findTreeNode(nodes: TreeCheckNode[], key: string): TreeCheckNode | undefined {
  for (const node of nodes) {
    if (node.key === key) {
      return node
    }
    if (node.children?.length) {
      const found = findTreeNode(node.children, key)
      if (found) {
        return found
      }
    }
  }
}

function collectDescendantKeys(node: TreeCheckNode): string[] {
  const keys: string[] = []
  const walk = (list: TreeCheckNode[]) => {
    for (const child of list) {
      keys.push(child.key)
      if (child.children?.length) {
        walk(child.children)
      }
    }
  }
  if (node.children?.length) {
    walk(node.children)
  }
  return keys
}

function handleTreeCheck(
  keys: string[] | { checked: string[]; halfChecked: string[] },
  info: { checked: boolean; node: { key: string | number } },
) {
  const checked = Array.isArray(keys) ? keys : keys.checked
  const nodeKey = String(info.node.key)
  const node = findTreeNode(currentTreeData.value, nodeKey)
  const descendants = node ? collectDescendantKeys(node) : []

  if (info.checked) {
    syncPlatformCheckedKeys([...new Set([...checked, ...descendants])])
    return
  }

  const remove = new Set([nodeKey, ...descendants])
  syncPlatformCheckedKeys(checked.filter((id) => !remove.has(id)))
}

async function loadPlatformTree(platform: PlatformNode) {
  if (permissionTreeCache.value[platform.code]) {
    currentTreeData.value = permissionTreeCache.value[platform.code]
    return
  }

  treeLoading.value = true
  try {
    const children = await fetchTenantPermissionTree(OAUTH_CLIENT.tenantId, platform.code)
    const records = Array.isArray(children) ? children : []
    const treeNodes = toTreeNodes(records)
    permissionTreeCache.value[platform.code] = treeNodes
    permissionIdsByPlatform.value[platform.code] = collectPermissionIds(records)
    currentTreeData.value = treeNodes
  } finally {
    treeLoading.value = false
  }
}

async function selectPlatform(platform: PlatformNode) {
  selectedPlatformCode.value = platform.code
  await loadPlatformTree(platform)
}

async function loadData() {
  if (!props.roleId) {
    return
  }
  loading.value = true
  try {
    platforms.value = []
    selectedPlatformCode.value = undefined
    currentTreeData.value = []
    permissionIdsByPlatform.value = {}
    permissionTreeCache.value = {}

    const [platformList, held] = await Promise.all([
      fetchPlatforms(),
      fetchRolePermissions(props.roleId),
    ])

    platforms.value = Array.isArray(platformList) ? platformList : []
    checkedPermissionIds.value = (Array.isArray(held) ? held : []).map((item) => item.id)

    if (platforms.value.length) {
      await selectPlatform(platforms.value[0])
    }
  } finally {
    loading.value = false
  }
}

watch(
  () => props.open,
  (open) => {
    if (open) {
      loadData()
    }
  },
)

async function handleSubmit() {
  await assignRolePermissions({
    id: props.roleId,
    permissionIds: checkedPermissionIds.value,
  })
  message.success(t('rolePage.assignSuccess'))
  emit('update:open', false)
}
</script>

<template>
  <a-modal
    :open="open"
    :title="t('rolePage.assign')"
    width="860px"
    :confirm-loading="loading"
    destroy-on-close
    class="role-assign-modal"
    @update:open="emit('update:open', $event)"
    @ok="handleSubmit"
  >
    <a-spin :spinning="loading">
      <div v-if="platforms.length" class="assign-layout">
        <div class="platform-pane">
          <div class="pane-title">{{ t('permissionPage.platform') }}</div>
          <div class="platform-list">
            <button
              v-for="platform in platforms"
              :key="platform.code"
              type="button"
              class="platform-item"
              :class="{ active: selectedPlatformCode === platform.code }"
              @click="selectPlatform(platform)"
            >
              <span class="platform-name">{{ platform.name }}</span>
              <span class="platform-code">{{ platform.code }}</span>
            </button>
          </div>
        </div>

        <div class="tree-pane">
          <div class="pane-title">{{ t('rolePage.assignTree') }}</div>
          <a-spin :spinning="treeLoading">
            <div class="permission-tree-panel">
              <a-tree
                v-if="currentTreeData.length"
                :checked-keys="currentPlatformCheckedKeys"
                checkable
                check-strictly
                default-expand-all
                :tree-data="currentTreeData"
                @check="handleTreeCheck"
              />
              <a-empty v-else :description="t('rolePage.noPermissions')" />
            </div>
          </a-spin>
        </div>
      </div>
      <a-empty v-else :description="t('platformTree.empty')" />
    </a-spin>
  </a-modal>
</template>

<style scoped>
.assign-layout {
  display: grid;
  grid-template-columns: 220px 1fr;
  gap: 16px;
  min-height: 420px;
}

.pane-title {
  margin-bottom: 10px;
  font-size: 13px;
  font-weight: 600;
  color: var(--omes-color-text-secondary);
}

.platform-pane {
  display: flex;
  flex-direction: column;
  min-height: 0;
}

.platform-list {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 8px;
  overflow: auto;
  border: 1px solid var(--omes-color-border);
  border-radius: var(--omes-radius-md);
  background: var(--omes-color-bg-elevated);
}

.platform-item {
  display: flex;
  flex-direction: column;
  gap: 2px;
  width: 100%;
  padding: 10px 12px;
  text-align: left;
  border: 1px solid transparent;
  border-radius: var(--omes-radius-md);
  background: var(--omes-color-bg-container);
  cursor: pointer;
  transition: background 0.2s, border-color 0.2s, box-shadow 0.2s;
}

.platform-item:hover {
  border-color: var(--omes-color-primary-border);
  background: var(--omes-color-primary-bg-hover);
}

.platform-item.active {
  border-color: var(--omes-color-primary);
  background: #f0f5ff;
  box-shadow: 0 0 0 1px rgba(22, 119, 255, 0.15);
}

.platform-name {
  font-weight: 500;
  color: var(--omes-color-text);
}

.platform-code {
  font-size: 12px;
  color: var(--omes-color-text-quaternary);
  font-family: ui-monospace, SFMono-Regular, Menlo, Consolas, monospace;
}

.tree-pane {
  display: flex;
  flex-direction: column;
  min-width: 0;
  min-height: 0;
}

.permission-tree-panel {
  flex: 1;
  min-height: 380px;
  max-height: 480px;
  padding: 12px;
  overflow: auto;
  border: 1px solid var(--omes-color-border);
  border-radius: var(--omes-radius-md);
  background: var(--omes-color-bg-elevated);
}

.permission-tree-panel :deep(.ant-tree) {
  background: transparent;
}

.permission-tree-panel :deep(.ant-tree-treenode) {
  padding: 2px 0;
}

.permission-tree-panel :deep(.ant-tree-node-content-wrapper) {
  border-radius: 4px;
}

.permission-tree-panel :deep(.ant-tree-node-content-wrapper:hover) {
  background: var(--omes-color-primary-bg-hover);
}

@media (max-width: 768px) {
  .assign-layout {
    grid-template-columns: 1fr;
  }

  .platform-list {
    max-height: 160px;
  }
}
</style>
