package com.mirror.hojbackendjudgeservice.judge.strategy;

import cn.hutool.json.JSONUtil;
import com.mirror.hojbackendmodel.model.codesandbox.JudgeInfo;
import com.mirror.hojbackendmodel.model.dto.question.JudgeCase;
import com.mirror.hojbackendmodel.model.dto.question.JudgeConfig;
import com.mirror.hojbackendmodel.model.entity.Question;
import com.mirror.hojbackendmodel.model.enums.JudgeInfoMessageEnum;

import java.util.List;
import java.util.Objects;

/**
 * @author Mirror
 * @date 2024/8/5
 */
public class DefaultJudgeStrategy implements JudgeStrategy {

    @Override
    public JudgeInfo doJudge(JudgeContext judgeContext) {
        //获取相关配置
        Question question = judgeContext.getQuestion();
        List<String> outputList = judgeContext.getOutputList();
        String judgeConfigStr = question.getJudgeConfig();
        List<String> inputList = judgeContext.getInputList();
        List<JudgeCase> judgeCaseList = judgeContext.getJudgeCaseList();
        JudgeInfo judgeInfo = judgeContext.getJudgeInfo();
        JudgeConfig judgeConfig = JSONUtil.toBean(judgeConfigStr, JudgeConfig.class);
        Long executeTime = judgeInfo.getTime();
        Long executeMemory = judgeInfo.getMemory();
        //判断默认结果为通过
        JudgeInfoMessageEnum judgeInfoMessageEnum = JudgeInfoMessageEnum.ACCEPTED;

        JudgeInfo judgeInfoResult = new JudgeInfo();
        judgeInfoResult.setTime(executeTime);
        judgeInfoResult.setMemory(executeMemory);
        //判断是否超时
        if (executeTime > judgeConfig.getTimeLimit()) {
            judgeInfoMessageEnum = JudgeInfoMessageEnum.TIME_LIMIT_EXCEEDED;
            judgeInfoResult.setMessage(judgeInfoMessageEnum.getText());
            return judgeInfoResult;
        }
        //判断是否超内存
        if (executeMemory > judgeConfig.getMemoryLimit()) {
            judgeInfoMessageEnum = JudgeInfoMessageEnum.MEMORY_LIMIT_EXCEEDED;
            judgeInfoResult.setMessage(judgeInfoMessageEnum.getText());
            return judgeInfoResult;
        }
        //输出个数不同，直接返回错误
        if (outputList.size() != inputList.size()) {
            judgeInfoMessageEnum = JudgeInfoMessageEnum.WRONG_ANSWER;
            judgeInfoResult.setMessage(judgeInfoMessageEnum.getText());
            return judgeInfoResult;
        }
        //依次比对每一项输出和预期输出
        for (int i = 0; i < outputList.size(); i++) {
            if (!Objects.equals(outputList.get(i), judgeCaseList.get(i).getOutput())) {
                judgeInfoMessageEnum = JudgeInfoMessageEnum.WRONG_ANSWER;
                judgeInfoResult.setMessage(judgeInfoMessageEnum.getText());
                return judgeInfoResult;
            }
        }

        judgeInfoResult.setMessage(judgeInfoMessageEnum.getText());
        return judgeInfoResult;
    }
}
