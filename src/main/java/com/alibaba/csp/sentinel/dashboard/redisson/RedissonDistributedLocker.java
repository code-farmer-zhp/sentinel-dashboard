package com.alibaba.csp.sentinel.dashboard.redisson;

import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

public class RedissonDistributedLocker implements DistributedLocker {

    private Logger logger = LoggerFactory.getLogger(RedissonDistributedLocker.class);

    private RedissonClient redissonClient;

    public RedissonDistributedLocker() {
    }

    public RedissonDistributedLocker(RedissonClient redissonClient) {
        this.redissonClient = redissonClient;
    }

    /**
     * 没有超时时间,默认30s
     */
    @Override
    public RLock lock(String lockKey) {
        RLock lock = redissonClient.getLock(lockKey);
        lock.lock();
        return lock;
    }

    /**
     * 自己设置超时时间
     *
     * @param lockKey 锁的key
     * @param timeout 秒 如果是-1，直到自己解锁，否则不会自动解锁
     */
    @Override
    public RLock lock(String lockKey, int timeout) {
        RLock lock = redissonClient.getLock(lockKey);
        lock.lock(timeout, TimeUnit.SECONDS);
        return lock;
    }

    @Override
    public RLock lock(String lockKey, TimeUnit unit, int timeout) {
        RLock lock = redissonClient.getLock(lockKey);
        lock.lock(timeout, unit);
        return lock;
    }

    /**
     * @param lockKey   锁key
     * @param unit      锁单位
     * @param waitTime  等到最大时间，强制获取锁
     * @param leaseTime 锁失效时间
     */
    @Override
    public boolean tryLock(String lockKey, TimeUnit unit, int waitTime, int leaseTime) {
        RLock lock = redissonClient.getLock(lockKey);
        try {
            return lock.tryLock(waitTime, leaseTime, unit);
        } catch (InterruptedException e) {
            logger.error("try to get Lock occur some exception", e);
        }
        return false;
    }

    @Override
    public void unlock(String lockKey) {
        RLock lock = redissonClient.getLock(lockKey);
        if (lock.isLocked()) {
            lock.unlock();
        }
    }

    @Override
    public void unlock(RLock lock) {
        lock.unlock();
    }
}
