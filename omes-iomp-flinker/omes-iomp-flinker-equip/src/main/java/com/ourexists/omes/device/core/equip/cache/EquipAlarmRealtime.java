package com.ourexists.omes.device.core.equip.cache;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Getter
@Setter
@Accessors(chain = true)
public class EquipAlarmRealtime {

    private String map;
    /** 比较类型: 0=相等, 1=大于, 2=大于等于, 3=小于, 4=小于等于, 5=范围 */
    private Integer type;
    private String val;
    /** 范围比较时的下限 (类型5时使用) */
    private String min;
    /** 范围比较时的上限 (类型5时使用) */
    private String max;
    private String text;
    /** 报警等级: 0=轻微, 1=一般, 2=严重, 3=故障 */
    private Integer level;
}
