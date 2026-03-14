package com.tony.kingdetective.mapper;

import com.tony.kingdetective.bean.entity.CfCfg;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tony.kingdetective.bean.response.cf.ListCfCfgPageRsp;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @author Tony Wang
 * @description cf_cfgMapper
 * @createDate 2025-03-19 16:10:18
 * @Entity com.tony.kingdetective.bean.entity.CfCfg
 */
public interface CfCfgMapper extends BaseMapper<CfCfg> {

    List<ListCfCfgPageRsp> listCfg(@Param("offset") long offset,
                                   @Param("size") long size,
                                   @Param("keyword") String keyword);

    Long listCfgTotal(@Param("keyword") String keyword);
}




