<script setup lang="ts">
import CompactSearchActions from '@/components/admin/CompactSearchActions.vue'
import { computed, nextTick, onMounted, onUnmounted, ref, shallowRef, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import dayjs, { type Dayjs } from 'dayjs'
import * as echarts from 'echarts'
import type { CustomSeriesRenderItemAPI, CustomSeriesRenderItemParams } from 'echarts'
import type { TableColumnType } from 'ant-design-vue'
import type { EquipRecord } from '@/api/device'
import { fetchEquipConfigBySn } from '@/api/equip-detail'
import {
  fetchEquipAlarmSegments,
  fetchEquipCollectPage,
  fetchEquipOnlineSegments,
  fetchEquipRunSegments,
} from '@/api/equip-detail'
import type { EquipAttrRow } from '@/types/equip-config'
import { buildGanttRows, capEndDateToNow, type GanttRow } from '../utils/gantt'
import { resolveGanttSegmentColor } from '@/utils/equip-status'
import TableScrollWrap from '@/components/common/TableScrollWrap.vue'

const props = defineProps<{
  equip: EquipRecord | null
  active: boolean
}>()

const { t } = useI18n()

const chartRef = ref<HTMLElement | null>(null)
const trendRef = ref<HTMLElement | null>(null)
let runChart: echarts.ECharts | null = null
let trendChart: echarts.ECharts | null = null
let resizeObserver: ResizeObserver | null = null

const loading = ref(false)
const runHasData = ref(false)
const collectHasRows = ref(false)
const collectAttrs = shallowRef<EquipAttrRow[]>([])
const collectRows = shallowRef<Record<string, unknown>[]>([])
const tableLoading = ref(false)
const aggregateInterval = ref<string | undefined>(undefined)

const range = ref<[Dayjs, Dayjs]>([
  dayjs().startOf('day'),
  dayjs().endOf('day'),
])

const defaultRange = (): [Dayjs, Dayjs] => [dayjs().startOf('day'), dayjs().endOf('day')]

const hasCollect = computed(() => collectAttrs.value.some((item) => item.needCollect))

const tableColumns = computed<TableColumnType[]>(() => {
  const cols: TableColumnType[] = [
    { title: t('equipDetailPage.time'), dataIndex: 'time', key: 'time', width: 180 },
  ]
  for (const attr of collectAttrs.value) {
    if (attr.needCollect && attr.name) {
      cols.push({ title: attr.name, dataIndex: attr.name, key: attr.name, ellipsis: true })
    }
  }
  return cols
})

const tableData = computed(() =>
  collectRows.value.map((row, index) => ({
    key: `${row.time}-${index}`,
    time: row.time,
    ...flattenCollectRow(row),
  })),
)

function flattenCollectRow(row: Record<string, unknown>) {
  const data = (row.data as Record<string, unknown> | undefined) || {}
  const flat: Record<string, unknown> = {}
  for (const attr of collectAttrs.value) {
    if (!attr.needCollect || !attr.name) {
      continue
    }
    const name = attr.name
    if (data[`${name}_avg`] != null) {
      flat[name] = `${t('equipDetailPage.avg')} ${data[`${name}_avg`]} | ${t('equipDetailPage.min')} ${data[`${name}_min`]} | ${t('equipDetailPage.max')} ${data[`${name}_max`]}`
    } else {
      flat[name] = data[name] ?? '-'
    }
  }
  return flat
}

function formatRange(): { startDate: string; endDate: string } {
  return {
    startDate: range.value[0].format('YYYY-MM-DD HH:mm:ss'),
    endDate: range.value[1].format('YYYY-MM-DD HH:mm:ss'),
  }
}

async function loadCollectConfig() {
  if (!props.equip?.selfCode) {
    collectAttrs.value = []
    return
  }
  const binding = await fetchEquipConfigBySn(props.equip.selfCode)
  collectAttrs.value = (binding?.config?.attrs || []).filter((item) => item.needCollect)
}

function clearRunChart() {
  runChart?.dispose()
  runChart = null
}

function clearTrendChart() {
  trendChart?.dispose()
  trendChart = null
}

async function loadRunChart() {
  if (!props.equip?.selfCode) {
    runHasData.value = false
    clearRunChart()
    return
  }
  const { startDate, endDate } = formatRange()
  const query = { sn: props.equip.selfCode, startDate, endDate, requirePage: false }
  const [online, run, alarm] = await Promise.all([
    fetchEquipOnlineSegments(query),
    fetchEquipRunSegments(query),
    fetchEquipAlarmSegments(query),
  ])
  const rows = buildGanttRows(online || [], run || [], alarm || [])
  runHasData.value = rows.length > 0
  if (!rows.length) {
    clearRunChart()
    return
  }
  await nextTick()
  if (!runChart) initRunChart()
  if (!runChart) return
  const min = capEndDateToNow(range.value[0].toDate()).getTime()
  const max = capEndDateToNow(range.value[1].toDate()).getTime()
  runChart.setOption({
    xAxis: { min, max },
    series: [{ data: rows }],
  })
  runChart.resize()
}

async function loadTrendChart() {
  if (!props.equip?.selfCode || !hasCollect.value) {
    clearTrendChart()
    return
  }
  const { startDate, endDate } = formatRange()
  const result = await fetchEquipCollectPage({
    sn: props.equip.selfCode,
    startDate,
    endDate,
    requirePage: false,
    page: 1,
    pageSize: 50000,
    aggregateInterval: 'AUTO',
  })
  const list = result?.records || []
  const attrs = collectAttrs.value.filter((item) => item.needCollect && item.name)
  if (!attrs.length || !list.length) {
    clearTrendChart()
    return
  }
  await nextTick()
  if (!trendChart) initTrendChart()
  if (!trendChart) return
  const series = attrs.map((attr, index) => ({
    name: attr.name,
    type: 'line' as const,
    showSymbol: false,
    smooth: true,
    xAxisIndex: index,
    yAxisIndex: index,
    data: list.map((row) => [row.time, row.data?.[attr.name!]]),
  }))
  const grids = attrs.map((_, index) => ({
    top: `${8 + index * 28}%`,
    height: '22%',
    left: '8%',
    right: '4%',
  }))
  const xAxis = attrs.map((_, index) => ({ type: 'time' as const, gridIndex: index }))
  const yAxis = attrs.map((_, index) => ({ type: 'value' as const, gridIndex: index, splitLine: { show: false } }))
  trendChart.setOption({ grid: grids, xAxis, yAxis, series, tooltip: { trigger: 'axis' } }, true)
  trendChart.resize()
}

async function loadCollectTable() {
  if (!props.equip?.selfCode || !hasCollect.value) {
    collectRows.value = []
    collectHasRows.value = false
    return
  }
  tableLoading.value = true
  try {
    const { startDate, endDate } = formatRange()
    const result = await fetchEquipCollectPage({
      sn: props.equip.selfCode,
      startDate,
      endDate,
      requirePage: false,
      page: 1,
      pageSize: 500,
      aggregateInterval: aggregateInterval.value || undefined,
    })
    collectRows.value = (result?.records || []) as Record<string, unknown>[]
    collectHasRows.value = collectRows.value.length > 0
  } finally {
    tableLoading.value = false
  }
}

function initRunChart() {
  if (!chartRef.value) {
    return
  }
  runChart?.dispose()
  runChart = echarts.init(chartRef.value)
  runChart.setOption({
    title: {
      text: t('equipDetailPage.runTrend'),
      textStyle: { fontSize: 14, fontWeight: 500, color: '#64748b' },
    },
    tooltip: {
      confine: true,
      formatter(params: { data: GanttRow }) {
        const [type, start, end, state] = params.data
        const label =
          type === 0
            ? state === 1
              ? t('realtimePage.online')
              : t('realtimePage.offline')
            : type === 1
              ? state === 1
                ? t('realtimePage.run')
                : t('realtimePage.stop')
              : t('realtimePage.alarm')
        return `${label}<br/>${dayjs(start).format('YYYY-MM-DD HH:mm:ss')} ~ ${dayjs(end).format('YYYY-MM-DD HH:mm:ss')}`
      },
    },
    xAxis: { type: 'time' },
    yAxis: { type: 'category', data: [t('realtimePage.online'), t('realtimePage.run'), t('realtimePage.alarm')] },
    series: [
      {
        type: 'custom',
        renderItem(_params: CustomSeriesRenderItemParams, api: CustomSeriesRenderItemAPI) {
          const yIndex = api.value(0) as number
          const start = api.coord([api.value(1), yIndex])
          const end = api.coord([api.value(2), yIndex])
          const type = api.value(0) as number
          const state = api.value(3) as number
          const fill = resolveGanttSegmentColor(type, state as number)
          return {
            type: 'rect',
            shape: { x: start[0], y: start[1] - 10, width: end[0] - start[0], height: 20 },
            style: { fill },
          }
        },
        encode: { x: [1, 2], y: 0 },
        data: [] as GanttRow[],
      },
    ],
  })
}

function initTrendChart() {
  if (!trendRef.value) {
    return
  }
  trendChart?.dispose()
  trendChart = echarts.init(trendRef.value)
}

async function reloadAll() {
  if (!props.active || !props.equip?.selfCode) {
    return
  }
  loading.value = true
  try {
    await loadCollectConfig()
    await Promise.all([loadRunChart(), loadTrendChart(), loadCollectTable()])
    runChart?.resize()
    trendChart?.resize()
  } finally {
    loading.value = false
  }
  await nextTick()
  setupResizeObserver()
}

function onResetRange() {
  range.value = defaultRange()
  void reloadAll()
}

function setupResizeObserver() {
  resizeObserver?.disconnect()
  resizeObserver = new ResizeObserver(() => {
    runChart?.resize()
    trendChart?.resize()
  })
  if (chartRef.value) {
    resizeObserver.observe(chartRef.value)
  }
  if (trendRef.value) {
    resizeObserver.observe(trendRef.value)
  }
}

watch(
  () => [props.active, props.equip?.selfCode] as const,
  ([active]) => {
    if (active) {
      void nextTick().then(() => {
        setupResizeObserver()
        void reloadAll()
      })
    }
  },
  { immediate: true },
)

onMounted(() => {
  if (props.active) {
    setupResizeObserver()
  }
})

onUnmounted(() => {
  resizeObserver?.disconnect()
  runChart?.dispose()
  trendChart?.dispose()
  runChart = null
  trendChart = null
})
</script>

<template>
  <div class="realtime-tab">
    <div class="toolbar search-toolbar--compact">
      <a-form layout="inline" class="search-form" @finish="reloadAll">
        <a-form-item>
          <a-range-picker
            v-model:value="range"
            show-time
            format="YYYY-MM-DD HH:mm:ss"
            size="small"
            class="search-range-picker"
          />
        </a-form-item>
        <CompactSearchActions
          :query-title="t('equipDetailPage.query')"
          :reset-title="t('equipRecordPage.reset')"
          :loading="loading"
          @reset="onResetRange"
        />
      </a-form>
    </div>

    <a-spin :spinning="loading" class="realtime-body">
    <div v-if="!loading && !runHasData" class="chart-empty">
      <div class="chart-empty__title">{{ t('equipDetailPage.runTrend') }}</div>
      <a-empty :description="t('equipDetailPage.runEmpty')" />
      <p class="chart-empty__hint">{{ t('equipDetailPage.runEmptyHint') }}</p>
    </div>
    <div v-else ref="chartRef" class="run-chart" />

    <div v-if="hasCollect" class="collect-block" :class="{ 'is-empty': !loading && !collectHasRows }">
      <div v-if="!loading && !collectHasRows" class="chart-empty chart-empty--flat">
        <a-empty :description="t('equipDetailPage.collectEmpty')" />
      </div>
      <template v-else>
        <div ref="trendRef" class="trend-chart" />
        <div class="table-toolbar">
          <a-select
            v-model:value="aggregateInterval"
            allow-clear
            size="small"
            style="width: 160px"
            :placeholder="t('equipDetailPage.rawData')"
            :options="[
              { value: 'MIN30', label: t('equipDetailPage.agg30m') },
              { value: 'HOUR', label: t('equipDetailPage.aggHour') },
              { value: 'DAY', label: t('equipDetailPage.aggDay') },
              { value: 'MONTH', label: t('equipDetailPage.aggMonth') },
            ]"
            @change="loadCollectTable"
          />
        </div>
        <TableScrollWrap
          :min-height="160"
          :refresh-keys="[tableData.length, aggregateInterval]"
        >
          <template #default="{ scrollY }">
            <a-table
              size="small"
              bordered
              class="scroll-table collect-table"
              :columns="tableColumns"
              :data-source="tableData"
              :loading="tableLoading"
              :pagination="{ pageSize: 10, showSizeChanger: true }"
              :scroll="{ x: true, y: scrollY }"
              :locale="{ emptyText: t('equipDetailPage.collectEmpty') }"
            />
          </template>
        </TableScrollWrap>
      </template>
    </div>
    </a-spin>
  </div>
