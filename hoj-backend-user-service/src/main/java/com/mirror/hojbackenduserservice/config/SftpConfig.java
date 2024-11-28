//package com.mirror.hojbackenduserservice.config;
//
//import com.jcraft.jsch.*;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.stereotype.Component;
//
//import java.util.Properties;
//
///**
// * SFTP 文件传输配置器
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
//    private Session session;
//    private ChannelSftp channelSftp;
//
//    @Bean
//    public ChannelSftp channelSftp() {
//        ensureConnected();  // 在获取Bean时保证连接
//        return channelSftp;
//    }
//
//    private void ensureConnected() {
//        try {
//            if (session == null || !session.isConnected()) {
//                connectSession();
//            }
//
//            if (channelSftp == null || !channelSftp.isConnected()) {
//                connectChannel();
//            }
//        } catch (Exception e) {
//            log.error("SFTP 连接建立失败", e);
//            throw new RuntimeException("SFTP 连接失败", e);
//        }
//    }
//
//    private void connectSession() throws Exception {
//        JSch jsch = new JSch();
//        session = jsch.getSession(username, remoteHost, port);
//        session.setPassword(password);
//        Properties config = new Properties();
//        config.put("StrictHostKeyChecking", "no");
//        config.put("ServerAliveInterval", "60");  // 心跳包，60秒
//        session.setConfig(config);
//        session.connect();
//        log.info("SSH 会话已连接");
//    }
//
//    private void connectChannel() throws Exception {
//        if (session == null || !session.isConnected()) {
//            throw new RuntimeException("SSH 会话未连接，无法创建 SFTP 通道");
//        }
//        channelSftp = (ChannelSftp) session.openChannel("sftp");
//        channelSftp.connect();
//        log.info("SFTP 通道已连接");
//    }
//
//    public void disconnect() {
//        if (channelSftp != null && channelSftp.isConnected()) {
//            channelSftp.disconnect();
//            log.info("SFTP 通道已断开");
//        }
//        if (session != null && session.isConnected()) {
//            session.disconnect();
//            log.info("SSH 会话已断开");
//        }
//    }
//}
