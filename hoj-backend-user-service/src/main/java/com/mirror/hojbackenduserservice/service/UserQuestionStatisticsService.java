package com.mirror.hojbackenduserservice.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.mirror.hojbackendmodel.model.dto.userQuestion.UserQuestionAddRequest;
import com.mirror.hojbackendmodel.model.entity.UserQuestionStatistics;

import java.util.List;
import java.util.Map;

public interface UserQuestionStatisticsService extends IService<UserQuestionStatistics> {

    /**
     * 添加用户通过题目记录（DTO版本）
     * @param request 包含用户ID、题目ID、提交ID、分数的请求对象
     * @return id>0 正常插入 =0无数据插入  <0理应有数据插入但是插入失败
     */
    Long addAcceptedRecord(UserQuestionAddRequest request);

    /**
     * 获取用户通过的题目列表
     * @param userId 用户ID
     * @return 通过题目ID列表
     */
    List<Long> getUserAcceptedQuestions(Long userId);

    /**
     * 批量获取用户指定题目的最高分
     * @param userId 用户ID
     * @param questionIds 题目ID集合
     * @return Map<题目ID, 最高分> (包含存在的记录，不包含未查询到的题目)
     */
    Map<Long, Integer> getUserHighestScores(Long userId, List<Long> questionIds);

    /**
     * 全量更新redis用户过题排行榜数据
     */
    void fullSyncRedisRank();
}