</template>

<style scoped>
.realtime-tab {
  display: flex;
  flex-direction: column;
  gap: 16px;
  height: 100%;
  min-height: 0;
}

.realtime-body {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.realtime-body :deep(.ant-spin-container) {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.toolbar {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
  padding: 12px 16px;
  background: var(--omes-color-bg-container);
  border: 1px solid #e8eef5;
  border-radius: 12px;
}

.run-chart {
  flex-shrink: 0;
  height: 320px;
  background: var(--omes-color-bg-container);
  border: 1px solid #e8eef5;
  border-radius: 12px;
}

.chart-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  min-height: 220px;
  padding: 28px 16px 20px;
  background: var(--omes-color-bg-container);
  border: 1px dashed #dbe3ee;
  border-radius: 12px;
}

.chart-empty--flat {
  min-height: 0;
  padding: 8px 0 4px;
  background: transparent;
  border: none;
}

.chart-empty__title {
  font-size: 14px;
  font-weight: 500;
  color: #64748b;
}

.chart-empty :deep(.ant-empty) {
  margin: 8px 0 0;
}

.chart-empty__hint {
  margin: 0 0 4px;
  font-size: 12px;
  line-height: 1.5;
  color: var(--omes-color-text-quaternary);
}

.collect-block {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding: 12px;
  background: var(--omes-color-bg-container);
  border: 1px solid #e8eef5;
  border-radius: 12px;
}

.collect-block.is-empty {
  flex: none;
}

.trend-chart {
  flex-shrink: 0;
  height: 280px;
}

.table-toolbar {
  flex-shrink: 0;
  display: flex;
  justify-content: flex-end;
}
</style>
