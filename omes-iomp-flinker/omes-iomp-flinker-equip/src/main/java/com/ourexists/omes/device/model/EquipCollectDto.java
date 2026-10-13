package com.ourexists.omes.device.model;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.util.Date;
import java.util.Map;

@Getter
@Setter
@Accessors(chain = true)
public class EquipCollectDto {

    private String sn;

    private Map<String, String> data;

    private Date time;

    private String tenantId;
}
