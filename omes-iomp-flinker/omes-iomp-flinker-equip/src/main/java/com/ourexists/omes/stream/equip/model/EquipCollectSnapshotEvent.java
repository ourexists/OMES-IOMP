package com.ourexists.omes.stream.equip.model;

import com.ourexists.omes.device.model.EquipCollectDto;

public class EquipCollectSnapshotEvent {
    private final EquipCollectDto collect;

    public EquipCollectSnapshotEvent(EquipCollectDto collect) {
        this.collect = collect;
    }

    public EquipCollectDto getCollect() {
        return collect;
    }
}
