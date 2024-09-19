package com.mirror.hojbackendjudgeservice.judge.codesandbox;


import com.mirror.hojbackendmodel.model.codesandbox.ExecuteCodeRequest;
import com.mirror.hojbackendmodel.model.codesandbox.ExecuteCodeResponse;

/**
 * @author Mirror
 * @date 2024/7/23
 */
public interface CodeSandbox {
    /**
     * 执行代码
     * @param executeCodeRequest
     * @return
     */
    ExecuteCodeResponse executeCode(ExecuteCodeRequest executeCodeRequest);
}
