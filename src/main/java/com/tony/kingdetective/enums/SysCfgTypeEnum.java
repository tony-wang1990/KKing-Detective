package com.tony.kingdetective.enums;

import lombok.Getter;

/**
 * @projectName: king-detective
 * @package: com.tony.kingdetective.enums
 * @className: SysCfgTypeEnum
 * @author: Tony Wang
 * @date: 2024/11/30 17:29
 */
@Getter
public enum SysCfgTypeEnum {

    /**
     * 
     */
    SYS_INIT_CFG("Y001", "??????"),
    SYS_MFA_CFG("Y002", "??MFA??"),
    SYS_INFO("Y003", "????"),

    ;

    SysCfgTypeEnum(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    private String code;
    private String desc;
}
