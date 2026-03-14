package com.tony.kingdetective.mapper;

import com.tony.kingdetective.bean.entity.IpData;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tony.kingdetective.bean.response.ipdata.IpDataPageRsp;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
* @author Tony Wang
* @description ip_dataMapper
* @createDate 2025-08-04 17:28:41
* @Entity com.tony.kingdetective.bean.entity.IpData
*/
public interface IpDataMapper extends BaseMapper<IpData> {

    List<IpDataPageRsp> pageIpData(@Param("offset") long offset,
                                   @Param("size") long size,
                                   @Param("keyword") String keyword);

    Long pageIpDataTotal(@Param("keyword") String keyword);
}




