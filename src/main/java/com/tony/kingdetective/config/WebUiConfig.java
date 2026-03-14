package com.tony.kingdetective.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 *  Web UI 
 *  (SPA)  index.html
 */
@Configuration
public class WebUiConfig implements WebMvcConfigurer {

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        // 
        registry.addViewController("/").setViewName("forward:/index.html");
        
        //  /dashboard, /settings  index.html
        //  Alpine.js/Vue  SPA
        registry.addViewController("/{path:[^\\.]+}").setViewName("forward:/index.html");
        registry.addViewController("/**/{path:[^\\.]+}").setViewName("forward:/index.html");
    }
}
