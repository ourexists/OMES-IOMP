<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { AppstoreOutlined, BarcodeOutlined, PlusOutlined } from '@ant-design/icons-vue'
import type { ProductRecord } from '@/api/product'
import {
  fetchProductById,
  resolveProductImageUrl,
  saveProduct,
  uploadProductImage,
} from '@/api/product'
import { message } from 'ant-design-vue'

const props = defineProps<{
  open: boolean
  record: ProductRecord | null
}>()

const emit = defineEmits<{
  'update:open': [value: boolean]
  success: []
}>()

const { t } = useI18n()

const loading = ref(false)
const saving = ref(false)
const uploading = ref(false)
const imagePreview = ref('')
const fileInputRef = ref<HTMLInputElement | null>(null)

const initialSnapshot = ref<Pick<typeof formState, 'name' | 'code' | 'imageUrl'>>({
  name: '',
  code: '',
  imageUrl: '',
})

const isEdit = computed(() => Boolean(props.record?.id))
const title = computed(() => (isEdit.value ? t('productPage.formEdit') : t('productPage.formAdd')))

const formState = reactive({
  id: '',
  name: '',
  code: '',
  imageUrl: '',
})

function snapshotForm() {
  initialSnapshot.value = {
    name: formState.name,
    code: formState.code,
    imageUrl: formState.imageUrl,
  }
  imagePreview.value = resolveProductImageUrl(formState.imageUrl)
}

watch(
  () => props.open,
  async (open) => {
    if (!open) {
      return
    }
    loading.value = true
    try {
      if (props.record?.id) {
        const detail = await fetchProductById(props.record.id)
        formState.id = detail?.id || props.record.id
        formState.name = detail?.name || ''
        formState.code = detail?.code || ''
        formState.imageUrl = detail?.imageUrl || ''
      } else {
        formState.id = ''
        formState.name = ''
        formState.code = ''
        formState.imageUrl = ''
      }
      snapshotForm()
    } finally {
      loading.value = false
    }
  },
)

function resetForm() {
  formState.name = initialSnapshot.value.name
  formState.code = initialSnapshot.value.code
  formState.imageUrl = initialSnapshot.value.imageUrl
  imagePreview.value = resolveProductImageUrl(formState.imageUrl)
}

function closeModal() {
  emit('update:open', false)
}

function openFilePicker() {
  if (!uploading.value) {
    fileInputRef.value?.click()
  }
}

async function onImageChange(file: File) {
  uploading.value = true
  try {
    const relativePath = await uploadProductImage(file)
    formState.imageUrl = relativePath
    imagePreview.value = resolveProductImageUrl(relativePath)
    message.success(t('productPage.uploadSuccess'))
  } finally {
    uploading.value = false
  }
}

function onFileInputChange(event: Event) {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  if (file) {
    onImageChange(file)
  }
  input.value = ''
}

function onImageError() {
  imagePreview.value = ''
}

async function handleSubmit() {
  const name = formState.name.trim()
  const code = formState.code.trim()
  if (!name) {
    message.warning(t('productPage.nameRequired'))
    return
  }
  if (!code) {
    message.warning(t('productPage.codeRequired'))
    return
  }
  saving.value = true
  try {
    await saveProduct({
      id: formState.id || undefined,
      name,
      code,
      imageUrl: formState.imageUrl || undefined,
    })
    message.success(t('productPage.saveSuccess'))
    emit('update:open', false)
    emit('success')
  } finally {
    saving.value = false
  }
}
</script>

<template>
  <a-modal
    :open="open"
    :title="title"
    width="640px"
    destroy-on-close
    class="product-form-modal"
    :footer="null"
    @cancel="closeModal"
    @update:open="emit('update:open', $event)"
  >
    <a-spin :spinning="loading">
      <a-form layout="vertical" class="product-form">
        <div class="form-section">
          <div class="section-title">
            <AppstoreOutlined />
            {{ t('productPage.formBasic') }}
          </div>

          <div class="form-layout">
            <div class="form-layout__image">
              <label class="image-label">{{ t('productPage.image') }}</label>
              <div
                class="image-box"
                :class="{ 'image-box--filled': !!imagePreview, 'image-box--busy': uploading }"
                role="button"
                tabindex="0"
                :aria-label="t('productPage.upload')"
                @click="openFilePicker"
                @keyup.enter="openFilePicker"
              >
                <a-spin v-if="uploading" class="image-spin" />
                <template v-else-if="imagePreview">
                  <img :src="imagePreview" alt="" class="image-preview" @error="onImageError" />
                  <div class="image-mask">
                    <PlusOutlined />
                    <span>{{ t('productPage.changeImage') }}</span>
                  </div>
                </template>
                <div v-else class="image-placeholder">
                  <div class="image-placeholder__icon">
                    <PlusOutlined />
                  </div>
                  <span class="image-placeholder__text">{{ t('productPage.upload') }}</span>
                </div>
              </div>
              <p class="image-hint">{{ t('productPage.imageHint') }}</p>
            </div>

            <div class="form-layout__fields">
              <a-form-item :label="t('productPage.name')" required>
                <a-input
                  v-model:value="formState.name"
                  allow-clear
                  :placeholder="t('productPage.namePlaceholder')"
                >
                  <template #prefix>
                    <AppstoreOutlined class="input-prefix-icon" />
                  </template>
                </a-input>
              </a-form-item>
              <a-form-item :label="t('productPage.code')" required>
                <a-input
                  v-model:value="formState.code"
                  allow-clear
                  :disabled="isEdit"
                  :placeholder="t('productPage.codePlaceholder')"
                >
                  <template #prefix>
                    <BarcodeOutlined class="input-prefix-icon" />
                  </template>
                </a-input>
                <div v-if="isEdit" class="field-hint">{{ t('productPage.codeEditHint') }}</div>
              </a-form-item>
            </div>
          </div>
        </div>
      </a-form>
    </a-spin>

    <input
      ref="fileInputRef"
      type="file"
      accept="image/*"
      tabindex="-1"
      aria-hidden="true"
      class="product-file-input"
      @change="onFileInputChange"
    />

    <div class="modal-footer">
      <a-button @click="resetForm">{{ t('productPage.reset') }}</a-button>
      <a-button @click="closeModal">{{ t('productPage.cancel') }}</a-button>
      <a-button type="primary" :loading="saving || uploading" @click="handleSubmit">
        {{ t('productPage.save') }}
      </a-button>
    </div>
  </a-modal>
