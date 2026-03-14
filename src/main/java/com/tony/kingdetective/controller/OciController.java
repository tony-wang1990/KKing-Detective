package com.tony.kingdetective.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tony.kingdetective.bean.ResponseData;
import com.tony.kingdetective.bean.dto.InstanceCfgDTO;
import com.tony.kingdetective.bean.params.*;
import com.tony.kingdetective.bean.params.oci.cfg.*;
import com.tony.kingdetective.bean.params.oci.instance.*;
import com.tony.kingdetective.bean.params.oci.securityrule.ReleaseSecurityRuleParams;
import com.tony.kingdetective.bean.params.oci.task.CreateTaskPageParams;
import com.tony.kingdetective.bean.params.oci.task.StopChangeIpParams;
import com.tony.kingdetective.bean.params.oci.task.StopCreateParams;
import com.tony.kingdetective.bean.params.oci.volume.UpdateBootVolumeCfgParams;
import com.tony.kingdetective.bean.response.oci.task.CreateTaskRsp;
import com.tony.kingdetective.bean.response.oci.cfg.OciCfgDetailsRsp;
import com.tony.kingdetective.bean.response.oci.cfg.OciUserListRsp;
import com.tony.kingdetective.service.IInstanceService;
import com.tony.kingdetective.service.IOciService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import java.util.List;

/**
 * <p>
 * OciController -  REST API
 * </p>
 *
 * @author Tony Wang
 * @since 2024/11/12 17:17
 * @version 4.1.1 (BUG-FIX: 2026-03-14 )
 */
@RestController
@RequestMapping(path = "/api/oci")
public class OciController {

    @Resource
    private IOciService ociService;

    @Resource
    private IInstanceService instanceService;

    // ================== OCI  ==================

    /**
     *  OCI 
     */
    @PostMapping(path = "/userPage")
    public ResponseData<Page<OciUserListRsp>> userPage(@Validated @RequestBody GetOciUserListParams params) {
        return ResponseData.successData(ociService.userPage(params), "????????");
    }

    /**
     *  OCI 
     */
    @PostMapping(path = "/details")
    public ResponseData<OciCfgDetailsRsp> details(@Validated @RequestBody GetOciCfgDetailsParams params) {
        return ResponseData.successData(ociService.details(params), "????????");
    }

    /**
     *  OCI 
     */
    @PostMapping(path = "/addCfg")
    public ResponseData<Void> addCfg(@Validated AddCfgParams params) {
        ociService.addCfg(params);
        return ResponseData.successData("??????");
    }

    /**
     *  OCI 
     */
    @PostMapping(path = "/uploadCfg")
    public ResponseData<Void> uploadCfg(@Validated UploadCfgParams params) {
        ociService.uploadCfg(params);
        return ResponseData.successData("??????");
    }

    /**
     *  OCI 
     */
    @PostMapping(path = "/updateCfgName")
    public ResponseData<Void> updateCfgName(@Validated @RequestBody UpdateCfgNameParams params) {
        ociService.updateCfgName(params);
        return ResponseData.successData();
    }

    /**
     *  OCI 
     */
    @PostMapping(path = "/removeCfg")
    public ResponseData<Void> removeCfg(@Validated @RequestBody IdListParams params) {
        ociService.removeCfg(params);
        return ResponseData.successData("??????");
    }

    // ==================  ==================

    /**
     *  /  / 
     */
    @PostMapping(path = "/updateInstanceState")
    public ResponseData<Void> updateInstanceState(@Validated @RequestBody UpdateInstanceStateParams params) {
        instanceService.updateInstanceState(params);
        return ResponseData.successData("???????");
    }

    /**
     * 
     */
    @PostMapping(path = "/updateInstanceName")
    public ResponseData<Void> updateInstanceName(@Validated @RequestBody UpdateInstanceNameParams params) {
        instanceService.updateInstanceName(params);
        return ResponseData.successData("????????");
    }

    /**
     *  OCPU / 
     */
    @PostMapping(path = "/updateInstanceCfg")
    public ResponseData<Void> updateInstanceCfg(@Validated @RequestBody UpdateInstanceCfgParams params) {
        instanceService.updateInstanceCfg(params);
        return ResponseData.successData("????????");
    }

    /**
     *  Shape
     */
    @PostMapping(path = "/updateInstanceShape")
    public ResponseData<Void> updateInstanceShape(@Validated @RequestBody UpdateShapeParams params) {
        instanceService.updateInstanceShape(params);
        return ResponseData.successData("???? Shape ??");
    }

    /**
     * 
     */
    @PostMapping(path = "/getInstanceCfgInfo")
    public ResponseData<InstanceCfgDTO> getInstanceCfgInfo(@Validated @RequestBody GetInstanceCfgInfoParams params) {
        return ResponseData.successData(instanceService.getInstanceCfgInfo(params), "????????");
    }

    /**
     * 
     */
    @PostMapping(path = "/terminateInstance")
    public ResponseData<Void> terminateInstance(@Validated @RequestBody TerminateInstanceParams params) {
        instanceService.terminateInstance(params);
        return ResponseData.successData("?????????");
    }

