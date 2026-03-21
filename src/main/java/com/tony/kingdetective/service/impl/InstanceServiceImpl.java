package com.tony.kingdetective.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.date.DatePattern;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.oracle.bmc.core.ComputeClient;
import com.oracle.bmc.core.VirtualNetworkClient;
import com.oracle.bmc.core.model.*;
import com.oracle.bmc.core.requests.*;
import com.oracle.bmc.model.BmcException;
import com.oracle.bmc.networkloadbalancer.NetworkLoadBalancerClient;
import com.oracle.bmc.networkloadbalancer.model.*;
import com.oracle.bmc.networkloadbalancer.requests.CreateNetworkLoadBalancerRequest;
import com.oracle.bmc.networkloadbalancer.requests.DeleteNetworkLoadBalancerRequest;
import com.oracle.bmc.networkloadbalancer.requests.GetNetworkLoadBalancerRequest;
import com.oracle.bmc.networkloadbalancer.requests.ListNetworkLoadBalancersRequest;
import com.tony.kingdetective.bean.Tuple2;
import com.tony.kingdetective.bean.constant.CacheConstant;
import com.tony.kingdetective.bean.dto.CreateInstanceDTO;
import com.tony.kingdetective.bean.dto.InstanceCfgDTO;
import com.tony.kingdetective.bean.dto.InstanceDetailDTO;
import com.tony.kingdetective.bean.dto.SysUserDTO;
import com.tony.kingdetective.bean.entity.OciCreateTask;
import com.tony.kingdetective.bean.entity.OciKv;
import com.tony.kingdetective.bean.params.oci.instance.Close500MParams;
import com.tony.kingdetective.bean.params.oci.instance.CreateNetworkLoadBalancerParams;
import com.tony.kingdetective.bean.params.oci.instance.UpdateShapeParams;
import com.tony.kingdetective.config.OracleInstanceFetcher;
import com.tony.kingdetective.enums.ArchitectureEnum;
import com.tony.kingdetective.enums.OciRegionsEnum;
import com.tony.kingdetective.enums.SysCfgEnum;
import com.tony.kingdetective.exception.OciException;
import com.tony.kingdetective.service.IInstanceService;
import com.tony.kingdetective.service.IOciCreateTaskService;
import com.tony.kingdetective.service.IOciKvService;
import com.tony.kingdetective.service.ISysService;
import com.tony.kingdetective.utils.CommonUtils;
import com.tony.kingdetective.utils.CustomExpiryGuavaCache;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import static com.oracle.bmc.core.model.RouteTable.LifecycleState.Available;
import static com.tony.kingdetective.service.impl.OciServiceImpl.TEMP_MAP;

/**
 * <p>
 * InstanceServiceImpl
 * </p >
 *
 * @author yuhui.fan
 * @since 2024/11/11 14:30
 */
@Slf4j
@Service
public class InstanceServiceImpl implements IInstanceService {

    @Resource
    private ISysService sysService;
    @Resource
    private IOciCreateTaskService createTaskService;
    @Resource
    private IOciKvService kvService;
    @Resource
    private ExecutorService virtualExecutor;
    @Resource
    private CustomExpiryGuavaCache<String, Object> customCache;

    @Value("${oci-cfg.boot-broadcast-url}")
    private String bootBroadcastUrl;

    private static final String LEGACY_MESSAGE_TEMPLATE =
            "?????? \n\n? ???[%s] ???? ?\n" +
                    "??? %s\n" +
                    "Region? %s\n" +
                    "CPU??? %s\n" +
                    "CPU? %s\n" +
                    "???GB?? %s\n" +
                    "?????GB?? %s\n" +
                    "Shape? %s\n" +
                    "??IP? %s\n" +
                    "root??? %s\n" +
                    "?????%s\n" +
                    "?????%s";
    private static final String CHANNEL_MESSAGE_TEMPLATE =
            "??????????????\n\n" +
                    "??? %s\n" +
                    "Region? %s\n" +
                    "??? %s\n" +
                    "CPU??? %s\n" +
                    "CPU???? %s\n" +
                    "???GB?? %s\n" +
                    "?????GB?? %s\n" +
                    "?????%s\n" +
                    "?????%s";

    @Override
    public List<SysUserDTO.CloudInstance> listRunningInstances(SysUserDTO sysUserDTO) {
        try (OracleInstanceFetcher fetcher = new OracleInstanceFetcher(sysUserDTO)) {
            return fetcher.listInstances().parallelStream()
                    .map(x -> SysUserDTO.CloudInstance.builder()
                            .region(x.getRegion())
                            .name(x.getDisplayName())
                            .ocId(x.getId())
                            .shape(x.getShape())
                            .publicIp(fetcher.listInstanceIPs(x.getId()).stream().map(Vnic::getPublicIp).collect(Collectors.toList()))
                            .build())
                    .collect(Collectors.toList());
        } catch (Exception e) {
            throw new OciException(-1, "????????");
        }

    }

