/*
 * Copyright 1999-2018 Alibaba Group Holding Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.csp.sentinel.dashboard.repository.metric;

import com.alibaba.csp.sentinel.dashboard.datasource.entity.MetricEntity;
import com.alibaba.csp.sentinel.dashboard.repository.metric.mapper.MetricsMapper;
import com.alibaba.csp.sentinel.util.StringUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

/**
 * Caches metrics data in a period of time in memory.
 *
 * @author Carpenter Lee
 * @author Eric Zhao
 */
@Component
public class MysqlMetricsRepository extends ServiceImpl<MetricsMapper, MetricEntity> implements MetricsRepository<MetricEntity> {

    @Override
    public void saveAll(Collection<MetricEntity> metrics) {
        if (metrics == null) {
            return;
        }
        metrics.stream().sorted(Comparator.comparing(MetricEntity::getTimestamp));
        this.saveBatch(metrics);

    }

    @Override
    public List<MetricEntity> queryByAppAndResourceBetween(String app, String resource,
                                                           long startTime, long endTime) {
        List<MetricEntity> results = new ArrayList<>();
        if (StringUtil.isBlank(app)) {
            return results;
        }
        LambdaQueryWrapper<MetricEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MetricEntity::getApp, app);
        wrapper.eq(MetricEntity::getResource, resource);
        wrapper.ge(MetricEntity::getTimestamp, new Date(startTime));
        wrapper.le(MetricEntity::getTimestamp, new Date(endTime));
        return this.baseMapper.selectList(wrapper);
    }

    @Override
    public List<String> listResourcesOfApp(String app) {
        List<String> results = new ArrayList<>();
        if (StringUtil.isBlank(app)) {
            return results;
        }
        Calendar instance = Calendar.getInstance();
        instance.add(Calendar.HOUR, -1);

        return this.baseMapper.selectResource(app, instance.getTime());

    }
}
