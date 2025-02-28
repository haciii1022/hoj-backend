package com.mirror.hojbackendcommon.utils;

import cn.hutool.extra.spring.SpringUtil;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.SftpATTRS;
import com.jcraft.jsch.SftpException;
import com.mirror.hojbackendcommon.client.SftpClient;
import com.mirror.hojbackendcommon.common.ErrorCode;
import com.mirror.hojbackendcommon.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.util.DigestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;;

/**
 * 文件工具类
 *
 * @author Mirror.
 * @date 2024/11/27
 */
@Slf4j
public final class FileUtil {

    private FileUtil(){}

    private static ChannelSftp channelSftp;

    private static final SftpClient sftpClient;

    private static final int PERMISSIONS_755 = 0755;

    static {
        sftpClient = SpringUtil.getBean(SftpClient.class);
    }

    /**
     * 通过 SFTP 上传文件到远程服务器
     *
     * @param originalFile 需要上传的文件
     * @param fullFilePath 远程服务器上文件保存的完整路径
     */
    public static void saveFileViaSFTP(MultipartFile originalFile, String fullFilePath) throws Exception {
        channelSftp = sftpClient.getChannelSftp();
        // 获取文件所在的目录
        String folderPath = Paths.get(fullFilePath).getParent().toString();
        ensureDirectoryExists(folderPath);
        try (InputStream inputStream = originalFile.getInputStream()) {
            // 上传文件到远程服务器的指定路径
            log.info("保存文件：{}", fullFilePath);
            channelSftp.put(inputStream, fullFilePath);
            setFilePermissions(fullFilePath, PERMISSIONS_755);
        }catch (Exception e){
            log.error("保存文件失败,上传路径：{}",fullFilePath,e);
        }
    }
    public static void saveFileViaSFTP(File file, String fullFilePath) throws Exception {
        channelSftp = sftpClient.getChannelSftp();
        // 获取文件所在的目录
        String folderPath = Paths.get(fullFilePath).getParent().toString();
        ensureDirectoryExists(folderPath);
        try (InputStream inputStream = Files.newInputStream(file.toPath())) {
            // 上传文件到远程服务器的指定路径
            log.info("保存文件：{}", fullFilePath);
            channelSftp.put(inputStream, fullFilePath);
            setFilePermissions(fullFilePath, PERMISSIONS_755);
        } catch (Exception e) {
            log.error("保存文件失败,上传路径：{}", fullFilePath, e);
            throw e;
        }
    }

