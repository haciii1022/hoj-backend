package com.mirror.hoj.judge.codesandbox.impl;

import com.mirror.hoj.judge.codesandbox.CodeSandbox;
import com.mirror.hoj.judge.codesandbox.model.ExecuteCodeRequest;
import com.mirror.hoj.judge.codesandbox.model.ExecuteCodeResponse;
import com.mirror.hoj.judge.codesandbox.model.JudgeInfo;
import com.mirror.hoj.model.enums.QuestionSubmitStatusEnum;

import java.util.List;

/**
 * 示例代码沙箱（仅为了跑通业务流程）
 *
 * @author Mirror
 * @date 2024/7/23
 */
public class ExampleCodeSandbox implements CodeSandbox {
    // 静态变量保存类的唯一实例
    private static volatile ExampleCodeSandbox instance;

    // 私有构造函数防止外部实例化
    private ExampleCodeSandbox() {
    }

    /**
     * 单例模式，双重检查锁定
     * 为了在确保线程安全的同时，尽可能减少同步的开销，提高程序的性能。
     * @return
     */
    public static ExampleCodeSandbox getInstance() {
        if (instance == null) {
            synchronized (ExampleCodeSandbox.class) {
                if (instance == null) {
                    //加上volatile关键字，保证instance在所有线程中同步
                    instance = new ExampleCodeSandbox();
                }
            }
        }
        return instance;
    }


    /**
     * 示例代码沙箱（仅为了跑通业务流程）
     */
    @Override
    public ExecuteCodeResponse executeCode(ExecuteCodeRequest executeCodeRequest) {
        List<String> inputList = executeCodeRequest.getInputList();
        ExecuteCodeResponse executeCodeResponse = new ExecuteCodeResponse();
        executeCodeResponse.setOutputList(inputList);
        executeCodeResponse.setMessage("测试执行成功");
        executeCodeResponse.setStatus(QuestionSubmitStatusEnum.SUCCEED.getValue());
        JudgeInfo judgeInfo = new JudgeInfo();
//        judgeInfo.setMessage(JudgeInfoMessageEnum.ACCEPTED.getText());
//        judgeInfo.setMemory(100L);
//        judgeInfo.setTime(100L);
//        executeCodeResponse.setJudgeInfo(judgeInfo);
        return executeCodeResponse;
    }

}
