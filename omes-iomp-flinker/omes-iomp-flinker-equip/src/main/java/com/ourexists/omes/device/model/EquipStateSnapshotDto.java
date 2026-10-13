package com.ourexists.omes.device.model;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.util.Date;

@Getter
@Setter
@Accessors(chain = true)
public class EquipStateSnapshotDto {

    protected String id;

    protected String sn;

    protected Date time;

    protected Integer runState;

    protected Integer alarmState;

    protected Integer onlineState;

    protected String tenantId;
}
