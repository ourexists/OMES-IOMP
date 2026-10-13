package com.ourexists.omes.device.core.workshop.cache;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.util.List;

@Getter
@Setter
@Accessors(chain = true)
public class WorkshopRealtimeConfig {

    private List<WorkshopRealtimeCollect> attrs;
}
