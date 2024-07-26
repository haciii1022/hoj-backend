package com.mirror.hoj.judge.codesandbox.impl;

import com.mirror.hoj.judge.codesandbox.CodeSandbox;
import com.mirror.hoj.judge.codesandbox.model.ExecuteCodeRequest;
import com.mirror.hoj.judge.codesandbox.model.ExecuteCodeResponse;

/**
 * 远程代码沙箱（实际调用接口的沙箱）
 *
 * @author Mirror
 * @date 2024/7/23
 */
public class RemoteCodeSandbox implements CodeSandbox {
    // 静态变量保存类的唯一实例
    private static RemoteCodeSandbox instance;

    // 私有构造函数防止外部实例化
    private RemoteCodeSandbox() {
    }

    // 提供一个静态方法来获取实例
    public static RemoteCodeSandbox getInstance() {
        if (instance == null) {
            synchronized (RemoteCodeSandbox.class) {
                if (instance == null) {
                    instance = new RemoteCodeSandbox();
                }
            }
        }
        return instance;
    }

    @Override
    public ExecuteCodeResponse executeCode(ExecuteCodeRequest executeCodeRequest) {
        System.out.println("远程代码沙箱");
        return new ExecuteCodeResponse();
    }
}
