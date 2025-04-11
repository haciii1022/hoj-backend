package com.mirror.hojbackenduserservice.scheduler;

import com.mirror.hojbackenduserservice.service.UserQuestionStatisticsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Component
public class RankSyncScheduler {

    @Autowired
    private UserQuestionStatisticsService rankSyncService;

    private final AtomicBoolean initialized = new AtomicBoolean(false);

    /**
     * 每日3点定时执行
     */
    @Scheduled(cron = "0 0 3 * * ?")
    public void dailySync() {
        log.info("全局加载用户题目排行榜数据定时任务触发...");
        try {
            rankSyncService.fullSyncRedisRank();
        } catch (Exception e) {
            log.error("定时任务执行异常", e);
        }
    }

    /**
     * 项目启动后立即执行
     */
    @EventListener(ContextRefreshedEvent.class)
    public void onApplicationStart() {
        if (!initialized.getAndSet(true)) {
            log.info("项目启动，开始全量加载用户题目排行榜数据....");
            try {
                rankSyncService.fullSyncRedisRank();
            } catch (Exception e) {
                log.error("定时任务执行异常", e);
            }
        }
    }
}
