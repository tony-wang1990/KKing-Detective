package com.tony.kingdetective.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.tony.kingdetective.bean.entity.OciKv;
import com.tony.kingdetective.bean.entity.OciUser;

/**
* @author Administrator
* @description oci_kvService
* @createDate 2024-11-12 16:44:39
*/
public interface IOciKvService extends IService<OciKv> {

    OciKv getByKey(String key);

}
