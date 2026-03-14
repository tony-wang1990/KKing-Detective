package com.tony.kingdetective.enums;

import lombok.Getter;

/**
 * <p>
 * MessageTypeEnum
 * </p >
 *
 * @author yuhui.fan
 * @since 2024/11/8 12:12
 */
@Getter
public enum MessageTypeEnum {

    /**
     * 
     */
    MSG_TYPE_TELEGRAM("TG", "telegram????"),
    MSG_TYPE_DING_DING("DING", "??????"),
    ;

    private String type;
    private String desc;

    MessageTypeEnum(String type, String desc) {
        this.type = type;
        this.desc = desc;
    }
}
