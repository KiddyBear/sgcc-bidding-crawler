package com.sgcc.crawler.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 爬虫配置类
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "crawler")
public class CrawlerConfig {

    /**
     * 目标URL
     */
    private String targetUrl = "https://ecp.sgcc.com.cn/ecp2.0/portal/#/";

    /**
     * 招标采购页面URL
     */
    private String biddingUrl = "https://ecp.sgcc.com.cn/ecp2.0/portal/#/zbcg";

    /**
     * 最小请求间隔(毫秒)
     */
    private int intervalMin = 3000;

    /**
     * 最大请求间隔(毫秒)
     */
    private int intervalMax = 8000;

    /**
     * 重试次数
     */
    private int retryTimes = 3;

    /**
     * 页面加载超时时间(秒)
     */
    private int pageLoadTimeout = 30;

    /**
     * 元素等待超时时间(秒)
     */
    private int elementWaitTimeout = 10;

    /**
     * Chrome驱动路径(可选，不设置则自动下载)
     */
    private String chromeDriverPath;

    /**
     * 是否使用无头模式
     */
    private boolean headless = true;

    /**
     * 代理地址(可选)
     */
    private String proxyHost;

    /**
     * 代理端口(可选)
     */
    private Integer proxyPort;

    /**
     * 下载路径
     */
    private String downloadFilePath;
}
