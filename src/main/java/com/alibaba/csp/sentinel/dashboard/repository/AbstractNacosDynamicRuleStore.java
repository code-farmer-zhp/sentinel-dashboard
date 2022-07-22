package com.alibaba.csp.sentinel.dashboard.repository;

import com.alibaba.csp.sentinel.dashboard.config.NacosConfigUtil;
import com.alibaba.csp.sentinel.dashboard.datasource.entity.rule.RuleEntity;
import com.alibaba.csp.sentinel.util.AssertUtil;
import com.alibaba.csp.sentinel.util.StringUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.nacos.api.config.ConfigService;
import com.alibaba.nacos.api.exception.NacosException;

import org.springframework.beans.factory.annotation.Autowired;

import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import cn.hutool.core.lang.Snowflake;

public abstract class AbstractNacosDynamicRuleStore<T extends RuleEntity> implements DynamicRuleStore<T> {

    @Autowired
    private ConfigService configService;

    private Snowflake snowflake = new Snowflake();

    @Override
    public T findById(String app, Long id) {
        List<T> collect = list(app).stream().filter(v -> v.getId().equals(id)).collect(Collectors.toList());
        if (collect.isEmpty()) {
            return null;
        }
        return collect.get(0);
    }

    @Override
    public void save(T rule) {
        if (rule == null) {
            return;
        }
        AssertUtil.notEmpty(rule.getApp(), "app name cannot be empty");
        if (rule.getId() == null) {
            rule.setId(snowflake.nextId());
        }
        preSave(rule);
        List<T> list = list(rule.getApp());
        list.add(rule);
        try {
            configService.publishConfig(rule.getApp() + postfix(), NacosConfigUtil.GROUP_ID, JSON.toJSONString(list));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void preSave(T rule) {

    }

    @Override
    public void delete(String app, Long id) {
        List<T> list = list(app).stream().filter(v -> !v.getId().equals(id)).collect(Collectors.toList());
        try {
            configService.publishConfig(app + postfix(), NacosConfigUtil.GROUP_ID, JSON.toJSONString(list));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void update(T rule) {
        List<T> list = list(rule.getApp()).stream().filter(v -> !v.getId().equals(rule.getId())).collect(Collectors.toList());
        list.add(rule);
        try {
            configService.publishConfig(rule.getApp() + postfix(), NacosConfigUtil.GROUP_ID, JSON.toJSONString(list));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<T> list(String app) {
        try {
            String rules = configService.getConfig(app + postfix(), NacosConfigUtil.GROUP_ID, 3000);
            if (StringUtil.isEmpty(rules)) {
                return new ArrayList<>();
            }
            List<T> list = JSON.parseArray(rules, getTClass());
            list.sort((o1, o2) -> o2.getId().compareTo(o1.getId()));
            return list;
        } catch (NacosException e) {
            throw new RuntimeException(e);
        }
    }


    private Class<T> getTClass() {
        return (Class<T>) ((ParameterizedType) this.getClass().getGenericSuperclass()).getActualTypeArguments()[0];
    }
}
