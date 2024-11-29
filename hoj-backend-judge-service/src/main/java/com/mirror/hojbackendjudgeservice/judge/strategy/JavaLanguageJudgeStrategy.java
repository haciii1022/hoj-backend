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
public class JavaLanguageJudgeStrategy implements JudgeStrategy {

    /**
     *
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

        List<JudgeInfo> judgeInfoList = judgeContext.getJudgeInfoList();
        long maxExecuteTime = 0L;
        long maxExecuteMemory = 0L;
        for(JudgeInfo judgeInfo : judgeInfoList) {
            maxExecuteTime = Math.max(maxExecuteTime, judgeInfo.getTime());
            maxExecuteMemory = Math.max(maxExecuteMemory, judgeInfo.getMemory());
        }
        JudgeConfig judgeConfig = JSONUtil.toBean(judgeConfigStr, JudgeConfig.class);

        //判断默认结果为通过
        JudgeInfoMessageEnum judgeInfoMessageEnum = JudgeInfoMessageEnum.ACCEPTED;

        JudgeInfo judgeInfoResult = new JudgeInfo();
        judgeInfoResult.setTime(maxExecuteTime);
        judgeInfoResult.setMemory(maxExecuteMemory);
        //判断是否超时,java默认是两倍
        if (maxExecuteTime > judgeConfig.getTimeLimit() * 2) {
            judgeInfoMessageEnum = JudgeInfoMessageEnum.TIME_LIMIT_EXCEEDED;
            judgeInfoResult.setMessage(judgeInfoMessageEnum.getText());
            return judgeInfoResult;
        }
        //判断是否超内存，java默认是两倍
        //左边是B，右边是KB
        if (maxExecuteMemory > judgeConfig.getMemoryLimit() * 1024) {
            judgeInfoMessageEnum = JudgeInfoMessageEnum.MEMORY_LIMIT_EXCEEDED;
            judgeInfoResult.setMessage(judgeInfoMessageEnum.getText());
            return judgeInfoResult;
        }
        /*
          1、RUNTIME_ERROR
          2、Memory Limit Exceeded
          3、Time Limit Exceeded
          4、Wrong Answer / Accepted
         */
        // TODO 获取judgeCaseOutputFilePathList
        List<String> judgeCaseOutputFilePathList = judgeContext.getJudgeCaseOutputFilePathList();
        List<String> outputFilePathList = judgeContext.getOutputFilePathList();
        // TODO 改为文件比较，调用FileUtil方法，比较.out和.ans是否一致
        //输出个数不同，直接返回错误
        if (judgeCaseOutputFilePathList.size() != outputFilePathList.size()) {
            judgeInfoMessageEnum = JudgeInfoMessageEnum.WRONG_ANSWER;
            judgeInfoResult.setMessage(judgeInfoMessageEnum.getText());
            return judgeInfoResult;
        }
        //依次比对每一项输出和预期输出
//        for (int i = 0; i < outputList.size(); i++) {
//            if (!Objects.equals(outputList.get(i), judgeCaseList.get(i).getOutput())) {
//                System.out.println("outputList.get(i): " + outputList.get(i));
//                System.out.println("judgeCaseList.get(i).getOutput(): " + judgeCaseList.get(i).getOutput());
//                judgeInfoMessageEnum = JudgeInfoMessageEnum.WRONG_ANSWER;
//                judgeInfoResult.setMessage(judgeInfoMessageEnum.getText());
//                return judgeInfoResult;
//            }
//        }
        judgeInfoResult.setMessage(judgeInfoMessageEnum.getText());
        return judgeInfoResult;
    }
}