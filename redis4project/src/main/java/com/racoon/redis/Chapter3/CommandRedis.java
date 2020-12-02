package com.racoon.redis.Chapter3;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.Pipeline;

public class CommandRedis {
    public static void main(String[] args) {
        Jedis conn = new Jedis("127.0.0.1");
        CommandRedis c = new CommandRedis();
        c.StrCommand(conn);
    }
    public void StrCommand(Jedis conn){
        conn.set("dwx", "11");
        System.out.println(conn.get("dwx"));
        //进行自增操作incrBy 加上一个amount
        //decrBy
        //incr自增加1
        //decr
        System.out.println(conn.incrBy("dwx", 10).toString());
        conn.append("dwx", "1211");
        //substr取子串
        System.out.println(conn.substr("dwx", 0, 2));
    }
    public void ListCommand(Jedis conn){
//        conn.rpush();
//        conn.lpush();
//        conn.rpop();
//        conn.lpop();
//        conn.lindex();
//        conn.lrange();
//        conn.ltrim();

        //阻塞式的列表弹出命令
//        conn.blpop();
//        conn.brpop();
        //弹出并推出命令
//        conn.rpoplpush();
//        conn.brpoplpush();
        //创建一个事务的流
        Pipeline pipeline = conn.pipelined();
    }
}
