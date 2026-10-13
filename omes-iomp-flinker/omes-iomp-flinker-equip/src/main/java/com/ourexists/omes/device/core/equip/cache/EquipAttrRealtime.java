package com.ourexists.omes.device.core.equip.cache;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Getter
@Setter
@Accessors(chain = true)
public class EquipAttrRealtime {

    private String name;

    private String map;

    private String unit;

    private Boolean needCollect = false;

    private String value;

    /** 是否开启波动检测 */
    private Boolean fluctuationEnabled = true;

    /** 波动检测阈值比例 */
    private Double fluctuationThresholdRatio;

    /** 波动检测最小有效幅值 */
    private Double fluctuationMinDelta;

    /** 波动检测连续窗口数 */
    private Integer fluctuationConsecutiveWindows;
}
