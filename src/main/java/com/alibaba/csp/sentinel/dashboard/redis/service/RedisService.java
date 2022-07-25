package com.alibaba.csp.sentinel.dashboard.redis.service;

import com.google.common.collect.Lists;

import com.alibaba.csp.sentinel.dashboard.redis.common.RedisLuaScript;
import com.alibaba.fastjson.JSON;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.BoundSetOperations;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * spring redis 工具类
 */
@SuppressWarnings(value = {"unchecked", "rawtypes"})
@Component
public class RedisService {

    /**
     * 列表如果不存在该值则插入
     */
    private RedisScript<Long> lPushIfAbsent = new DefaultRedisScript<>(
            "local list = redis.call(\"lrange\", KEYS[1], 0, -1);\n" +
                    "for k, v in pairs(list) do\n" +
                    "  if v == ARGV[1] then\n" +
                    "    return 0;\n" +
                    "  end\n" +
                    "end\n" +
                    "return redis.call(\"lpush\", KEYS[1], ARGV[1]);", Long.class);

    /**
     * 插入列表, 并保证以从小到大排序
     */
    private RedisScript<Long> lPushSorted = new DefaultRedisScript<>(
            "local list = redis.call(\"lrange\", KEYS[1], 0, -1);\n" +
                    "for k, v in pairs(list) do\n" +
                    "  if v == ARGV[1] then\n" +
                    "    return 0;\n" +
                    "  elseif tonumber(v) > tonumber(ARGV[1]) then\n" +
                    "    return redis.call(\"linsert\", KEYS[1], \"before\", v, ARGV[1]);\n" +
                    "  end\n" +
                    "end\n" +
                    "return redis.call(\"rpush\", KEYS[1], ARGV[1]);", Long.class);

    /**
     * 从 key1 中 rPop 一个值 value, 如果对应 key2:value(ext1) 长度为0, 则抛弃, 否则执行指定操作 (lPush 或 rPush) 并返回 value
     */
    private RedisScript<String> rPopIfNotEmpty = new DefaultRedisScript<>(
            "while true do\n" +
                    "  local value = redis.call(\"rpop\", KEYS[1]);\n" +
                    "  if not value then\n" +
                    "    return nil;\n" +
                    "  elseif redis.call(\"llen\", KEYS[2] .. \":\" .. value .. ARGV[1]) > 0 then\n" +
                    "    redis.call(ARGV[2], KEYS[1], value);\n" +
                    "    return value;\n" +
                    "  end\n" +
                    "end", String.class);

    @Autowired
    public RedisTemplate redisTemplate;

    @Autowired
    public StringRedisTemplate stringRedisTemplate;

    /**
     * 缓存基本的对象，Integer、String、实体类等
     *
     * @param key   缓存的键值
     * @param value 缓存的值
     */
    public <T> void setCacheObject(final String key, final T value) {
        redisTemplate.opsForValue().set(key, value);
    }

    /**
     * 缓存基本的对象，Integer、String、实体类等
     *
     * @param key      缓存的键值
     * @param value    缓存的值
     * @param timeout  时间
     * @param timeUnit 时间颗粒度
     */
    public <T> void setCacheObject(
            final String key, final T value, final Long timeout, final TimeUnit timeUnit) {
        redisTemplate.opsForValue().set(key, value, timeout, timeUnit);
    }

    /**
     * 设置有效时间
     *
     * @param key     Redis键
     * @param timeout 超时时间
     * @return true=设置成功；false=设置失败
     */
    public boolean expire(final String key, final long timeout) {
        return expire(key, timeout, TimeUnit.SECONDS);
    }

    /**
     * 设置有效时间
     *
     * @param key     Redis键
     * @param timeout 超时时间
     * @param unit    时间单位
     * @return true=设置成功；false=设置失败
     */
    public boolean expire(final String key, final long timeout, final TimeUnit unit) {
        return redisTemplate.expire(key, timeout, unit);
    }

    /**
     * 获得缓存的基本对象。
     *
     * @param key 缓存键值
     * @return 缓存键值对应的数据
     */
    public <T> T getCacheObject(final String key) {
        ValueOperations<String, T> operation = redisTemplate.opsForValue();
        return operation.get(key);
    }

    /**
     * 删除单个对象
     */
    public boolean deleteObject(final String key) {
        return redisTemplate.delete(key);
    }

    /**
     * 删除集合对象
     *
     * @param collection 多个对象
     */
    public long deleteObject(final Collection collection) {
        return redisTemplate.delete(collection);
    }

