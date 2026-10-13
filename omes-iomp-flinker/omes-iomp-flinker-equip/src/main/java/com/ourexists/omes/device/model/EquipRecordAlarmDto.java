package com.ourexists.omes.device.model;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.util.Date;

@Getter
@Setter
@Accessors(chain = true)
public class EquipRecordAlarmDto {

    protected String id;

    protected String sn;

    protected Date startTime;

    protected Integer state;

    protected String tenantId;

    protected String reason;

    /** 报警等级: 0=轻微, 1=一般, 2=严重, 3=故障，见 AlarmLevelEnum，为空时按一般处理 */
    protected Integer level;

    /** 当前区间行的业务事件 ID（流侧生成，入库） */
    protected String eventId;

    /** 上一段未闭合行的 event_id；有值时优先按此更新 end_time */
    protected String prevEventId;
}
