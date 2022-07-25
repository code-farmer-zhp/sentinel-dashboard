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
package com.alibaba.csp.sentinel.dashboard.domain.vo;

import com.alibaba.csp.sentinel.dashboard.datasource.entity.MetricEntity;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

/**
 * @author leyou
 */
public class MetricVo implements Comparable<MetricVo> {
    private Long id;
    private String app;
    private Date timestamp;
    private Date gmtCreate;
    private String resource;
    private Long passQps;
    private Long blockQps;
    private Long successQps;
    private Long exceptionQps;
    /**
     * average rt
     */
    private Double rt;
    private Integer count;

    public MetricVo() {
    }

    public static List<MetricVo> fromMetricEntities(Collection<MetricEntity> entities) {
        List<MetricVo> list = new ArrayList<>();
        if (entities != null) {
            for (MetricEntity entity : entities) {
                list.add(fromMetricEntity(entity));
            }
        }
        return list;
    }

    /**
     * 保留资源名为identity的结果。
     *
     * @param entities 通过hashCode查找到的MetricEntities
     * @param identity 真正需要查找的资源名
     */
    public static List<MetricVo> fromMetricEntities(Collection<MetricEntity> entities, String identity) {
        List<MetricVo> list = new ArrayList<>();
        if (entities != null) {
            for (MetricEntity entity : entities) {
                if (entity.getResource().equals(identity)) {
                    list.add(fromMetricEntity(entity));
                }
            }
        }
        return list;
    }

    public static MetricVo fromMetricEntity(MetricEntity entity) {
        MetricVo vo = new MetricVo();
        vo.id = entity.getId();
        vo.app = entity.getApp();
        vo.timestamp = entity.getTimestamp();
        vo.gmtCreate = entity.getGmtCreate();
        vo.resource = entity.getResource();
        vo.passQps = entity.getPassQps();
        vo.blockQps = entity.getBlockQps();
        vo.successQps = entity.getSuccessQps();
        vo.exceptionQps = entity.getExceptionQps();
        if (entity.getSuccessQps() != 0) {
            vo.rt = entity.getRt() / entity.getSuccessQps();
        } else {
            vo.rt = 0D;
        }
        vo.count = entity.getCount();
        return vo;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getApp() {
        return app;
    }

    public void setApp(String app) {
        this.app = app;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }

    public Date getGmtCreate() {
        return gmtCreate;
    }

    public void setGmtCreate(Date gmtCreate) {
        this.gmtCreate = gmtCreate;
    }

    public String getResource() {
        return resource;
    }

    public void setResource(String resource) {
        this.resource = resource;
    }

    public Long getPassQps() {
        return passQps;
    }

    public void setPassQps(Long passQps) {
        this.passQps = passQps;
    }

    public Long getBlockQps() {
        return blockQps;
    }

    public void setBlockQps(Long blockQps) {
        this.blockQps = blockQps;
    }

    public Long getSuccessQps() {
        return successQps;
    }

    public void setSuccessQps(Long successQps) {
        this.successQps = successQps;
    }

    public Long getExceptionQps() {
        return exceptionQps;
    }

    public void setExceptionQps(Long exceptionQps) {
        this.exceptionQps = exceptionQps;
    }

    public Double getRt() {
        return rt;
    }

    public void setRt(Double rt) {
        this.rt = rt;
    }

    public Integer getCount() {
        return count;
    }

    public void setCount(Integer count) {
        this.count = count;
    }

    @Override
    public int compareTo(MetricVo o) {
        return this.timestamp.compareTo(o.timestamp);
    }
}
