package com.alibaba.csp.sentinel.dashboard.redis.common;

public class RedisLuaScript {
    public static final String INCREMENT_AND_SET_EXPIRE_SCRIPT = "local num =  redis.call('incr',KEYS[1]);if(num == 1) then redis.call('expire',KEYS[1],ARGV[1]);end return num;";
}
