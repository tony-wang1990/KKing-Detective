package com.tony.kingdetective.enums;

import lombok.Getter;

/**
 * <p>
 * InstanceActionEnum
 * </p >
 *
 * @author yuhui.fan
 * @since 2024/11/26 11:22
 */
@Getter
public enum InstanceActionEnum {
    /**
     * 
     */
    ACTION_STOP("STOP", "????"),
    ACTION_START("START", "????"),
    ACTION_RESET("RESET", "??????????"),

    ;

    private String action;
    private String desc;

    InstanceActionEnum(String action, String desc) {
        this.action = action;
        this.desc = desc;
    }

    public static InstanceActionEnum getActionEnum(String action) {
        for (InstanceActionEnum actionEnum : InstanceActionEnum.values()) {
            if (action.equals(actionEnum.getAction())) {
                return actionEnum;
            }
        }
        return null;
    }
}
