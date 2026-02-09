package com.sgcc.crawler.parser;

import com.sgcc.crawler.entity.AnnouncementType;
import com.sgcc.crawler.entity.BiddingAnnouncement;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.List;

/**
 * 公告解析器接口 - 策略模式
 */
public interface AnnouncementParser {

    /**
     * 获取解析器支持的公告类型
     */
    AnnouncementType getSupportedType();

    /**
     * 获取Tab选择器（用于切换到对应Tab）
     */
    String getTabSelector();

    /**
     * 解析列表页数据
     * @param driver WebDriver实例
     * @return 公告列表（仅包含列表页信息）
     */
    List<BiddingAnnouncement> parseList(WebDriver driver);

    /**
     * 解析详情页数据
     * @param driver WebDriver实例
     * @param announcement 已有的公告基础信息
     * @return 补充完整信息的公告对象
     */
    BiddingAnnouncement parseDetail(WebDriver driver, BiddingAnnouncement announcement);

    /**
     * 从列表行元素解析基础信息
     * @param row 行元素
     * @return 公告基础信息
     */
    BiddingAnnouncement parseListRow(WebElement row);

    /**
     * 获取详情页链接
     * @param row 行元素
     * @return 详情页URL
     */
    String getDetailUrl(WebElement row);

    /**
     * 查找列表行元素
     * @param driver WebDriver
     * @return 行元素列表
     */
    List<WebElement> findListRows(WebDriver driver);
}
