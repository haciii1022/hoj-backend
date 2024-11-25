package com.mirror.hojbackendgateway;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class})
@EnableDiscoveryClient
public class HojBackendGatewayApplication {

    public static void main(String[] args) {
        // 加载 .env 文件
        Dotenv dotenv = Dotenv.load();
        // 手动将 dotenv 中的环境变量添加到系统环境变量
        dotenv.entries().forEach(entry -> {
            System.setProperty(entry.getKey(), entry.getValue());
        });
        SpringApplication.run(HojBackendGatewayApplication.class, args);
    }

}
