package com.mirror.hoj.judge.strategy;

import cn.hutool.json.JSONUtil;
import com.mirror.hoj.model.dto.question.JudgeCase;
import com.mirror.hoj.model.dto.question.JudgeConfig;
import com.mirror.hoj.judge.codesandbox.model.JudgeInfo;
import com.mirror.hoj.model.entity.Question;
import com.mirror.hoj.model.enums.JudgeInfoMessageEnum;

import java.util.List;
import java.util.Objects;

/**
 * @author Mirror
 * @date 2024/8/5
 */
public class JavaLanguageJudgeStrategy implements JudgeStrategy {

    @Override
    public JudgeInfo doJudge(JudgeContext judgeContext) {
        //获取相关配置
        Question question = judgeContext.getQuestion();
        List<String> outputList = judgeContext.getOutputList();
        String judgeConfigStr = question.getJudgeConfig();
        List<String> inputList = judgeContext.getInputList();
        List<JudgeCase> judgeCaseList = judgeContext.getJudgeCaseList();
        JudgeInfo judgeInfo = judgeContext.getJudgeInfo();
        judgeInfo = new JudgeInfo();
        judgeInfo.setTime(500L);
        judgeInfo.setMemory(1024L);
        JudgeConfig judgeConfig = JSONUtil.toBean(judgeConfigStr, JudgeConfig.class);
        Long executeTime = judgeInfo.getTime();
        Long executeMemory = judgeInfo.getMemory();
        //判断默认结果为通过
        JudgeInfoMessageEnum judgeInfoMessageEnum = JudgeInfoMessageEnum.ACCEPTED;

        JudgeInfo judgeInfoResult = new JudgeInfo();
        judgeInfoResult.setTime(executeTime);
        judgeInfoResult.setMemory(executeMemory);
        //判断是否超时,java默认是两倍
        if (executeTime > judgeConfig.getTimeLimit() * 2) {
            judgeInfoMessageEnum = JudgeInfoMessageEnum.TIME_LIMIT_EXCEEDED;
            judgeInfoResult.setMessage(judgeInfoMessageEnum.getValue());
            return judgeInfoResult;
        }
        //判断是否超内存，java默认是两倍
        if (executeMemory > judgeConfig.getMemoryLimit() * 2) {
            judgeInfoMessageEnum = JudgeInfoMessageEnum.MEMORY_LIMIT_EXCEEDED;
            judgeInfoResult.setMessage(judgeInfoMessageEnum.getValue());
            return judgeInfoResult;
        }
        //输出个数不同，直接返回错误
        if (outputList.size() != inputList.size()) {
            judgeInfoMessageEnum = JudgeInfoMessageEnum.WRONG_ANSWER;
            judgeInfoResult.setMessage(judgeInfoMessageEnum.getValue());
            return judgeInfoResult;
        }
        //依次比对每一项输出和预期输出
        for (int i = 0; i < outputList.size(); i++) {
            if (!Objects.equals(outputList.get(i), judgeCaseList.get(i).getOutput())) {
                judgeInfoMessageEnum = JudgeInfoMessageEnum.WRONG_ANSWER;
                judgeInfoResult.setMessage(judgeInfoMessageEnum.getValue());
                return judgeInfoResult;
            }
        }

        judgeInfoResult.setMessage(judgeInfoMessageEnum.getValue());
        return judgeInfoResult;
    }
}