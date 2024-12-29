package com.mirror.hojbackendquestionservice.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.mirror.hojbackendmodel.model.dto.questionSubmit.QuestionSubmitAddRequest;
import com.mirror.hojbackendmodel.model.dto.questionSubmit.QuestionSubmitQueryRequest;
import com.mirror.hojbackendmodel.model.entity.QuestionSubmit;
import com.mirror.hojbackendmodel.model.entity.User;
import com.mirror.hojbackendmodel.model.vo.QuestionSubmitVO;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Map;


/**
* @author Mirror
* @description 针对表【question_submit(题目提交)】的数据库操作Service
* @createDate 2024-06-13 10:35:37
*/
public interface QuestionSubmitService extends IService<QuestionSubmit> {
    /**
     * 题目提交
     *
     * @param questionSubmitAddRequest
     * @param loginUser
     * @return 题目提交id
     */
    long doQuestionSubmit(QuestionSubmitAddRequest questionSubmitAddRequest, User loginUser);

    /**
     * 获取查询条件
     *
     * @param questionSubmitQueryRequest
     * @return
     */
    QueryWrapper<QuestionSubmit> getQueryWrapper(QuestionSubmitQueryRequest questionSubmitQueryRequest);



    /**
     * 获取题目封装
     *
     * @param questionSubmit  题目提交记录
     * @param loginUser       登录用户
     * @param isValidate      是否需要再次校验
     * @param withRelatedData 是否需要封装关联信息
     * @return
     */
    QuestionSubmitVO getQuestionSubmitVO(QuestionSubmit questionSubmit, User loginUser,  boolean isValidate, boolean withRelatedData);

    /**
     * 分页获取题目封装
     *
     * @param questionSubmitPage
     * @param loginUser
     * @return
     */
    Page<QuestionSubmitVO> getQuestionSubmitVOPage(Page<QuestionSubmit> questionSubmitPage, User loginUser);

    Boolean isAuthorizedToViewDetail(QuestionSubmit questionSubmit, User loginUser);

    /**
     * 返回题目提交记录的统计数据，供前端Echarts展示。
     * @param questionId
     * @return
     */
    Map<String, Object> getQuestionScoreData(Long questionId);
}
