package com.sgcc.crawler.service;

import com.sgcc.crawler.entity.BiddingProject;

import java.util.List;

/**
 * 爬虫服务接口
 */
public interface CrawlerService {

    /**
     * 招标采购页面中，出现的第一个页面中分页查询出的数据
     * 执行爬取任务
     * @return 爬取到的项目列表
     */
    List<BiddingProject> crawl();

    /**
     * 手动触发爬取
     * @return 新增项目数量
     */
    int manualCrawl();
}
