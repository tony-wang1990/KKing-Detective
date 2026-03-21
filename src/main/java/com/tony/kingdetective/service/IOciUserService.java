package com.tony.kingdetective.service;

import com.tony.kingdetective.bean.entity.OciUser;
import com.baomidou.mybatisplus.extension.service.IService;

public interface IOciUserService extends IService<OciUser> {
    java.util.List<OciUser> getEnabledOciUserList();
}
