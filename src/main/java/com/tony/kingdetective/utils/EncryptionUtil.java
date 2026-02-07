package com.tony.kingdetective.utils;

import cn.hutool.crypto.SecureUtil;
import cn.hutool.crypto.symmetric.AES;
import lombok.extern.slf4j.Slf4j;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * 敏感信息加密工具类
 * 使用 AES-256 加密算法
 * 
 * @author Tony Wang
 */
@Slf4j
public class EncryptionUtil {
    
    private static final String ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES/ECB/PKCS5Padding";
    
    // 从环境变量获取密钥，如果不存在则使用默认密钥（生产环境必须设置环境变量）
    private static final String SECRET_KEY = System.getenv("KING_DETECTIVE_SECRET_KEY") != null
            ? System.getenv("KING_DETECTIVE_SECRET_KEY")
            : "KingDetective2026SecretKey!!"; // 32字符
    
    private static final AES aes;
    
    static {
        try {
            // 确保密钥长度为32字节(256位)
            String key = SECRET_KEY.length() >= 32
                    ? SECRET_KEY.substring(0, 32)
                    : String.format("%-32s", SECRET_KEY).replace(' ', '0');
            
            aes = SecureUtil.aes(key.getBytes(StandardCharsets.UTF_8));
            log.info("加密工具初始化成功，使用 AES-256 算法");
        } catch (Exception e) {
            log.error("加密工具初始化失败", e);
            throw new RuntimeException("加密工具初始化失败", e);
        }
    }
    
    /**
     * 加密字符串
     *
     * @param plainText 明文
     * @return Base64编码的密文
     */
    public static String encrypt(String plainText) {
        if (plainText == null || plainText.isEmpty()) {
            return plainText;
        }
        
        try {
            byte[] encrypted = aes.encrypt(plainText);
            return Base64.getEncoder().encodeToString(encrypted);
        } catch (Exception e) {
            log.error("加密失败: {}", plainText.substring(0, Math.min(10, plainText.length())) + "...", e);
            throw new RuntimeException("加密失败", e);
        }
    }
    
    /**
     * 解密字符串
     *
     * @param encryptedText Base64编码的密文
     * @return 明文
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
            log.error("解密失败", e);
            throw new RuntimeException("解密失败", e);
        }
    }
    
    /**
     * 判断字符串是否为加密格式（Base64）
     *
     * @param text 待检测字符串
     * @return true如果是加密格式
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
     * 加密敏感字段（智能加密 - 如果已经是加密格式则跳过）
     *
     * @param text 待加密文本
     * @return 加密后的文本
     */
    public static String encryptIfNeeded(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        
        // 如果已经加密，直接返回
        if (isEncrypted(text)) {
            return text;
        }
        
        return encrypt(text);
    }
}
