//package com.mirror.hojbackenduserservice.config;
//
//import com.jcraft.jsch.ChannelSftp;
//import com.jcraft.jsch.Session;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.stereotype.Component;
//
//import javax.annotation.PreDestroy;
//
///**
// * 确保在项目关闭时ChannelSftp和Session这两个bean会被自动断开连接
// *
// * @author Mirror.
// * @date 2024/11/27
// */
//@Slf4j
//@Component
//public class SftpShutdown {
//
//    private final ChannelSftp channelSftp;
//    private final Session session;
//
//    public SftpShutdown(ChannelSftp channelSftp, Session session) {
//        this.channelSftp = channelSftp;
//        this.session = session;
//    }
//
//    @PreDestroy
//    public void closeSftpChannel() {
//        if (channelSftp != null && channelSftp.isConnected()) {
//            channelSftp.disconnect();
//            log.info("SFTP 连接已关闭");
//        }
//        if (session != null && session.isConnected()) {
//            session.disconnect();
//            log.info("SSH 会话已关闭");
//        }
//    }
//}
