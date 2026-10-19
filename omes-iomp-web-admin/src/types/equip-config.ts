export interface EquipAttrRow {
  name?: string
  map?: string
  value?: string
  unit?: string
  needCollect?: boolean
  fluctuationEnabled?: boolean
  fluctuationThresholdRatio?: number | null
  fluctuationMinDelta?: number | null
  fluctuationConsecutiveWindows?: number | null
  fromProduct?: boolean
}

export interface EquipAlarmRow {
  name?: string
  map?: string
  type?: number
  val?: string
  min?: string
  max?: string
  text?: string
  level?: number
  fromProduct?: boolean
}

export interface EquipControlRow {
  name?: string
  map?: string
  type?: number
  unit?: string
  min?: string
  max?: string
  value?: string
  fromProduct?: boolean
}

export interface EquipConfigDetail {
  gwId?: string
  deviceIdMap?: string
  runMap?: string
  attrs?: EquipAttrRow[]
  alarms?: EquipAlarmRow[]
  controls?: EquipControlRow[]
}

export interface ProductAttrConfig {
  attrs?: EquipAttrRow[]
  alarms?: EquipAlarmRow[]
  controls?: EquipControlRow[]
}

export interface EquipGwBinding {
  equipId?: string
  gwId?: string
  config?: EquipConfigDetail
}
