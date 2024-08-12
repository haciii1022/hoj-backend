package com.mirror.hoj.judge.codesandbox;

import com.mirror.hoj.judge.codesandbox.impl.ExampleCodeSandbox;
import com.mirror.hoj.judge.codesandbox.impl.RemoteCodeSandbox;
import com.mirror.hoj.judge.codesandbox.impl.ThirdPartyCodeSandbox;

/**
 * 静态工厂模式获取代码沙箱
 * @author Mirror
 * @date 2024/7/26
 */
public class CodeSandboxFactory {
    /**
     * 获取代码沙箱
     * @param type
     * @return
     */
    public static CodeSandbox getCodeSandbox(String type) {
        //TODO 后续可以改成常量类
        //单例模式
        switch (type) {
            case CodeSandboxConstant.REMOTE:
                return  RemoteCodeSandbox.getInstance();
            case CodeSandboxConstant.THIRD_PARTY:
                return  ThirdPartyCodeSandbox.getInstance();
            default:
                return ExampleCodeSandbox.getInstance();
        }
    }
}
