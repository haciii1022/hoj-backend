package com.mirror.hojbackendquestionservice.scheduler;

import com.mirror.hojbackendquestionservice.service.QuestionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Component
public class RankSyncScheduler {

    @Autowired
    private QuestionService questionService;

    public static final int minCount = 100;

    public static final Set<Long> possibleSet = new HashSet<>();

    private final AtomicBoolean initialized = new AtomicBoolean(false);

    /**
     * 每日3点定时执行
     */
    @Scheduled(cron = "0 0 3 * * ?")
    public void dailySync() {
        log.info("全局加载热门题目排行榜数据定时任务触发...");
        try {
            questionService.fullSyncRedisRank();
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
            log.info("项目启动，开始全量加载热门题目排行榜数据....");
            try {
                questionService.fullSyncRedisRank();
            } catch (Exception e) {
                log.error("定时任务执行异常", e);
            }
        }
    }
}
