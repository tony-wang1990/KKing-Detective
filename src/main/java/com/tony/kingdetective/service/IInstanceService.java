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
     * ?
     *
     * @param sysUserDTO oci
     * @return ?
     */
    List<SysUserDTO.CloudInstance> listRunningInstances(SysUserDTO sysUserDTO);

    /**
     * ?
     *
     * @param fetcher oci
     * @return 
     */
    CreateInstanceDTO createInstance(OracleInstanceFetcher fetcher);

    /**
     *  CIDR IP
     *
     * @param instanceId Id
     * @param vnicId     vnicId
     * @param sysUserDTO oci
     * @param cidrList   CIDR  ip?
     * @return IP?
     */
    Tuple2<String, Instance> changeInstancePublicIp(String instanceId, String vnicId, SysUserDTO sysUserDTO, List<String> cidrList);

    /**
     * ?
     *
     * @param sysUserDTO oci
     * @param instanceId Id
     * @return ?
     */
    InstanceCfgDTO getInstanceCfgInfo(SysUserDTO sysUserDTO, String instanceId);

    /**
     * 
     *
     * @param sysUserDTO oci
     */
    void releaseSecurityRule(SysUserDTO sysUserDTO);

    /**
     * IPV6
     *
     * @param sysUserDTO oci
     * @param instanceId Id
     */
    String createIpv6(SysUserDTO sysUserDTO, String instanceId);

    /**
     * 
     *
     * @param sysUserDTO oci
     * @param instanceId Id
     * @param name       
     */
    void updateInstanceName(SysUserDTO sysUserDTO, String instanceId, String name);

    /**
     * 
     *
     * @param sysUserDTO oci
     * @param instanceId Id
     * @param ocpus      cpu
     * @param memory     
     */
    void updateInstanceCfg(SysUserDTO sysUserDTO, String instanceId, float ocpus, float memory);

    /**
     * ?
     *
     * @param sysUserDTO oci
     * @param instanceId id
     * @param size       ?
     * @param vpusPer    vpu [10,120]
     */
    void updateBootVolumeCfg(SysUserDTO sysUserDTO, String instanceId, long size, long vpusPer);

    /**
     * ?00M
     * @param params 
     */
    void oneClick500M(CreateNetworkLoadBalancerParams params);

    /**
     * ?00M
     * @param params 
     */
    void oneClickClose500M(Close500MParams params);

    /**
     * Shape
     * @param params 
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
