package com.alibaba.csp.sentinel.dashboard.repository.metric.mapper;

import com.alibaba.csp.sentinel.dashboard.datasource.entity.MetricEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;

public interface MetricsMapper extends BaseMapper<MetricEntity> {
    List<String> selectResource(@Param("app") String app, @Param("latelyTime") Date latelyTime);
}
