package com.mirror.hojbackendjudgeservice.judge.codesandbox.impl;


import com.mirror.hojbackendjudgeservice.judge.codesandbox.CodeSandbox;
import com.mirror.hojbackendmodel.model.codesandbox.ExecuteCodeRequest;
import com.mirror.hojbackendmodel.model.codesandbox.ExecuteCodeResponse;

/**
 * 第三方代码沙箱（非自定义开发，为现成的沙箱）
 * 适配器模式
 *
 * @author Mirror
 * @date 2024/7/23
 */
public class ThirdPartyCodeSandbox implements CodeSandbox {
    // 静态变量保存类的唯一实例
    private static volatile ThirdPartyCodeSandbox instance;

    // 私有构造函数防止外部实例化
    private ThirdPartyCodeSandbox() {
    }

    /**
     * 单例模式，双重检查锁定
     * 为了在确保线程安全的同时，尽可能减少同步的开销，提高程序的性能。
     * @return
     */
    public static ThirdPartyCodeSandbox getInstance() {
        if (instance == null) {
            synchronized (ThirdPartyCodeSandbox.class) {
                if (instance == null) {
                    //加上volatile关键字，保证instance在所有线程中同步
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
