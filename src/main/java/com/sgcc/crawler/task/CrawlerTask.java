package com.sgcc.crawler.task;

import com.sgcc.crawler.entity.AnnouncementType;
import com.sgcc.crawler.service.AnnouncementCrawlerService;
import com.sgcc.crawler.service.CrawlerService;
import jakarta.annotation.Resource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.HashMap;
import java.util.Map;

/**
 * 爬虫定时任务
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CrawlerTask {

    private final CrawlerService crawlerService;

    @Resource
    private AnnouncementCrawlerService announcementCrawlerService;

    /**
     * 定时爬取任务
     * 默认每30分钟执行一次
     */
//    @Scheduled(cron = "${schedule.cron:0 */30 * * * ?}")
//    public void scheduledCrawl() {
//        log.info("==== 开始执行定时爬取任务 ====");
//        try {
//            int newCount = crawlerService.manualCrawl();
//            log.info("==== 定时爬取任务完成，新增 {} 个项目 ====", newCount);
//        } catch (Exception e) {
//            log.error("定时爬取任务执行失败", e);
//        }
//    }

    /**
     * 定时爬取招标公告
     * 默认每3小时执行一次
     */
    @Scheduled(cron = "${schedule.cron.bidding:0 0 */3 * * ?}")
    public void scheduledCrawlBidding() {
        log.info("==== 开始执行定时爬取招标公告任务 ====");
        try {
            AnnouncementType announcementType = AnnouncementType.valueOf("BIDDING_ANNOUNCEMENT");
            log.info("当前时间是：{}", java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            log.info("定时爬取招标公告信息: {}", announcementType.getDisplayName());
            int newCount = announcementCrawlerService.manualCrawlAndSave(announcementType);
            log.info("爬取完成，新获取招标数量为：{}", newCount);
        } catch (Exception e) {
            log.warn("定时爬取出现异常，异常打印：{}", e.toString());
        }
    }
}
