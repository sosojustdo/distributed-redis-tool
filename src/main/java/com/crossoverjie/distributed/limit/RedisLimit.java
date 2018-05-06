package com.crossoverjie.distributed.limit;

import com.crossoverjie.distributed.constant.RedisToolsConstant;
import com.crossoverjie.distributed.util.ScriptUtil;
import org.springframework.data.redis.connection.RedisClusterConnection;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisCluster;
import redis.clients.jedis.JedisPool;

import java.util.Collections;

/**
 * Function:
 *
 * @author crossoverJie
 *         Date: 22/04/2018 15:54
 * @since JDK 1.8
 */
public class RedisLimit {

    private JedisConnectionFactory jedisConnectionFactory;
    private int type ;
    private int limit = 200;

    private static final int FAIL_CODE = 0;

    /**
     * lua script
     */
    private String script;

    private RedisLimit(Builder builder) {
        this.limit = builder.limit ;
        this.jedisConnectionFactory = builder.jedisConnectionFactory;
        this.type = builder.type ;
        buildScript();
    }


    /**
     * limit traffic
     * @return if true
     */
    public boolean limit() {

        Object connection ;
        if (type == RedisToolsConstant.SINGLE){
            RedisConnection redisConnection = jedisConnectionFactory.getConnection();
            connection = redisConnection.getNativeConnection();
        }else {
            RedisClusterConnection clusterConnection = jedisConnectionFactory.getClusterConnection();
            connection = clusterConnection.getNativeConnection() ;
        }

        Object result = null;
        String key = String.valueOf(System.currentTimeMillis() / 1000);
        if (connection instanceof Jedis){
            result = ((Jedis)connection).eval(script, Collections.singletonList(key), Collections.singletonList(String.valueOf(limit)));
        }else {
            result = ((JedisCluster) connection).eval(script, Collections.singletonList(key), Collections.singletonList(String.valueOf(limit)));
        }


        if (FAIL_CODE != (Long) result) {
            return true;
        } else {
            return false;
        }
    }


    /**
     * read lua script
     */
    private void buildScript() {
        script = ScriptUtil.getScript("limit.lua");
    }


    /**
     *  the builder
     */
    public static class Builder{
        private JedisConnectionFactory jedisConnectionFactory = null ;

        private int limit = 200;
        private int type ;


        public Builder(JedisConnectionFactory jedisConnectionFactory,int type){
            this.jedisConnectionFactory = jedisConnectionFactory;
            this.type = type ;
        }

        public Builder limit(int limit){
            this.limit = limit ;
            return this;
        }

        public RedisLimit build(){
            return new RedisLimit(this) ;
        }

    }
}
