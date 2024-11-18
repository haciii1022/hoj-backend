package com.mirror.hojbackenduserservice.config;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 阿里云对象存储客户端
 * @author Mirror.
 */
@Configuration
@ConfigurationProperties(prefix = "cos.client")
@Data
public class OssClientConfig {

    @Value("${aliyun.oss.endpoint}")
    private String endpoint;

    @Value("${aliyun.oss.accessKeyId}")
    private String accessKeyId;

    @Value("${aliyun.oss.accessKeySecret}")
    private String accessKeySecret;

    @Value("${aliyun.oss.bucketName}")
    private String bucketName;

    @Bean
    public OSS ossClient() {
        OSS oss = new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);
        return  oss;
    }
}