    @Override
    public CreateInstanceDTO createInstance(OracleInstanceFetcher fetcher) {
        Long currentCount = (Long) TEMP_MAP.compute(
                CommonUtils.CREATE_COUNTS_PREFIX + fetcher.getUser().getTaskId(),
                (key, value) -> value == null ? 1L : Long.parseLong(String.valueOf(value)) + 1
        );
        log.info("????????:[{}],??:[{}],????:[{}],????:[{}],????? [{}] ???????...",
                fetcher.getUser().getUsername(), fetcher.getUser().getOciCfg().getRegion(),
                fetcher.getUser().getArchitecture(), fetcher.getUser().getCreateNumbers(), currentCount);

        OciKv bootBroadcastTokenCfg = kvService.getOne(new LambdaQueryWrapper<OciKv>()
                .eq(OciKv::getCode, SysCfgEnum.BOOT_BROADCAST_TOKEN.getCode()));
        OciCreateTask createTask = createTaskService.getById(fetcher.getUser().getTaskId());
        List<InstanceDetailDTO> instanceList = new ArrayList<>();
        for (int i = 0; i < fetcher.getUser().getCreateNumbers(); i++) {
            InstanceDetailDTO instanceDetail = fetcher.createInstanceData();
            if (instanceDetail.isTooManyReq()) {
                log.info("????????:[{}],??:[{}],????:[{}],????:[{}],??? [{}] ??????????? [{}] ??????,??????",
                        fetcher.getUser().getUsername(), fetcher.getUser().getOciCfg().getRegion(),
                        fetcher.getUser().getArchitecture(), fetcher.getUser().getCreateNumbers(), currentCount, i + 1
                );
                break;
            }
            instanceList.add(instanceDetail);

            if (instanceDetail.isSuccess()) {
                log.info("---------------- ? ??:[{}]????,CPU??:{},??IP: {},root??: {} ? ----------------",
                        instanceDetail.getUsername(), instanceDetail.getArchitecture(),
                        instanceDetail.getPublicIp(), instanceDetail.getRootPassword());
                String message = String.format(LEGACY_MESSAGE_TEMPLATE,
                        instanceDetail.getUsername(),
                        LocalDateTime.now().format(DateTimeFormatter.ofPattern(DatePattern.NORM_DATETIME_PATTERN)),
                        instanceDetail.getRegion(),
                        instanceDetail.getArchitecture(),
                        instanceDetail.getOcpus().longValue(),
                        instanceDetail.getMemory().longValue(),
                        instanceDetail.getDisk(),
                        instanceDetail.getShape(),
                        instanceDetail.getPublicIp(),
                        instanceDetail.getRootPassword(),
                        currentCount,
                        createTask == null ? "??" : CommonUtils.getTimeDifference(createTask.getCreateTime())
                );

                sysService.sendMessage(message);

                virtualExecutor.execute(() -> {
                    // 
                    if (Arrays.asList("ARM", "AMD").contains(instanceDetail.getArchitecture())) {
                        String arch = instanceDetail.getArchitecture().toLowerCase();
                        try (HttpResponse response = HttpRequest.get(bootBroadcastUrl)
                                .form("region", OciRegionsEnum.getKeyById(instanceDetail.getRegion()))
                                .form("arch", arch)
                                .form("token", bootBroadcastTokenCfg.getValue())
                                .timeout(20_000)
                                .execute()) {

                            int status = response.getStatus();
                            String body = response.body();

                            if (status == 200) {
//                                log.info(",status:{}", status);
//                                log.info(",body:{}", body);
                                log.info("????????");
                            } else {
                                log.warn("??????,status:{},body:{}", status, body);
                            }

                        } catch (Exception e) {
                            log.error("??????", e);
                        }
                    }

                    // TG 
                    if (fetcher.getUser().isJoinChannelBroadcast()) {
                        String channelMsg = String.format(CHANNEL_MESSAGE_TEMPLATE,
                                LocalDateTime.now().format(DateTimeFormatter.ofPattern(DatePattern.NORM_DATETIME_PATTERN)),
                                instanceDetail.getRegion(),
                                OciRegionsEnum.getNameById(instanceDetail.getRegion()).get(),
                                instanceDetail.getArchitecture(),
                                instanceDetail.getOcpus().longValue(),
                                instanceDetail.getMemory().longValue(),
                                instanceDetail.getDisk(),
                                currentCount,
                                createTask == null ? "??" : CommonUtils.getTimeDifference(createTask.getCreateTime()));
                        try (HttpResponse response = HttpRequest.get(bootBroadcastChannel)
                                .form("text", channelMsg)
                                .timeout(20_000)
                                .execute()) {
                            int status = response.getStatus();
                            String body = response.body();

                            if (status == 200) {
                                log.info("??????????");
                            } else {
                                log.warn("????????,status:{},body:{}", status, body);
                            }
                        } catch (Exception e) {
                            log.error("????????", e);
                        }
                    }
                });

            }
        }

        return new CreateInstanceDTO(instanceList);
    }

    @Override
    public Tuple2<String, Instance> changeInstancePublicIp(String instanceId,
                                                           String vnicId,
                                                           SysUserDTO sysUserDTO,
                                                           List<String> cidrList) {
        String publicIp = null;
        String instanceName = null;
        Instance instance = null;
        Tuple2<String, Instance> tuple2;
        try (OracleInstanceFetcher fetcher = new OracleInstanceFetcher(sysUserDTO)) {
            instance = fetcher.getInstanceById(instanceId);
            instanceName = instance.getDisplayName();
            publicIp = fetcher.reassignEphemeralPublicIp(fetcher.getVirtualNetworkClient().getVnic(GetVnicRequest.builder()
                    .vnicId(vnicId)
                    .build()).getVnic());
            tuple2 = Tuple2.of(publicIp, instance);
            return tuple2;
        } catch (BmcException ociException) {
            log.error("?????IP???:[{}],??:[{}],??:[{}],????IP??,??:{}",
                    sysUserDTO.getUsername(), sysUserDTO.getOciCfg().getRegion(), instanceName,
                    ociException.getLocalizedMessage());
            tuple2 = Tuple2.of(publicIp, instance);
        } catch (Exception e) {
            log.error("?????IP???:[{}],??:[{}],??:[{}],????IP????:{}",
                    sysUserDTO.getUsername(), sysUserDTO.getOciCfg().getRegion(), instanceName,
                    e.getLocalizedMessage());
            tuple2 = Tuple2.of(publicIp, instance);
        }
        return tuple2;
    }

