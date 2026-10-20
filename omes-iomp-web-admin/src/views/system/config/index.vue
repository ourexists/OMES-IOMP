<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import {
  CheckCircleOutlined,
  EnvironmentOutlined,
  ExclamationCircleOutlined,
  LinkOutlined,
  ReloadOutlined,
  SaveOutlined,
  SettingOutlined,
} from '@ant-design/icons-vue'
import { message } from 'ant-design-vue'
import AdminPanelTitle from '@/components/admin/AdminPanelTitle.vue'
import { fetchSystemConfig, saveSystemConfig } from '@/api/system-config'
import { setBaiduMapRuntimeAk } from '@/config/baidu-map'
import type { SystemConfigRecord } from '@/types/system-config'

const { t } = useI18n()

const loading = ref(false)
const saving = ref(false)
const form = reactive({
  baiduMapAk: '',
})
let cached: SystemConfigRecord | null = null
const initialAk = ref('')

const akConfigured = computed(() => form.baiduMapAk.trim().length > 0)
const dirty = computed(() => form.baiduMapAk.trim() !== initialAk.value)

async function loadConfig() {
  loading.value = true
  try {
    const data = await fetchSystemConfig()
    cached = data || null
    form.baiduMapAk = data?.config?.baiduMapAk?.trim() || ''
    initialAk.value = form.baiduMapAk
    if (form.baiduMapAk) {
      setBaiduMapRuntimeAk(form.baiduMapAk)
    }
  } catch {
    cached = null
    form.baiduMapAk = ''
    initialAk.value = ''
  } finally {
    loading.value = false
  }
}

async function onSave() {
  saving.value = true
  try {
    const payload: SystemConfigRecord = {
      id: cached?.id,
      configKey: cached?.configKey || 'app',
      config: {
        baiduMapAk: form.baiduMapAk.trim(),
      },
    }
    await saveSystemConfig(payload)
    setBaiduMapRuntimeAk(payload.config?.baiduMapAk)
    message.success(t('systemConfigPage.saveSuccess'))
    await loadConfig()
  } catch {
    // 错误已由 request 拦截器提示
  } finally {
    saving.value = false
  }
}

onMounted(() => {
  void loadConfig()
})
</script>

<template>
  <div class="admin-page system-module-page system-module-page--config">
    <a-card size="small" class="admin-panel-card panel-card">
      <template #title>
        <AdminPanelTitle :subtitle="t('systemConfigPage.subtitle')">
          <template #icon><SettingOutlined /></template>
          {{ t('systemConfigPage.title') }}
        </AdminPanelTitle>
      </template>
      <template #extra>
        <a-space :size="8" class="extra-tags">
          <a-tag v-if="dirty" color="warning">{{ t('systemConfigPage.unsaved') }}</a-tag>
          <a-tag :color="akConfigured ? 'success' : 'default'">
            {{
              akConfigured
                ? t('systemConfigPage.mapConfigured')
                : t('systemConfigPage.mapNotConfigured')
            }}
          </a-tag>
        </a-space>
      </template>

      <div class="admin-panel-body">
        <a-spin :spinning="loading">
          <a-form :model="form" layout="vertical" class="system-config-form" @finish="onSave">
            <section class="system-config-section">
              <header class="system-config-section__head">
                <span class="system-config-section__icon">
                  <EnvironmentOutlined />
                </span>
                <div class="system-config-section__meta">
                  <h3 class="system-config-section__title">
                    {{ t('systemConfigPage.mapSectionTitle') }}
                  </h3>
                  <p class="system-config-section__desc">
                    {{ t('systemConfigPage.mapSectionDesc') }}
                  </p>
                </div>
                <span
                  class="system-config-section__status"
                  :class="
                    akConfigured
                      ? 'system-config-section__status--ok'
                      : 'system-config-section__status--warn'
                  "
                >
                  <CheckCircleOutlined v-if="akConfigured" />
                  <ExclamationCircleOutlined v-else />
                  {{
                    akConfigured
                      ? t('systemConfigPage.mapConfigured')
                      : t('systemConfigPage.mapNotConfigured')
                  }}
                </span>
              </header>

              <div class="system-config-section__body">
                <a-form-item
                  :label="t('systemConfigPage.baiduMapAk')"
                  name="baiduMapAk"
                  class="system-config-form-item"
                >
                  <a-input
                    v-model:value="form.baiduMapAk"
                    class="system-config-ak-input"
                    size="large"
                    :placeholder="t('systemConfigPage.baiduMapAkPlaceholder')"
                    allow-clear
                  />
                  <p class="system-config-hint">
                    {{ t('systemConfigPage.baiduMapAkHint') }}
                    <a
                      class="system-config-hint__link"
                      href="https://lbsyun.baidu.com/apiconsole/key"
                      target="_blank"
                      rel="noopener noreferrer"
                    >
                      <LinkOutlined />
                      {{ t('systemConfigPage.openConsole') }}
                    </a>
                  </p>
                </a-form-item>
              </div>
            </section>

            <div class="system-config-actions">
              <a-space>
                <a-button
                  type="primary"
                  html-type="submit"
                  :loading="saving"
                  :disabled="!dirty && !saving"
                >
                  <template #icon><SaveOutlined /></template>
                  {{ t('systemConfigPage.save') }}
                </a-button>
                <a-button :disabled="loading || saving" @click="loadConfig">
                  <template #icon><ReloadOutlined /></template>
                  {{ t('systemConfigPage.refresh') }}
                </a-button>
              </a-space>
            </div>
          </a-form>
        </a-spin>
      </div>
    </a-card>
  </div>
