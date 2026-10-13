package com.ourexists.omes.device.core.equip.cache;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Getter
@Setter
@Accessors(chain = true)
public class EquipRealtime {

    private String id;

    private String name;

    private String selfCode;

    private EquipRealtimeConfig equipRealtimeConfig;

    private Integer onlineState = 0;

    /** 运行：-1 未知，0 停止，1 运行 */
    private Integer runState = -1;

    /** 报警：-1 未知，0 正常，1 报警 */
    private Integer alarmState = -1;

    private List<EquipAttrRealtime> equipAttrRealtimes;

    private List<EquipControlRealtime> equipControlRealtimes;

    private List<String> alarmTexts;

    private Integer alarmLevel;

    private String tenantId;

    private String workshopCode;

    private Date time;

    private Date onlineChangeTime;

    private Date runChangeTime;

    private Date alarmChangeTime;

    private Boolean onlineChange;

    /**
     * 进入 Flink 作业时的单调序号（反序列化时赋值）。用于滑动窗口 reduce / 变化检测：
     * 当设备 {@link #time} 为空或多条相同导致无法区分先后时，按入站顺序取“最后一条”真实态，避免误把离线态当作最新（必定离线）。
     */
    private Long streamIngressSeq;

    public void offline() {
        Date now = new Date();
        if (this.onlineState != 0) {
            this.onlineState = 0;
            this.onlineChangeTime = now;
            if (this.equipRealtimeConfig != null) {
                this.equipAttrRealtimes = this.equipRealtimeConfig.getAttrs();
            } else {
                this.equipAttrRealtimes = null;
            }
        }
        if (this.runState == null || this.runState != -1) {
            this.runState = -1;
            this.runChangeTime = now;
        }
        if (this.alarmState == null || this.alarmState != -1) {
            this.alarmState = -1;
            this.alarmChangeTime = now;
            this.alarmTexts = new ArrayList<>();
        }
    }
}
