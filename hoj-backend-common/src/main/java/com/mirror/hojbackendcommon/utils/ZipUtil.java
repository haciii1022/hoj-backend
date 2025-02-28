package com.mirror.hojbackendcommon.utils;

import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipFile;
import org.springframework.core.io.Resource;

import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * 文件压缩包工具类
 *
 * @author Mirror.
 * @date 2025/2/16
 */
public final class ZipUtil {
    /**
     * 解压ZIP文件，并对其中后缀为 .in 和 .out 的文件进行分类、排序处理。
     *
     * @param zipFilePath ZIP文件路径
     * @param destDir     解压目标目录
     * @throws Exception  当ZIP文件不存在或没有符合要求的文件时抛出异常
     */
    public static void extractAndProcessZip(String zipFilePath, String destDir, List<File>inFiles, List<File> outFiles) throws Exception {
        File zipFile = new File(zipFilePath);
        if (!zipFile.exists()) {
            //不存在的话，从文件服务器下载备份到本地
            throw new IOException("文件不存在");
        }

        try (ZipFile zf = new ZipFile(zipFile)) {
            Enumeration<ZipArchiveEntry> entries = zf.getEntries();
            boolean hasValidFiles = false; // 标记是否有符合要求的文件
            while (entries.hasMoreElements()) {
                ZipArchiveEntry entry = entries.nextElement();
                if (entry.isDirectory()) {
                    continue;
                }

                // 保持原有文件名和后缀
                String fileName = entry.getName();
                String fileExtension = getFileExtension(fileName);

                // 只处理 .in 和 .out 文件
                if (!".in".equalsIgnoreCase(fileExtension) && !".out".equalsIgnoreCase(fileExtension)) {
                    continue;
                }
                hasValidFiles = true;

                // 构造解压后的文件对象，并确保其所在目录存在
                File outFile = new File(destDir, fileName);
                outFile.getParentFile().mkdirs();

                // 写入解压后的文件
                try (InputStream is = zf.getInputStream(entry);
                     OutputStream os = Files.newOutputStream(outFile.toPath())) {
                    byte[] buffer = new byte[1024];
                    int len;
                    while ((len = is.read(buffer)) != -1) {
                        os.write(buffer, 0, len);
                    }
                }

                // 根据文件后缀归类
                if (".in".equalsIgnoreCase(fileExtension)) {
                    inFiles.add(outFile);
                } else if (".out".equalsIgnoreCase(fileExtension)) {
                    outFiles.add(outFile);
                }
            }

            // 如果没有符合要求的文件，则抛出异常
            if (!hasValidFiles) {
                throw new Exception("ZIP文件为空或没有符合要求的文件 (.in, .out) 可解压");
            }

            // 按文件名排序
            Comparator<File> fileNameComparator = Comparator.comparing(File::getName);
            inFiles.sort(fileNameComparator);
            outFiles.sort(fileNameComparator);

            // 打印结果或后续处理（这里仅打印文件名）
            inFiles.forEach(file -> System.out.println(file.getName()));

            outFiles.forEach(file -> System.out.println(file.getName()));
        }
    }

    /**
     * 获取文件的后缀（包含点号），如 ".in" 或 ".out"。
     *
     * @param fileName 文件名
     * @return 文件后缀，如果没有则返回空字符串
     */
    private static String getFileExtension(String fileName) {
        int index = fileName.lastIndexOf('.');
        return (index >= 0) ? fileName.substring(index) : "";
    }
    public static void main(String[] args) {
        try {
            String zipFilePath = "/home/ubuntu/hoj-backend/2 A+B Problem.zip";
            String destDir = "/home/ubuntu/hoj-backend/test";

            List<File> ins = new ArrayList<>();
            List<File> outs = new ArrayList<>();
            extractAndProcessZip(zipFilePath, destDir, ins, outs);
//            deleteLocalDirectory(destDir);
            System.out.println("ZIP文件解压并处理完成！");
        }  catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
