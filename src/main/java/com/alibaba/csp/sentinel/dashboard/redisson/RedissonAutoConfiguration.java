package com.alibaba.csp.sentinel.dashboard.redisson;

import org.apache.commons.lang.StringUtils;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;
import org.redisson.config.ClusterServersConfig;
import org.redisson.config.Config;
import org.redisson.config.SentinelServersConfig;
import org.redisson.config.SingleServerConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(value = {RedissonProperties.class})
public class RedissonAutoConfiguration {

    @Autowired
    private RedissonProperties redissonProperties;

    /**
     * 单机
     */
    @Bean(name = "redissonClient")
    @ConditionalOnProperty(value = "spring.redisson.address")
    public RedissonClient redissonSingle() {
        Config config = new Config();
        SingleServerConfig serverConfig =
                config.setCodec(StringCodec.INSTANCE)
                        .useSingleServer()
                        .setAddress(redissonProperties.getAddress())
                        .setTimeout(redissonProperties.getTimeout())
                        .setDatabase(redissonProperties.getDatabase())
                        .setConnectionPoolSize(redissonProperties.getConnectionPoolSize())
                        .setConnectionMinimumIdleSize(redissonProperties.getConnectionMiniumIdleSize());
        if (StringUtils.isNotEmpty(redissonProperties.getPassword())) {
            serverConfig.setPassword(redissonProperties.getPassword());
        }
        return Redisson.create(config);
    }

    /**
     * 集群的
     */
    @Bean(name = "redissonClient")
    @ConditionalOnProperty(value = "spring.redisson.masterAddresses")
    public RedissonClient redissonCluster() {
        Config config = new Config();
        ClusterServersConfig serverConfig =
                config.setCodec(StringCodec.INSTANCE)
                        .useClusterServers()
                        .addNodeAddress(redissonProperties.getMasterAddresses())
                        .setTimeout(redissonProperties.getTimeout())
                        // 设置集群扫描时间
                        .setScanInterval(redissonProperties.getScanInterval())
                        // 主节点线程池数量
                        .setMasterConnectionPoolSize(redissonProperties.getMasterConnectionPoolSize())
                        // 从节点线程池数量
                        .setSlaveConnectionPoolSize(redissonProperties.getSlaveConnectionPoolSize());

        if (StringUtils.isNotEmpty(redissonProperties.getPassword())) {
            serverConfig.setPassword(redissonProperties.getPassword());
        }
        return Redisson.create(config);
    }

    /**
     * 哨兵模式
     */
    @Bean(name = "redissonClient")
    @ConditionalOnProperty(value = "spring.redisson.sentinelAddresses")
    public RedissonClient redissonSentinel() {
        Config config = new Config();
        SentinelServersConfig serverConfig =
                config.setCodec(StringCodec.INSTANCE)
                        .useSentinelServers()
                        .addSentinelAddress(redissonProperties.getSentinelAddresses())
                        .setMasterName(redissonProperties.getMasterName())
                        .setTimeout(redissonProperties.getTimeout())
                        // 设置集群扫描时间
                        .setScanInterval(redissonProperties.getScanInterval())
                        // 主节点线程池数量
                        .setMasterConnectionPoolSize(redissonProperties.getMasterConnectionPoolSize())
                        // 从节点线程池数量
                        .setSlaveConnectionPoolSize(redissonProperties.getSlaveConnectionPoolSize());

        if (StringUtils.isNotEmpty(redissonProperties.getPassword())) {
            serverConfig.setPassword(redissonProperties.getPassword());
        }
        return Redisson.create(config);
    }

    @Bean
    @ConditionalOnBean(name = "redissonClient")
    public DistributedLocker distributedLocker(RedissonClient redissonClient) {
        return new RedissonDistributedLocker(redissonClient);
    }
}
