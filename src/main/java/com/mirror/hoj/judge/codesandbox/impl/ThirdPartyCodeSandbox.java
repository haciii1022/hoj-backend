package com.mirror.hoj.judge.codesandbox.impl;

import com.mirror.hoj.judge.codesandbox.CodeSandbox;
import com.mirror.hoj.judge.codesandbox.model.ExecuteCodeRequest;
import com.mirror.hoj.judge.codesandbox.model.ExecuteCodeResponse;

/**
 * 第三方代码沙箱（非自定义开发，为现成的沙箱）
 * 适配器模式
 *
 * @author Mirror
 * @date 2024/7/23
 */
public class ThirdPartyCodeSandbox implements CodeSandbox {
    // 静态变量保存类的唯一实例
    private static ThirdPartyCodeSandbox instance;

    // 私有构造函数防止外部实例化
    private ThirdPartyCodeSandbox() {
    }

    // 提供一个静态方法来获取实例
    public static ThirdPartyCodeSandbox getInstance() {
        if (instance == null) {
            synchronized (ThirdPartyCodeSandbox.class) {
                if (instance == null) {
                    instance = new ThirdPartyCodeSandbox();
                }
            }
        }
        return instance;
    }

    @Override
    public ExecuteCodeResponse executeCode(ExecuteCodeRequest executeCodeRequest) {
        System.out.println("第三方代码沙箱");
        return new ExecuteCodeResponse();
    }
}
