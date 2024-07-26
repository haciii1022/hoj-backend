package com.mirror.hoj.judge.codesandbox;

import com.mirror.hoj.judge.codesandbox.model.ExecuteCodeRequest;
import com.mirror.hoj.judge.codesandbox.model.ExecuteCodeResponse;

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
