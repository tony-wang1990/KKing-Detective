package com.tony.kingdetective.telegram.factory;

import com.tony.kingdetective.telegram.handler.CallbackHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * 回调处理器工�?
 * 
 * @author Tony Wang
 */
@Slf4j
@Component
public class CallbackHandlerFactory {
    
    private final List<CallbackHandler> handlers;
    
    @Autowired
    public CallbackHandlerFactory(List<CallbackHandler> handlers) {
        this.handlers = handlers;
        log.info("已加�?{} 个回调处理器", handlers.size());
    }
    
    /**
     * 根据回调数据获取处理�?
     * 
     * @param callbackData 回调数据
     * @return 处理�?
     */
    public Optional<CallbackHandler> getHandler(String callbackData) {
        return handlers.stream()
                .filter(handler -> handler.canHandle(callbackData))
                .findFirst();
    }
}
