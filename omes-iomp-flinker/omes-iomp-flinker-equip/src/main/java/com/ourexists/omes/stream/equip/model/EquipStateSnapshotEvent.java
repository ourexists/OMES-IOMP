package com.ourexists.omes.stream.equip.model;

import com.ourexists.omes.device.model.EquipStateSnapshotDto;

public class EquipStateSnapshotEvent {
    private final EquipStateSnapshotDto snapshot;

    public EquipStateSnapshotEvent(EquipStateSnapshotDto snapshot) {
        this.snapshot = snapshot;
    }

    public EquipStateSnapshotDto getSnapshot() {
        return snapshot;
    }
}
