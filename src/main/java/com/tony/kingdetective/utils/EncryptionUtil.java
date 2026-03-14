package com.tony.kingdetective.utils;

import cn.hutool.crypto.SecureUtil;
import cn.hutool.crypto.symmetric.AES;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * 
 *  AES-256 
 * 
 * @author Tony Wang
 */
@Slf4j
public class EncryptionUtil {
    
    private static final String ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES/ECB/PKCS5Padding";
    
    // 
    private static final String SECRET_KEY = getOrCreateSecretKey();
    
    private static final AES aes;
    
    static {
        try {
            // 32(256)
            String key = SECRET_KEY.length() >= 32
                    ? SECRET_KEY.substring(0, 32)
                    : String.format("%-32s", SECRET_KEY).replace(' ', '0');
            
            aes = SecureUtil.aes(key.getBytes(StandardCharsets.UTF_8));
            log.info("???????????? AES-256 ??");
        } catch (Exception e) {
            log.error("?????????", e);
            throw new RuntimeException("?????????", e);
        }
    }
    
    /**
     * 
     *
     * @param plainText 
     * @return Base64
     */
    public static String encrypt(String plainText) {
        if (plainText == null || plainText.isEmpty()) {
            return plainText;
        }
        
        try {
            byte[] encrypted = aes.encrypt(plainText);
            return Base64.getEncoder().encodeToString(encrypted);
        } catch (Exception e) {
            log.error("????: {}", plainText.substring(0, Math.min(10, plainText.length())) + "...", e);
            throw new RuntimeException("????", e);
        }
    }
    
    /**
     * 
     *
     * @param encryptedText Base64
     * @return 
     */
    public static String decrypt(String encryptedText) {
        if (encryptedText == null || encryptedText.isEmpty()) {
            return encryptedText;
        }
        
        try {
            byte[] encrypted = Base64.getDecoder().decode(encryptedText);
            byte[] decrypted = aes.decrypt(encrypted);
            return new String(decrypted, StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error("????", e);
            throw new RuntimeException("????", e);
        }
    }
    
    /**
     * Base64
     *
     * @param text 
     * @return true
     */
    public static boolean isEncrypted(String text) {
        if (text == null || text.isEmpty()) {
            return false;
        }
        
        try {
            Base64.getDecoder().decode(text);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
    
    /**
     *  - 
     *
     * @param text 
     * @return 
     */
    public static String encryptIfNeeded(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        
        // 
        if (isEncrypted(text)) {
            return text;
        }
        
        return encrypt(text);
    }
    
    /**
     * 
     *  .secret_key 
     */
    private static String getOrCreateSecretKey() {
        // 1. 
        String envKey = System.getenv("KING_DETECTIVE_SECRET_KEY");
        if (envKey != null && !envKey.isEmpty()) {
            log.info("? ????????????");
            return ensureKeyLength(envKey);
        }
        
        // 2. 
        java.io.File keyFile = new java.io.File(System.getProperty("user.dir"), ".secret_key");
        if (keyFile.exists()) {
            try {
                String fileKey = java.nio.file.Files.readString(keyFile.toPath(), StandardCharsets.UTF_8).trim();
                if (!fileKey.isEmpty()) {
                    log.info("? ?? .secret_key ????????");
                    return ensureKeyLength(fileKey);
                }
            } catch (Exception e) {
                log.warn("?? .secret_key ???????????", e);
            }
        }
        
        // 3. 
        String newKey = generateRandomKey();
        try {
            java.nio.file.Files.writeString(keyFile.toPath(), newKey, StandardCharsets.UTF_8);
            log.info("? ?????????????: {}", keyFile.getAbsolutePath());
            log.info("??  ???????????????????????");
        } catch (Exception e) {
            log.error("????????", e);
        }
        
        return newKey;
    }
    
    /**
     * 32
     */
    private static String generateRandomKey() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*";
        java.util.Random random = new java.security.SecureRandom();
        StringBuilder key = new StringBuilder(32);
        for (int i = 0; i < 32; i++) {
            key.append(chars.charAt(random.nextInt(chars.length())));
        }
        return key.toString();
    }
    
    /**
     * 32
     */
    private static String ensureKeyLength(String key) {
        if (key.length() >= 32) {
            return key.substring(0, 32);
        } else {
            return String.format("%-32s", key).replace(' ', '0');
        }
    }
    
    private EncryptionUtil() {
        throw new AssertionError("??????????");
    }
}
