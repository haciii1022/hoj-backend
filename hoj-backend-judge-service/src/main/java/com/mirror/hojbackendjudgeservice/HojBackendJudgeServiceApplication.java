package com.mirror.hojbackendjudgeservice;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class})
@EnableScheduling
@EnableAspectJAutoProxy(proxyTargetClass = true, exposeProxy = true)
@ComponentScan("com.mirror")
@EnableDiscoveryClient
@EnableFeignClients(basePackages = {"com.mirror.hojbackendserverclient.service"})
public class HojBackendJudgeServiceApplication {

    public static void main(String[] args) {
        // 加载 .env 文件
        Dotenv dotenv = Dotenv.load();
        // 手动将 dotenv 中的环境变量添加到系统环境变量
        dotenv.entries().forEach(entry -> {
            System.setProperty(entry.getKey(), entry.getValue());
        });
        //初始化消息队列
        SpringApplication.run(HojBackendJudgeServiceApplication.class, args);
    }

}
