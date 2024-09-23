package com.mirror.hojbackendjudgeservice;

import com.mirror.hojbackendjudgeservice.rabbitmq.InitRabbitMq;
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
        //初始化消息队列
        InitRabbitMq.init();
        SpringApplication.run(HojBackendJudgeServiceApplication.class, args);
    }

}
