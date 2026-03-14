package com.tony.kingdetective.telegram.storage;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Telegram Bot 
 * 
 * 
 * @author Tony Wang
 */
public class TaskSelectionStorage {
    
    private static final TaskSelectionStorage INSTANCE = new TaskSelectionStorage();
    
    // : chatId -> ID
    private final Map<Long, Set<String>> selections = new ConcurrentHashMap<>();
    
    private TaskSelectionStorage() {
    }
    
    public static TaskSelectionStorage getInstance() {
        return INSTANCE;
    }
    
    /**
     * ?
     * 
     * @param chatId ID
     * @param taskId ID
     * @return truefalse
     */
    public boolean toggleTask(long chatId, String taskId) {
        Set<String> selected = selections.computeIfAbsent(chatId, k -> new HashSet<>());
        
        if (selected.contains(taskId)) {
            selected.remove(taskId);
            return false;
        } else {
            selected.add(taskId);
            return true;
        }
    }
    
    /**
     * 
     * 
     * @param chatId ID
     * @param taskId ID
     */
    public void selectTask(long chatId, String taskId) {
        selections.computeIfAbsent(chatId, k -> new HashSet<>()).add(taskId);
    }
    
    /**
     * 
     * 
     * @param chatId ID
     * @param taskId ID
     */
    public void deselectTask(long chatId, String taskId) {
        Set<String> selected = selections.get(chatId);
        if (selected != null) {
            selected.remove(taskId);
        }
    }
    
    /**
     * 
     * 
     * @param chatId ID
     * @param taskId ID
     * @return true
     */
    public boolean isSelected(long chatId, String taskId) {
        Set<String> selected = selections.get(chatId);
        return selected != null && selected.contains(taskId);
    }
    
    /**
     * ?
     * 
     * @param chatId ID
     * @return ID
     */
    public Set<String> getSelectedTasks(long chatId) {
        return new HashSet<>(selections.getOrDefault(chatId, new HashSet<>()));
    }
    
    /**
     * 
     * 
     * @param chatId ID
     */
    public void clearSelection(long chatId) {
        selections.remove(chatId);
    }
}
