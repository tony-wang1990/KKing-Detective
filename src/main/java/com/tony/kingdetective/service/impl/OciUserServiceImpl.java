package com.tony.kingdetective.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tony.kingdetective.bean.entity.OciUser;
import com.tony.kingdetective.service.IOciUserService;
import org.springframework.stereotype.Service;
import com.tony.kingdetective.mapper.OciUserMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.List;

@Service
public class OciUserServiceImpl extends ServiceImpl<OciUserMapper, OciUser> implements IOciUserService {
    @Override
    public List<OciUser> getEnabledOciUserList() {
        return this.list(new LambdaQueryWrapper<OciUser>().eq(OciUser::getDeleted, 0).or().isNull(OciUser::getDeleted));
    }
}
