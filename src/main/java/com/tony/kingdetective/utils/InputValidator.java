package com.tony.kingdetective.utils;

import com.tony.kingdetective.exception.OciException;
import lombok.extern.slf4j.Slf4j;

import java.util.regex.Pattern;

/**
 * 
 * 
 * 
 * @author Tony Wang
 */
@Slf4j
public class InputValidator {
    
    // URL 
    private static final Pattern URL_PATTERN = Pattern.compile(
        "^https?://[\\w.-]+(:\\d+)?(/.*)?$"
    );
    
    // IP 
    private static final Pattern IP_PATTERN = Pattern.compile(
        "^((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$"
    );
    
    // CIDR 
    private static final Pattern CIDR_PATTERN = Pattern.compile(
        "^((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)/([0-9]|[1-2][0-9]|3[0-2])$"
    );
    
    // 
    private static final Pattern HOSTNAME_PATTERN = Pattern.compile(
        "^[a-zA-Z0-9]([a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?(\\.[a-zA-Z0-9]([a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?)*$"
    );
    
    /**
     *  URL
     *
     * @param url URL
     * @throws OciException 
     */
    public static void validateUrl(String url) {
        if (url == null || url.trim().isEmpty()) {
            throw new OciException(-400, "URL 不能为空");
        }
        
        if (!URL_PATTERN.matcher(url).matches()) {
            throw new OciException(-400, 
                "无效的 URL 格式\n\n" +
                "必须以 http:// 或 https:// 开头\n\n" +
                "示例：\n" +
                "• http://192.168.1.100:6080\n" +
                "• https://vnc.example.com"
            );
        }
        
        log.debug("URL 验证通过: {}", url);
    }
    
    /**
     * 
     *
     * @param port 
     * @throws OciException 
     */
    public static void validatePort(int port) {
        if (port < 1 || port > 65535) {
            throw new OciException(-400, "端口号必须在 1-65535 之间");
        }
        
        log.debug("端口验证通过: {}", port);
    }
    
    /**
     *  IP 
     *
     * @param ip IP
     * @throws OciException IP
     */
    public static void validateIpAddress(String ip) {
        if (ip == null || ip.trim().isEmpty()) {
            throw new OciException(-400, "IP 地址不能为空");
        }
        
        if (!IP_PATTERN.matcher(ip).matches()) {
            throw new OciException(-400, 
                "无效的 IP 地址格式\n\n" +
                "示例: 192.168.1.100"
            );
        }
        
        log.debug("IP 地址验证通过: {}", ip);
    }
    
    /**
     *  CIDR 
     *
     * @param cidr CIDR
     * @throws OciException CIDR
     */
    public static void validateCidr(String cidr) {
        if (cidr == null || cidr.trim().isEmpty()) {
            throw new OciException(-400, "CIDR 块不能为空");
        }
        
        if (!CIDR_PATTERN.matcher(cidr).matches()) {
            throw new OciException(-400, 
                "无效的 CIDR 格式\n\n" +
                "示例: 10.0.0.0/16 或 192.168.1.0/24"
            );
        }
        
        log.debug("CIDR 验证通过: {}", cidr);
    }
    
    /**
     * 
     *
     * @param hostname 
     * @throws OciException 
     */
    public static void validateHostname(String hostname) {
        if (hostname == null || hostname.trim().isEmpty()) {
            throw new OciException(-400, "主机名不能为空");
        }
        
        if (!HOSTNAME_PATTERN.matcher(hostname).matches()) {
            throw new OciException(-400, 
                "无效的主机名格式\n\n" +
                "示例: example.com 或 server01"
            );
        }
        
        log.debug("主机名验证通过: {}", hostname);
    }
    
    /**
     * SSH
     *
     * @param username 
     * @throws OciException 
     */
    public static void validateUsername(String username) {
        if (username == null || username.trim().isEmpty()) {
            throw new OciException(-400, "用户名不能为空");
        }
        
        if (username.length() < 2 || username.length() > 32) {
            throw new OciException(-400, "用户名长度必须在 2-32 个字符之间");
        }
        
        if (!username.matches("^[a-zA-Z0-9_-]+$")) {
            throw new OciException(-400, 
                "用户名只能包含字母、数字、下划线和连字符"
            );
        }
        
        log.debug("用户名验证通过: {}", username);
    }
    
    /**
     * 
     *
     * @param password 
     * @param minLength 
     * @throws OciException 
     */
    public static void validatePassword(String password, int minLength) {
        if (password == null || password.isEmpty()) {
            throw new OciException(-400, "密码不能为空");
        }
        
        if (password.length() < minLength) {
            throw new OciException(-400, 
                String.format("密码长度至少为 %d 个字符\n\n建议使用强密码以提高安全性", minLength)
            );
        }
        
        log.debug("密码验证通过（长度: {}）", password.length());
    }
    
    /**
     * 
     *
     * @param text 
     * @param fieldName 
     * @param minLength 
     * @param maxLength 
     * @throws OciException 
     */
    public static void validateLength(String text, String fieldName, int minLength, int maxLength) {
        if (text == null) {
            throw new OciException(-400, fieldName + " 不能为空");
        }
        
        if (text.length() < minLength || text.length() > maxLength) {
            throw new OciException(-400, 
                String.format("%s 长度必须在 %d-%d 个字符之间", fieldName, minLength, maxLength)
            );
        }
        
        log.debug("长度验证通过: {} (长度: {})", fieldName, text.length());
    }
    
    /**
     * 
     *
     * @param text 
     * @param fieldName 
     * @throws OciException 
     */
    public static void validateNotEmpty(String text, String fieldName) {
        if (text == null || text.trim().isEmpty()) {
            throw new OciException(-400, fieldName + " 不能为空");
        }
        
        log.debug("非空验证通过: {}", fieldName);
    }
}
