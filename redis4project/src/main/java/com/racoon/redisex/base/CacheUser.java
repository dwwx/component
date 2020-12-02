package com.racoon.redisex.base;

import com.alibaba.fastjson.JSONObject;
import redis.clients.jedis.Jedis;

public class CacheUser {
    public static void main(String[] args) {
        Jedis conn = new Jedis("127.0.0.1");
        conn.auth("foobared");
        CacheUser cacheUser = new CacheUser();
        User user = new User("aaa", "play cheese", "university");
        String userString = JSONObject.toJSONString(user, true);
        System.out.println(userString);
        cacheUser.saveUser(conn, userString);
        User user1 = JSONObject.parseObject(conn.get("user:1"), User.class);
        System.out.println(user1);
    }
    //这是以String的格式来保存用户信息
    //如何使用hash的结构来存储呢
    public void saveUser(Jedis conn, String user){
        String cacheId = conn.incr("user:").toString();
        String userId = "user:"+cacheId;
        conn.set(userId, user);
    }
    public String getUser(Jedis conn, String key){
        String user = conn.get(key);
        return user;
    }
}
