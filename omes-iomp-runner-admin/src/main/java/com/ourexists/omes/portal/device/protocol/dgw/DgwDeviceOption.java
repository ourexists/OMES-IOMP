package com.ourexists.omes.portal.device.protocol.dgw;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Getter
@Setter
@Accessors(chain = true)
public class DgwDeviceOption {

    private String id;

    private String code;

    private String name;

    private String protocol;

    private Boolean enabled;
}