    /**
     * 缓存List数据
     *
     * @param key      缓存的键值
     * @param dataList 待缓存的List数据
     * @return 缓存的对象
     */
    public <T> long setCacheList(final String key, final List<T> dataList) {
        Long count = redisTemplate.opsForList().rightPushAll(key, dataList);
        return count == null ? 0 : count;
    }

    /**
     * 获得缓存的list对象
     *
     * @param key 缓存的键值
     * @return 缓存键值对应的数据
     */
    public <T> List<T> getCacheList(final String key) {
        return redisTemplate.opsForList().range(key, 0, -1);
    }

    /**
     * 缓存Set
     *
     * @param key     缓存键值
     * @param dataSet 缓存的数据
     * @return 缓存数据的对象
     */
    public <T> BoundSetOperations<String, T> setCacheSet(final String key, final Set<T> dataSet) {
        BoundSetOperations<String, T> setOperation = redisTemplate.boundSetOps(key);
        Iterator<T> it = dataSet.iterator();
        while (it.hasNext()) {
            setOperation.add(it.next());
        }
        return setOperation;
    }

    /**
     * 获得缓存的set
     */
    public <T> Set<T> getCacheSet(final String key) {
        return redisTemplate.opsForSet().members(key);
    }

    /**
     * 缓存Map
     */
    public <T> void setCacheMap(final String key, final Map<String, T> dataMap) {
        if (dataMap != null) {
            redisTemplate.opsForHash().putAll(key, dataMap);
        }
    }

    /**
     * 获得缓存的Map
     */
    public <T> Map<String, T> getCacheMap(final String key) {
        return redisTemplate.opsForHash().entries(key);
    }

    /**
     * 往Hash中存入数据
     *
     * @param key   Redis键
     * @param hKey  Hash键
     * @param value 值
     */
    public <T> void setCacheMapValue(final String key, final String hKey, final T value) {
        redisTemplate.opsForHash().put(key, hKey, value);
    }

    /**
     * 获取Hash中的数据
     *
     * @param key  Redis键
     * @param hKey Hash键
     * @return Hash中的对象
     */
    public <T> T getCacheMapValue(final String key, final String hKey) {
        HashOperations<String, String, T> opsForHash = redisTemplate.opsForHash();
        return opsForHash.get(key, hKey);
    }

    /**
     * 获取多个Hash中的数据
     *
     * @param key   Redis键
     * @param hKeys Hash键集合
     * @return Hash对象集合
     */
    public <T> List<T> getMultiCacheMapValue(final String key, final Collection<Object> hKeys) {
        return redisTemplate.opsForHash().multiGet(key, hKeys);
    }

    /**
     * 获得缓存的基本对象列表
     *
     * @param pattern 字符串前缀
     * @return 对象列表
     */
    public Collection<String> keys(final String pattern) {
        return redisTemplate.keys(pattern);
    }

    /**
     * 获取自增值
     *
     * @param key   自增key
     * @param delta 自增因子
     * @return 增加后的值
     */
    public Long increment(final String key, final Long delta) {
        return redisTemplate.opsForValue().increment(key, delta);
    }

    /**
     * 获取自增值
     *
     * @param key 自增key
     * @return 增加后的值
     */
    public Long increment(final String key) {
        return redisTemplate.opsForValue().increment(key);
    }

    /**
     * 自减
     *
     * @param key 自减key
     * @return 减少后的值
     */
    public Long decrement(final String key) {
        return redisTemplate.opsForValue().decrement(key);
    }

    /**
     * 自减
     *
     * @param key   自减key
     * @param delta 自减因子
     * @return 减少后的值
     */
    public Long decrement(final String key, final Long delta) {
        return redisTemplate.opsForValue().decrement(key, delta);
    }

    public <K, V> void mset(final Map<K, V> keyValueMap) {
        redisTemplate.opsForValue().multiSet(keyValueMap);
    }

    public <V, K> Map<K, V> mget(List<K> keys) {
        List<V> valueList = redisTemplate.opsForValue().multiGet(keys);
        if (valueList == null || valueList.isEmpty()) {
            return new HashMap<>();
        }
        Map<K, V> result = new HashMap<>();
        for (int i = 0; i < keys.size(); i++) {
            V v = valueList.get(i);
            if (v != null) {
                result.put(keys.get(i), v);
            }
        }
        return result;
    }

    public Long ttl(final String key) {
        return redisTemplate.boundValueOps(key).getExpire();
    }

    /**
     * 发送消息
     */
    public void publish(String topic, String msg) {
        redisTemplate.convertAndSend(topic, msg);
    }


