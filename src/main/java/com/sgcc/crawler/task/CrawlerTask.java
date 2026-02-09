package com.sgcc.crawler.task;

import com.sgcc.crawler.service.CrawlerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 爬虫定时任务
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CrawlerTask {

    private final CrawlerService crawlerService;

    /**
     * 定时爬取任务
     * 默认每30分钟执行一次
     */
//    @Scheduled(cron = "${schedule.cron:0 */30 * * * ?}")
    public void scheduledCrawl() {
        log.info("==== 开始执行定时爬取任务 ====");
        try {
            int newCount = crawlerService.manualCrawl();
            log.info("==== 定时爬取任务完成，新增 {} 个项目 ====", newCount);
        } catch (Exception e) {
            log.error("定时爬取任务执行失败", e);
        }
    }
}
