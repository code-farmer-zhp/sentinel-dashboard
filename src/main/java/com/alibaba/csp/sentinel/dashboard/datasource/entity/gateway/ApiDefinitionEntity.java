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
package com.alibaba.csp.sentinel.dashboard.datasource.entity.gateway;

import com.alibaba.csp.sentinel.adapter.gateway.common.api.ApiDefinition;
import com.alibaba.csp.sentinel.adapter.gateway.common.api.ApiPathPredicateItem;
import com.alibaba.csp.sentinel.adapter.gateway.common.api.ApiPredicateItem;
import com.alibaba.csp.sentinel.dashboard.datasource.entity.rule.RuleEntity;
import com.alibaba.csp.sentinel.slots.block.Rule;

import java.util.Date;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Entity for {@link ApiDefinition}.
 *
 * @author cdfive
 * @since 1.7.0
 */
public class ApiDefinitionEntity implements RuleEntity {

    private Long id;
    private String app;

    private Date gmtCreate;
    private Date gmtModified;

    private String apiName;
    private Set<ApiPredicateItemEntity> predicateItems;

    public static ApiDefinitionEntity fromApiDefinition(String app, String ip, Integer port, ApiDefinition apiDefinition) {
        ApiDefinitionEntity entity = new ApiDefinitionEntity();
        entity.setApp(app);
        entity.setApiName(apiDefinition.getApiName());

        Set<ApiPredicateItemEntity> predicateItems = new LinkedHashSet<>();
        entity.setPredicateItems(predicateItems);

        Set<ApiPredicateItem> apiPredicateItems = apiDefinition.getPredicateItems();
        if (apiPredicateItems != null) {
            for (ApiPredicateItem apiPredicateItem : apiPredicateItems) {
                ApiPredicateItemEntity itemEntity = new ApiPredicateItemEntity();
                predicateItems.add(itemEntity);
                ApiPathPredicateItem pathPredicateItem = (ApiPathPredicateItem) apiPredicateItem;
                itemEntity.setPattern(pathPredicateItem.getPattern());
                itemEntity.setMatchStrategy(pathPredicateItem.getMatchStrategy());
            }
        }

        return entity;
    }

    public ApiDefinition toApiDefinition() {
        ApiDefinition apiDefinition = new ApiDefinition();
        apiDefinition.setApiName(apiName);

        Set<ApiPredicateItem> apiPredicateItems = new LinkedHashSet<>();
        apiDefinition.setPredicateItems(apiPredicateItems);

        if (predicateItems != null) {
            for (ApiPredicateItemEntity predicateItem : predicateItems) {
                ApiPathPredicateItem apiPredicateItem = new ApiPathPredicateItem();
                apiPredicateItems.add(apiPredicateItem);
                apiPredicateItem.setMatchStrategy(predicateItem.getMatchStrategy());
                apiPredicateItem.setPattern(predicateItem.getPattern());
            }
        }

        return apiDefinition;
    }

    public ApiDefinitionEntity() {

    }

    public ApiDefinitionEntity(String apiName, Set<ApiPredicateItemEntity> predicateItems) {
        this.apiName = apiName;
        this.predicateItems = predicateItems;
    }

    public String getApiName() {
        return apiName;
    }

    public void setApiName(String apiName) {
        this.apiName = apiName;
    }

    public Set<ApiPredicateItemEntity> getPredicateItems() {
        return predicateItems;
    }

    public void setPredicateItems(Set<ApiPredicateItemEntity> predicateItems) {
        this.predicateItems = predicateItems;
    }

    @Override
    public Long getId() {
        return id;
    }

    @Override
    public void setId(Long id) {
        this.id = id;
    }

    @Override
    public String getApp() {
        return app;
    }

    public void setApp(String app) {
        this.app = app;
    }

    @Override
    public Date getGmtCreate() {
        return gmtCreate;
    }

    public void setGmtCreate(Date gmtCreate) {
        this.gmtCreate = gmtCreate;
    }

    public Date getGmtModified() {
        return gmtModified;
    }

    public void setGmtModified(Date gmtModified) {
        this.gmtModified = gmtModified;
    }

    @Override
    public Rule toRule() {
        return null;
    }

}
