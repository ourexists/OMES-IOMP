package com.ourexists.omes.device.core.equip.cache;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Getter
@Setter
@Accessors(chain = true)
public class EquipControlRealtime {

    /** 控制名称 */
    private String name;

    /** 映射地址（PLC 可写地址） */
    private String map;

    /** 控制类型: 0=开关量, 1=模拟量 */
    private Integer type;

    /** 当前值（轮询读回） */
    private String value;

    /** 单位（模拟量使用） */
    private String unit;

    /** 最小值（模拟量使用） */
    private String min;

    /** 最大值（模拟量使用） */
    private String max;
}
