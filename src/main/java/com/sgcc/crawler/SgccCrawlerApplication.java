package com.sgcc.crawler;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 国网招标信息爬虫系统启动类
 */
@SpringBootApplication
@EnableScheduling
@MapperScan("com.sgcc.crawler.mapper")
public class SgccCrawlerApplication {

    public static void main(String[] args) {
        SpringApplication.run(SgccCrawlerApplication.class, args);
    }
}
