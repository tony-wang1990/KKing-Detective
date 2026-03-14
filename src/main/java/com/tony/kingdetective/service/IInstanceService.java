package com.tony.kingdetective.service;

import com.oracle.bmc.core.model.Instance;
import com.tony.kingdetective.bean.Tuple2;
import com.tony.kingdetective.bean.dto.CreateInstanceDTO;
import com.tony.kingdetective.bean.dto.InstanceCfgDTO;
import com.tony.kingdetective.bean.dto.SysUserDTO;
import com.tony.kingdetective.bean.params.oci.instance.Close500MParams;
import com.tony.kingdetective.bean.params.oci.instance.CreateNetworkLoadBalancerParams;
import com.tony.kingdetective.bean.params.oci.instance.UpdateShapeParams;
import com.tony.kingdetective.config.OracleInstanceFetcher;

import java.util.List;

/**
 * <p>
 * IInstanceService
 * </p >
 *
 * @author Tony Wang
 * @since 2024/11/11 14:30
 */
public interface IInstanceService {

    /**
     * 获取已开机实例信?
     *
     * @param sysUserDTO oci配置
     * @return 已开机实例信?
     */
    List<SysUserDTO.CloudInstance> listRunningInstances(SysUserDTO sysUserDTO);

    /**
     * 开?
     *
     * @param fetcher oci配置
     * @return 成功开机的实例信息
     */
    CreateInstanceDTO createInstance(OracleInstanceFetcher fetcher);

    /**
     * 根据 CIDR 网段更换实例公共IP
     *
     * @param instanceId 实例Id
     * @param vnicId     vnicId
     * @param sysUserDTO oci配置
     * @param cidrList   CIDR 网段 （传为空则随机更换一个ip?
     * @return 新的实例公共IP，实?
     */
    Tuple2<String, Instance> changeInstancePublicIp(String instanceId, String vnicId, SysUserDTO sysUserDTO, List<String> cidrList);

    /**
     * 获取实例需修改的配置信?
     *
     * @param sysUserDTO oci配置
     * @param instanceId 实例Id
     * @return 实例需修改的配置信?
     */
    InstanceCfgDTO getInstanceCfgInfo(SysUserDTO sysUserDTO, String instanceId);

    /**
     * 安全列表放行
     *
     * @param sysUserDTO oci配置
     */
    void releaseSecurityRule(SysUserDTO sysUserDTO);

    /**
     * 附加IPV6
     *
     * @param sysUserDTO oci配置
     * @param instanceId 实例Id
     */
    String createIpv6(SysUserDTO sysUserDTO, String instanceId);

    /**
     * 修改实例名称
     *
     * @param sysUserDTO oci配置
     * @param instanceId 实例Id
     * @param name       实例名称
     */
    void updateInstanceName(SysUserDTO sysUserDTO, String instanceId, String name);

    /**
     * 修改实例配置
     *
     * @param sysUserDTO oci配置
     * @param instanceId 实例Id
     * @param ocpus      cpu
     * @param memory     内存
     */
    void updateInstanceCfg(SysUserDTO sysUserDTO, String instanceId, float ocpus, float memory);

    /**
     * 修改引导卷配?
     *
     * @param sysUserDTO oci配置
     * @param instanceId 实例id
     * @param size       引导卷大?
     * @param vpusPer    引导卷vpu [10,120]
     */
    void updateBootVolumeCfg(SysUserDTO sysUserDTO, String instanceId, long size, long vpusPer);

    /**
     * 一键开?00M
     * @param params 参数
     */
    void oneClick500M(CreateNetworkLoadBalancerParams params);

    /**
     * 一键关?00M
     * @param params 参数
     */
    void oneClickClose500M(Close500MParams params);

    /**
     * 修改实例Shape
     * @param params 参数
     */
    void updateInstanceShape(UpdateShapeParams params);

    void createSnapshot(com.tony.kingdetective.bean.params.oci.instance.CreateSnapshotParams params);
    void updateTags(com.tony.kingdetective.bean.params.oci.instance.UpdateTagsParams params);

    String getScheduledPower(String instanceId);
    void setScheduledPower(String instanceId, String ociCfgId, String stopTime, String startTime);

    java.util.List<com.oracle.bmc.identity.model.ApiKey> listApiKeys(String ociCfgId);
    void deleteApiKey(String ociCfgId, String fingerprint);
    void addApiKey(String ociCfgId, String publicKeyContent);
}
