package com.mirror.hojbackendjudgeservice.judge.strategy;

import cn.hutool.json.JSONUtil;
import com.mirror.hojbackendcommon.utils.FileUtil;
import com.mirror.hojbackendmodel.model.codesandbox.JudgeInfo;
import com.mirror.hojbackendmodel.model.dto.question.JudgeCase;
import com.mirror.hojbackendmodel.model.dto.question.JudgeConfig;
import com.mirror.hojbackendmodel.model.entity.Question;
import com.mirror.hojbackendmodel.model.enums.JudgeInfoMessageEnum;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.Objects;

/**
 * @author Mirror
 * @date 2024/8/5
 */
public class JavaLanguageJudgeStrategy implements JudgeStrategy {

    /**
     * @param judgeContext
     * @return JudgeInfo 为总判题信息
     */
    @Override
    public JudgeInfo doJudge(JudgeContext judgeContext) {
        //获取相关配置
        Question question = judgeContext.getQuestion();
        // 旧版本
//        List<String> outputList = judgeContext.getOutputList();
//        List<String> inputList = judgeContext.getInputList();
//        List<JudgeCase> judgeCaseList = judgeContext.getJudgeCaseList();
        String judgeConfigStr = question.getJudgeConfig();
        /*
          1、RUNTIME_ERROR
          2、Memory Limit Exceeded
          3、Time Limit Exceeded
          4、Wrong Answer / Accepted
         */
        List<JudgeInfo> judgeInfoList = judgeContext.getJudgeInfoList();
        long maxExecuteTime = 0L;
        long maxExecuteMemory = 0L;
        JudgeConfig judgeConfig = JSONUtil.toBean(judgeConfigStr, JudgeConfig.class);

        List<String> judgeCaseOutputFilePathList = judgeContext.getJudgeCaseOutputFilePathList();
        List<String> outputFilePathList = judgeContext.getOutputFilePathList();
        // 1ac 2wa 3tle 4mle 5re
        int flag = 1;
        for (int i = 0; i < judgeInfoList.size(); i++) {
            JudgeInfo judgeInfo = judgeInfoList.get(i);
            Long time = judgeInfo.getTime();
            Long memory = judgeInfo.getMemory();
            maxExecuteTime = Math.max(maxExecuteTime, time);
            maxExecuteMemory = Math.max(maxExecuteMemory, memory);
            String message = judgeInfo.getMessage();
            int nowFlag = 1;
            if (StringUtils.isNotBlank(message)) {
                // TODO 判断是不是re，这里先写死都是re
                judgeInfo.setMessage(JudgeInfoMessageEnum.RUNTIME_ERROR.getText());
                nowFlag = 5;
            } else {
                if (memory > judgeConfig.getMemoryLimit() * 1024 * 2) {
                    //判断是否超时,java默认是两倍
                    judgeInfo.setMessage(JudgeInfoMessageEnum.MEMORY_LIMIT_EXCEEDED.getText());
                    nowFlag = 4;
                } else if (time > judgeConfig.getTimeLimit()  * 2) {
                    //判断是否超内存，java默认是两倍
                    //左边是B，右边是KB
                    judgeInfo.setMessage(JudgeInfoMessageEnum.TIME_LIMIT_EXCEEDED.getText());
                    nowFlag = 3;
                } else {
                    if (FileUtil.compareFilesIgnoringLastLineEnding(judgeCaseOutputFilePathList.get(i), outputFilePathList.get(i))) {
                        judgeInfo.setMessage(JudgeInfoMessageEnum.ACCEPTED.getText());
                    } else {
                        judgeInfo.setMessage(JudgeInfoMessageEnum.WRONG_ANSWER.getText());
                        nowFlag = 2;
                    }
                }
            }
            flag = Math.max(flag, nowFlag);
        }

        JudgeInfo judgeInfoResult = new JudgeInfo();
        judgeInfoResult.setTime(maxExecuteTime);
        judgeInfoResult.setMemory(maxExecuteMemory);
        switch (flag) {
            case 1:
                judgeInfoResult.setMessage(JudgeInfoMessageEnum.ACCEPTED.getText());
                break;
            case 2:
                judgeInfoResult.setMessage(JudgeInfoMessageEnum.WRONG_ANSWER.getText());
                break;
            case 3:
                judgeInfoResult.setMessage(JudgeInfoMessageEnum.MEMORY_LIMIT_EXCEEDED.getText());
                break;
            case 4:
                judgeInfoResult.setMessage(JudgeInfoMessageEnum.TIME_LIMIT_EXCEEDED.getText());
                break;
            case 5:
                judgeInfoResult.setMessage(JudgeInfoMessageEnum.RUNTIME_ERROR.getText());
                break;
            default:
                judgeInfoResult.setMessage(JudgeInfoMessageEnum.SYSTEM_ERROR.getText());
        }

        return judgeInfoResult;
    }
}