<script setup lang="ts">
import { ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import type { RoleRecord } from '@/api/ucenter'
import { bindAccountRoles, fetchAllRoles, fetchRolesByAccount } from '@/api/ucenter'
import { message } from 'ant-design-vue'

const props = defineProps<{
  open: boolean
  accountId: string
}>()

const emit = defineEmits<{
  'update:open': [value: boolean]
}>()

const { t } = useI18n()

const loading = ref(false)
const roleOptions = ref<RoleRecord[]>([])
const checkedRoleIds = ref<string[]>([])

async function loadData() {
  if (!props.accountId) {
    return
  }
  loading.value = true
  roleOptions.value = []
  checkedRoleIds.value = []
  try {
    // 分拆请求：已持有角色失败时仍展示可分配列表
    try {
      const roles = await fetchAllRoles()
      roleOptions.value = Array.isArray(roles) ? roles : []
    } catch {
      roleOptions.value = []
    }
    try {
      const held = await fetchRolesByAccount(props.accountId)
      checkedRoleIds.value = (Array.isArray(held) ? held : []).map((item) => String(item.id))
    } catch {
      checkedRoleIds.value = []
    }
  } finally {
    loading.value = false
  }
}

watch(
  () => [props.open, props.accountId] as const,
  ([open]) => {
    if (open) {
      loadData()
    }
  },
)

async function handleSubmit() {
  await bindAccountRoles({
    accId: props.accountId,
    roleIds: checkedRoleIds.value,
  })
  message.success(t('accountPage.assignSuccess'))
  emit('update:open', false)
}
</script>

<template>
  <a-modal
    :open="open"
    :title="t('accountPage.assignRole')"
    width="520px"
    :confirm-loading="loading"
    destroy-on-close
    @update:open="emit('update:open', $event)"
    @ok="handleSubmit"
  >
    <a-spin :spinning="loading">
      <div v-if="roleOptions.length" class="role-list">
        <a-checkbox-group v-model:value="checkedRoleIds" class="role-group">
          <label v-for="role in roleOptions" :key="role.id" class="role-item">
            <a-checkbox :value="String(role.id)">
              <span class="role-name">{{ role.name }}</span>
              <span class="role-code">{{ role.code }}</span>
            </a-checkbox>
          </label>
        </a-checkbox-group>
      </div>
      <a-empty v-else :description="t('accountPage.noRoles')" />
    </a-spin>
  </a-modal>
</template>

<style scoped>
.role-list {
  max-height: 420px;
  overflow: auto;
}

.role-group {
  display: flex;
  flex-direction: column;
  gap: 8px;
  width: 100%;
}

.role-item {
  display: block;
  padding: 10px 12px;
  border: 1px solid var(--omes-color-border);
  border-radius: var(--omes-radius-md);
  transition: background 0.2s, border-color 0.2s;
}

.role-item:hover {
  background: var(--omes-color-primary-bg-hover);
  border-color: var(--omes-color-primary-border);
}

.role-name {
  margin-right: 8px;
  font-weight: 500;
}

.role-code {
  font-size: 12px;
  color: var(--omes-color-text-quaternary);
  font-family: ui-monospace, SFMono-Regular, Menlo, Consolas, monospace;
}
</style>