    /**
     * 将 MultipartFile 保存到本地指定路径（包括文件名）
     *
     * @param file      MultipartFile 文件对象
     * @param filePath  完整的文件路径（包含目录和文件名）
     * @throws Exception 当保存文件过程中出现异常时抛出
     */
    public static void saveFileToLocal(MultipartFile file, String filePath) throws Exception {
        // 获取文件所在的目录路径
        File destinationFile = new File(filePath);
        File parentDir = destinationFile.getParentFile();

        // 确保目标目录存在，如果不存在则创建目录
        if (parentDir != null && !parentDir.exists()) {
            boolean dirsCreated = parentDir.mkdirs();  // 创建目录及其父目录
            if (dirsCreated) {
                log.info("创建目录: {}", parentDir.getAbsolutePath());
            } else {
                throw new IOException("无法创建目录: " + parentDir.getAbsolutePath());
            }
        }

        // 将 MultipartFile 写入目标文件路径
        try (InputStream inputStream = file.getInputStream();
             FileOutputStream fos = new FileOutputStream(destinationFile)) {
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                fos.write(buffer, 0, bytesRead);
            }
            fos.flush();
            log.info("文件 {} 已成功保存到 {}", destinationFile.getName(), destinationFile.getAbsolutePath());
        } catch (IOException e) {
            log.error("保存文件时发生错误: ", e);
            throw new IOException("保存文件失败", e);
        }
    }

    /**
     * 删除远程服务器上的文件
     *
     * @param remoteFilePath 需要删除的远程文件的完整路径
     * @throws SftpException 如果删除文件失败或文件不存在
     */
    public static void deleteFileViaSFTP(String remoteFilePath) throws SftpException {
        channelSftp = sftpClient.getChannelSftp();
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
     * 删除指定的文件或目录及其内容
     * 如果传入的是文件路径，直接删除文件
     * 如果传入的是目录路径，递归删除目录及其下的所有文件和子目录
     *
     * @param path 待删除的文件或目录路径
     * @throws Exception 当删除过程中出现异常时抛出异常
     */
    public static void deleteLocalPath(String path) throws Exception {
        File file = new File(path);

        if (!file.exists()) {
            return;  // 如果文件或目录不存在，则直接返回
        }

        if (file.isDirectory()) {
            // 如果是目录，则递归删除目录及其下的所有文件和子目录
            Path dirPath = file.toPath();
            Files.walkFileTree(dirPath, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws java.io.IOException {
                    Files.delete(file);  // 删除文件
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path dir, java.io.IOException exc) throws java.io.IOException {
                    Files.delete(dir);  // 删除目录
                    return FileVisitResult.CONTINUE;
                }
            });
            log.info("{} 目录下的文件及子目录删除成功", path);
        } else if (file.isFile()) {
            // 如果是文件，则直接删除文件
            boolean deleted = file.delete();
            if (deleted) {
                log.info("文件 {} 删除成功", path);
            } else {
                throw new IOException("删除文件失败: " + path);
            }
        } else {
            throw new IOException("无法处理路径: " + path);
        }
    }
    /**
     * 通过 SFTP 下载文件并返回 ResponseEntity<byte[]> 以供浏览器下载
     *
     * @param remoteFilePath 远程文件的完整路径
     * @return ResponseEntity<byte[]> 包含文件的字节流
     */
    public static Resource downloadFileViaSFTP(String remoteFilePath) {
        channelSftp = sftpClient.getChannelSftp();
        // 从远程路径获取文件流
        InputStream inputStream = null;
        log.info("下载文件：{}", remoteFilePath);
        try {
            inputStream = channelSftp.get(remoteFilePath);
        } catch (SftpException e) {
            log.error("文件下载失败，远程文件路径：{}", remoteFilePath, e);
            throw new BusinessException(ErrorCode.DOWNLOAD_FILE_ERROR);
        }
        // 使用 InputStreamResource 将文件流包装成 Resource 对象
        return new InputStreamResource(inputStream);
    }

    /**
     * 从文件服务器中下载已存在的文件到本地
     * @param remoteFilePath
     * @param localFile
     * @throws IOException
     */
    public static void downloadFileToExistingFileViaSFTP(String remoteFilePath, File localFile) throws IOException {
        channelSftp = sftpClient.getChannelSftp();
        InputStream inputStream = null;
        log.info("下载文件：{}", remoteFilePath);

        try {
            // 获取远程文件的输入流
            inputStream = channelSftp.get(remoteFilePath);
        } catch (SftpException e) {
            log.error("文件下载失败，远程文件路径：{}", remoteFilePath, e);
            throw new BusinessException(ErrorCode.DOWNLOAD_FILE_ERROR);
        }

        // 将输入流的内容写入到本地已存在的文件中
        try (OutputStream outputStream = Files.newOutputStream(localFile.toPath())) {
            byte[] buffer = new byte[4096];  // 4KB 缓冲区
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
        } catch (IOException e) {
            log.error("将文件写入本地文件时出错：{}", localFile.getAbsolutePath(), e);
            throw new IOException("文件保存失败", e);
        }
    }
    /**
     * 确保远程服务器的目录存在
     * 如果目录不存在，则逐级创建
     */
    private static void ensureDirectoryExists(String remoteDir) throws SftpException {
        channelSftp = sftpClient.getChannelSftp();
        String[] folders = remoteDir.split("/");
        StringBuilder pathBuilder = new StringBuilder("/");

        for (String folder : folders) {
            if (folder.isEmpty()) continue;  // 忽略空字符串（路径开始部分的斜杠）
            pathBuilder.append(folder).append("/");
            String currentPath = pathBuilder.toString();

            if (!isDirectoryExist(currentPath)) {
                channelSftp.mkdir(currentPath); // 创建目录
                setFilePermissions(currentPath, PERMISSIONS_755);
            }
        }
    }

    /**
     * 判断远程目录是否存在
     */
    public static boolean isDirectoryExist(String remoteDir) {
        channelSftp = sftpClient.getChannelSftp();
        try {
            SftpATTRS attrs = channelSftp.stat(remoteDir);
            return attrs.isDir();  // 如果是目录，返回 true
        } catch (SftpException e) {
            return false;  // 如果发生异常，目录不存在
        }
    }

    /**
     * 比对两个文件的内容是否一致，忽略最后一行的换行符（\n 或 \r\n）
     *
     * @param filePath1 第一个文件的完整路径
     * @param filePath2 第二个文件的完整路径
     * @return true 如果文件内容一致（忽略最后一行的换行符），否则 false
     */
    public static boolean compareFilesIgnoringLastLineEnding(String filePath1, String filePath2) {
        channelSftp = sftpClient.getChannelSftp();
        try (InputStream inputStream1 = channelSftp.get(filePath1);
             InputStream inputStream2 = channelSftp.get(filePath2)) {

            // 将输入流读取为去掉最后一行换行符后的字节数组
            byte[] file1Bytes = readStreamIgnoringLastLineEnding(inputStream1);
            byte[] file2Bytes = readStreamIgnoringLastLineEnding(inputStream2);

            // 比较两个文件的字节数组是否相等
            return DigestUtils.md5DigestAsHex(file1Bytes).equals(DigestUtils.md5DigestAsHex(file2Bytes));

        } catch (Exception e) {
            log.error("文件比对失败，文件路径：{} 和 {}", filePath1, filePath2, e);
            return false;
        }
    }

    /**
     * 读取输入流，并忽略最后一行的换行符（\n 或 \r\n）
     *
     * @param inputStream 文件输入流
     * @return 去掉最后一行换行符的字节数组
     */
    private static byte[] readStreamIgnoringLastLineEnding(InputStream inputStream) throws Exception {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int bytesRead;

        while ((bytesRead = inputStream.read(buffer)) != -1) {
            outputStream.write(buffer, 0, bytesRead);
        }

        // 转换为字符串并去除末尾的换行符
        String fileContent = outputStream.toString("UTF-8").replaceFirst("(\\r?\\n)+$", "");

        // 将处理后的字符串转换回字节数组
        return fileContent.getBytes("UTF-8");
    }
    /**
     * 设置远程文件或目录的权限
     *
     * @param remotePath 远程文件或目录的路径
     * @param permissions 文件权限（例如：0755）
     */
    private static void setFilePermissions(String remotePath, int permissions) throws SftpException {
        String chmodCommand = "chmod " + String.format("%04o", permissions) + " " + remotePath;
        channelSftp.chmod(permissions, remotePath);
        log.info("设置权限 {} 给文件 {}", String.format("%04o", permissions), remotePath);
    }
}
