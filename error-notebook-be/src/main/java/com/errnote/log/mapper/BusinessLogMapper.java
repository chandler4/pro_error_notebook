package com.errnote.log.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.errnote.log.entity.BusinessLog;
import org.apache.ibatis.annotations.Mapper;

/** business_log Mapper（MyBatis-Plus 通用 CRUD + 分页）。 */
@Mapper
public interface BusinessLogMapper extends BaseMapper<BusinessLog> {}
