<script setup lang="ts">
import { computed, reactive, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import dayjs, { type Dayjs } from 'dayjs'
import { CalendarOutlined, IdcardOutlined, UserOutlined } from '@ant-design/icons-vue'
import type { AccountRecord, PlatformNode } from '@/api/ucenter'
import { modifyAccount, registerAccount } from '@/api/ucenter'
import { message } from 'ant-design-vue'

const props = defineProps<{
  open: boolean
  record: AccountRecord | null
  platform: PlatformNode | null
}>()

const emit = defineEmits<{
  'update:open': [value: boolean]
  success: []
}>()

const { t } = useI18n()

const isEdit = computed(() => Boolean(props.record?.id))
const title = computed(() =>
  isEdit.value ? t('accountPage.formEdit') : t('accountPage.formAdd'),
)

const formState = reactive({
  accName: '',
  password: '',
  nickName: '',
  mobile: '',
  email: '',
  userName: '',
  idCard: '',
  sex: 1,
  settledTime: null as Dayjs | null,
  expireTime: null as Dayjs | null,
})

function resetForm() {
  formState.accName = ''
  formState.password = ''
  formState.nickName = ''
  formState.mobile = ''
  formState.email = ''
  formState.userName = ''
  formState.idCard = ''
  formState.sex = 1
  formState.settledTime = null
  formState.expireTime = null
}

watch(
  () => props.open,
  (open) => {
    if (!open) {
      return
    }
    resetForm()
    if (props.record) {
      formState.accName = props.record.accName || ''
      formState.nickName = props.record.nickName || ''
      formState.mobile = props.record.mobile || ''
      formState.email = props.record.email || ''
      formState.userName = props.record.userName || ''
      formState.idCard = props.record.idCard || ''
      formState.sex = props.record.sex ?? 1
      formState.settledTime = props.record.settledTime ? dayjs(props.record.settledTime) : null
      formState.expireTime = props.record.expireTime ? dayjs(props.record.expireTime) : null
    }
  },
)

async function handleSubmit() {
  if (!props.platform?.code) {
    message.warning(t('accountPage.selectPlatform'))
    return
  }

  const payload: AccountRecord = {
    ...(props.record || { id: '' }),
    accName: formState.accName,
    nickName: formState.nickName,
    mobile: formState.mobile,
    email: formState.email,
    userName: formState.userName,
    idCard: formState.idCard,
    sex: formState.sex,
    platform: props.platform.code,
    settledTime: formState.settledTime?.format('YYYY-MM-DD HH:mm:ss'),
    expireTime: formState.expireTime?.format('YYYY-MM-DD HH:mm:ss'),
  }

  if (isEdit.value) {
    await modifyAccount(payload)
  } else {
    payload.password = formState.password
    payload.source = 'platform'
    await registerAccount(payload)
  }

  message.success(t('accountPage.saveSuccess'))
  emit('update:open', false)
  emit('success')
}
</script>

<template>
  <a-modal
    :open="open"
    width="760px"
    destroy-on-close
    class="admin-form-modal"
    @update:open="emit('update:open', $event)"
    @ok="handleSubmit"
  >
    <template #title>
      <span class="modal-title">
        <span class="modal-title__icon modal-title__icon--purple">
          <UserOutlined />
        </span>
        {{ title }}
      </span>
    </template>
    <a-form layout="vertical" class="system-form">
      <div class="system-form-section">
        <div class="system-form-section__title">
          <UserOutlined />
          {{ t('accountPage.formLogin') }}
        </div>
        <a-form-item :label="t('accountPage.account')" required>
          <a-input
            v-model:value="formState.accName"
            :disabled="isEdit"
            :placeholder="t('accountPage.accountPlaceholder')"
          />
        </a-form-item>
        <a-form-item v-if="!isEdit" :label="t('accountPage.password')" required>
          <a-input-password v-model:value="formState.password" :placeholder="t('accountPage.passwordPlaceholder')" />
        </a-form-item>
      </div>

      <div class="system-form-section">
        <div class="system-form-section__title">
          <IdcardOutlined />
          {{ t('accountPage.formProfile') }}
        </div>
        <a-row :gutter="16">
          <a-col :span="8">
            <a-form-item :label="t('accountPage.colNickname')" required>
              <a-input v-model:value="formState.nickName" :placeholder="t('accountPage.nicknamePlaceholder')" />
            </a-form-item>
          </a-col>
          <a-col :span="8">
            <a-form-item :label="t('accountPage.mobile')">
              <a-input v-model:value="formState.mobile" :placeholder="t('accountPage.mobilePlaceholder')" />
            </a-form-item>
          </a-col>
          <a-col :span="8">
            <a-form-item :label="t('accountPage.email')">
              <a-input v-model:value="formState.email" :placeholder="t('accountPage.emailPlaceholder')" />
            </a-form-item>
          </a-col>
        </a-row>
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item :label="t('accountPage.realName')">
              <a-input v-model:value="formState.userName" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item :label="t('accountPage.idCard')">
              <a-input v-model:value="formState.idCard" />
            </a-form-item>
          </a-col>
        </a-row>
        <a-form-item :label="t('accountPage.sex')">
          <a-radio-group v-model:value="formState.sex">
            <a-radio :value="1">{{ t('accountPage.male') }}</a-radio>
            <a-radio :value="2">{{ t('accountPage.female') }}</a-radio>
          </a-radio-group>
        </a-form-item>
      </div>

      <div class="system-form-section">
        <div class="system-form-section__title">
          <CalendarOutlined />
          {{ t('accountPage.formValidity') }}
        </div>
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item :label="t('accountPage.colSettledTime')">
              <a-date-picker v-model:value="formState.settledTime" show-time style="width: 100%" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item :label="t('accountPage.colExpireTime')">
              <a-date-picker v-model:value="formState.expireTime" show-time style="width: 100%" />
            </a-form-item>
          </a-col>
        </a-row>
      </div>
    </a-form>
  </a-modal>
</template>
