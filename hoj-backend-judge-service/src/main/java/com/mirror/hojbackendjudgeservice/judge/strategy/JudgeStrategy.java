package com.mirror.hojbackendjudgeservice.judge.strategy;


import com.mirror.hojbackendmodel.model.codesandbox.JudgeInfo;

/**
 * 判题策略
 * 根据沙箱返回回来的执行结果，生成判题信息
 * @author Mirror
 * @date 2024/8/5
 */
public interface JudgeStrategy {

    /**
     * 执行判题
     * @param judgeContext
     * @return
     */
    JudgeInfo doJudge(JudgeContext judgeContext);
}
