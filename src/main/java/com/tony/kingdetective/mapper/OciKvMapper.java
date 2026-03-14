package com.tony.kingdetective.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tony.kingdetective.bean.entity.OciKv;
import org.apache.ibatis.annotations.Param;

/**
 * @author Administrator
 * @description oci_userMapper
 * @createDate 2024-11-12 16:44:39
 * @Entity com.tony.kingdetective.bean.entity.OciUser
 */
public interface OciKvMapper extends BaseMapper<OciKv> {

    void  removeAllData(@Param("tableName") String table);
}




