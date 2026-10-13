import { get, post, postPage } from '@/api/request'
import { resolveLocalFileUrl, uploadLocalFile } from '@/api/local-file'
import type { ProductAttrConfig } from '@/types/equip-config'
import type { IdsPayload } from '@/types/api'
import type { PageQuery } from '@/types/api'

export interface ProductRecord {
  id: string
  name?: string
  code?: string
  imageUrl?: string
  attrConfig?: ProductAttrConfig
}

export function resolveProductImageUrl(path?: string): string {
  return resolveLocalFileUrl(path)
}

export function fetchProductPage(params: PageQuery & { name?: string; code?: string }) {
  return postPage<ProductRecord>('/product/selectByPage', params)
}

export function fetchProductById(id: string) {
  return get<ProductRecord>('/product/selectById', { id })
}

export function fetchProductByCode(code: string) {
  return get<ProductRecord>('/product/selectByCode', { code })
}

export function saveProduct(data: Partial<ProductRecord>) {
  return post<boolean>('/product/addOrUpdate', data)
}

export function deleteProducts(ids: string[]) {
  return post<boolean>('/product/delete', { ids } satisfies IdsPayload)
}

/** 返回存储相对路径（如 product/a.png），展示时再用 resolveProductImageUrl */
export async function uploadProductImage(file: File, dir = 'product') {
  return uploadLocalFile(file, dir)
}
