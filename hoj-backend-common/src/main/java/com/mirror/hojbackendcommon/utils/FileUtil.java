package com.mirror.hojbackendcommon.utils;

import cn.hutool.extra.spring.SpringUtil;;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.SftpATTRS;
import com.jcraft.jsch.SftpException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.DigestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;;

/**
 * 文件工具类
 *
 * @author Mirror.
 * @date 2024/11/27
 */
@Slf4j
public final class FileUtil {

    private FileUtil(){}

    private static final ChannelSftp channelSftp;

    static {
        channelSftp = SpringUtil.getBean(ChannelSftp.class);
    }

    /**
     * 通过 SFTP 上传文件到远程服务器
     *
     * @param originalFile 需要上传的文件
     * @param fullFilePath 远程服务器上文件保存的完整路径
     */
    public static void saveFileViaSFTP(MultipartFile originalFile, String fullFilePath) throws Exception {
        // 获取文件所在的目录
        String folderPath = Paths.get(fullFilePath).getParent().toString();
        ensureDirectoryExists(folderPath);
        try (InputStream inputStream = originalFile.getInputStream()) {
            // 上传文件到远程服务器的指定路径
            log.info("保存文件：{}", fullFilePath);
            channelSftp.put(inputStream, fullFilePath);
        }
    }

    /**
     * 删除远程服务器上的文件
     *
     * @param remoteFilePath 需要删除的远程文件的完整路径
     * @throws SftpException 如果删除文件失败或文件不存在
     */
    public static void deleteFileViaSFTP(String remoteFilePath) throws SftpException {
        try {
            // 检查文件是否存在
            SftpATTRS attrs = channelSftp.stat(remoteFilePath);
            if (attrs != null) {
                channelSftp.rm(remoteFilePath); // 删除文件
                log.info("文件已删除：{}", remoteFilePath);
            }
        } catch (SftpException e) {
            // 如果文件不存在或无法访问，则抛出异常
            if (e.id == ChannelSftp.SSH_FX_NO_SUCH_FILE) {
                log.info("文件不存在：{}", remoteFilePath);
            } else {
                throw e;  // 重新抛出其他异常
            }
        }
    }

    /**
     * 通过 SFTP 下载文件并返回 ResponseEntity<byte[]> 以供浏览器下载
     *
     * @param remoteFilePath 远程文件的完整路径
     * @return ResponseEntity<byte[]> 包含文件的字节流
     */
    public static Resource downloadFileViaSFTP(String remoteFilePath) {
        // 从远程路径获取文件流
        InputStream inputStream = null;
        log.info("下载文件：{}", remoteFilePath);
        try {
            inputStream = channelSftp.get(remoteFilePath);
        } catch (SftpException e) {
            throw new RuntimeException(e);
        }

        // 使用 InputStreamResource 将文件流包装成 Resource 对象
        return new InputStreamResource(inputStream);
//        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
//        try (InputStream inputStream = channelSftp.get(remoteFilePath)) {
//            byte[] buffer = new byte[1024];
//            int bytesRead;
//            while ((bytesRead = inputStream.read(buffer)) != -1) {
//                outputStream.write(buffer, 0, bytesRead);
//            }
//
//            // 设置文件下载的响应头
//            HttpHeaders headers = new HttpHeaders();
//            headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + Paths.get(remoteFilePath).getFileName().toString() + "\"");
//            headers.add(HttpHeaders.CONTENT_TYPE, "application/octet-stream");
//
//            return new ResponseEntity<>(outputStream.toByteArray(), headers, HttpStatus.OK);
//
//        } catch (SftpException e) {
//            log.error("文件下载失败，远程文件路径：{}", remoteFilePath, e);
//            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
//        } catch (Exception e) {
//            log.error("文件下载过程中发生错误：{}", remoteFilePath, e);
//            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
//        }
    }
    /**
     * 确保远程服务器的目录存在
     * 如果目录不存在，则逐级创建
     */
    private static void ensureDirectoryExists(String remoteDir) throws SftpException {
        String[] folders = remoteDir.split("/");
        StringBuilder pathBuilder = new StringBuilder("/");

        for (String folder : folders) {
            if (folder.isEmpty()) continue;  // 忽略空字符串（路径开始部分的斜杠）
            pathBuilder.append(folder).append("/");
            String currentPath = pathBuilder.toString();

            if (!isDirectoryExist(currentPath)) {
                channelSftp.mkdir(currentPath); // 创建目录
            }
        }
    }

    /**
     * 判断远程目录是否存在
     */
    public static boolean isDirectoryExist(String remoteDir) {
        try {
            SftpATTRS attrs = channelSftp.stat(remoteDir);
            return attrs.isDir();  // 如果是目录，返回 true
        } catch (SftpException e) {
            return false;  // 如果发生异常，目录不存在
        }
    }
}
