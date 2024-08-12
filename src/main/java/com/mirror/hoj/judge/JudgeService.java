package com.mirror.hoj.judge;

import com.mirror.hoj.model.entity.QuestionSubmit;

/**
 * 判题服务
 *
 * @author Mirror
 * @date 2024/8/5
 */
public interface JudgeService {
    QuestionSubmit doJudge(Long questionSubmitId);
}
