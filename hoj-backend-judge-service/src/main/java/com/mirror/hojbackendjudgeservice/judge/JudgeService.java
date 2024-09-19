package com.mirror.hojbackendjudgeservice.judge;


import com.mirror.hojbackendmodel.model.entity.QuestionSubmit;

/**
 * 判题服务
 *
 * @author Mirror
 * @date 2024/8/5
 */
public interface JudgeService {
    QuestionSubmit doJudge(Long questionSubmitId);
}
