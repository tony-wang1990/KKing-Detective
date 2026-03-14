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
            throw new OciException(-400, "URL ????");
        }
        
        if (!URL_PATTERN.matcher(url).matches()) {
            throw new OciException(-400, 
                "??? URL ??\n\n" +
                "??? http:// ? https:// ??\n\n" +
                "???\n" +
                "? http://192.168.1.100:6080\n" +
                "? https://vnc.example.com"
            );
        }
        
        log.debug("URL ????: {}", url);
    }
    
    /**
     * 
     *
     * @param port 
     * @throws OciException 
     */
    public static void validatePort(int port) {
        if (port < 1 || port > 65535) {
            throw new OciException(-400, "?????? 1-65535 ??");
        }
        
        log.debug("??????: {}", port);
    }
    
    /**
     *  IP 
     *
     * @param ip IP
     * @throws OciException IP
     */
    public static void validateIpAddress(String ip) {
        if (ip == null || ip.trim().isEmpty()) {
            throw new OciException(-400, "IP ??????");
        }
        
        if (!IP_PATTERN.matcher(ip).matches()) {
            throw new OciException(-400, 
                "??? IP ????\n\n" +
                "??: 192.168.1.100"
            );
        }
        
        log.debug("IP ??????: {}", ip);
    }
    
    /**
     *  CIDR 
     *
     * @param cidr CIDR
     * @throws OciException CIDR
     */
    public static void validateCidr(String cidr) {
        if (cidr == null || cidr.trim().isEmpty()) {
            throw new OciException(-400, "CIDR ?????");
        }
        
        if (!CIDR_PATTERN.matcher(cidr).matches()) {
            throw new OciException(-400, 
                "??? CIDR ??\n\n" +
                "??: 10.0.0.0/16 ? 192.168.1.0/24"
            );
        }
        
        log.debug("CIDR ????: {}", cidr);
    }
    
    /**
     * 
     *
     * @param hostname 
     * @throws OciException 
     */
    public static void validateHostname(String hostname) {
        if (hostname == null || hostname.trim().isEmpty()) {
            throw new OciException(-400, "???????");
        }
        
        if (!HOSTNAME_PATTERN.matcher(hostname).matches()) {
            throw new OciException(-400, 
                "????????\n\n" +
                "??: example.com ? server01"
            );
        }
        
        log.debug("???????: {}", hostname);
    }
    
    /**
     * SSH
     *
     * @param username 
     * @throws OciException 
     */
    public static void validateUsername(String username) {
        if (username == null || username.trim().isEmpty()) {
            throw new OciException(-400, "???????");
        }
        
        if (username.length() < 2 || username.length() > 32) {
            throw new OciException(-400, "???????? 2-32 ?????");
        }
        
        if (!username.matches("^[a-zA-Z0-9_-]+$")) {
            throw new OciException(-400, 
                "????????????????????"
            );
        }
        
        log.debug("???????: {}", username);
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
            throw new OciException(-400, "??????");
        }
        
        if (password.length() < minLength) {
            throw new OciException(-400, 
                String.format("??????? %d ???\n\n?????????????", minLength)
            );
        }
        
        log.debug("?????????: {}?", password.length());
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
            throw new OciException(-400, fieldName + " ????");
        }
        
        if (text.length() < minLength || text.length() > maxLength) {
            throw new OciException(-400, 
                String.format("%s ????? %d-%d ?????", fieldName, minLength, maxLength)
            );
        }
        
        log.debug("??????: {} (??: {})", fieldName, text.length());
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
            throw new OciException(-400, fieldName + " ????");
        }
        
        log.debug("??????: {}", fieldName);
    }
}
