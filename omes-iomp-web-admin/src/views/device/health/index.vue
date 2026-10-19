<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import {
  ApartmentOutlined,
  CheckCircleOutlined,
  EditOutlined,
  HeartOutlined,
  PlusOutlined,
  ReloadOutlined,
} from '@ant-design/icons-vue'
import type { HealthRuleTemplate } from '@/api/equip-health'
import { computeHealthByTemplate, fetchHealthRuleTemplates } from '@/api/equip-health'
import HealthTemplateApplyModal from './components/HealthTemplateApplyModal.vue'
import HealthTemplateFormModal from './components/HealthTemplateFormModal.vue'
import TableScrollWrap from '@/components/common/TableScrollWrap.vue'
import AdminPanelTitle from '@/components/admin/AdminPanelTitle.vue'
import { message } from 'ant-design-vue'

const { t } = useI18n()

const loading = ref(false)
const computingId = ref<string | null>(null)
const dataSource = ref<HealthRuleTemplate[]>([])

const formOpen = ref(false)
const editingRecord = ref<HealthRuleTemplate | null>(null)
const applyOpen = ref(false)
const applyingRecord = ref<HealthRuleTemplate | null>(null)

const columns = computed(() => [
  { title: t('equipHealth.templateName'), dataIndex: 'name', key: 'name', ellipsis: true, minWidth: 160 },
  {
    title: t('equipHealth.periodHours'),
    key: 'periodHours',
    width: 130,
    align: 'center' as const,
  },
  {
    title: t('equipHealth.healthyThreshold'),
    key: 'healthyThreshold',
    width: 90,
    align: 'center' as const,
  },
  {
    title: t('equipHealth.attentionThreshold'),
    key: 'attentionThreshold',
    width: 90,
    align: 'center' as const,
  },
  {
    title: t('equipHealth.warningThreshold'),
    key: 'warningThreshold',
    width: 90,
    align: 'center' as const,
  },
  { title: t('healthPage.operate'), key: 'action', width: 250, fixed: 'right' as const },
])

function configValue(record: HealthRuleTemplate, field: keyof NonNullable<HealthRuleTemplate['config']>) {
  const top = record[field as keyof HealthRuleTemplate]
  if (top != null && typeof top !== 'object') {
    return top
  }
  return record.config?.[field]
}

async function loadTable() {
  loading.value = true
  try {
    const list = await fetchHealthRuleTemplates()
    dataSource.value = Array.isArray(list) ? list : []
  } finally {
    loading.value = false
  }
}

function openCreate() {
  editingRecord.value = null
  formOpen.value = true
}

function openEdit(record: HealthRuleTemplate) {
  editingRecord.value = record
  formOpen.value = true
}

function openApply(record: HealthRuleTemplate) {
  applyingRecord.value = record
  applyOpen.value = true
}

async function startScoring(record: HealthRuleTemplate) {
  if (!record.id) {
    return
  }
  computingId.value = record.id
  try {
    const result = await computeHealthByTemplate(record.id)
    const count = result?.count ?? 0
    const msg =
      result?.message ||
      t('equipHealth.computeByTemplateDone', { count })
    message.success(msg)
  } finally {
    computingId.value = null
  }
}

onMounted(loadTable)
</script>

<template>
  <div class="admin-page health-page">
    <a-card size="small" class="admin-panel-card panel-card">
      <template #title>
        <AdminPanelTitle>
          <template #icon><HeartOutlined /></template>
          {{ t('healthPage.title', t('menu.equipHealthTemplateList', '健康评分')) }}
        </AdminPanelTitle>
      </template>
      <template #extra>
        <a-tag v-if="dataSource.length" color="processing">
          {{ t('healthPage.total', { count: dataSource.length }) }}
        </a-tag>
      </template>

      <div class="admin-panel-body">
      <div class="table-toolbar">
        <a-space wrap>
          <a-button type="primary" @click="openCreate">
            <template #icon><PlusOutlined /></template>
            {{ t('healthPage.add') }}
          </a-button>
          <a-button :loading="loading" @click="loadTable">
            <template #icon><ReloadOutlined /></template>
            {{ t('healthPage.refresh') }}
          </a-button>
        </a-space>
      </div>

      <TableScrollWrap :refresh-keys="[dataSource.length]">
        <template #default="{ scrollY }">
      <a-table
        row-key="id"
        size="middle"
        bordered
        class="scroll-table health-table"
        :columns="columns"
        :data-source="dataSource"
        :scroll="{ x: 980, y: scrollY }"
        :pagination="false"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'periodHours'">
            {{ configValue(record, 'periodHours') ?? '-' }}
          </template>
          <template v-else-if="column.key === 'healthyThreshold'">
            {{ configValue(record, 'healthyThreshold') ?? '-' }}
          </template>
          <template v-else-if="column.key === 'attentionThreshold'">
            {{ configValue(record, 'attentionThreshold') ?? '-' }}
          </template>
          <template v-else-if="column.key === 'warningThreshold'">
            {{ configValue(record, 'warningThreshold') ?? '-' }}
          </template>
          <template v-else-if="column.key === 'action'">
            <a-space wrap size="small" class="action-group">
              <a-button type="link" size="small" @click="openEdit(record)">
                <EditOutlined />
                {{ t('healthPage.edit') }}
              </a-button>
              <a-button
                type="link"
                size="small"
                :loading="computingId === record.id"
                @click="startScoring(record)"
              >
                <CheckCircleOutlined />
                {{ t('equipHealth.startScoring') }}
              </a-button>
              <a-button type="link" size="small" @click="openApply(record)">
                <ApartmentOutlined />
                {{ t('healthPage.apply') }}
              </a-button>
            </a-space>
          </template>
        </template>

        <template #emptyText>
          <a-empty :description="t('healthPage.empty', '暂无健康规则模板')">
            <a-button type="primary" @click="openCreate">
              <PlusOutlined />
              {{ t('healthPage.add') }}
            </a-button>
          </a-empty>
        </template>
      </a-table>
        </template>
      </TableScrollWrap>
      </div>
    </a-card>

    <HealthTemplateFormModal v-model:open="formOpen" :record="editingRecord" @success="loadTable" />
    <HealthTemplateApplyModal v-model:open="applyOpen" :template="applyingRecord" />
  </div>
</template>

<style scoped>
.health-page {
  /* layout via .admin-page */
}

.panel-card {
  border-radius: var(--omes-radius-md);
  box-shadow: var(--omes-shadow-card-sm);
}

.panel-card :deep(.ant-card-head) {
  min-height: 48px;
  border-bottom: 1px solid var(--omes-color-border);
}

.card-title {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  font-weight: 600;
}

.table-toolbar {
  margin-bottom: 16px;
}

.health-table :deep(.ant-table) {
  border-radius: var(--omes-radius-md);
  overflow: hidden;
}

.health-table :deep(.ant-table-thead > tr > th) {
  background: var(--omes-color-bg-elevated);
  font-weight: 600;
}

.health-table :deep(.ant-table-tbody > tr > td) {
  vertical-align: middle;
}

.action-group :deep(.ant-btn-link) {
  padding-inline: 4px;
}
</style>
