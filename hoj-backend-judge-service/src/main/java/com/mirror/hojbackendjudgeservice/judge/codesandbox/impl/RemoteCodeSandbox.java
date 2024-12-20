package com.mirror.hojbackendjudgeservice.judge.codesandbox.impl;

import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONUtil;
import com.mirror.hojbackendcommon.common.ErrorCode;
import com.mirror.hojbackendcommon.exception.BusinessException;
import com.mirror.hojbackendjudgeservice.judge.codesandbox.CodeSandbox;
import com.mirror.hojbackendmodel.model.codesandbox.ExecuteCodeRequest;
import com.mirror.hojbackendmodel.model.codesandbox.ExecuteCodeResponse;
import org.apache.commons.lang3.StringUtils;

/**
 * 远程代码沙箱（实际调用接口的沙箱）
 *
 * @author Mirror
 * @date 2024/7/23
 */
public class RemoteCodeSandbox implements CodeSandbox {
    private static final String AUTH_REQUEST_HEADER = "auth";

    private static final String AUTH_REQUEST_SECRET = "secretKey";
    // 静态变量保存类的唯一实例
    private static volatile RemoteCodeSandbox instance;

    // 私有构造函数防止外部实例化
    private RemoteCodeSandbox() {
    }

    /**
     * 单例模式，双重检查锁定
     * 为了在确保线程安全的同时，尽可能减少同步的开销，提高程序的性能。
     *
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
//        String url = "http://47.115.53.171:8090/executeCode";
        String url = "http://192.168.1.210:8090/executeCode";
        System.out.println("url: " + url);
        String json = JSONUtil.toJsonStr(executeCodeRequest);
        // 设置超时时间（单位：毫秒）
        long timeout = executeCodeRequest.getTimeLimit() + 20000;

        // 捕获 HTTP 请求异常
        // 捕获超时异常
        try {
            // 发起 HTTP POST 请求
            String responseStr = HttpUtil.createPost(url)
                    .header(AUTH_REQUEST_HEADER, AUTH_REQUEST_SECRET)
                    .body(json)
                    .timeout(Math.toIntExact(timeout)) // 设置超时时间
                    .execute()
                    .body();

            // 判断响应是否为空
            if (StringUtils.isBlank(responseStr)) {
                throw new BusinessException(ErrorCode.API_REQUEST_ERROR, "远程代码沙箱请求失败，响应为空");
            }

            // 将响应转换为目标对象
            return JSONUtil.toBean(responseStr, ExecuteCodeResponse.class);

        } catch (RuntimeException e) {
            // 捕获其他异常
            throw new BusinessException(ErrorCode.API_REQUEST_ERROR, "远程代码沙箱请求异常: " + e.getMessage());
        }
    }
}
