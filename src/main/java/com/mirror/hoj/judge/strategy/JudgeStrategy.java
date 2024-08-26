package com.mirror.hoj.judge.strategy;

import com.mirror.hoj.judge.codesandbox.model.JudgeInfo;

/**
 * 判题策略
 *
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
