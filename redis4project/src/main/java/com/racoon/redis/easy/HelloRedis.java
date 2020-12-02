package com.racoon.redis.easy;

import redis.clients.jedis.Jedis;

public class HelloRedis {
    public static void main(String[] args) {
        //连接redis服务
        Jedis jedis = new Jedis("127.0.0.1");
        jedis.auth("foobared");

        //查看是否运行
        System.out.println("Server is running:"+ jedis.ping());
        jedis.set("title", "helloword");
        System.out.println(jedis.get("title"));
    }
}
