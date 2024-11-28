package com.mirror.hojbackenduserservice.client;


import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PreDestroy;
import java.util.Properties;

/**
 * SFTP 客户端
 * 负责管理 SFTP 会话和通道的连接、重连、断开。
 *
 * @author Mirror
 * @date 2024/11/28
 */
@Component
@Slf4j
public class SftpClient {

    @Value("${sftp.remoteHost}")
    private String remoteHost;

    @Value("${sftp.port}")
    private Integer port;

    @Value("${sftp.username}")
    private String username;

    @Value("${sftp.password}")
    private String password;

    private Session session;
    private ChannelSftp channelSftp;

    /**
     * 确保 SFTP 连接可用，若不可用则重连。
     */
    public ChannelSftp getChannelSftp() {
        ensureConnected();  // 在每次调用时检查连接状态
        return channelSftp;
    }

    /**
     * 确保 SFTP 会话和通道连接
     */
    private void ensureConnected() {
        try {
            if (session == null || !session.isConnected()) {
                connectSession();
            }

            if (channelSftp == null || !channelSftp.isConnected()) {
                connectChannel();
            }
        } catch (Exception e) {
            log.error("SFTP 连接建立失败", e);
            throw new RuntimeException("SFTP 连接失败", e);
        }
    }

    /**
     * 连接会话
     */
    private void connectSession() throws Exception {
        JSch jsch = new JSch();
        session = jsch.getSession(username, remoteHost, port);
        session.setPassword(password);

        Properties config = new Properties();
        config.put("StrictHostKeyChecking", "no");  // 跳过 host key 检查
        config.put("ServerAliveInterval", "60");    // 心跳机制
        session.setConfig(config);

        session.connect();
        log.info("SSH 会话已连接");
    }

    /**
     * 连接 SFTP 通道
     */
    private void connectChannel() throws Exception {
        if (session == null || !session.isConnected()) {
            throw new RuntimeException("SSH 会话未连接，无法创建 SFTP 通道");
        }
        channelSftp = (ChannelSftp) session.openChannel("sftp");
        channelSftp.connect();
        log.info("SFTP 通道已连接");
    }

    /**
     * 断开 SFTP 连接
     */
    @PreDestroy
    public void disconnect() {
        if (channelSftp != null && channelSftp.isConnected()) {
            channelSftp.disconnect();
            log.info("SFTP 通道已断开");
        }
        if (session != null && session.isConnected()) {
            session.disconnect();
            log.info("SSH 会话已断开");
        }
    }
}
