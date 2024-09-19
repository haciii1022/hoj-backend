package com.mirror.hojbackendjudgeservice.judge.strategy;


import com.mirror.hojbackendmodel.model.codesandbox.JudgeInfo;
import com.mirror.hojbackendmodel.model.dto.question.JudgeCase;
import com.mirror.hojbackendmodel.model.entity.Question;
import com.mirror.hojbackendmodel.model.entity.QuestionSubmit;
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

    private List<JudgeInfo> judgeInfoList;

    private List<String> inputList;

    private List<String> outputList;

    private List<JudgeCase> judgeCaseList;

    private Question question;

    private QuestionSubmit questionSubmit;

}
