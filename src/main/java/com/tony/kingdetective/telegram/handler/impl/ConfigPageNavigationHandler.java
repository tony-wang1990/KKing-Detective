package com.tony.kingdetective.telegram.handler.impl;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.extra.spring.SpringUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tony.kingdetective.bean.entity.OciUser;
import com.tony.kingdetective.service.IOciUserService;
import com.tony.kingdetective.telegram.builder.KeyboardBuilder;
import com.tony.kingdetective.telegram.handler.AbstractCallbackHandler;
import com.tony.kingdetective.telegram.storage.PaginationStorage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.botapimethods.BotApiMethod;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * ?
 * 
 * @author Tony Wang
 */
@Slf4j
@Component
public class ConfigPageNavigationHandler extends AbstractCallbackHandler {
    
    private static final String PAGE_TYPE = "config_list";
    private static final int PAGE_SIZE = 8;
    
    @Override
    public BotApiMethod<? extends Serializable> handle(CallbackQuery callbackQuery, TelegramClient telegramClient) {
        String data = callbackQuery.getData();
        boolean isNext = data.equals("config_page_next");
        
        IOciUserService userService = SpringUtil.getBean(IOciUserService.class);
        List<OciUser> userList = userService.list(new LambdaQueryWrapper<OciUser>()
                .select(OciUser::getId, OciUser::getUsername, OciUser::getOciRegion));
        
        if (CollectionUtil.isEmpty(userList)) {
            return buildEditMessage(
                    callbackQuery,
                    "�?暂无配置信息，请先添�?OCI 配置",
                    new InlineKeyboardMarkup(KeyboardBuilder.buildMainMenu())
            );
        }
        
        long chatId = callbackQuery.getMessage().getChatId();
        PaginationStorage paginationStorage = PaginationStorage.getInstance();
        
        int totalPages = PaginationStorage.calculateTotalPages(userList.size(), PAGE_SIZE);
        
        // 
        if (isNext) {
            paginationStorage.nextPage(chatId, PAGE_TYPE, totalPages);
        } else {
            paginationStorage.previousPage(chatId, PAGE_TYPE);
        }
        
        return buildConfigListMessage(callbackQuery, userList, chatId, paginationStorage);
    }
    
    /**
     * 
     */
    private BotApiMethod<? extends Serializable> buildConfigListMessage(
            CallbackQuery callbackQuery,
            List<OciUser> userList,
            long chatId,
            PaginationStorage paginationStorage) {
        
        int currentPage = paginationStorage.getCurrentPage(chatId, PAGE_TYPE);
        int totalPages = PaginationStorage.calculateTotalPages(userList.size(), PAGE_SIZE);
        int startIndex = PaginationStorage.getStartIndex(currentPage, PAGE_SIZE);
        int endIndex = PaginationStorage.getEndIndex(currentPage, PAGE_SIZE, userList.size());
        
        // 
        List<OciUser> pageUsers = userList.subList(startIndex, endIndex);
        
        List<InlineKeyboardRow> keyboard = new ArrayList<>();
        
        // ?
        List<InlineKeyboardRow> configRows = buildConfigRows(pageUsers);
        keyboard.addAll(configRows);
        
        // 
        if (totalPages > 1) {
            keyboard.add(KeyboardBuilder.buildPaginationRow(
                    currentPage,
                    totalPages,
                    "config_page_prev",
                    "config_page_next"
            ));
        }
        
        // 
        keyboard.add(KeyboardBuilder.buildBackToMainMenuRow());
        keyboard.add(KeyboardBuilder.buildCancelRow());
        
        String message = String.format(
                "【配置列表】\n\n�?%d 个配置，当前�?%d/%d 页\n请选择需要开机的配置�?",
                userList.size(),
                currentPage + 1,
                totalPages
        );
        
        return buildEditMessage(callbackQuery, message, new InlineKeyboardMarkup(keyboard));
    }
    
    /**
     * ?
     */
    private List<InlineKeyboardRow> buildConfigRows(List<OciUser> userList) {
        List<InlineKeyboardRow> rows = new ArrayList<>();
        for (int i = 0; i < userList.size(); i += 2) {
            InlineKeyboardRow row = new InlineKeyboardRow();
            
            OciUser user1 = userList.get(i);
            row.add(KeyboardBuilder.button(
                    String.format("%s [%s]", user1.getUsername(), user1.getOciRegion()),
                    "select_config:" + user1.getId()
            ));
            
            if (i + 1 < userList.size()) {
                OciUser user2 = userList.get(i + 1);
                row.add(KeyboardBuilder.button(
                        String.format("%s [%s]", user2.getUsername(), user2.getOciRegion()),
                        "select_config:" + user2.getId()
                ));
            }
            
            rows.add(row);
        }
        return rows;
    }
    
    @Override
    public String getCallbackPattern() {
        return "config_page_";
    }
    
    @Override
    public boolean canHandle(String callbackData) {
        return callbackData != null && 
               (callbackData.equals("config_page_prev") || callbackData.equals("config_page_next"));
    }
}
