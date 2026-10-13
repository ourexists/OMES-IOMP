package com.ourexists.omes.message.enums;

import lombok.Getter;

public enum MessageTypeEnum {

    COMMON(0),
    ALARM(1);

    @Getter
    private final Integer code;

    MessageTypeEnum(int code) {
        this.code = code;
    }

    public static MessageTypeEnum valueOf(Integer code) {
        for (MessageTypeEnum value : MessageTypeEnum.values()) {
            if (value.getCode().equals(code)) {
                return value;
            }
        }
        return MessageTypeEnum.COMMON;
    }
}
