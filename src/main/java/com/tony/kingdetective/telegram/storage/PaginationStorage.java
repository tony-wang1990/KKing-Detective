package com.tony.kingdetective.telegram.storage;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Telegram Bot 
 * ?
 * 
 * @author Tony Wang
 */
public class PaginationStorage {
    
    private static final PaginationStorage INSTANCE = new PaginationStorage();
    
    // : chatId + pageType -> 
    private final Map<String, Integer> pageNumbers = new ConcurrentHashMap<>();
    
    // 
    public static final int DEFAULT_PAGE_SIZE = 8;
    
    private PaginationStorage() {
    }
    
    public static PaginationStorage getInstance() {
        return INSTANCE;
    }
    
    /**
     * ?
     * 
     * @param chatId ID
     * @param pageType  "config_list", "task_management"?
     * @return ?
     */
    private String buildKey(long chatId, String pageType) {
        return chatId + ":" + pageType;
    }
    
    /**
     * 
     * 
     * @param chatId ID
     * @param pageType 
     * @return 0
     */
    public int getCurrentPage(long chatId, String pageType) {
        return pageNumbers.getOrDefault(buildKey(chatId, pageType), 0);
    }
    
    /**
     * 
     * 
     * @param chatId ID
     * @param pageType 
     * @param page 
     */
    public void setCurrentPage(long chatId, String pageType, int page) {
        pageNumbers.put(buildKey(chatId, pageType), page);
    }
    
    /**
     * ?
     * 
     * @param chatId ID
     * @param pageType 
     */
    public void resetPage(long chatId, String pageType) {
        pageNumbers.remove(buildKey(chatId, pageType));
    }
    
    /**
     * ?
     * 
     * @param chatId ID
     * @param pageType 
     * @param totalPages ?
     * @return ?
     */
    public int nextPage(long chatId, String pageType, int totalPages) {
        int current = getCurrentPage(chatId, pageType);
        int next = Math.min(current + 1, totalPages - 1);
        setCurrentPage(chatId, pageType, next);
        return next;
    }
    
    /**
     * ?
     * 
     * @param chatId ID
     * @param pageType 
     * @return ?
     */
    public int previousPage(long chatId, String pageType) {
        int current = getCurrentPage(chatId, pageType);
        int prev = Math.max(current - 1, 0);
        setCurrentPage(chatId, pageType, prev);
        return prev;
    }
    
    /**
     * ?
     * 
     * @param totalItems ?
     * @param pageSize 
     * @return ?
     */
    public static int calculateTotalPages(int totalItems, int pageSize) {
        return (int) Math.ceil((double) totalItems / pageSize);
    }
    
    /**
     * 
     * 
     * @param page 0
     * @param pageSize 
     * @return 
     */
    public static int getStartIndex(int page, int pageSize) {
        return page * pageSize;
    }
    
    /**
     * 
     * 
     * @param page 0
     * @param pageSize 
     * @param totalItems ?
     * @return ?
     */
    public static int getEndIndex(int page, int pageSize, int totalItems) {
        return Math.min((page + 1) * pageSize, totalItems);
    }
    
    /**
     * ?
     * 
     * @param chatId ID
     */
    public void clearChat(long chatId) {
        String prefix = chatId + ":";
        pageNumbers.keySet().removeIf(key -> key.startsWith(prefix));
    }
}
