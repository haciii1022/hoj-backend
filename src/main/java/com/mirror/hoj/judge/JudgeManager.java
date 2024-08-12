package com.mirror.hoj.judge;

import com.mirror.hoj.judge.strategy.DefaultJudgeStrategy;
import com.mirror.hoj.judge.strategy.JavaLanguageJudgeStrategy;
import com.mirror.hoj.judge.strategy.JudgeContext;
import com.mirror.hoj.judge.strategy.JudgeStrategy;
import com.mirror.hoj.model.dto.questionSubmit.JudgeInfo;
import com.mirror.hoj.model.entity.QuestionSubmit;
import org.springframework.stereotype.Service;

/**
 * @author Mirror
 * @date 2024/8/5
 */
@Service
public class JudgeManager {

    public JudgeInfo doJudge(JudgeContext judgeContext) {
        JudgeStrategy judgeStrategy = new DefaultJudgeStrategy();
        QuestionSubmit questionSubmit = judgeContext.getQuestionSubmit();
        if ("java".equals(questionSubmit.getLanguage())) {
            judgeStrategy = new JavaLanguageJudgeStrategy();
        }
        return judgeStrategy.doJudge(judgeContext);
    }
}
