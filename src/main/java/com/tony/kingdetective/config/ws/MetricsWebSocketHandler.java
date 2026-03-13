package com.tony.kingdetective.config.ws;

import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import cn.hutool.jwt.JWTUtil;
import com.tony.kingdetective.exception.OciException;
import com.tony.kingdetective.utils.CommonUtils;
import jakarta.websocket.OnClose;
import jakarta.websocket.OnMessage;
import jakarta.websocket.OnOpen;
import jakarta.websocket.Session;
import jakarta.websocket.server.PathParam;
import jakarta.websocket.server.ServerEndpoint;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;
import oshi.hardware.GlobalMemory;
import oshi.hardware.HardwareAbstractionLayer;
import oshi.hardware.NetworkIF;

import java.io.IOException;
import java.time.LocalTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static com.tony.kingdetective.service.impl.OciServiceImpl.TEMP_MAP;

/**
 * @author Tony Wang
 * @date 2024-12-25 16:23:39
 */
@Slf4j
@Component
@ServerEndpoint("/metrics/{token}")
public class MetricsWebSocketHandler {

    private static final ConcurrentHashMap<String, Session> SESSION_MAP = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, Boolean> IS_OPEN_MAP = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, Future<?>> FUTURE_MAP = new ConcurrentHashMap<>();
    Map<String, Object> metrics = new HashMap<>();
    List<String> timestamps = new LinkedList<>();
    List<Double> inRates = new LinkedList<>();
    List<Double> outRates = new LinkedList<>();
    int interval = 5;
    int size = 15;

    private boolean validateToken(String token) {
        return !CommonUtils.isTokenExpired(token) && JWTUtil.verify(token, ((String) TEMP_MAP.get("password")).getBytes());
    }

    @OnOpen
    public void onOpen(Session session, @PathParam(value = "token") String token) {
        if (token == null || !validateToken(token)) {
            throw new OciException(-1, "无效的token");
        }

        // 如果已存在旧�?session，先关闭�?
        Session oldSession = SESSION_MAP.get(token);
        if (oldSession != null) {
            try {
                oldSession.close();
            } catch (IOException e) {
                log.error("Close old session error", e);
            }
        }

        SESSION_MAP.put(token, session);
        IS_OPEN_MAP.put(token, true);

        genCpuMemData(token);
        execGenTrafficData(token);
    }

    @OnClose
    public void onClose(Session session, @PathParam(value = "token") String token) {
        SESSION_MAP.remove(token);
        IS_OPEN_MAP.remove(token);
        // 取消正在运行的任�?
        Future<?> future = FUTURE_MAP.remove(token);
        if (future != null && !future.isDone()) {
            future.cancel(true);
        }
    }

