package com.ourexists.omes.device.core.workshop.cache;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.util.Date;
import java.util.List;

@Getter
@Setter
@Accessors(chain = true)
public class WorkshopRealtime {

    private String id;

    private WorkshopRealtimeConfig config;

    private List<WorkshopRealtimeCollect> attrsRealtime;

    private Date time;

    private String tenantId;

    private Long streamIngressSeq;
}
