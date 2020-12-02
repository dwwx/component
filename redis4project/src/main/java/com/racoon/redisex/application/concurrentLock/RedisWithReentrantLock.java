package com.racoon.redisex.application.concurrentLock;

import redis.clients.jedis.Jedis;

import java.util.HashMap;
import java.util.Map;

/**
 * 本章介绍redis的分布式锁
 *
 * 用threadLocal实现redis的可重入锁
 *
 */
public class RedisWithReentrantLock {
    private ThreadLocal<Map> lockers = new ThreadLocal<Map>();
    private Jedis jedis;
    public RedisWithReentrantLock(Jedis jedis){
        this.jedis = jedis;
    }
    private boolean _lock(String key){
        return jedis.setex(key, 5, "")!=null;
    }
    private void _unlock(String key){
        jedis.del(key);
    }
    private Map<String, Integer> currentLockers(){
        Map<String, Integer> refs = lockers.get();
        if(refs != null)return refs;
        lockers.set(new HashMap());
        return lockers.get();
    }
    public boolean lock(String key){
        Map refs = currentLockers();
        Integer refCnt = (Integer) refs.get(key);
        if(refCnt != null){
            refs.put(key, refCnt+1);
            return true;
        }
        boolean ok = this._lock(key);
        if(!ok) return false;
        refs.put(key, 1);
        return true;
    }
    public boolean unlock(String key){
        Map refs = currentLockers();
        Integer refCnt = (Integer) refs.get(key);
        if(refCnt == null) return false;
        refCnt -= 1;
        if(refCnt > 0){
            refs.put(key, refCnt);
        }else {
            refs.remove(key);
            this._lock(key);
        }
        return true;
    }

    public static void main(String[] args) {
        Jedis jedis = new Jedis();
        jedis.auth("foobared");
        RedisWithReentrantLock redis = new RedisWithReentrantLock(jedis);
        System.out.println(redis.lock("co"));
        System.out.println(redis.lock("co"));
        System.out.println(redis.unlock("co"));
        System.out.println(redis.unlock("co"));
    }
}