</template>

<style scoped>
.system-config-form {
  max-width: 720px;
}

.system-config-section {
  margin-bottom: 16px;
  border: 1px solid var(--omes-color-border-secondary);
  border-radius: var(--omes-radius-lg);
  background: var(--omes-color-bg-container);
  overflow: hidden;
}

.system-config-section__head {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 14px 16px;
  background: linear-gradient(
    180deg,
    var(--omes-color-bg-elevated) 0%,
    var(--omes-color-bg-muted) 100%
  );
  border-bottom: 1px solid var(--omes-color-border-tertiary);
}

.system-config-section__icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  flex-shrink: 0;
  border-radius: 10px;
  color: #fff;
  font-size: 16px;
  background: linear-gradient(
    135deg,
    var(--omes-color-accent-cyan-from),
    var(--omes-color-accent-cyan-to)
  );
  box-shadow: 0 4px 10px color-mix(in srgb, var(--omes-color-accent-cyan-from) 28%, transparent);
}

.system-config-section__meta {
  flex: 1;
  min-width: 0;
}

.system-config-section__title {
  margin: 0;
  font-size: 14px;
  font-weight: 600;
  line-height: 22px;
  color: var(--omes-color-text-heading);
}

.system-config-section__desc {
  margin: 2px 0 0;
  font-size: 12px;
  line-height: 18px;
  color: var(--omes-color-text-quaternary);
}

.system-config-section__status {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  flex-shrink: 0;
  margin-top: 2px;
  padding: 2px 8px;
  border-radius: 999px;
  font-size: 12px;
  line-height: 20px;
  white-space: nowrap;
}

.system-config-section__status--ok {
  color: var(--omes-color-success);
  background: var(--omes-color-success-bg);
  border: 1px solid var(--omes-color-success-border);
}

.system-config-section__status--warn {
  color: var(--omes-color-warning);
  background: color-mix(in srgb, var(--omes-color-warning) 8%, var(--omes-color-bg-container));
  border: 1px solid color-mix(in srgb, var(--omes-color-warning) 28%, var(--omes-color-bg-container));
}

.system-config-section__body {
  padding: 16px 16px 8px;
}

.system-config-form-item {
  margin-bottom: 8px;
}

.system-config-ak-input :deep(.ant-input),
.system-config-ak-input.ant-input-affix-wrapper {
  font-family: ui-monospace, SFMono-Regular, Menlo, Consolas, monospace;
  letter-spacing: 0.02em;
}

.system-config-hint {
  margin: 8px 0 0;
  font-size: 12px;
  line-height: 1.6;
  color: var(--omes-color-text-quaternary);
}

.system-config-hint__link {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  margin-left: 6px;
  color: var(--omes-color-primary);
  white-space: nowrap;
}

.system-config-hint__link:hover {
  color: var(--omes-color-primary-hover);
}

.system-config-actions {
  display: flex;
  align-items: center;
  margin-top: 4px;
  padding-top: 14px;
  border-top: 1px dashed var(--omes-color-border-tertiary);
}

@media (max-width: 640px) {
  .system-config-section__head {
    flex-wrap: wrap;
  }

  .system-config-section__status {
    margin-left: 48px;
  }
}
</style>
