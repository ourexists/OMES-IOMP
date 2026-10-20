<script setup lang="ts">
import { computed } from 'vue'
import { PictureOutlined } from '@ant-design/icons-vue'
import { resolveProductImageUrl } from '@/api/product'

const props = defineProps<{
  path?: string
}>()

const url = computed(() => resolveProductImageUrl(props.path))

function onError(event: Event) {
  ;(event.target as HTMLImageElement).style.display = 'none'
}
</script>

<template>
  <span class="equip-product-thumb">
    <PictureOutlined class="equip-product-thumb__ph" />
    <img v-if="url" :key="url" :src="url" alt="" @error="onError" />
  </span>
</template>

<style scoped>
.equip-product-thumb {
  position: relative;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  height: 100%;
  overflow: hidden;
  border-radius: inherit;
  background: #f8fafc;
}

.equip-product-thumb img {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  object-fit: contain;
  background: #fff;
}

.equip-product-thumb__ph {
  font-size: 16px;
  color: #cbd5e1;
}
</style>
