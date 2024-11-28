//package com.mirror.hojbackendquestionservice.config;
//
//
//import com.jcraft.jsch.ChannelSftp;
//import com.jcraft.jsch.JSch;
//import com.jcraft.jsch.Session;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//
//import java.util.Properties;
//
///**
// * sftp文件传输配置器
// *
// * @author Mirror
// * @date 2024/11/27
// */
//@Configuration
//@Slf4j
//public class SftpConfig {
//
//    @Value("${sftp.remoteHost}")
//    private String remoteHost;
//
//    @Value("${sftp.port}")
//    private Integer port;
//
//    @Value("${sftp.username}")
//    private String username;
//
//    @Value("${sftp.password}")
//    private String password;
//
//    @Bean
//    public Session sftpSession() throws Exception {
//        // 创建 JSch 对象
//        JSch jsch = new JSch();
//        // 创建会话
//        Session session = jsch.getSession(username, remoteHost, port);
//        session.setPassword(password);
//        // 配置 SSH
//        Properties config = new Properties();
//        config.put("StrictHostKeyChecking", "no");  // 忽略 HostKey 校验
//        session.setConfig(config);
//        // 建立连接
//        session.connect();
//        log.info("SSH 会话已连接");
//        return session;
//    }
//
//    @Bean
//    public ChannelSftp channelSftp(Session sftpSession) throws Exception {
//        ChannelSftp channelSftp = (ChannelSftp) sftpSession.openChannel("sftp");
//        channelSftp.connect();
//        log.info("SFTP 已链接");
//        return channelSftp;
//    }
//}
//