    @Override
    public InstanceCfgDTO getInstanceCfgInfo(SysUserDTO sysUserDTO, String instanceId) {
        String instanceName = null;
        try (OracleInstanceFetcher fetcher = new OracleInstanceFetcher(sysUserDTO)) {
            Instance instance = fetcher.getInstanceById(instanceId);
            instanceName = instance.getDisplayName();
            return fetcher.getInstanceCfg(instanceId);
        } catch (Exception e) {
            log.error("??:[{}],??:[{}],??:[{}] ??????????,??:{}",
                    sysUserDTO.getUsername(), sysUserDTO.getOciCfg().getRegion(),
                    instanceName, e.getLocalizedMessage(), e);
            throw new OciException(-1, "??????????");
        }
    }

    @Override
    public void releaseSecurityRule(SysUserDTO sysUserDTO) {
        try (OracleInstanceFetcher fetcher = new OracleInstanceFetcher(sysUserDTO)) {
            List<Vcn> vcns = fetcher.listVcn();
            if (null == vcns || vcns.isEmpty()) {
                throw new OciException(-1, "???????VCN,????????");
            }
            vcns.parallelStream().forEach(x -> {
                fetcher.releaseSecurityRule(x, 0, "0.0.0.0/0", "::/0");
                log.info("??:[{}],??:[{}],?? vcn: [{}] ?????????????",
                        sysUserDTO.getUsername(), sysUserDTO.getOciCfg().getRegion(), x.getDisplayName());
            });
        } catch (Exception e) {
            log.error("??:[{}],??:[{}],?????????????,??:{}",
                    sysUserDTO.getUsername(), sysUserDTO.getOciCfg().getRegion(),
                    e.getLocalizedMessage(), e);
            throw new OciException(-1, "???????????????");
        }
    }

    @Override
    public String createIpv6(SysUserDTO sysUserDTO, String instanceId) {
        String instanceName = null;
        try (OracleInstanceFetcher fetcher = new OracleInstanceFetcher(sysUserDTO)) {
            Vcn vcn = fetcher.getVcnByInstanceId(instanceId);
            Instance instance = fetcher.getInstanceById(instanceId);
            Vnic vnic = fetcher.getVnicByInstanceId(instanceId);
            Ipv6 ipv6 = fetcher.createIpv6(vnic, vcn);
            instanceName = instance.getDisplayName();
            log.info("??:[{}],??:[{}],??:[{}] ?? IPV6 ??,IPV6??:{}",
                    sysUserDTO.getUsername(), sysUserDTO.getOciCfg().getRegion(),
                    instanceName, ipv6.getIpAddress());
            return ipv6.getIpAddress();
        } catch (Exception e) {
            log.error("??:[{}],??:[{}],??:[{}] ?? IPV6 ??,??:{}",
                    sysUserDTO.getUsername(), sysUserDTO.getOciCfg().getRegion(),
                    instanceName, e.getLocalizedMessage(), e);
            throw new OciException(-1, "?? IPV6 ??");
        }
    }

    @Override
    public void updateInstanceName(SysUserDTO sysUserDTO, String instanceId, String name) {
        String instanceName = null;
        try (OracleInstanceFetcher fetcher = new OracleInstanceFetcher(sysUserDTO)) {
            instanceName = fetcher.getInstanceById(instanceId).getDisplayName();
            fetcher.updateInstanceName(instanceId, name);
            log.info("??:[{}],??:[{}],??:[{}] ??????",
                    sysUserDTO.getUsername(), sysUserDTO.getOciCfg().getRegion(), instanceName);
        } catch (Exception e) {
            log.error("??:[{}],??:[{}],??:[{}] ??????,??:{}",
                    sysUserDTO.getUsername(), sysUserDTO.getOciCfg().getRegion(),
                    instanceName, e.getLocalizedMessage(), e);
            throw new OciException(-1, "????????");
        }
    }

    @Override
    public void updateInstanceCfg(SysUserDTO sysUserDTO, String instanceId, float ocpus, float memory) {
        String instanceName = null;
        try (OracleInstanceFetcher fetcher = new OracleInstanceFetcher(sysUserDTO)) {
            instanceName = fetcher.getInstanceById(instanceId).getDisplayName();
            fetcher.updateInstanceCfg(instanceId, ocpus, memory);
            log.info("??:[{}],??:[{}],??:[{}] ????????",
                    sysUserDTO.getUsername(), sysUserDTO.getOciCfg().getRegion(), instanceName);
        } catch (Exception e) {
            log.error("??:[{}],??:[{}],??:[{}] ????????,??:{}",
                    sysUserDTO.getUsername(), sysUserDTO.getOciCfg().getRegion(),
                    instanceName, e.getLocalizedMessage(), e);
            throw new OciException(-1, "????????");
        }
    }

    @Override
    public void updateBootVolumeCfg(SysUserDTO sysUserDTO, String instanceId, long size, long vpusPer) {
        String bootVolumeName = null;
        try (OracleInstanceFetcher fetcher = new OracleInstanceFetcher(sysUserDTO)) {
            BootVolume bootVolume = fetcher.getBootVolumeByInstanceId(instanceId);
            bootVolumeName = bootVolume.getDisplayName();
            fetcher.updateBootVolumeCfg(bootVolume.getId(), size, vpusPer);
            log.info("??:[{}],??:[{}],???:[{}] ?????????",
                    sysUserDTO.getUsername(), sysUserDTO.getOciCfg().getRegion(), bootVolumeName);
        } catch (Exception e) {
            log.error("??:[{}],??:[{}],???:[{}] ?????????,??:{}",
                    sysUserDTO.getUsername(), sysUserDTO.getOciCfg().getRegion(),
                    bootVolumeName, e.getLocalizedMessage(), e);
            throw new OciException(-1, "?????????");
        }
    }

