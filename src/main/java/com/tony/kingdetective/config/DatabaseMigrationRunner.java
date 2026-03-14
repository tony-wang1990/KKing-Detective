package com.tony.kingdetective.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.stream.Collectors;

/**
 * 
 * 
 * 
 * @author Tony Wang
 */
@Slf4j
@Component
public class DatabaseMigrationRunner implements ApplicationRunner {
    
    @Autowired
    private DataSource dataSource;
    
    @Override
    public void run(ApplicationArguments args) throws Exception {
        log.info("================== ????????? ==================");
        
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            
            //  ()
            if (!tableExists(conn, "audit_log") || 
                !tableExists(conn, "ip_blacklist") || 
                !tableExists(conn, "login_attempts")) {
                log.info("??????????????? v4.0 ????...");
                executeMigrationScript(stmt);
                log.info("? ????????");
            } else {
                log.info("? ??????????????");
            }
            
        } catch (Exception e) {
            log.error("? ???????", e);
            // 
            log.warn("??????????????????");
        }
        
        log.info("================== ????????? ==================");
    }
    
    /**
     * 
     */
    private boolean tableExists(Connection conn, String tableName) throws Exception {
        String sql = "SELECT name FROM sqlite_master WHERE type='table' AND name='" + tableName + "'";
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            return rs.next();
        }
    }
    
    /**
     * 
     */
    private void executeMigrationScript(Statement stmt) throws Exception {
        //  SQL 
        ClassPathResource resource = new ClassPathResource("db/migration_v4_0.sql");
        
        String sql;
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
            sql = reader.lines().collect(Collectors.joining("\n"));
        }
        
        //  SQL 
        String[] statements = sql.split(";");
        for (String statement : statements) {
            String trimmed = statement.trim();
            if (!trimmed.isEmpty() && !trimmed.startsWith("--")) {
                try {
                    log.debug("?? SQL: {}", trimmed.substring(0, Math.min(50, trimmed.length())) + "...");
                    stmt.execute(trimmed);
                } catch (Exception e) {
                    //  "duplicate column"  "table already exists" 
                    String msg = e.getMessage().toLowerCase();
                    if (msg.contains("duplicate column") || msg.contains("exists")) {
                        log.warn("????????: {}", trimmed.split("\n")[0]);
                    } else {
                        throw e;
                    }
                }
            }
        }
    }
}
