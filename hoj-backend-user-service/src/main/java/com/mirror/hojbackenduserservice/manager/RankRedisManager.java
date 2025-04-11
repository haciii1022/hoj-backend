package com.mirror.hojbackenduserservice.manager;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Arrays;
import java.util.List;

@Component
@Slf4j
public class RankRedisManager {

    public static final String USER_STATS_PREFIX = "user_stats:";
    public static final String RANK_KEY = "user_pass_rank";

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    // 加载 Lua 脚本
    private final RedisScript<Long> userStatUpdateScript;


    public RankRedisManager() {
        // 从 classpath 读取 Lua 脚本
        ClassPathResource resource = new ClassPathResource("lua/userStatLuaScript.lua");
        log.info("加载Lua脚本...");
        this.userStatUpdateScript = RedisScript.of(resource, Long.class);
    }

    public void updateUserStats(Long userId, int submitDelta, int acceptDelta, int solvedDelta) {
        String userKey = USER_STATS_PREFIX + userId;
        List<String> keys = Arrays.asList(userKey, RANK_KEY);
        Object[] args = { (long)submitDelta, (long)acceptDelta, (long)solvedDelta, userId.toString() };
        // 执行 Lua 脚本
        redisTemplate.execute(userStatUpdateScript, keys, args);
    }

    // 原子化更新操作
    public void updateUserStats2(Long userId, int submitDelta, int acceptDelta, int solvedDelta) {
        String userKey = USER_STATS_PREFIX + userId;

        redisTemplate.executePipelined((RedisCallback<Object>) connection -> {
            // 更新提交数
            if(submitDelta > 0) {
                connection.hIncrBy(userKey.getBytes(), "submitNum".getBytes(), submitDelta);
            }

            // 更新通过次数
            if(acceptDelta > 0) {
                connection.hIncrBy(userKey.getBytes(), "acceptedNum".getBytes(), acceptDelta);
            }

            // 更新通过题目数
            if(solvedDelta > 0) {
                connection.hIncrBy(userKey.getBytes(), "solvedNum".getBytes(), solvedDelta);
                connection.zIncrBy(RANK_KEY.getBytes(), solvedDelta, userId.toString().getBytes());
            }
            return null;
        });
    }


}