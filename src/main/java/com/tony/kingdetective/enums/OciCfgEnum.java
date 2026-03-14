package com.tony.kingdetective.enums;

import lombok.Getter;

/**
 * <p>
 * OciCfgEnum
 * </p >
 *
 * @author yohann
 * @since 2024/11/8 12:12
 */
@Getter
public enum OciCfgEnum {

    /**
     * OCI
     */
    OCI_CFG_USER_ID("user", "??id"),
    OCI_CFG_TENANT_ID("tenancy", "??id"),
    OCI_CFG_REGION("region", "??"),
    OCI_CFG_FINGERPRINT("fingerprint", "??"),
    OCI_CFG_KEY_FILE("key_file", "???????"),
    ;

    private String type;
    private String desc;

    OciCfgEnum(String type, String desc) {
        this.type = type;
        this.desc = desc;
    }
}
