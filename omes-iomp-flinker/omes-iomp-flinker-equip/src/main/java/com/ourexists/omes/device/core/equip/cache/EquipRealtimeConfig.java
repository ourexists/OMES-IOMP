package com.ourexists.omes.device.core.equip.cache;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.util.List;

@Getter
@Setter
@Accessors(chain = true)
public class EquipRealtimeConfig {

    private String gwId;

    private String deviceIdMap;

    private String runMap;

    private List<EquipAttrRealtime> attrs;

    private List<EquipAlarmRealtime> alarms;

    private List<EquipControlRealtime> controls;
}