    @Override
    public void oneClick500M(CreateNetworkLoadBalancerParams params) {
        virtualExecutor.execute(() -> {
            SysUserDTO sysUserDTO = sysService.getOciUser(params.getOciCfgId());
            String publicIp = null;
            String instanceName = null;
            try (OracleInstanceFetcher fetcher = new OracleInstanceFetcher(sysUserDTO);) {
                String compartmentId = fetcher.getCompartmentId();
                VirtualNetworkClient virtualNetworkClient = fetcher.getVirtualNetworkClient();

                // AMDvnicnat
                String instanceId = params.getInstanceId();
                Instance instance = fetcher.getInstanceById(instanceId);
                instanceName = instance.getDisplayName();
                if (!instance.getShape().contains(ArchitectureEnum.AMD.getShapeDetail())) {
                    log.error("???????500Mbps?????Shape: [{}] ?????????500Mbps", instance.getShape());
                    throw new OciException(-1, "????????????500Mbps");
                }

                Vcn vcn = fetcher.getVcnByInstanceId(instanceId);
                Vnic vnic = fetcher.getVnicByInstanceId(instanceId);
                String instanceVnicId = vnic.getId();
                String instancePriIp = virtualNetworkClient.listPrivateIps(ListPrivateIpsRequest.builder()
                        .vnicId(vnic.getId())
                        .build()).getItems().getFirst().getIpAddress();

                // NAT
                NatGateway natGateway;
                List<NatGateway> natGatewayList = virtualNetworkClient.listNatGateways(ListNatGatewaysRequest.builder()
                        .compartmentId(compartmentId)
                        .lifecycleState(NatGateway.LifecycleState.Available)
                        .vcnId(vcn.getId())
                        .build()).getItems();
                if (CollectionUtil.isNotEmpty(natGatewayList)) {
                    natGateway = natGatewayList.getFirst();
                    log.info("???????500Mbps??????????NAT??: " + natGateway.getDisplayName());
                } else {
                    natGateway = virtualNetworkClient.createNatGateway(CreateNatGatewayRequest.builder()
                            .createNatGatewayDetails(CreateNatGatewayDetails.builder()
                                    .vcnId(vcn.getId())
                                    .compartmentId(compartmentId)
                                    .displayName("nat-gateway")
                                    .build())
                            .build()).getNatGateway();

                    while (!virtualNetworkClient.getNatGateway(GetNatGatewayRequest.builder()
                            .natGatewayId(natGateway.getId())
                            .build()).getNatGateway().getLifecycleState().getValue().equals(NatGateway.LifecycleState.Available.getValue())) {
                        Thread.sleep(1000);
                    }
                    log.info("???????500Mbps???NAT??????: " + natGateway.getDisplayName());
                }

                // 
                RouteTable routeTable = null;
                List<RouteTable> routeTableList = virtualNetworkClient.listRouteTables(ListRouteTablesRequest.builder()
                        .vcnId(vcn.getId())
                        .compartmentId(compartmentId)
                        .lifecycleState(Available)
                        .build()).getItems();
                try {
                    if (CollectionUtil.isNotEmpty(routeTableList)) {
                        for (RouteTable table : routeTableList) {
                            for (RouteRule routeRule : table.getRouteRules()) {
                                if (routeRule.getNetworkEntityId().equals(natGateway.getId()) && routeRule.getCidrBlock().equals("0.0.0.0/0")
                                        && routeRule.getDestinationType().getValue().equals(RouteRule.DestinationType.CidrBlock.getValue())) {
                                    routeTable = table;
                                    break;
                                }
                            }
                            if (routeTable != null) {
                                break;
                            }
                        }
                    }
                } catch (Exception e) {

                }

                if (routeTable != null) {
                    for (Instance x : fetcher.listInstances()) {
                        if (x.getShape().contains(ArchitectureEnum.AMD.getShapeDetail())) {
                            Vnic xvnic = fetcher.getVnicByInstanceId(x.getId());
                            if (StrUtil.isNotBlank(xvnic.getRouteTableId()) && xvnic.getRouteTableId().equals(routeTable.getId())
                                    && !x.getId().equals(instanceId)) {
                                throw new OciException(-1, "??????AMD????NAT???");
                            }
                        }
                    }
                }

                log.warn("???????500Mbps?????:[{}],??:[{}],??:[{}] ??????????500Mbps??...", sysUserDTO.getUsername(), sysUserDTO.getOciCfg().getRegion(), instance.getDisplayName());

                // 
                Subnet subnet = virtualNetworkClient.listSubnets(ListSubnetsRequest.builder()
                        .compartmentId(compartmentId)
                        .vcnId(vcn.getId())
                        .build()).getItems().getFirst();

                // 
                NetworkLoadBalancerClient networkLoadBalancerClient = fetcher.getNetworkLoadBalancerClient();
                List<NetworkLoadBalancerSummary> networkLoadBalancerSummaries = networkLoadBalancerClient.listNetworkLoadBalancers(ListNetworkLoadBalancersRequest.builder()
                        .compartmentId(compartmentId)
                        .lifecycleState(LifecycleState.Active)
                        .build()).getNetworkLoadBalancerCollection().getItems();
                if (CollectionUtil.isNotEmpty(networkLoadBalancerSummaries)) {
                    networkLoadBalancerSummaries.forEach(x -> {
                        log.info("???????500Mbps??????????????: " + x.getDisplayName());
                        networkLoadBalancerClient.deleteNetworkLoadBalancer(DeleteNetworkLoadBalancerRequest.builder()
                                .networkLoadBalancerId(x.getId())
                                .build());
                    });
                }

                log.info("???????500Mbps??????????????...");

                NetworkLoadBalancer networkLoadBalancer = null;
                boolean isNormal = false;
                int retryCount = 0;
                final int MAX_RETRY = 10;

                while (!isNormal) {
                    try {
                        networkLoadBalancer = networkLoadBalancerClient.createNetworkLoadBalancer(CreateNetworkLoadBalancerRequest.builder()
                                .createNetworkLoadBalancerDetails(CreateNetworkLoadBalancerDetails.builder()
                                        .displayName("nlb-" + LocalDateTime.now().format(CommonUtils.DATETIME_FMT_PURE))
                                        .compartmentId(compartmentId)
                                        .isPrivate(false)
                                        .subnetId(subnet.getId())
                                        .listeners(Map.of(
                                                "listener1", ListenerDetails.builder()
                                                        .name("listener1")
                                                        .defaultBackendSetName("backend1")
                                                        .protocol(ListenerProtocols.TcpAndUdp)
                                                        .port(0)
                                                        .build()
                                        ))
                                        .backendSets(Map.of(
                                                "backend1", BackendSetDetails.builder()
                                                        .isPreserveSource(true)
                                                        .isFailOpen(true)
                                                        .policy(NetworkLoadBalancingPolicy.TwoTuple)
                                                        .healthChecker(HealthChecker.builder()
                                                                .protocol(HealthCheckProtocols.Tcp)
                                                                .port(params.getSshPort())
                                                                .build())
                                                        .backends(Collections.singletonList(Backend.builder()
                                                                .targetId(instanceId)
                                                                .ipAddress(instancePriIp)
                                                                .port(0)
                                                                .weight(1)
                                                                .build()))
                                                        .build()
                                        ))
                                        .build())
                                .build()).getNetworkLoadBalancer();

                        isNormal = true;
                    } catch (Exception e) {
                        retryCount++;
                        log.warn("???????500Mbps???? " + retryCount + " ????????????,???...");
                        if (retryCount >= MAX_RETRY) {
                            log.error("???????500Mbps?????????????????? " + MAX_RETRY + " ?,????");
                            throw new OciException(-1, "???????????????????", e);
                        }
                        Thread.sleep(30000);
                    }
                }

                while (!networkLoadBalancerClient.getNetworkLoadBalancer(GetNetworkLoadBalancerRequest.builder()
                        .networkLoadBalancerId(networkLoadBalancer.getId())
                        .build()).getNetworkLoadBalancer().getLifecycleState().getValue().equals(LifecycleState.Active.getValue())) {
                    Thread.sleep(1000);
                }

                log.info("???????500Mbps??????????????");
                for (IpAddress x : networkLoadBalancerClient.getNetworkLoadBalancer(GetNetworkLoadBalancerRequest.builder()
                        .networkLoadBalancerId(networkLoadBalancer.getId())
                        .build()).getNetworkLoadBalancer().getIpAddresses()) {
                    if (!CommonUtils.isPrivateIp(x.getIpAddress())) {
                        publicIp = x.getIpAddress();
                        log.info("???????500Mbps????????????IP:" + x.getIpAddress());
                    }
                }

                // NAT
                if (routeTable != null) {
                    virtualNetworkClient.updateRouteTable(UpdateRouteTableRequest.builder()
                            .rtId(routeTable.getId())
                            .updateRouteTableDetails(UpdateRouteTableDetails.builder()
                                    .routeRules(Collections.singletonList(RouteRule.builder()
                                            .cidrBlock("0.0.0.0/0")
                                            .networkEntityId(natGateway.getId())
                                            .destinationType(RouteRule.DestinationType.CidrBlock)
                                            .build()))
                                    .build())
                            .build());
                    log.info("???????500Mbps??????????NAT???:" + routeTable.getDisplayName());
                } else {
                    routeTable = virtualNetworkClient.createRouteTable(CreateRouteTableRequest.builder()
                            .createRouteTableDetails(CreateRouteTableDetails.builder()
                                    .compartmentId(compartmentId)
                                    .vcnId(vcn.getId())
                                    .displayName("nat-route")
                                    .routeRules(Collections.singletonList(RouteRule.builder()
                                            .cidrBlock("0.0.0.0/0")
                                            .networkEntityId(natGateway.getId())
                                            .destinationType(RouteRule.DestinationType.CidrBlock)
                                            .build()))
                                    .build())
                            .build()).getRouteTable();

                    while (!virtualNetworkClient.getRouteTable(GetRouteTableRequest.builder()
                            .rtId(routeTable.getId())
                            .build()).getRouteTable().getLifecycleState().getValue().equals(Available.getValue())) {
                        Thread.sleep(1000);
                    }

                    log.info("???????500Mbps???NAT???????:" + routeTable.getDisplayName());
                }

                // vnic,/
                virtualNetworkClient.updateVnic(UpdateVnicRequest.builder()
                        .vnicId(instanceVnicId)
                        .updateVnicDetails(UpdateVnicDetails.builder()
                                .skipSourceDestCheck(true)
                                .routeTableId(routeTable.getId())
                                .build())
                        .build());

                // 
                fetcher.releaseSecurityRule(vcn, 0, "10.0.0.0/16", "::/0");

                log.info("???????500Mbps?????vnic???????,??:?{}????????500Mbps?,??IP:{}", instance.getDisplayName(), publicIp);
                sysService.sendMessage(String.format("???????500Mbps?????:[%s],??:[%s],??:[%s] ???????500Mbps?,??IP:%s",
                        sysUserDTO.getUsername(), sysUserDTO.getOciCfg().getRegion(), instance.getDisplayName(), publicIp));
                customCache.remove(CacheConstant.PREFIX_NETWORK_LOAD_BALANCER + params.getOciCfgId());
            } catch (Exception e) {
                log.error("???????500Mbps?????:[{}],??:[{}],??:[{}] ????500Mbps???",
                        sysUserDTO.getUsername(), sysUserDTO.getOciCfg().getRegion(), instanceName, e);
                sysService.sendMessage(String.format("???????500Mbps?????:[%s],??:[%s],??:[%s] ????500Mbps???,??:%s",
                        sysUserDTO.getUsername(), sysUserDTO.getOciCfg().getRegion(), instanceName, e.getLocalizedMessage()));
            }
        });
    }

