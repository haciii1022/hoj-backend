package com.mirror.hojbackendquestionservice;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication
@MapperScan("com.mirror.hojbackendquestionservice.mapper")
@EnableScheduling
@EnableTransactionManagement
@EnableAspectJAutoProxy(proxyTargetClass = true, exposeProxy = true)
@ComponentScan("com.mirror")
@EnableDiscoveryClient
@EnableFeignClients(basePackages = {"com.mirror.hojbackendserverclient.service"})
public class HojBackendQuestionServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(HojBackendQuestionServiceApplication.class, args);
    }

}
