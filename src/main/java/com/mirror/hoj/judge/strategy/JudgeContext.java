package com.mirror.hoj.judge.strategy;

import com.mirror.hoj.model.dto.question.JudgeCase;
import com.mirror.hoj.model.dto.questionSubmit.JudgeInfo;
import com.mirror.hoj.model.entity.Question;
import com.mirror.hoj.model.entity.QuestionSubmit;
import lombok.Data;

import java.util.List;

/**
 * 判题上下文参数
 *
 * @author Mirror
 * @date 2024/8/5
 */
@Data
public class JudgeContext {

    private JudgeInfo judgeInfo;

    private List<String> inputList;

    private List<String> outputList;

    private List<JudgeCase> judgeCaseList;

    private Question question;

    private QuestionSubmit questionSubmit;

}