    @Override
    public void oneClickClose500M(Close500MParams params) {
        virtualExecutor.execute(() -> {
            String instanceName = null;
            SysUserDTO sysUserDTO = sysService.getOciUser(params.getOciCfgId());
            try (OracleInstanceFetcher fetcher = new OracleInstanceFetcher(sysUserDTO)) {
                VirtualNetworkClient virtualNetworkClient = fetcher.getVirtualNetworkClient();
                Instance instance = fetcher.getInstanceById(params.getInstanceId());
                instanceName = instance.getDisplayName();
                Vcn vcn = fetcher.getVcnByInstanceId(params.getInstanceId());
                Vnic vnic = fetcher.getVnicByInstanceId(params.getInstanceId());

                if (!instance.getShape().contains(ArchitectureEnum.AMD.getShapeDetail())) {
                    log.error("???????500Mbps?????Shape: [{}] ?????????500Mbps", instance.getShape());
                    throw new OciException(-1, "??????????500Mbps");
                }

                List<RouteTable> routeTableList = virtualNetworkClient.listRouteTables(ListRouteTablesRequest.builder()
                        .compartmentId(fetcher.getCompartmentId())
                        .vcnId(vcn.getId())
                        .lifecycleState(Available)
                        .build()).getItems();
                if (CollectionUtil.isEmpty(routeTableList)) {
                    throw new OciException(-1, "???????");
                }

                // Nat
                List<RouteTable> routeTables = new ArrayList<>(routeTableList);
                List<NatGateway> natGatewayList = virtualNetworkClient.listNatGateways(ListNatGatewaysRequest.builder()
                        .compartmentId(fetcher.getCompartmentId())
                        .lifecycleState(NatGateway.LifecycleState.Available)
                        .vcnId(vcn.getId())
                        .build()).getItems();
                if (CollectionUtil.isNotEmpty(natGatewayList)) {
                    for (NatGateway natGateway : natGatewayList) {
                        // 
                        try {
                            if (CollectionUtil.isNotEmpty(routeTableList)) {
                                for (RouteTable table : routeTableList) {
                                    if (CollectionUtil.isEmpty(table.getRouteRules())) {
                                        routeTables.removeIf(x -> x.getId().equals(table.getId()));
                                        continue;
                                    }
                                    for (RouteRule routeRule : table.getRouteRules()) {
                                        if (routeRule.getNetworkEntityId().equals(natGateway.getId()) && routeRule.getCidrBlock().equals("0.0.0.0/0")
                                                && routeRule.getDestinationType().getValue().equals(RouteRule.DestinationType.CidrBlock.getValue())) {
                                            routeTables.removeIf(x -> x.getId().equals(table.getId()));
                                            if (!params.getRetainNatGw()) {
                                                log.info("???????500Mbps??????????:[{}]...", table.getDisplayName());
                                                // 
                                                virtualNetworkClient.updateRouteTable(UpdateRouteTableRequest.builder()
                                                        .rtId(table.getId())
                                                        .updateRouteTableDetails(UpdateRouteTableDetails.builder()
                                                                .routeRules(Collections.emptyList())
                                                                .build())
                                                        .build());
                                            }
                                            break;
                                        }
                                    }
                                }
                            }
                        } catch (Exception e) {
                            log.error("???????500Mbps?????:[{}],??:[{}],??:[{}] ??????? ?",
                                    sysUserDTO.getUsername(), sysUserDTO.getOciCfg().getRegion(), instanceName, e);
                        }
                        // NAT
                        if (!params.getRetainNatGw()) {
                            log.info("???????500Mbps???????NAT??:[{}] ...", natGateway.getDisplayName());
                            virtualNetworkClient.deleteNatGateway(DeleteNatGatewayRequest.builder()
                                    .natGatewayId(natGateway.getId())
                                    .build());
                        }
                    }
                }

                // vnic
                log.info("???????500Mbps???????vnic:[{}] ?????:[{}]...", vnic.getDisplayName(), routeTables.getFirst().getDisplayName());
                virtualNetworkClient.updateVnic(UpdateVnicRequest.builder()
                        .vnicId(vnic.getId())
                        .updateVnicDetails(UpdateVnicDetails.builder()
                                .skipSourceDestCheck(true)
                                .routeTableId(routeTables.getFirst().getId())
                                .build())
                        .build());

                try {
                    for (RouteTable rt : routeTableList) {
                        if (!rt.getId().equals(routeTables.getFirst().getId())) {
                            log.info("???????500Mbps???????NAT???:[{}]...", rt.getDisplayName());
                            virtualNetworkClient.deleteRouteTable(DeleteRouteTableRequest.builder()
                                    .rtId(rt.getId())
                                    .build());
                        }
                    }
                } catch (Exception e) {
                    log.error("???????500Mbps?????:[{}],??:[{}],??:[{}] ??????? ?",
                            sysUserDTO.getUsername(), sysUserDTO.getOciCfg().getRegion(), instanceName, e);
                }

                // 
                if (!params.getRetainBl()) {
                    NetworkLoadBalancerClient networkLoadBalancerClient = fetcher.getNetworkLoadBalancerClient();
                    List<NetworkLoadBalancerSummary> networkLoadBalancerSummaries = networkLoadBalancerClient.listNetworkLoadBalancers(ListNetworkLoadBalancersRequest.builder()
                            .compartmentId(fetcher.getCompartmentId())
                            .build()).getNetworkLoadBalancerCollection().getItems();
                    for (NetworkLoadBalancerSummary networkLoadBalancerSummary : networkLoadBalancerSummaries) {
                        log.info("???????500Mbps??????????????:[{}] ...", networkLoadBalancerSummary.getDisplayName());
                        networkLoadBalancerClient.deleteNetworkLoadBalancer(DeleteNetworkLoadBalancerRequest.builder()
                                .networkLoadBalancerId(networkLoadBalancerSummary.getId())
                                .build());
                    }
                }

                log.info("???????500Mbps?????:[{}],??:[{}],??:[{}] ?????????500Mbps??",
                        sysUserDTO.getUsername(), sysUserDTO.getOciCfg().getRegion(), instanceName);
                sysService.sendMessage(String.format("???????500Mbps?????:[%s],??:[%s],??:[%s] ?????????500Mbps?",
                        sysUserDTO.getUsername(), sysUserDTO.getOciCfg().getRegion(), instance.getDisplayName()));
            } catch (Exception e) {
                log.error("???????500Mbps?????:[{}],??:[{}],??:[{}] ????500Mbps???",
                        sysUserDTO.getUsername(), sysUserDTO.getOciCfg().getRegion(), instanceName, e);
                sysService.sendMessage(String.format("???????500Mbps?????:[%s],??:[%s],??:[%s] ????500Mbps???,??:%s",
                        sysUserDTO.getUsername(), sysUserDTO.getOciCfg().getRegion(), instanceName, e.getLocalizedMessage()));
            }
        });
    }

