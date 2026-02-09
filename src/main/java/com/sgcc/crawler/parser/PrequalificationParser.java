package com.sgcc.crawler.parser;

import com.sgcc.crawler.entity.AnnouncementType;
import com.sgcc.crawler.entity.BiddingAnnouncement;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 资格预审公告解析器
 */
@Slf4j
@Component
public class PrequalificationParser extends AbstractAnnouncementParser {

    @Override
    public AnnouncementType getSupportedType() {
        return AnnouncementType.PREQUALIFICATION;
    }

    @Override
    public String getTabSelector() {
        // 资格预审公告是第一个tab
        return "//span[contains(text(),'资格预审公告')] | //a[contains(text(),'资格预审公告')] | //div[contains(text(),'资格预审公告')]";
    }

    @Override
    public BiddingAnnouncement parseListRow(WebElement row) {
        BiddingAnnouncement announcement = new BiddingAnnouncement();

        try {
            List<WebElement> cells = row.findElements(By.tagName("td"));
            
            if (cells.isEmpty()) {
                cells = row.findElements(By.cssSelector(".cell, .el-table__cell, [class*='col']"));
            }

            if (cells.size() >= 4) {
                announcement.setProjectName(getTextSafe(cells.get(0)));
                announcement.setProjectCode(getTextSafe(cells.get(1)));
                announcement.setProjectStatus(getTextSafe(cells.get(2)));
                announcement.setPublishTime(parseDateTime(getTextSafe(cells.get(3))));
            }

            announcement.setDetailUrl(getDetailUrl(row));

            log.debug("解析资格预审公告: {} - {}", announcement.getProjectCode(), announcement.getProjectName());

        } catch (Exception e) {
            log.warn("解析资格预审公告列表行失败: {}", e.getMessage());
        }

        return announcement;
    }

    @Override
    public String getDetailUrl(WebElement row) {
        try {
            WebElement link = row.findElement(By.tagName("a"));
            return link.getAttribute("href");
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public BiddingAnnouncement parseDetail(WebDriver driver, BiddingAnnouncement announcement) {
        try {
            log.info("开始解析资格预审详情页: {}", driver.getCurrentUrl());
            announcement.setDetailUrl(driver.getCurrentUrl());

            randomSleep(1000, 2000);

            // 资格预审公告的字段与招标公告类似，但可能有不同的结构
            // 这里提供差异化解析逻辑

            // 采购项目名称
            String title = getDetailValueByLabel(driver, "采购项目名称");
            if (!title.isEmpty()) {
                announcement.setProcurementName(title);
            }

            // 采购项目编号
            String code = getDetailValueByLabel(driver, "采购项目编号");
            if (!code.isEmpty()) {
                announcement.setProjectCode(code);
            }

            // 项目状态
            announcement.setProjectStatus(getDetailValueByLabel(driver, "采购项目状态"));

            // 资格预审特有字段处理
            parsePrequalificationSpecificFields(driver, announcement);

            // 联系信息
            parseContactInfo(driver, announcement);

            log.info("资格预审详情解析完成: {}", announcement.getProjectCode());

        } catch (Exception e) {
            log.error("解析资格预审详情页失败: {}", e.getMessage(), e);
        }

        return announcement;
    }

    /**
     * 解析资格预审特有字段
     */
    private void parsePrequalificationSpecificFields(WebDriver driver, BiddingAnnouncement announcement) {
        try {
            // 资格预审文件获取截止时间
            String deadline = getDetailValueByLabel(driver, "资格预审文件获取截止时间");
            if (deadline.isEmpty()) {
                deadline = getDetailValueByLabel(driver, "招标文件获取截止时间");
            }
            announcement.setFileDeadline(parseDateTime(deadline));

            // 资格预审截止时间
            String bidTime = getDetailValueByLabel(driver, "资格预审截止时间");
            if (bidTime.isEmpty()) {
                bidTime = getDetailValueByLabel(driver, "开标时间");
            }
            announcement.setBidOpenTime(parseDateTime(bidTime));

            // 招标人
            announcement.setTenderer(getDetailValueByLabel(driver, "招标人"));

        } catch (Exception e) {
            log.debug("解析资格预审特有字段失败: {}", e.getMessage());
        }
    }

    /**
     * 解析联系信息
     */
    private void parseContactInfo(WebDriver driver, BiddingAnnouncement announcement) {
        try {
            announcement.setContactPerson(getDetailValueByLabel(driver, "联系人"));
            announcement.setContactPhone(getDetailValueByLabel(driver, "联系电话"));
            announcement.setEmail(getDetailValueByLabel(driver, "电子邮箱"));

        } catch (Exception e) {
            log.debug("解析联系信息失败: {}", e.getMessage());
        }
    }
}