    /**
     *  Freeform Tags
     */
    @PostMapping(path = "/updateTags")
    public ResponseData<Void> updateTags(@Validated @RequestBody UpdateTagsParams params) {
        instanceService.updateTags(params);
        return ResponseData.successData("????????");
    }

    // ================== Auto Rescue ==================

    /**
     *  NETBOOTiPXE  REIMAGE
     */
    @PostMapping(path = "/autoRescue")
    public ResponseData<Void> autoRescue(@Validated @RequestBody AutoRescueParams params) {
        instanceService.autoRescue(params);
        return ResponseData.successData("???????????????");
    }

    // ==================  ==================

    /**
     * 
     */
    @PostMapping(path = "/createSnapshot")
    public ResponseData<Void> createSnapshot(@Validated @RequestBody CreateSnapshotParams params) {
        instanceService.createSnapshot(params);
        return ResponseData.successData("????????????????");
    }

    // ==================  ==================

    /**
     * 
     */
    @PostMapping(path = "/getScheduledPower")
    public ResponseData<String> getScheduledPower(@RequestParam("id") String id) {
        return ResponseData.successData(instanceService.getScheduledPower(id), "????????");
    }

    /**
     * 
     */
    @PostMapping(path = "/setScheduledPower")
    public ResponseData<Void> setScheduledPower(@Validated @RequestBody ScheduledPowerParams params) {
        instanceService.setScheduledPower(params.getInstanceId(), params.getOciCfgId(), params.getStopTime(), params.getStartTime());
        return ResponseData.successData("????????");
    }

    // ================== API Keys / SSH  ==================

    /**
     *  OCI  API Keys 
     */
    @PostMapping(path = "/listApiKeys")
    public ResponseData<List<com.oracle.bmc.identity.model.ApiKey>> listApiKeys(@RequestParam("ociCfgId") String ociCfgId) {
        return ResponseData.successData(instanceService.listApiKeys(ociCfgId), "????????");
    }

    /**
     *  API 
     */
    @PostMapping(path = "/addApiKey")
    public ResponseData<Void> addApiKey(@Validated @RequestBody AddApiKeyParams params) {
        instanceService.addApiKey(params.getOciCfgId(), params.getPublicKeyContent());
        return ResponseData.successData("??????");
    }

    /**
     *  API 
     */
    @PostMapping(path = "/deleteApiKey")
    public ResponseData<Void> deleteApiKey(@Validated @RequestBody DeleteApiKeyParams params) {
        instanceService.deleteApiKey(params.getOciCfgId(), params.getFingerprint());
        return ResponseData.successData("??????");
    }

    // ==================  ==================

    /**
     * 
     */
    @PostMapping(path = "/releaseSecurityRule")
    public ResponseData<Void> releaseSecurityRule(@Validated @RequestBody ReleaseSecurityRuleParams params) {
        ociService.releaseSecurityRule(params);
        return ResponseData.successData("????????");
    }

    // ================== IPV6 ==================

    /**
     *  IPv6 
     */
    @PostMapping(path = "/createIpv6")
    public ResponseData<Void> createIpv6(@Validated @RequestBody CreateIpv6Params params) {
        ociService.createIpv6(params);
        return ResponseData.successData("IPv6 ????");
    }

    // ================== 500M  ==================

    /**
     *  500M 
     */
    @PostMapping(path = "/oneClick500M")
    public ResponseData<Void> oneClick500M(@Validated @RequestBody CreateNetworkLoadBalancerParams params) {
        instanceService.oneClick500M(params);
        return ResponseData.successData("500M ???????????");
    }

    /**
     *  500M 
     */
    @PostMapping(path = "/oneClickClose500M")
    public ResponseData<Void> oneClickClose500M(@Validated @RequestBody Close500MParams params) {
        instanceService.oneClickClose500M(params);
        return ResponseData.successData("500M ??????????");
    }

    // ==================  ==================

    /**
     * 
     */
    @PostMapping(path = "/createTaskPage")
    public ResponseData<Page<CreateTaskRsp>> createTaskPage(@Validated @RequestBody CreateTaskPageParams params) {
        return ResponseData.successData(ociService.createTaskPage(params), "??????????");
    }

    /**
     * 
     */
    @PostMapping(path = "/stopCreate")
    public ResponseData<Void> stopCreate(@Validated @RequestBody StopCreateParams params) {
        ociService.stopCreate(params);
        return ResponseData.successData("????????");
    }

    /**
     *  IP 
     */
    @PostMapping(path = "/stopChangeIp")
    public ResponseData<Void> stopChangeIp(@Validated @RequestBody StopChangeIpParams params) {
        ociService.stopChangeIp(params);
        return ResponseData.successData("???IP????");
    }

    // ==================  ==================

    /**
     *  / VPU
     */
    @PostMapping(path = "/updateBootVolumeCfg")
    public ResponseData<Void> updateBootVolumeCfg(@Validated @RequestBody UpdateBootVolumeCfgParams params) {
        instanceService.updateBootVolumeCfg(params);
        return ResponseData.successData("?????????");
    }

    // ================== VNC  ==================

    /**
     *  VNC Console Connection
     */
    @PostMapping(path = "/startVnc")
    public ResponseData<String> startVnc(@Validated @RequestBody StartVncParams params) {
        return ResponseData.successData(ociService.startVnc(params), "VNC ????????");
    }
}