    @Override
    public void updateInstanceShape(UpdateShapeParams params) {
        SysUserDTO sysUserDTO = sysService.getOciUser(params.getOciCfgId());
        try (OracleInstanceFetcher fetcher = new OracleInstanceFetcher(sysUserDTO)) {
            ComputeClient computeClient = fetcher.getComputeClient();
            if (params.getShape().equals(ArchitectureEnum.AMD.getShapeDetail())) {
                computeClient.updateInstance(UpdateInstanceRequest.builder()
                        .instanceId(params.getInstanceId())
                        .updateInstanceDetails(UpdateInstanceDetails.builder()
                                .shape(params.getShape())
                                .shapeConfig(UpdateInstanceShapeConfigDetails.builder()
                                        .ocpus(1f)
                                        .memoryInGBs(1f)
                                        .build())
                                .build())
                        .build());
            } else {
                computeClient.updateInstance(UpdateInstanceRequest.builder()
                        .instanceId(params.getInstanceId())
                        .updateInstanceDetails(UpdateInstanceDetails.builder()
                                .shape(params.getShape())
                                .shapeConfig(UpdateInstanceShapeConfigDetails.builder()
                                        .ocpus(1f)
                                        .memoryInGBs(2f)
                                        .build())
                                .build())
                        .build());
            }
        } catch (Exception e) {
            log.error("更新Shape失败，账号:[{}],地区:[{}],实例ID:[{}] 修改 Shape 为:[{}] 失败",
                    sysUserDTO.getUsername(), sysUserDTO.getOciCfg().getRegion(), params.getInstanceId(), params.getShape(), e);
            throw new OciException(-1, "修改 Shape 为:" + params.getShape() + " 失败");
        }
    }

