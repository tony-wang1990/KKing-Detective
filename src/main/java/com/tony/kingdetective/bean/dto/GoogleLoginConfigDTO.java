package com.tony.kingdetective.bean.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @projectName: king-detective
 * @package: com.tony.kingdetective.bean.dto
 * @className: GoogleLoginConfigDTO
 * @author: Tony Wang
 * @date: 2026/01/02
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GoogleLoginConfigDTO {

    /**
     * Google
     */
    private Boolean enabled;

    /**
     * Google OAuthID
     */
    private String clientId;

    /**
     * Google
     * user1@gmail.com,user2@company.com,admin@example.com
     */
    private String allowedEmails;
}
