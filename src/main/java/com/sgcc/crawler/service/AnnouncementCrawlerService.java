package com.sgcc.crawler.service;

import com.sgcc.crawler.entity.AnnouncementType;
import com.sgcc.crawler.entity.BiddingAnnouncement;

import java.util.List;

/**
 * 公告爬虫服务接口
 */
public interface AnnouncementCrawlerService {

    /**
     * 爬取指定类型的公告列表
     * @param type 公告类型
     * @return 公告列表
     */
    List<BiddingAnnouncement> crawlAnnouncements(AnnouncementType type);

    /**
     * 爬取指定类型的公告列表并获取详情
     * @param type 公告类型
     * @param fetchDetail 是否获取详情
     * @param limit 限制爬取的条数（-1为不限制）
     * @return 公告列表（含详情）
     */
    List<BiddingAnnouncement> crawlAnnouncements(AnnouncementType type, boolean fetchDetail, int limit);

    /**
     * 爬取公告详情
     * @param announcement 已有公告基础信息
     * @return 补充完整的公告信息
     */
    BiddingAnnouncement crawlAnnouncementDetail(BiddingAnnouncement announcement);

    /**
     * 手动触发爬取并保存
     * @param type 公告类型
     * @return 新增数量
     */
    int manualCrawlAndSave(AnnouncementType type);

    /**
     * 测试爬取并保存（支持限制条数）
     * @param type 公告类型
     * @param limit 限制爬取的条数
     * @return 新增数量
     */
    int testCrawlAndSave(AnnouncementType type, int limit);
}
