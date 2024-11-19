package com.mirror.hojbackendcommon.utils;

import cn.hutool.core.lang.UUID;
import cn.hutool.extra.spring.SpringUtil;
import com.aliyun.oss.OSS;
import com.aliyun.oss.model.PutObjectResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Optional;

/**
 * @author Mirror
 * @date 2024/11/19
 */
@Slf4j
public final class OssUtil {
    private OssUtil() {
    }

    private static final String bucketName;

    private static final String endpoint;

    private static final OSS ossClient;

    static {
        ossClient = SpringUtil.getBean(OSS.class);
        Environment environment = SpringUtil.getBean(Environment.class);
        bucketName = environment.getProperty("aliyun.oss.bucketName");
        endpoint = environment.getProperty("aliyun.oss.endpoint");
    }

    public static String uploadFile(MultipartFile file, String originalFilename) {
        //获取原生文件名
        String fileName = file.getOriginalFilename();
        // 拼装OSS上存储的路径
        // String fileName = file.getName();
        String extension = fileName.substring(fileName.lastIndexOf("."));
        String contentType = file.getContentType();
        //在OSS上bucket下的文件名
        String finalFileName = Optional.ofNullable(originalFilename).orElse(java.lang.String.valueOf(UUID.fastUUID()));
        String uploadFileName = "hoj/user" + "/" + finalFileName + extension;
        //获取文件后缀
        try {
            PutObjectResult result = ossClient.putObject(bucketName, uploadFileName, file.getInputStream());
            //拼装返回路径
            if (result != null) {
                return "https://" + bucketName + "." + endpoint + "/" + uploadFileName;
            }
        }
        catch (IOException e) {
            log.error("文件上传失败:{}", e.getMessage());
        }
        return "fail";
    }
}
