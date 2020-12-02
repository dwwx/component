package com.racoon.redisex.application.messageQueue;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import redis.clients.jedis.Jedis;

import java.lang.reflect.Type;
import java.util.Set;
import java.util.UUID;

/**
 * 用redis演示了实现延迟队列
 * 思考：redis作为消息队列为什么不能保证100%的可靠性
 *
 * @param <T>
 */
public class RedisDelayingQueue<T> {
    static class TaskItem<T>{
        public String id;
        public T msg;
    }
    private Type TasKType = new TypeReference<TaskItem<T>>(){}.getType();
    private Jedis jedis;
    private String queueKey;
    public RedisDelayingQueue(Jedis jedis, String queueKey){
        this.jedis = jedis;
        this.queueKey = queueKey;
    }
    public void delay(T msg){
        TaskItem taskItem = new TaskItem();
        taskItem.id = UUID.randomUUID().toString();
        taskItem.msg = msg;
        String s = JSON.toJSONString(taskItem);
        jedis.zadd(queueKey, System.currentTimeMillis()+5000, s);
    }
    public void loop(){
        while (!Thread.interrupted()){
            //只取一条
            Set values = jedis.zrangeByScore(queueKey, 0, System.currentTimeMillis(),0,1);
            if(values.isEmpty()){
                try {
                    Thread.sleep(500);
                }catch (InterruptedException e){
                    e.printStackTrace();
                    break;
                }
                continue;
            }
            String s = (String) values.iterator().next();
            if(jedis.zrem(queueKey, s) >0){
                TaskItem task = JSON.parseObject(s, TasKType);
                this.handleMsg((T)task.msg);
            }
        }
    }
    public void handleMsg(T msg){
        System.out.println(msg);
    }

    public static void main(String[] args) {
        Jedis jedis = new Jedis();
        jedis.auth("foobared");
        final RedisDelayingQueue queue = new RedisDelayingQueue(jedis, "q-demo");
        Thread producer = new Thread(){
            public void run(){
                for(int i=0; i<10;i++){
                    queue.delay("codehole"+i);
                }
            }
        };
        Thread consumer = new Thread(){
            public void run(){
                queue.loop();
            }
        };
        producer.start();
        consumer.start();
        try{
            producer.join();
            Thread.sleep(6000);
            consumer.interrupt();
            consumer.join();
        }catch (InterruptedException e){
            e.printStackTrace();
        }
    }
}
