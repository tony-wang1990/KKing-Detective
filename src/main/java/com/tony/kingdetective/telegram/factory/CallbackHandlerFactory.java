package com.tony.kingdetective.telegram.factory;

import com.tony.kingdetective.telegram.handler.CallbackHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * 
 * 
 * @author yohann
 */
@Slf4j
@Component
public class CallbackHandlerFactory {
    
    private final List<CallbackHandler> handlers;
    
    @Autowired
    public CallbackHandlerFactory(List<CallbackHandler> handlers) {
        this.handlers = handlers;
        log.info("??? {} ??????", handlers.size());
    }
    
    /**
     * 
     * 
     * @param callbackData 
     * @return 
     */
    public Optional<CallbackHandler> getHandler(String callbackData) {
        return handlers.stream()
                .filter(handler -> handler.canHandle(callbackData))
                .findFirst();
    }
}
