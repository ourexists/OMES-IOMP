package com.ourexists.omes.stream.workshop.model;

import com.ourexists.omes.device.model.WorkshopCollectDto;

public class WorkshopCollectSnapshotEvent {

    private final WorkshopCollectDto collect;

    public WorkshopCollectSnapshotEvent(WorkshopCollectDto collect) {
        this.collect = collect;
    }

    public WorkshopCollectDto getCollect() {
        return collect;
    }
}
