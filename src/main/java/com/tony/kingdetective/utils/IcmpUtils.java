package com.tony.kingdetective.utils;

import cn.hutool.core.util.RuntimeUtil;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @ClassName IcmpUtils
 * @Description:
 * @author: Tony Wang_Fan
 * @CreateTime: 2025-04-30 13:58
 **/
public class IcmpUtils {

    /**
     *  Ping 
     *
     * @param hostOrIp IPv4  IPv6
     * @param count    
     * @param timeout  
     * @return Ping 
     */
    public static String ping(String hostOrIp, int count, int timeout) {
        boolean isWindows = System.getProperty("os.name").toLowerCase().contains("win");
        boolean isIpv6 = isIpv6Address(hostOrIp);

        String cmd;
        if (isWindows) {
            // Windows  -n -w 
            if (isIpv6) {
                cmd = String.format("ping -n %d -w %d %s", count, timeout, hostOrIp);
            } else {
                cmd = String.format("ping -n %d -w %d %s", count, timeout, hostOrIp);
            }
        } else {
            // Linux/macOS  -c -W/-w 
            int timeoutSec = Math.max(1, timeout / 1000);  // 1
            if (isIpv6) {
                cmd = String.format("ping6 -c %d -W %d %s", count, timeoutSec, hostOrIp);
            } else {
                cmd = String.format("ping -c %d -W %d %s", count, timeoutSec, hostOrIp);
            }
        }

        try {
            return RuntimeUtil.execForStr(cmd);
        } catch (Exception e) {
            return "Ping ?????" + e.getMessage();
        }
    }

    /**
     *  IPv6 
     */
    private static boolean isIpv6Address(String address) {
        try {
            InetAddress inetAddress = InetAddress.getByName(address);
            return inetAddress instanceof java.net.Inet6Address;
        } catch (UnknownHostException e) {
            return address.contains(":"); // fallback 
        }
    }

    /**
     *  ping  100% 
     *
     * Windows/Linux/Mac  ping 
     *
     * @param pingOutput ping 
     * @return true 
     */
    public static boolean isPacketLoss100(String pingOutput) {
        if (pingOutput == null || pingOutput.isEmpty()) {
            return true;
        }

        //  Sent = 4, Received = 0  Packets: Sent = 4, Received = 0
        Pattern pattern = Pattern.compile(
                "(?i)(Sent|??|Transmitted)\\s*=*\\s*(\\d+)[^\\d]+(Received|??|received)\\s*=*\\s*(\\d+)",
                Pattern.MULTILINE);

        Matcher matcher = pattern.matcher(pingOutput);
        while (matcher.find()) {
            try {
                int sent = Integer.parseInt(matcher.group(2));
                int received = Integer.parseInt(matcher.group(4));
                return sent > 0 && received == 0;
            } catch (NumberFormatException e) {
                return false;  // 
            }
        }

        //  packet loss 
        Pattern fallbackLoss = Pattern.compile("([1-9][0-9]*)%\\s*packet loss");
        Matcher m2 = fallbackLoss.matcher(pingOutput);
        if (m2.find()) {
            return "100".equals(m2.group(1));
        }

        return false;
    }

    // 
    public static void main(String[] args) {
        String result = ping("s14.serv00.com", 4, 3000);
        System.out.println(result);

        if (isPacketLoss100(result)) {
            System.out.println("?? ??????100% ??");
        } else {
            System.out.println("? ???????????");
        }
    }
}