</template>

<style scoped>
.product-form-modal :deep(.ant-modal-body) {
  padding: 16px 24px 0;
}

.product-form {
  padding-bottom: 8px;
}

.form-section {
  padding: 16px 18px;
  background: var(--omes-color-bg-elevated);
  border: 1px solid var(--omes-color-border);
  border-radius: var(--omes-radius-lg);
}

.section-title {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 16px;
  font-size: 14px;
  font-weight: 600;
  color: var(--omes-color-text);
}

.section-title::before {
  content: '';
  width: 3px;
  height: 16px;
  margin-right: 4px;
  background: var(--omes-color-primary);
  border-radius: 2px;
  flex-shrink: 0;
}

.section-title :deep(.anticon) {
  font-size: 15px;
  color: var(--omes-color-primary);
}

.form-layout {
  display: flex;
  align-items: flex-start;
  gap: 24px;
}

.form-layout__image {
  flex: 0 0 180px;
}

.form-layout__fields {
  flex: 1;
  min-width: 0;
  padding-top: 28px;
}

.image-label {
  display: block;
  margin-bottom: 8px;
  font-size: 14px;
  font-weight: 500;
  color: var(--omes-color-text-label);
}

.image-box {
  position: relative;
  width: 180px;
  height: 180px;
  border: 1px dashed #d9d9d9;
  border-radius: 12px;
  overflow: hidden;
  cursor: pointer;
  background: var(--omes-color-bg-container);
  transition: border-color 0.2s, box-shadow 0.2s;
}

.image-box:hover:not(.image-box--busy) {
  border-color: var(--omes-color-primary);
  box-shadow: 0 4px 12px rgba(22, 119, 255, 0.12);
}

.image-box--filled {
  border-style: solid;
  border-color: var(--omes-color-border);
}

.image-box--busy {
  cursor: wait;
}

.image-spin {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(255, 255, 255, 0.72);
  z-index: 2;
}

.image-placeholder {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  gap: 10px;
  color: var(--omes-color-text-quaternary);
}

.image-placeholder__icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 48px;
  height: 48px;
  font-size: 22px;
  color: var(--omes-color-primary);
  background: var(--omes-color-primary-bg);
  border-radius: 50%;
}

.image-placeholder__text {
  font-size: 13px;
}

.image-preview {
  width: 100%;
  height: 100%;
  object-fit: contain;
  background: var(--omes-color-bg-container);
}

.image-mask {
  position: absolute;
  inset: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 6px;
  font-size: 13px;
  color: #fff;
  background: var(--omes-color-text-quaternary);
  opacity: 0;
  transition: opacity 0.2s;
}

.image-box--filled:hover .image-mask {
  opacity: 1;
}

.image-hint {
  margin: 10px 0 0;
  font-size: 12px;
  line-height: 1.5;
  color: var(--omes-color-text-quaternary);
}


.product-form :deep(.ant-form-item) {
  margin-bottom: 16px;
}

.product-form :deep(.ant-form-item:last-child) {
  margin-bottom: 0;
}

.product-form :deep(.ant-form-item-label > label) {
  font-weight: 500;
  color: var(--omes-color-text-label);
}

.input-prefix-icon {
  color: var(--omes-color-text-placeholder);
}

.field-hint {
  margin-top: 6px;
  font-size: 12px;
  line-height: 1.5;
  color: var(--omes-color-text-quaternary);
}

.modal-footer {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  margin-top: 16px;
  padding: 16px 0 8px;
  border-top: 1px solid var(--omes-color-border);
}

@media (max-width: 576px) {
  .form-layout {
    flex-direction: column;
  }

  .form-layout__fields {
    padding-top: 0;
    width: 100%;
  }

  .form-layout__image,
  .image-box {
    width: 100%;
    max-width: 240px;
  }
}
</style>

<style>
/* 原生 file 控件完全隐藏；scoped 内部分浏览器仍会露出「选择文件」 */
.product-form-modal .product-file-input {
  display: none !important;
  visibility: hidden !important;
  position: fixed !important;
  left: -9999px !important;
  width: 0 !important;
  height: 0 !important;
  opacity: 0 !important;
  pointer-events: none !important;
}
</style>
