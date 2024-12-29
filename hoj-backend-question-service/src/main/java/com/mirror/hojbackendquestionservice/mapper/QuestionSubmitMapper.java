package com.mirror.hojbackendquestionservice.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mirror.hojbackendmodel.model.entity.QuestionSubmit;
import feign.Param;

import java.util.List;
import java.util.Map;


/**
* @author Mirror
* @description 针对表【question_submit(题目提交)】的数据库操作Mapper
* @createDate 2024-06-13 10:35:37
* @Entity com.mirror.hoj.model.entity.QuestionSubmit
*/
public interface QuestionSubmitMapper extends BaseMapper<QuestionSubmit> {

    /**
     * 查询分数分布
     * @param questionId
     * @return
     */
    List<Map<String, Object>> getScoreDistribution(@Param("questionId") Long questionId);

    /**
     * 查询其他统计数据：平均分，最大/最小分数，最常用语言
     * @param questionId
     * @return
     */
    Map<String, Object> getOtherStatistics(@Param("questionId") Long questionId);
}




