package com.tony.kingdetective.telegram.service;

import cn.hutool.extra.ssh.JschUtil;
import com.jcraft.jsch.ChannelExec;
import com.tony.kingdetective.telegram.utils.MarkdownFormatter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.*;

/**
 * SSH Service for Telegram Bot
 * Provides SSH command execution functionality with timeout and interactive command protection
 * 
 * @author Tony Wang
 */
@Slf4j
@Service
public class SshService {
    
    // Command execution timeout (30 seconds)
    private static final int COMMAND_TIMEOUT_SECONDS = 30;
    
    // Interactive commands that should be blocked
    private static final List<String> INTERACTIVE_COMMANDS = Arrays.asList(
        "vi", "vim", "nano", "emacs", "top", "htop", "less", "more", 
        "tail -f", "watch", "ssh", "telnet", "ftp", "mysql", "psql",
        "python", "node", "irb", "php -a"
    );
    
    /**
     * Execute SSH command with timeout protection
     * 
     * @param host SSH host
     * @param port SSH port
     * @param username SSH username
     * @param password SSH password
     * @param command command to execute
     * @return command output
     */
    public String executeCommand(String host, int port, String username, String password, String command) {
        // Check for interactive commands
        if (isInteractiveCommand(command)) {
            log.warn("Blocked interactive command: {}", command);
            return "�?不支持交互式命令\n\n" +
                   "检测到交互式命令（�?vi, top, ssh 等），这些命令会导致阻塞。\n" +
                   "请使用非交互式命令，例如：\n" +
                   "�?使用 `cat` 查看文件而不�?`vi`\n" +
                   "�?使用 `ps aux` 查看进程而不�?`top`\n" +
                   "�?使用 `head` �?`tail` 查看日志而不�?`tail -f`";
        }
        
        com.jcraft.jsch.Session session = null;
        ExecutorService executor = Executors.newSingleThreadExecutor();
        
        try {
            // Create and connect SSH session
            session = JschUtil.openSession(host, port, username, password);
            final com.jcraft.jsch.Session finalSession = session;
            
            // Execute command with timeout
            Future<String> future = executor.submit(() -> executeWithTimeout(finalSession, command));
            
            try {
                // Wait for command execution with timeout
                String result = future.get(COMMAND_TIMEOUT_SECONDS, TimeUnit.SECONDS);
                log.info("SSH command executed successfully: host={}, command={}", host, command);
                return result;
                
            } catch (TimeoutException e) {
                // Command timeout
                future.cancel(true);
                log.warn("SSH command timeout: host={}, command={}", host, command);
                return "⏱️ 命令执行超时（超�?" + COMMAND_TIMEOUT_SECONDS + " 秒）\n\n" +
                       "可能原因：\n" +
                       "�?命令执行时间过长\n" +
                       "�?命令需要交互式输入\n" +
                       "�?命令正在等待用户确认\n\n" +
                       "建议：使用更快的命令或添加参数避免交�?;
                       
            } catch (ExecutionException e) {
                log.error("SSH command execution failed: host={}, command={}", host, command, e);
                return "�?执行命令失败: " + e.getCause().getMessage();
            }
            
        } catch (Exception e) {
            log.error("Failed to execute SSH command: host={}, command={}", host, command, e);
            return "�?执行命令失败: " + e.getMessage();
        } finally {
            // Close session and shutdown executor
            if (session != null) {
                JschUtil.close(session);
            }
            executor.shutdownNow();
        }
    }
    
    /**
     * Execute command with custom channel (for better timeout control)
     */
    private String executeWithTimeout(com.jcraft.jsch.Session session, String command) throws Exception {
        ChannelExec channel = null;
        try {
            channel = (ChannelExec) session.openChannel("exec");
            channel.setCommand(command);
            
            // Set input stream to null to avoid interactive prompts
            channel.setInputStream(null);
            channel.setErrStream(System.err);
            
            // Get output stream
            InputStream in = channel.getInputStream();
            InputStream err = channel.getErrStream();
            
            // Connect channel
            channel.connect();
            
            // Read output
            StringBuilder output = new StringBuilder();
            StringBuilder errorOutput = new StringBuilder();
            
            BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
            BufferedReader errorReader = new BufferedReader(new InputStreamReader(err, StandardCharsets.UTF_8));
            
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
            
            while ((line = errorReader.readLine()) != null) {
                errorOutput.append(line).append("\n");
            }
            
            // Wait for channel to close
            while (!channel.isClosed()) {
                Thread.sleep(100);
            }
            
            int exitStatus = channel.getExitStatus();
            
            // Check exit status
            if (exitStatus != 0 && errorOutput.length() > 0) {
                return "�?命令执行失败 (退出码: " + exitStatus + ")\n\n" + errorOutput.toString();
            }
            
            return output.toString();
            
        } finally {
            if (channel != null) {
                channel.disconnect();
            }
        }
    }
    
    /**
     * Check if command is interactive
     */
    private boolean isInteractiveCommand(String command) {
        String normalizedCommand = command.trim().toLowerCase();
        
        // Check against known interactive commands
        for (String interactiveCmd : INTERACTIVE_COMMANDS) {
            // Check if command starts with the interactive command
            if (normalizedCommand.equals(interactiveCmd) || 
                normalizedCommand.startsWith(interactiveCmd + " ")) {
                return true;
            }
        }
        
        // Check for sudo commands requiring password (without -S flag)
        if (normalizedCommand.startsWith("sudo ") && !normalizedCommand.contains("-S")) {
            log.warn("Detected sudo command without -S flag, might require password");
            // Allow sudo but warn user
            return false;
        }
        
        return false;
    }
    
    /**
     * Test SSH connection
     * 
     * @param host SSH host
     * @param port SSH port
     * @param username SSH username
     * @param password SSH password
     * @return true if connection successful
     */
    public boolean testConnection(String host, int port, String username, String password) {
        com.jcraft.jsch.Session session = null;
        try {
            // Create and connect session to test connection
            session = JschUtil.openSession(host, port, username, password);
            boolean connected = session != null && session.isConnected();
            log.info("SSH connection test: host={}, result={}", host, connected);
            return connected;
        } catch (Exception e) {
            log.error("SSH connection test failed: host={}", host, e);
            return false;
        } finally {
            if (session != null) {
                JschUtil.close(session);
            }
        }
    }
    
    /**
     * Format command output for Telegram
     * 
     * @param output raw output
     * @return formatted output with Markdown code block
     */
    public String formatOutput(String output) {
        if (output == null || output.trim().isEmpty()) {
            return "�?命令执行成功（无输出�?;
        }
        
        // Check if output is already an error message (starts with emoji)
        if (output.startsWith("�?) || output.startsWith("⏱️")) {
            return output; // Don't wrap error messages in code blocks
        }
        
        // Truncate if too long (Telegram limit is 4096 characters)
        String truncated = MarkdownFormatter.truncate(output, 3800);
        
        // Wrap in code block for better formatting
        return "```\n" + truncated + "\n```";
    }
}
