package com.ourexists.omes.device.model;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.util.Date;

@Getter
@Setter
@Accessors(chain = true)
public class EquipRecordRunDto {

    protected String id;

    protected String sn;

    protected Date startTime;

    protected Integer state;

    protected BigDecimal powerStart;

    protected String tenantId;

    /** 当前区间行的业务事件 ID（流侧生成，入库） */
    protected String eventId;

    /** 上一段未闭合行的 event_id；有值时优先按此更新 end_time */
    protected String prevEventId;
}
