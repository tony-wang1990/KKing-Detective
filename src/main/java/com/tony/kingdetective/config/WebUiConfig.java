package com.tony.kingdetective.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 🌐 Web UI 路由配置
 * 将前端单页应用 (SPA) 的路由请求映射到 index.html
 */
@Configuration
public class WebUiConfig implements WebMvcConfigurer {

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        // 映射根路径
        registry.addViewController("/").setViewName("forward:/index.html");
        
        // 如果有其他前端路由（如 /dashboard, /settings 等），也转发到 index.html
        // 因为前端用的 Alpine.js/Vue 等 SPA，由前端自己接管路由
        registry.addViewController("/{path:[^\\.]+}").setViewName("forward:/index.html");
        registry.addViewController("/**/{path:[^\\.]+}").setViewName("forward:/index.html");
    }
}