    // ==================== 以下为补充实现的接口方法 ====================

    @Override
    public void createSnapshot(com.tony.kingdetective.bean.params.oci.instance.CreateSnapshotParams params) {
        SysUserDTO sysUserDTO = sysService.getOciUser(params.getOciCfgId());
        try (OracleInstanceFetcher fetcher = new OracleInstanceFetcher(sysUserDTO)) {
            com.oracle.bmc.core.BlockstorageClient blockstorageClient = fetcher.getBlockstorageClient();
            com.oracle.bmc.core.model.BootVolume bootVolume = fetcher.getBootVolumeByInstanceId(params.getInstanceId());
            String snapshotName = (params.getSnapshotName() != null && !params.getSnapshotName().isBlank())
                    ? params.getSnapshotName()
                    : "snapshot-" + java.time.LocalDateTime.now().format(CommonUtils.DATETIME_FMT_PURE);
            blockstorageClient.createBootVolumeBackup(
                    com.oracle.bmc.core.requests.CreateBootVolumeBackupRequest.builder()
                            .createBootVolumeBackupDetails(
                                    com.oracle.bmc.core.model.CreateBootVolumeBackupDetails.builder()
                                            .bootVolumeId(bootVolume.getId())
                                            .displayName(snapshotName)
                                            .type(com.oracle.bmc.core.model.CreateBootVolumeBackupDetails.Type.Full)
                                            .build()
                            )
                            .build()
            );
            log.info("小划账号:[{}],地区:[{}],实例:[{}] 快照已提交,名称:{}",
                    sysUserDTO.getUsername(), sysUserDTO.getOciCfg().getRegion(), params.getInstanceId(), snapshotName);
        } catch (Exception e) {
            log.error("创建快照失败,账号:[{}],实例ID:[{}],错误:{}",
                    sysUserDTO.getUsername(), params.getInstanceId(), e.getLocalizedMessage(), e);
            throw new OciException(-1, "创建快照失败");
        }
    }

