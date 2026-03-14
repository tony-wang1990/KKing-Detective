package com.tony.kingdetective.enums;

import lombok.Getter;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @projectName: king-detective
 * @package: com.tony.kingdetective.enums
 * @className: SysCfgEnum
 * @author: Tony Wang
 * @date: 2024/11/30 17:29
 */
@Getter
public enum SysCfgEnum {

    /**
     * 
     */
    SYS_TG_BOT_TOKEN("Y101", "telegram???token", SysCfgTypeEnum.SYS_INIT_CFG),
    SYS_TG_CHAT_ID("Y102", "telegram??ID", SysCfgTypeEnum.SYS_INIT_CFG),
    SYS_DING_BOT_TOKEN("Y103", "?????accessToken", SysCfgTypeEnum.SYS_INIT_CFG),
    SYS_DING_BOT_SECRET("Y104", "?????secret", SysCfgTypeEnum.SYS_INIT_CFG),
    SYS_MFA_SECRET("Y105", "??MFA", SysCfgTypeEnum.SYS_MFA_CFG),
    ENABLE_DAILY_BROADCAST("Y107", "????????", SysCfgTypeEnum.SYS_INIT_CFG),
    DAILY_BROADCAST_CRON("Y108", "????cron", SysCfgTypeEnum.SYS_INIT_CFG),
    ENABLED_VERSION_UPDATE_NOTIFICATIONS("Y109", "??????????", SysCfgTypeEnum.SYS_INIT_CFG),
    SILICONFLOW_AI_API("Y110", "????API", SysCfgTypeEnum.SYS_INIT_CFG),
    BOOT_BROADCAST_TOKEN("Y111", "????Token", SysCfgTypeEnum.SYS_INIT_CFG),
    BOOT_BROADCAST_CHANNEL("Y114", "TG??????", SysCfgTypeEnum.SYS_INIT_CFG),
    SYS_VNC("Y112", "??VNC??url", SysCfgTypeEnum.SYS_INIT_CFG),
    GOOGLE_ONE_CLICK_LOGIN("Y113", "??????????", SysCfgTypeEnum.SYS_INIT_CFG),

    SYS_INFO_VERSION("Y106", "?????", SysCfgTypeEnum.SYS_INFO),


    ;

    SysCfgEnum(String code, String desc, SysCfgTypeEnum type) {
        this.code = code;
        this.desc = desc;
        this.type = type;
    }

    private String code;
    private String desc;
    private SysCfgTypeEnum type;


    public static List<SysCfgEnum> getCodeListByType(SysCfgTypeEnum type) {
        return Arrays.stream(values())
                .filter(x -> x.getType() == type)
                .collect(Collectors.toList());
    }
}
