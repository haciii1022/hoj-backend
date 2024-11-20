package com.mirror.hojbackendcommon.utils;

import cn.hutool.core.lang.UUID;
import cn.hutool.extra.spring.SpringUtil;
import com.aliyun.oss.OSS;
import com.aliyun.oss.model.PutObjectResult;
import com.mirror.hojbackendcommon.constant.FileConstant;
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

    /**
     * 上传文件
     * @param file
     * @param originalFilename
     * @return
     */
    public static String uploadFile(MultipartFile file, String originalFilename, String pathPrefix) {
        //获取原生文件名
        String fileName = file.getOriginalFilename();
        // 拼装OSS上存储的路径
        String extension = fileName.substring(fileName.lastIndexOf("."));
        //在OSS上bucket下的文件名
        String finalFileName = Optional.ofNullable(originalFilename).orElse(String.valueOf(UUID.fastUUID()));
        String uploadFileName = pathPrefix + "/" + finalFileName + extension;
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
        return FileConstant.UPLOAD_FAIL;
    }

    /**
     * 删除文件
     * @param fileUrl
     * @return
     */
    public static Boolean deleteFile(String fileUrl) {
        boolean flag = true;
        try {
            ossClient.deleteObject(bucketName, fileUrl);
        }catch (Exception e){
            log.error("文件删除失败:{}",e.getMessage());
            flag = false;
        }
        return flag;
    }
}
