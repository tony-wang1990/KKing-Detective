package com.tony.kingdetective.constant;

/**
 * OCI 
 * 
 * @author Tony Wang
 */
public class OciConstants {
    
    // ====================  ====================
    
    /** API  */
    public static final int MAX_RETRY_COUNT = 10;
    
    /**  */
    public static final long RETRY_DELAY_MS = 30000;
    
    /** Work Request  */
    public static final long WORK_REQUEST_POLL_INTERVAL_MS = 5000;
    
    // ====================  ====================
    
    /**  CIDR  */
    public static final String DEFAULT_CIDR_BLOCK = "10.0.0.0/16";
    
    /**  CIDR */
    public static final String DEFAULT_SUBNET_CIDR = "10.0.0.0/24";
    
    /** SSH  */
    public static final int SSH_DEFAULT_PORT = 22;
    
    /** VNC  */
    public static final int VNC_DEFAULT_PORT = 5901;
    
    /** HTTP  */
    public static final int HTTP_DEFAULT_PORT = 80;
    
    /** HTTPS  */
    public static final int HTTPS_DEFAULT_PORT = 443;
    
    // ====================  ====================
    
    /**  */
    public static final int INSTANCE_CREATE_TIMEOUT_MINUTES = 30;
    
    /**  Shape */
    public static final String DEFAULT_SHAPE = "VM.Standard.A1.Flex";
    
    /**  OCPU  */
    public static final int DEFAULT_OCPU_COUNT = 4;
    
    /** GB */
    public static final int DEFAULT_MEMORY_GB = 24;
    
    /** GB */
    public static final int DEFAULT_BOOT_VOLUME_SIZE_GB = 50;
    
    // ==================== 0Mbps NLB  ====================
    
    /** NLB  */
    public static final String NLB_NAME_PREFIX = "king-detective-nlb-";
    
    /** NAT  */
    public static final String NAT_GATEWAY_NAME_PREFIX = "king-detective-nat-";
    
    /**  */
    public static final String ROUTE_TABLE_NAME_PREFIX = "king-detective-rt-";
    
    /** NLB Mbps */
    public static final int NLB_BANDWIDTH_MBPS = 500;
    
    // ==================== API  ====================
    
    /** / */
    public static final double QUOTA_QUERY_RATE_LIMIT = 2.0;
    
    /** / */
    public static final double COST_QUERY_RATE_LIMIT = 1.0;
    
    /** / */
    public static final double INSTANCE_CREATE_RATE_LIMIT = 0.5;
    
    /** / */
    public static final double USER_RATE_LIMIT = 10.0;
    
    // ====================  ====================
    
    /**  */
    public static final int USER_CONFIG_CACHE_MINUTES = 10;
    
    /**  */
    public static final int REGION_LIST_CACHE_MINUTES = 60;
    
    /**  */
    public static final int QUOTA_CACHE_MINUTES = 5;
    
    /**  */
    public static final int INSTANCE_LIST_CACHE_MINUTES = 2;
    
    // ==================== Telegram Bot  ====================
    
    /**  */
    public static final int MAX_MESSAGE_LENGTH = 4096;
    
    /**  */
    public static final int MAX_BUTTON_TEXT_LENGTH = 64;
    
    /**  */
    public static final int PAGE_SIZE = 10;
    
    // ====================  ====================
    
    /**  */
    public static final int PASSWORD_MIN_LENGTH = 8;
    
    /**  */
    public static final int USERNAME_MIN_LENGTH = 3;
    
    /**  */
    public static final int USERNAME_MAX_LENGTH = 32;
    
    /** API  */
    public static final int API_KEY_MIN_LENGTH = 32;
    
    // ====================  ====================
    
    /**  */
    public static final String BACKUP_DIR = "backups";
    
    /**  */
    public static final String LOG_DIR = "logs";
    
    /**  */
    public static final String TEMP_DIR = "temp";
    
    // ==================== HTTP  ====================
    
    /**  */
    public static final int HTTP_TOO_MANY_REQUESTS = 429;
    
    /**  */
    public static final int HTTP_SERVICE_UNAVAILABLE = 503;
    
    /**  */
    public static final int HTTP_GATEWAY_TIMEOUT = 504;
    
    // ====================  ====================
    
    public static class MessageTemplate {
        /**  */
        public static final String CREATE_SUCCESS_BROADCAST = 
            "【开机任务】 \n\n🎉 用户：[%s] 开机成功 🎉\n\n" +
            "📍 区域：%s\n" +
            "💾 配置：%s | %dC%dG\n" +
            "📊 计费：%s\n" +
            "🌐 公网IP：%s\n" +
            "🔐 SSH端口：%d\n\n" +
            "%s";
        
        /**  */
        public static final String CREATE_FAILURE_BROADCAST = 
            "【开机失败】\n\n❌ 用户：[%s]\n\n" +
            "📍 区域：%s\n" +
            "💾 配置：%s\n" +
            "❗错误：%s";
        
        /**  */
        public static final String TASK_COMPLETE = 
            "✅ 任务完成\n\n%s";
        
        /**  */
        public static final String TASK_FAILED = 
            "❌ 任务失败\n\n%s\n\n错误：%s";
        
        /** API  */
        public static final String API_CALL_FAILED = 
            "❌ API 调用失败\n\n" +
            "接口：%s\n" +
            "错误：%s";
        
        /**  */
        public static final String PERMISSION_DENIED = 
            "❌ 权限不足\n\n" +
            "您没有权限执行此操作";
        
        /**  */
        public static final String OPERATION_SUCCESS = 
            "✅ 操作成功\n\n%s";
        
        /**  */
        public static final String OPERATION_FAILED = 
            "❌ 操作失败\n\n%s";
    }
    
    // ====================  ====================
    
    public static class ConfigKey {
        /** VNC URL  */
        public static final String VNC_URL = "SYS_VNC";
        
        /**  */
        public static final String DAILY_REPORT_ENABLED = "daily_report_enabled";
        
        /**  */
        public static final String INSTANCE_MONITORING_ENABLED = "instance_monitoring_enabled";
        
        /**  */
        public static final String AUTO_RESTART_ENABLED = "auto_restart_enabled";
        
        /**  */
        public static final String DEFENSE_MODE_ENABLED = "defense_mode_enabled";
        
        /**  */
        public static final String AUTO_REGION_EXPANSION_ENABLED = "auto_region_expansion_enabled";
    }
    
    private OciConstants() {
        // 
        throw new AssertionError("常量类不应该被实例化");
    }
}