    @Override
    public void updateTags(com.tony.kingdetective.bean.params.oci.instance.UpdateTagsParams params) {
        SysUserDTO sysUserDTO = sysService.getOciUser(params.getOciCfgId());
        try (OracleInstanceFetcher fetcher = new OracleInstanceFetcher(sysUserDTO)) {
            com.oracle.bmc.core.ComputeClient computeClient = fetcher.getComputeClient();
            com.oracle.bmc.core.model.Instance instance = fetcher.getInstanceById(params.getInstanceId());
            java.util.Map<String, String> tags = new java.util.HashMap<>();
            if (instance.getFreeformTags() != null) {
                tags.putAll(instance.getFreeformTags());
            }
            if (params.getTagsJson() != null && !params.getTagsJson().isBlank()) {
                try {
                    @SuppressWarnings("unchecked")
                    java.util.Map<String, String> parsedTags = cn.hutool.json.JSONUtil.toBean(
                            params.getTagsJson(), java.util.Map.class);
                    tags.putAll(parsedTags);
                } catch (Exception ex) {
                    log.warn("解析 tagsJson 失败，跳过标签更新: {}", ex.getMessage());
                }
            }
            computeClient.updateInstance(
                    UpdateInstanceRequest.builder()
                            .instanceId(params.getInstanceId())
                            .updateInstanceDetails(
                                    UpdateInstanceDetails.builder()
                                            .freeformTags(tags)
                                            .build()
                            )
                            .build()
            );
            log.info("账号:[{}],实例:[{}] Tags 更新成功",
                    sysUserDTO.getUsername(), instance.getDisplayName());
        } catch (Exception e) {
            log.error("更新Tags失败,账号:[{}],实例ID:[{}],错误:{}",
                    sysUserDTO.getUsername(), params.getInstanceId(), e.getLocalizedMessage(), e);
            throw new OciException(-1, "更新实例 Tags 失败");
        }
    }

    @Override
    public String getScheduledPower(String instanceId) {
        try {
            String key = "scheduled_power:" + instanceId;
            com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<OciKv> wrapper =
                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<OciKv>()
                            .eq(OciKv::getCode, key);
            OciKv cfg = kvService.getOne(wrapper);
            return cfg != null ? cfg.getValue() : null;
        } catch (Exception e) {
            log.error("获取定时开关机配置失败,实例ID:[{}]", instanceId, e);
            return null;
        }
    }

    @Override
    public void setScheduledPower(String instanceId, String ociCfgId, String stopTime, String startTime) {
        try {
            String key = "scheduled_power:" + instanceId;
            String configStr = "STOP=" + stopTime + ",START=" + startTime + ",USER=" + ociCfgId;
            com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<OciKv> wrapper =
                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<OciKv>()
                            .eq(OciKv::getCode, key);
            OciKv cfg = kvService.getOne(wrapper);
            if (cfg != null) {
                cfg.setValue(configStr);
                kvService.updateById(cfg);
            } else {
                cfg = new OciKv();
                cfg.setId(cn.hutool.core.util.IdUtil.getSnowflakeNextIdStr());
                cfg.setCode(key);
                cfg.setValue(configStr);
                cfg.setType(com.tony.kingdetective.enums.SysCfgTypeEnum.SYS_INIT_CFG.getCode());
                kvService.save(cfg);
            }
            log.info("实例:[{}] 定时开关机配置已保存: {}", instanceId, configStr);
        } catch (Exception e) {
            log.error("设置定时开关机失败,实例ID:[{}]", instanceId, e);
            throw new OciException(-1, "设置定时开关机失败");
        }
    }

    @Override
    public java.util.List<com.oracle.bmc.identity.model.ApiKey> listApiKeys(String ociCfgId) {
        SysUserDTO sysUserDTO = sysService.getOciUser(ociCfgId);
        try (OracleInstanceFetcher fetcher = new OracleInstanceFetcher(sysUserDTO)) {
            return fetcher.getIdentityClient().listApiKeys(
                    com.oracle.bmc.identity.requests.ListApiKeysRequest.builder()
                            .userId(sysUserDTO.getOciCfg().getUserId())
                            .build()
            ).getItems();
        } catch (Exception e) {
            log.error("获取 API Keys 失败,账号:[{}],错误:{}",
                    sysUserDTO.getUsername(), e.getLocalizedMessage(), e);
            throw new OciException(-1, "获取 API Keys 失败");
        }
    }

    @Override
    public void deleteApiKey(String ociCfgId, String fingerprint) {
        SysUserDTO sysUserDTO = sysService.getOciUser(ociCfgId);
        try (OracleInstanceFetcher fetcher = new OracleInstanceFetcher(sysUserDTO)) {
            fetcher.getIdentityClient().deleteApiKey(
                    com.oracle.bmc.identity.requests.DeleteApiKeyRequest.builder()
                            .userId(sysUserDTO.getOciCfg().getUserId())
                            .fingerprint(fingerprint)
                            .build()
            );
            log.info("账号:[{}] 删除 API Key 成功,fingerprint:{}",
                    sysUserDTO.getUsername(), fingerprint);
        } catch (Exception e) {
            log.error("删除 API Key 失败,账号:[{}],fingerprint:[{}],错误:{}",
                    sysUserDTO.getUsername(), fingerprint, e.getLocalizedMessage(), e);
            throw new OciException(-1, "删除 API Key 失败");
        }
    }

    @Override
    public void addApiKey(String ociCfgId, String publicKeyContent) {
        SysUserDTO sysUserDTO = sysService.getOciUser(ociCfgId);
        try (OracleInstanceFetcher fetcher = new OracleInstanceFetcher(sysUserDTO)) {
            fetcher.getIdentityClient().uploadApiKey(
                    com.oracle.bmc.identity.requests.UploadApiKeyRequest.builder()
                            .userId(sysUserDTO.getOciCfg().getUserId())
                            .createApiKeyDetails(
                                    com.oracle.bmc.identity.model.CreateApiKeyDetails.builder()
                                            .key(publicKeyContent)
                                            .build()
                            )
                            .build()
            );
            log.info("账号:[{}] 添加 API Key 成功", sysUserDTO.getUsername());
        } catch (Exception e) {
            log.error("添加 API Key 失败,账号:[{}],错误:{}",
                    sysUserDTO.getUsername(), e.getLocalizedMessage(), e);
            throw new OciException(-1, "添加 API Key 失败");
        }
    }

}
