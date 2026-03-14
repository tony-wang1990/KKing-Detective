package com.tony.kingdetective.telegram.service;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.spring.SpringUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tony.kingdetective.bean.dto.SysUserDTO;
import com.tony.kingdetective.bean.entity.OciCreateTask;
import com.tony.kingdetective.bean.entity.OciUser;
import com.tony.kingdetective.bean.response.oci.traffic.FetchInstancesRsp;
import com.tony.kingdetective.config.OracleInstanceFetcher;
import com.tony.kingdetective.service.IOciCreateTaskService;
import com.tony.kingdetective.service.IOciUserService;
import com.tony.kingdetective.service.ISysService;
import com.tony.kingdetective.service.ITrafficService;
import com.tony.kingdetective.utils.CommonUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static com.tony.kingdetective.config.VirtualThreadConfig.VIRTUAL_EXECUTOR;
import static com.tony.kingdetective.service.impl.OciServiceImpl.TEMP_MAP;

/**
 * Telegram Bot 
 * 
 * @author Tony Wang
 */
@Slf4j
@Service
public class TelegramBotService {
    
    /**
     * ?OCI ?API 
     * 
     * @return 
     */
    public String checkAlive() {
        ISysService sysService = SpringUtil.getBean(ISysService.class);
        IOciUserService userService = SpringUtil.getBean(IOciUserService.class);
        
        List<String> ids = userService.listObjs(new LambdaQueryWrapper<OciUser>()
                .isNotNull(OciUser::getId)
                .select(OciUser::getId), String::valueOf);
        
        if (CollectionUtil.isEmpty(ids)) {
            return "????";
        }
        
        List<String> failNames = ids.parallelStream().filter(id -> {
            SysUserDTO ociUser = sysService.getOciUser(id);
            try (OracleInstanceFetcher fetcher = new OracleInstanceFetcher(ociUser)) {
                fetcher.getAvailabilityDomains();
            } catch (Exception e) {
                return true;
            }
            return false;
        }).map(id -> sysService.getOciUser(id).getUsername()).collect(Collectors.toList());
        
        return String.format(
                "?API?????\n\n" +
                "????????%s\n" +
                "????????%s\n" +
                "\uD83D\uDD11 ??????s\n" +
                "?\uFE0F ?????\n%s",
                ids.size() - failNames.size(),
                failNames.size(),
                ids.size(),
                CollectionUtil.isEmpty(failNames) ?" " : String.join("\n", failNames)
        );
    }
    
    /**
     * 
     * 
     * @return 
     */
    public String getTaskDetails() {
        IOciUserService userService = SpringUtil.getBean(IOciUserService.class);
        IOciCreateTaskService createTaskService = SpringUtil.getBean(IOciCreateTaskService.class);
        
        String message = "??????\n\n" +
                "\uD83D\uDD58 ???\t%s\n" +
                "\uD83D\uDECE ??????????\n%s\n";
        
        CompletableFuture<String> task = CompletableFuture.supplyAsync(() -> {
            List<OciCreateTask> ociCreateTaskList = createTaskService.list();
            if (ociCreateTaskList.isEmpty()) {
                return "?";
            }
            
            String template = "[%s] [%s] [%s] [%s??%sGB/%sGB] [%s?] [%s] [%s?]";
            return ociCreateTaskList.parallelStream().map(x -> {
                OciUser ociUser = userService.getById(x.getUserId());
                Long counts = (Long) TEMP_MAP.get(CommonUtils.CREATE_COUNTS_PREFIX + x.getId());
                return String.format(
                        template,
                        ociUser.getUsername(),
                        ociUser.getOciRegion(),
                        x.getArchitecture(),
                        x.getOcpus().longValue(),
                        x.getMemory().longValue(),
                        x.getDisk(),
                        x.getCreateNumbers(),
                        CommonUtils.getTimeDifference(x.getCreateTime()),
                        counts == null ? "0" : counts
                );
            }).collect(Collectors.joining("\n"));
        }, VIRTUAL_EXECUTOR);
        
        CompletableFuture.allOf(task).join();
        
        return String.format(
                message,
                LocalDateTime.now().format(CommonUtils.DATETIME_FMT_NORM),
                task.join()
        );
    }
    
    /**
     * 
     * 
     * @return 
     */
    public String getTrafficStatistics() {
        IOciUserService userService = SpringUtil.getBean(IOciUserService.class);
        ITrafficService trafficService = SpringUtil.getBean(ITrafficService.class);
        
        List<OciUser> ociUserList = userService.list();
        if (CollectionUtil.isEmpty(ociUserList)) {
            return "??????";
        }
        
        return "??????\n\n" + Optional.ofNullable(userService.list())
                .filter(CollectionUtil::isNotEmpty)
                .orElseGet(Collections::emptyList)
                .parallelStream()
                .map(ociCfg -> {
                    FetchInstancesRsp fetchInstancesRsp;
                    try {
                        fetchInstancesRsp = trafficService.fetchInstances(ociCfg.getId(), ociCfg.getOciRegion());
                    } catch (Exception e) {
                        return "";
                    }
                    return String.format(
                            "\uD83D\uDD58 ????s\n" +
                            " ??????s?\n" +
                            " ??????s?\n" +
                            "\uD83D\uDDA5 ???????s???\n" +
                            "????????????s\n" +
                            "????????????s\n",
                            LocalDateTime.now().format(CommonUtils.DATETIME_FMT_NORM),
                            ociCfg.getUsername(),
                            ociCfg.getOciRegion(),
                            fetchInstancesRsp.getInstanceCount(),
                            fetchInstancesRsp.getInboundTraffic(),
                            fetchInstancesRsp.getOutboundTraffic()
                    );
                })
                .filter(StrUtil::isNotBlank)
                .collect(Collectors.joining("\n"));
    }
}
