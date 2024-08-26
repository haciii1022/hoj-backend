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
    private static volatile RemoteCodeSandbox instance;

    // 私有构造函数防止外部实例化
    private RemoteCodeSandbox() {
    }

    /**
     * 单例模式，双重检查锁定
     * 为了在确保线程安全的同时，尽可能减少同步的开销，提高程序的性能。
     * @return
     */
    public static RemoteCodeSandbox getInstance() {
        //先过滤掉不需要的请求，因为同步块的开销比较大
        if (instance == null) {
            synchronized (RemoteCodeSandbox.class) {
                if (instance == null) {
                    //加上volatile关键字，保证instance在所有线程中同步
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
