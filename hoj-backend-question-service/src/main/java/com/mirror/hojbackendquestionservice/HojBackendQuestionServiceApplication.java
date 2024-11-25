package com.mirror.hojbackendquestionservice;

import io.github.cdimascio.dotenv.Dotenv;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@MapperScan("com.mirror.hojbackendquestionservice.mapper")
@EnableScheduling
@EnableAspectJAutoProxy(proxyTargetClass = true, exposeProxy = true)
@ComponentScan("com.mirror")
@EnableDiscoveryClient
@EnableFeignClients(basePackages = {"com.mirror.hojbackendserverclient.service"})
public class HojBackendQuestionServiceApplication {

    public static void main(String[] args) {
        // 加载 .env 文件
        Dotenv dotenv = Dotenv.load();
        // 手动将 dotenv 中的环境变量添加到系统环境变量
        dotenv.entries().forEach(entry -> {
            System.setProperty(entry.getKey(), entry.getValue());
        });
        SpringApplication.run(HojBackendQuestionServiceApplication.class, args);
    }

}