    @OnMessage
    public void onMessage(String message) {
        log.info("【WebSocket消息】收到客户端消息�? + message);
    }

    /**
     * 此为单点消息
     *
     * @param message 消息
     */
    public void sendOneMessage(Session session, String message) {
        if (session != null && session.isOpen()) {
            try {
                synchronized (session) {
                    session.getAsyncRemote().sendText(message);
                }
            } catch (Exception e) {
                log.error("仪表盘数据推送失�?, e);
            }
        }
    }

    private void genCpuMemData(String token) {
        SystemInfo systemInfo = new SystemInfo();

        // 获取 CPU 使用�?
        HardwareAbstractionLayer hardware = systemInfo.getHardware();
        CentralProcessor processor = hardware.getProcessor();
        long[] systemCpuLoadTicks = processor.getSystemCpuLoadTicks();
        double cpu = processor.getSystemCpuLoadBetweenTicks(systemCpuLoadTicks) * 100;
        String cpuUsage = String.format("%.2f", cpu);
        metrics.put("cpuUsage", MapUtil.builder()
                .put("used", cpuUsage)
                .put("free", String.valueOf(100 - Double.parseDouble(cpuUsage)))
                .build());

        // 获取内存使用情况
        GlobalMemory memory = systemInfo.getHardware().getMemory();
        long totalMemory = memory.getTotal();
        long availableMemory = memory.getAvailable();
        long usedMemory = totalMemory - availableMemory;
        // 计算内存使用率百分比
        double usedMemoryPercentage = ((double) usedMemory / totalMemory) * 100;
        double freeMemoryPercentage = ((double) availableMemory / totalMemory) * 100;

        metrics.put("memoryUsage", MapUtil.builder()
                .put("used", String.format("%.2f", usedMemoryPercentage))
                .put("free", String.format("%.2f", freeMemoryPercentage))
                .build());

        timestamps.sort((t1, t2) -> {
            LocalTime time1 = LocalTime.parse(t1);
            LocalTime time2 = LocalTime.parse(t2);
            return time1.compareTo(time2);
        });

        metrics.put("trafficData", MapUtil.builder()
                .put("timestamps", timestamps)
                .put("inbound", inRates)
                .put("outbound", outRates)
                .build());

        // 发送消息时使用对应�?session
        Session userSession = SESSION_MAP.get(token);
        if (userSession != null && userSession.isOpen()) {
            sendOneMessage(userSession, JSONUtil.toJsonStr(metrics));
        }
    }

    private void execGenTrafficData(String token) {
        Future<?> future = Executors.newSingleThreadExecutor().submit(() -> {
            SystemInfo systemInfo = new SystemInfo();
            List<NetworkIF> networkIFs = systemInfo.getHardware().getNetworkIFs();

            NetworkIF networkIF = networkIFs.stream()
                    .filter(NetworkIF::isConnectorPresent) // 必须是有物理连接
                    .filter(iface -> !Arrays.asList(iface.getIPv4addr()).isEmpty() || !Arrays.asList(iface.getIPv6addr()).isEmpty()) // 必须�?IP 地址
                    .filter(iface -> iface.getName().startsWith("e"))
                    .min((a, b) -> Long.compare(b.getSpeed(), a.getSpeed())) // 找到第一个匹配的网卡
                    .orElse(null); // 如果没有匹配，返�?null

            if (null != networkIF) {
                networkIF.updateAttributes();
                double previousRxBytes = networkIF.getBytesRecv();
                double previousTxBytes = networkIF.getBytesSent();

                double currentRxBytes = networkIF.getBytesRecv() / 1024.0;
                double currentTxBytes = networkIF.getBytesSent() / 1024.0;

                // 计算当前秒的流量速率（单位：KB/s�?
                double rxRate = (currentRxBytes - previousRxBytes) / 1024.0;
                double txRate = (currentTxBytes - previousTxBytes) / 1024.0;

                // 更新上一秒的字节�?
                previousRxBytes = currentRxBytes;
                previousTxBytes = currentTxBytes;

                while (IS_OPEN_MAP.getOrDefault(token, false)) {
                    Calendar calendar = Calendar.getInstance();

                    try {
                        Thread.sleep(interval * 1000L); // 每秒更新一�?
                    } catch (InterruptedException e) {

                    }
                    networkIF.updateAttributes();

                    currentRxBytes = networkIF.getBytesRecv() / 1024.0;
                    currentTxBytes = networkIF.getBytesSent() / 1024.0;

                    // 计算当前秒的流量速率（单位：KB/s�?
                    rxRate = (currentRxBytes - previousRxBytes) / 1024.0;
                    txRate = (currentTxBytes - previousTxBytes) / 1024.0;

                    // 更新上一秒的字节�?
                    previousRxBytes = currentRxBytes;
                    previousTxBytes = currentTxBytes;

                    // 维护队列大小�?0
                    if (inRates.size() == size) {
                        inRates.remove(0);
                    }
                    if (outRates.size() == size) {
                        outRates.remove(0);
                    }
                    if (timestamps.size() == size) {
                        timestamps.remove(0);
                    }
                    inRates.add(Double.valueOf(String.format("%.2f", rxRate)));
                    outRates.add(Double.valueOf(String.format("%.2f", txRate)));
                    timestamps.add(String.format("%02d:%02d:%02d",
                            calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), calendar.get(Calendar.SECOND)));
                    calendar.add(Calendar.SECOND, -interval);

                    genCpuMemData(token);
                }
            }
        });

        FUTURE_MAP.put(token, future);
    }
}
