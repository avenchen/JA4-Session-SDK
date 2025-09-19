package com.example.session;

import java.time.Duration;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

public final class RedisManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(RedisManager.class);
    private static final String DEFAULT_HOST = "redis";
    private static final int DEFAULT_PORT = 6379;
    private static final int DEFAULT_TIMEOUT = 5000;

    private static final JedisPool POOL;
    private static final boolean ENABLED;

    static {
        JedisPool pool = null;
        boolean enabled = false;
        try {
            String host = Optional.ofNullable(System.getenv("REDIS_HOST")).orElse(DEFAULT_HOST);
            int port = Optional.ofNullable(System.getenv("REDIS_PORT"))
                    .map(Integer::parseInt)
                    .orElse(DEFAULT_PORT);
            String password = System.getenv("REDIS_PASSWORD");

            JedisPoolConfig config = new JedisPoolConfig();
            config.setMaxTotal(16);
            config.setMaxIdle(8);
            config.setMinIdle(1);
            config.setMaxWait(Duration.ofSeconds(5));

            if (password != null && !password.isBlank()) {
                pool = new JedisPool(config, host, port, DEFAULT_TIMEOUT, password);
            } else {
                pool = new JedisPool(config, host, port, DEFAULT_TIMEOUT);
            }
            enabled = true;
            LOGGER.info("Redis support enabled (host={}, port={})", host, port);
        } catch (Exception ex) {
            LOGGER.warn("Redis disabled: {}", ex.getMessage());
        }
        POOL = pool;
        ENABLED = enabled;
    }

    private RedisManager() {
    }

    public static boolean isEnabled() {
        return ENABLED;
    }

    public static void closePool() {
        if (POOL != null) {
            POOL.close();
        }
    }

    public static <T> T execute(RedisCallback<T> callback) {
        if (!ENABLED || POOL == null) {
            return null;
        }
        try (Jedis jedis = POOL.getResource()) {
            return callback.doInRedis(jedis);
        }
    }

    @FunctionalInterface
    public interface RedisCallback<T> {
        T doInRedis(Jedis jedis);
    }
}
