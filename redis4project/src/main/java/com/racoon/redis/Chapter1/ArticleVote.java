package com.racoon.redis.Chapter1;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.ZParams;

import java.util.*;

public class ArticleVote {
    private static final int ONE_WEEK_IN_SECONDS = 7 * 86400;
    private static final int VOTE_SCORE = 432;
    private static final int ARTICLES_PER_PAGE = 25;

    private void printArticles(List<Map<String,String>> articles){
        for (Map<String,String> article : articles){
            System.out.println("  id: " + article.get("id"));
            for (Map.Entry<String,String> entry : article.entrySet()){
                if (entry.getKey().equals("id")){
                    continue;
                }
                System.out.println("    " + entry.getKey() + ": " + entry.getValue());
            }
        }
    }

    public String postArticle(Jedis conn, String user, String title, String link){
        //conn.incr("article:") redis会根据"article:"前缀去增量生成articleId
        //incr每次都会遍历数据库
        String articleId = String.valueOf(conn.incr("article:"));
        String voted = "voted:"+articleId;
        //将文章的发布用户添加到已投票的用户名单中，并设置投票的过期时间为一周
        conn.sadd(voted, user);
        conn.expire(voted, ONE_WEEK_IN_SECONDS);

        long now = System.currentTimeMillis() / 1000;
        String article = "article:"+articleId;
        HashMap<String, String> articleData = new HashMap<String, String>();
        articleData.put("title", title);
        articleData.put("link", link);
        articleData.put("user", user);
        articleData.put("now", String.valueOf(now));
        articleData.put("votes", "1");
        conn.hmset(article, articleData);

        conn.zadd("score:", now+VOTE_SCORE, article);
        conn.zadd("time:", now, article);
        return articleId;
    }
    /**
     * 给文章投票
     * @param conn Jedis连接
     * @param user 投票的用户
     * @param article 被投票的文章
     */
    public void articleVote(Jedis conn, String user, String article){
        long cutoff = (System.currentTimeMillis() / 1000) - ONE_WEEK_IN_SECONDS;
        //计算文章投票的截止时间
        if(conn.zscore("time:", article) < cutoff)
            return;
        //得到article的Id,用于拼接voted:id
        String articleId = article.substring(article.indexOf(':')+1);
        //如果用户是第一次为这个文章投票，那就增加这篇文章的投票数量和评分
        //这里涉及redis的事务过程，这三个操作要放在一个事务中执行
        if(conn.sadd("voted:"+articleId, user) == 1){
            conn.zincrby("score:", VOTE_SCORE, article);
            conn.hincrBy(article, "votes", 1);
        }
    }
    public List<Map<String, String>> getArticles(Jedis conn, int page){
        return getArticles(conn, page, "score:");
    }

    private List<Map<String, String>> getArticles(Jedis conn, int page, String order) {
        int start = (page - 1)*ARTICLES_PER_PAGE;
        int end = start + ARTICLES_PER_PAGE -1;

        Set<String> ids = conn.zrevrange(order, start, end);
        List<Map<String, String>> articles = new ArrayList<Map<String, String>>();
        for(String id: ids){
            Map<String, String> articleData = conn.hgetAll(id);
            articleData.put("id", id);
            articles.add(articleData);
        }
        return articles;
    }
    //进行文件在组中的增删
    public void addGroups(Jedis conn, String articleId, String[] toAdd){
        String article = "article:"+articleId;
        for(String group:toAdd){
            conn.sadd("group:"+group, article);
        }
    }
    public List<Map<String,String>> getGroupArticles(Jedis conn, String group, int page){
        return getGroupArticles(conn, group, page, "score:");
    }
    public List<Map<String,String>> getGroupArticles(Jedis conn, String group, int page, String order){
        String key = order + group;
        if (!conn.exists(key)) {
            ZParams params = new ZParams().aggregate(ZParams.Aggregate.MAX);
            conn.zinterstore(key, params, "group:" + group, order);
            conn.expire(key, 60);
        }
        return getArticles(conn, page, key);
    }
    public void run(){
        Jedis conn = new Jedis("127.0.0.1");
        conn.auth("foobared");
        conn.select(15);
        String articleId = postArticle(
                conn, "username", "A title", "http://www.google.com");
        System.out.println("We posted a new article with id:"+articleId);
        System.out.println("Its Hash looks like:");
        Map<String,String> articleData = conn.hgetAll("article:" + articleId);
        for (Map.Entry<String, String> entry: articleData.entrySet()){
            System.out.println(" "+entry.getKey()+": "+entry.getValue());
        }
        System.out.println();
        articleVote(conn, "other_user", "article:"+articleId);
        String votes = conn.hget("article:"+articleId, "votes");
        System.out.println("We Voted for the article, it now  has vote: "+votes);

        System.out.println("The currently highest-scoring articles are:");
        List<Map<String, String>> articles = getArticles(conn, 1);
        printArticles(articles);

        addGroups(conn, articleId, new String[]{"new-group"});
        System.out.println("We added the article to a new group, other articles include:");
        articles = getGroupArticles(conn, "new-group", 1);
        printArticles(articles);

    }

    public static void main(String[] args) {
        new ArticleVote().run();
    }
}
