package com.ourexists.omes.portal.device.protocol.dgw;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Getter
@Setter
@Accessors(chain = true)
public class DgwPointOption {

    private String code;

    private String name;

    /** READ_ONLY / READ_WRITE / WRITE_ONLY */
    private String accessType;

    private String unit;

    private boolean readable;

    private boolean writable;
}
