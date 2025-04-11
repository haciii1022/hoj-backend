package com.mirror.hojbackenduserservice.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mirror.hojbackendcommon.utils.SeqUtil;
import com.mirror.hojbackendmodel.model.dto.userQuestion.UserQuestionAddRequest;
import com.mirror.hojbackendmodel.model.entity.UserQuestionStatistics;
import com.mirror.hojbackendmodel.model.enums.BaseSequenceEnum;
import com.mirror.hojbackenduserservice.manager.RankRedisManager;
import com.mirror.hojbackenduserservice.mapper.UserAcceptedQuestionMapper;
import com.mirror.hojbackenduserservice.service.UserQuestionStatisticsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.mirror.hojbackenduserservice.manager.RankRedisManager.USER_STATS_PREFIX;

@Service
@Slf4j
public class UserQuestionStatisticsServiceImpl
        extends ServiceImpl<UserAcceptedQuestionMapper, UserQuestionStatistics>
        implements UserQuestionStatisticsService {

    @Resource
    RedisTemplate<String, Object> redisTemplate;

    @Resource
    RankRedisManager redisManager;

    public static final String RANK_KEY = "user_pass_rank";
    /**
     *
     * @param request 包含用户ID、题目ID、提交ID, 得分的请求对象
     * @return id>0 正常插入 =0无数据插入  <0理应有数据插入但是插入失败
     */
    @Override
    @Transactional(
            propagation = Propagation.REQUIRES_NEW,
            isolation = Isolation.READ_COMMITTED,
            rollbackFor = Exception.class
    )
    public Long addAcceptedRecord(UserQuestionAddRequest request) {
        Long userId = request.getUserId();
        Long questionId = request.getQuestionId();
        Long submitId = request.getSubmitId();
        Integer score = request.getScore();


        // 查询现有记录
        UserQuestionStatistics existing = this.getOne(
                new LambdaQueryWrapper<UserQuestionStatistics>()
                        .eq(UserQuestionStatistics::getUserId, userId)
                        .eq(UserQuestionStatistics::getQuestionId, questionId)
                        .eq(UserQuestionStatistics::getIsDelete, 0)
        );

        // 初始化增量参数
        int submitDelta = 1;
        int acceptDelta = 0;
        int solvedDelta = 0;

        // 数据库操作
        if (existing == null) {
            return handleNewRecord(request, submitDelta, acceptDelta, solvedDelta);
        } else {
            return handleExistingRecord(request, existing, submitDelta, acceptDelta, solvedDelta);
        }
    }

    // 处理新纪录
    private Long handleNewRecord(UserQuestionAddRequest request,
                                 int submitDelta, int acceptDelta, int solvedDelta) {
        UserQuestionStatistics record = createNewRecord(request);
        boolean saveSuccess = this.save(record);

        if (!saveSuccess) {
            return -1L;
        }

        // 如果是AC提交
        if (request.getScore() == 100) {
            acceptDelta = 1;
            solvedDelta = 1;
//            saveUserQuestionScore(request.getUserId(), request.getQuestionId(), 100);
        }
        saveUserQuestionScore(request.getUserId(), request.getQuestionId(), request.getScore());
        // 执行Redis更新
        redisManager.updateUserStats(request.getUserId(), submitDelta, acceptDelta, solvedDelta);
        return record.getId();
    }

    // 处理已有记录
    private Long handleExistingRecord(UserQuestionAddRequest request,
                                      UserQuestionStatistics existing,
                                      int submitDelta, int acceptDelta, int solvedDelta) {
        UserQuestionStatistics updateEntity = new UserQuestionStatistics();
        updateEntity.setId(existing.getId());
        updateEntity.setSubmitNum(existing.getSubmitNum() + 1);

        // AC逻辑处理
        if (request.getScore() == 100) {
            acceptDelta = 1;

            // 首次AC该题目
            if (existing.getFirstAcceptedId() == null) {
                updateEntity.setFirstAcceptedId(request.getSubmitId());
                updateEntity.setAcceptedNum(1);
                solvedDelta = 1;
                saveUserQuestionScore(request.getUserId(), request.getQuestionId(), 100);
            } else {
                updateEntity.setAcceptedNum(existing.getAcceptedNum() + 1);
            }
        } else if (request.getScore() > existing.getHighestScore()) {
            updateEntity.setHighestScore(request.getScore());
            saveUserQuestionScore(request.getUserId(), request.getQuestionId(), request.getScore());
        }

        boolean updateSuccess = this.updateById(updateEntity);
        if (!updateSuccess) {
            return -1L;
        }

        // 执行Redis更新
        redisManager.updateUserStats(request.getUserId(), submitDelta, acceptDelta, solvedDelta);
        return 0L;
    }

    // 创建新记录
    private UserQuestionStatistics createNewRecord(UserQuestionAddRequest request) {
        UserQuestionStatistics record = new UserQuestionStatistics();
        record.setId(SeqUtil.next(BaseSequenceEnum.USER_QUESTION_STATISTICS_ID.getName()));
        record.setUserId(request.getUserId());
        record.setQuestionId(request.getQuestionId());
        record.setSubmitNum(1);
        record.setHighestScore(request.getScore());

        if (request.getScore() == 100) {
            record.setFirstAcceptedId(request.getSubmitId());
            record.setAcceptedNum(1);
        }
        return record;
    }


//    @Override
//    @Transactional(rollbackFor = Exception.class)
//    public Long addAcceptedRecord(UserQuestionAddRequest request) {
//        // 参数校验由Controller完成
//        Long userId = request.getUserId();
//        Long questionId = request.getQuestionId();
//        Long submitId = request.getSubmitId();
//        Integer score = request.getScore();
//        long count = this.baseMapper.selectCount(new QueryWrapper<UserQuestionStatistics>()
//                .eq(userId != null, "userId", userId)
//                .eq(questionId != null, "questionId", questionId)
//                .eq("isDelete", 0));
//        Long id = 0L;
//        if (count == 0) {
//            log.debug("count: {}", count);
//            UserQuestionStatistics record = new UserQuestionStatistics();
//            id = SeqUtil.next(BaseSequenceEnum.USER_QUESTION_STATISTICS_ID.getName());
//            record.setId(id);
//            record.setUserId(userId);
//            record.setQuestionId(questionId);
//            record.setSubmitNum(1);
//            record.setHighestScore(score);
//            if (score == 100) {
//                record.setFirstAcceptedId(submitId);
//                record.setAcceptedNum(1);
//            }
//            boolean b = this.save(record);
//            if (!b) {
//                id = -1L;
//            } else {
//                saveUserQuestionScore(userId,questionId,score);
//            }
//        } else {
//            // 已经存在记录，更新统计信息
//            UserQuestionStatistics existing = this.getOne(
//                    new QueryWrapper<UserQuestionStatistics>()
//                            .eq("userId", userId)
//                            .eq("questionId", questionId)
//                            .eq("isDelete", 0));
//            if (existing == null) {
//                return -1L; // 理论上不会进入这里，防御性编程
//            }
//            // 初始化更新对象
//            UserQuestionStatistics updateEntity = new UserQuestionStatistics();
//            updateEntity.setId(existing.getId());
//
//            // 1. 提交次数+1
//            updateEntity.setSubmitNum(existing.getSubmitNum() + 1);
//
//            // 2.1 历史最高分不是100
//            if (score == 100) {
//                // 2.1 历史最高分不是100
//                if (existing.getHighestScore() != 100) {
//                    updateEntity.setHighestScore(100);
//                    updateEntity.setFirstAcceptedId(submitId);
//                    updateEntity.setAcceptedNum(1);
//                    saveUserQuestionScore(userId,questionId,score);
//                } else {
//                    // 2.2 历史最高分是100
//                    updateEntity.setAcceptedNum(existing.getAcceptedNum() + 1);
//                }
//            } else if (score > existing.getHighestScore()) {
//                //历史最高分小于此次提交且此次不是100分
//                updateEntity.setHighestScore(score);
//                saveUserQuestionScore(userId,questionId,score);
//            }
//            boolean b = this.updateById(updateEntity);
//            if (!b) {
//                id = -1L;
//            }
//        }
//        return id;
//    }

    @Override
    public List<Long> getUserAcceptedQuestions(Long userId) {
        return this.baseMapper.selectList(new QueryWrapper<UserQuestionStatistics>()
                        .select("questionId")
                        .eq("userId", userId)
                        .eq("highestScore", 100)
                        .eq("isDelete", 0)
                        .orderByDesc("questionId"))
                .stream()
                .map(UserQuestionStatistics::getQuestionId)
                .collect(Collectors.toList());
    }

    @Override
    public Map<Long, Integer> getUserHighestScores(Long userId, List<Long> questionIds) {
        // 1. 参数校验
        if (userId == null || questionIds == null || questionIds.isEmpty()) {
            return Collections.emptyMap();
        }
        // 2. 批量查询Redis
        HashOperations<String, String, Integer> hashOps = redisTemplate.opsForHash();
        String key = getUserKey(userId);
        List<Long> distinctQuestionIds = questionIds.stream().distinct().collect(Collectors.toList());
        // 将 Long 类型的字段转换为 String 类型
        List<String> stringQuestionIds = distinctQuestionIds.stream()
                .map(id -> id.toString())  // 显式转为 String
                .collect(Collectors.toList());
        List<Integer> cachedScores = hashOps.multiGet(key, stringQuestionIds);
        // 3. 构建结果集并识别缺失数据
        Map<Long, Integer> result = new ConcurrentHashMap<>();
        List<Long> missingQuestionIds = new ArrayList<>();

        IntStream.range(0, distinctQuestionIds.size()).forEach(i -> {
            Long qId = distinctQuestionIds.get(i);
            Integer score = cachedScores.get(i);
            if (score != null) {
                result.put(qId, score);
            } else {
                missingQuestionIds.add(qId);
            }
        });
        // 4. 批量查询数据库补全缺失数据
        if (!missingQuestionIds.isEmpty()) {
            List<UserQuestionStatistics> dbRecords = this.baseMapper.selectList(
                    new QueryWrapper<UserQuestionStatistics>()
                            .eq("userId", userId)
                            .in("questionId", missingQuestionIds)
            );
            // 5. 更新缓存并合并结果
            dbRecords.forEach(record -> {
                result.put(record.getQuestionId(), record.getHighestScore());
                hashOps.put(key, record.getQuestionId().toString(), record.getHighestScore());
            });

            // 6. 处理数据库中不存在的记录
            missingQuestionIds.removeAll(result.keySet());
            missingQuestionIds.forEach(id -> result.put(id, -1)); // 还是不存在的数据说明是-1
        }
        return result;
    }

    private Integer getScore(Long userId, Long questionId) {
        Integer score = (Integer) redisTemplate.opsForHash().get(getUserKey(userId), questionId);
        if(score == null){
            //访问数据库
            UserQuestionStatistics statistics = this.baseMapper.selectOne(new QueryWrapper<UserQuestionStatistics>()
                    .eq("userId", userId)
                    .eq("questionId", questionId));
            score = statistics == null ? -1: statistics.getHighestScore();
            saveUserQuestionScore(userId,questionId,score);
        }
        return score;
    }

    // 使用Spring RedisTemplate操作
    private void saveUserQuestionScore(Long userId, Long questionId, Integer score) {
        HashOperations<String, String, Integer> hashOps = redisTemplate.opsForHash();
        hashOps.put(getUserKey(userId), questionId.toString(), score);
    }

    private String getUserKey(Long userId) {
        return "user_scores:" + userId; // 示例：user_scores:1001
    }


    @Override
    public void fullSyncRedisRank() {
        //TODO 未来这里如果数据量太大的话，需要改成分批多次查询
        // 1. 从数据库获取全量数据
        List<UserQuestionStatistics> allStats = baseMapper.selectList(
                new LambdaQueryWrapper<UserQuestionStatistics>()
                        .eq(UserQuestionStatistics::getIsDelete, 0)
        );

        // 2. 按用户聚合数据
        Map<Long, UserStatsAggregation> aggregationMap = allStats.stream()
                .collect(Collectors.groupingBy(
                        UserQuestionStatistics::getUserId,
                        Collectors.reducing(
                                new UserStatsAggregation(),
                                stat -> new UserStatsAggregation(stat.getSubmitNum(), stat.getAcceptedNum(),
                                        stat.getFirstAcceptedId() != null ? 1 : 0),
                                UserStatsAggregation::merge
                        )
                ));

        // 3. 批量更新Redis
        aggregationMap.forEach((userId, stats) -> {
            String userKey = USER_STATS_PREFIX + userId;
            Map<String, Object> hashData = new HashMap<>();
            hashData.put("submitNum", stats.submits);
            hashData.put("acceptedNum", stats.accepts);
            hashData.put("solvedNum", stats.solved);
            // 计算复合分数
            double acceptedRate = (stats.submits > 0) ? (double) stats.accepts / stats.submits : 0;
            double score = stats.solved * 1e6 + acceptedRate * 10000;
            // 更新 Hash 和 ZSet
            redisTemplate.opsForHash().putAll(userKey, hashData);
            redisTemplate.opsForZSet().add(RANK_KEY, userId.toString(), score);
        });
    }

    // 聚合类
    private static class UserStatsAggregation {
        int submits;
        int accepts;
        int solved;

        // 添加三参数构造函数
        public UserStatsAggregation(int submits, int accepts, int solved) {
            this.submits = submits;
            this.accepts = accepts;
            this.solved = solved;
        }

        // 空构造函数（如果用于初始值需要）
        public UserStatsAggregation() {}

        UserStatsAggregation merge(UserStatsAggregation other) {
            this.submits += other.submits;
            this.accepts += other.accepts;
            this.solved += other.solved;
            return this;
        }
    }
}