    /**
     * @description: 是否包含key
     */
    public boolean hasKey(String key) {
        return redisTemplate.hasKey(key);
    }

    /**
     * @description: 设置位图
     */
    public boolean setBit(String key, Long offset, Boolean value) {
        return redisTemplate.opsForValue().setBit(key, offset, value);
    }

    /**
     * @description: 位图统计
     */
    public Long bitCount(final String key) {
        Object value = redisTemplate.execute((RedisCallback<Long>) con -> con.bitCount(key.getBytes()));
        return value == null ? null : Long.parseLong(value.toString());
    }

    /**
     * @description: 位图统计 带偏移量范围
     */
    public Long bitCount(String key, Long start, Long end) {
        Object value = redisTemplate.execute((RedisCallback<Long>) con -> con.bitCount(key.getBytes(), start, end));
        return value == null ? null : Long.parseLong(value.toString());
    }

    /**
     * @description: 自增并设置过期时间
     */
    public Long incrementAndSetExpire(String key, Long timeOut) {

        // 执行 lua 脚本
        DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>();

        // 指定 lua 脚本
        redisScript.setScriptText(RedisLuaScript.INCREMENT_AND_SET_EXPIRE_SCRIPT);

        // 指定返回类型
        redisScript.setResultType(Long.class);

        // 参数一：redisScript，参数二：key列表，参数三：arg（可多个）
        return stringRedisTemplate.execute(redisScript, Lists.newArrayList(key), timeOut.toString());
    }

    /**
     * List操作
     */

    public Long lPush(String key, String value) {
        ListOperations<String, String> listOperations = redisTemplate.opsForList();
        return listOperations.leftPush(key, value);
    }

    public Long lPush(String key, Object value) {
        String val;

        if (value instanceof String) {
            val = (String) value;
        } else {
            val = JSON.toJSONString(value);
        }

        ListOperations<String, String> listOperations = redisTemplate.opsForList();
        return listOperations.leftPush(key, val);
    }

    public Long lPush(String key, List<String> values) {
        ListOperations<String, String> listOperations = redisTemplate.opsForList();
        return listOperations.leftPushAll(key, values);
    }

    public Long rPush(String key, String value) {
        ListOperations<String, String> listOperations = redisTemplate.opsForList();
        return listOperations.rightPush(key, value);
    }

    public Long rPush(String key, List<String> values) {
        ListOperations<String, String> listOperations = redisTemplate.opsForList();
        return listOperations.rightPushAll(key, values);
    }

    public String lPop(String key) {
        ListOperations<String, String> listOperations = redisTemplate.opsForList();
        return listOperations.leftPop(key);
    }

    public List<String> lRange(String key, int start, int stop) {
        ListOperations<String, String> listOperations = redisTemplate.opsForList();
        return listOperations.range(key, start, stop);
    }

    public Long lLen(String key) {
        ListOperations<String, String> listOperations = redisTemplate.opsForList();
        return listOperations.size(key);
    }

    public String rPop(String key) {
        ListOperations<String, String> listOperations = redisTemplate.opsForList();
        return listOperations.rightPop(key);
    }

    public String rPop(String key, long timeout, TimeUnit unit) {
        ListOperations<String, String> listOperations = redisTemplate.opsForList();
        return listOperations.rightPop(key, timeout, unit);
    }

    public String rPopLPush(String source, String target) {
        ListOperations<String, String> listOperations = redisTemplate.opsForList();
        return listOperations.rightPopAndLeftPush(source, target);
    }

    public String brpop(String key, long timeout, TimeUnit unit) {
        return stringRedisTemplate.opsForList().rightPop(key, timeout, unit);
    }

    public Long lRem(String key, String value) {
        ListOperations<String, String> listOperations = stringRedisTemplate.opsForList();
        return listOperations.remove(key, 0, value);
    }

    public Long lPushIfAbsent(String key, String value) {
        return stringRedisTemplate.execute(lPushIfAbsent, Collections.singletonList(key), value);
    }

    public Long lPushSorted(String key, String value) {
        return stringRedisTemplate.execute(lPushSorted, Collections.singletonList(key), value);
    }

    public String rPopIfNotEmpty(String key1, String key2, String ext1, String operate) {
        if (!StringUtils.endsWithAny(operate, new String[]{"lpush", "rpush"})) {
            return null;
        }

        return stringRedisTemplate.execute(rPopIfNotEmpty, Arrays.asList(key1, key2), ext1, operate);
    }
}
