package com.mirror.hojbackendjudgeservice.judge;


import com.mirror.hojbackendjudgeservice.judge.strategy.DefaultJudgeStrategy;
import com.mirror.hojbackendjudgeservice.judge.strategy.JavaLanguageJudgeStrategy;
import com.mirror.hojbackendjudgeservice.judge.strategy.JudgeContext;
import com.mirror.hojbackendjudgeservice.judge.strategy.JudgeStrategy;
import com.mirror.hojbackendmodel.model.codesandbox.JudgeInfo;
import com.mirror.hojbackendmodel.model.entity.QuestionSubmit;
import org.springframework.stereotype.Service;

/**
 * JudgeManager 类负责管理代码提交的评判过程。
 * 它根据提交代码的编程语言来确定合适的评判策略并执行评判。
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
