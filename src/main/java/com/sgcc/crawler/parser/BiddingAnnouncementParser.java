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
 * 招标公告及投标邀请书解析器
 */
@Slf4j
@Component
public class BiddingAnnouncementParser extends AbstractAnnouncementParser {

    @Override
    public AnnouncementType getSupportedType() {
        return AnnouncementType.BIDDING_ANNOUNCEMENT;
    }

    @Override
    public String getTabSelector() {
        // 招标公告及投标邀请书是第二个tab
        return "//span[contains(text(),'招标公告及投标邀请书')] | //a[contains(text(),'招标公告及投标邀请书')] | //div[contains(text(),'招标公告及投标邀请书')]";
    }

    @Override
    public BiddingAnnouncement parseListRow(WebElement row) {
        BiddingAnnouncement announcement = new BiddingAnnouncement();

        try {
            // 获取所有单元格
            List<WebElement> cells = row.findElements(By.tagName("td"));
            
            if (cells.isEmpty()) {
                cells = row.findElements(By.cssSelector(".cell, .el-table__cell, [class*='col']"));
            }

            if (cells.size() >= 4) {
                // 根据列表页结构解析：项目名称 | 项目编号 | 项目状态 | 创建时间
                announcement.setProjectName(getTextSafe(cells.get(0)));
                announcement.setProjectCode(getTextSafe(cells.get(1)));
                announcement.setProjectStatus(getTextSafe(cells.get(2)));
                announcement.setPublishTime(parseDateTime(getTextSafe(cells.get(3))));
            } else if (!cells.isEmpty()) {
                // 尝试从单个单元格内获取
                String fullText = getTextSafe(cells.get(0));
                announcement.setProjectName(fullText);
            }

            // 尝试获取详情链接
            announcement.setDetailUrl(getDetailUrl(row));

            log.debug("解析列表行: {} - {}", announcement.getProjectCode(), announcement.getProjectName());

        } catch (Exception e) {
            log.warn("解析招标公告列表行失败: {}", e.getMessage());
        }

        return announcement;
    }

    @Override
    public String getDetailUrl(WebElement row) {
        // 逻辑迁移至 Service 层，通过点击跳转后实时获取
        return null;
    }

    @Override
    public BiddingAnnouncement parseDetail(WebDriver driver, BiddingAnnouncement announcement) {
        try {
            log.info("开始解析详情页: {}", driver.getCurrentUrl());
            announcement.setDetailUrl(driver.getCurrentUrl());

            randomSleep(1000, 2000);

            // 解析页面标题
            String title = getDetailValueByLabel(driver, "采购项目名称");
            if (!title.isEmpty()) {
                announcement.setProcurementName(title);
            }

            // 采购项目编号
            String code = getDetailValueByLabel(driver, "采购项目编号");
            if (!code.isEmpty() && announcement.getProjectCode() == null) {
                announcement.setProjectCode(code);
            }

            // 采购类型
            announcement.setProcurementType(getDetailValueByLabel(driver, "采购类型"));

            // 项目状态
            String status = getDetailValueByLabel(driver, "采购项目状态");
            if (!status.isEmpty()) {
                announcement.setProjectStatus(status);
            }

            // 解析原公告部分
            parseOriginalAnnouncement(driver, announcement);

            // 解析变更公告部分（如果有）
            parseChangeAnnouncement(driver, announcement);

            log.info("详情解析完成: {}", announcement.getProjectCode());

        } catch (Exception e) {
            log.error("解析详情页失败: {}", e.getMessage(), e);
        }

        return announcement;
    }

    /**
     * 解析原公告部分
     */
    private void parseOriginalAnnouncement(WebDriver driver, BiddingAnnouncement announcement) {
        try {
            // 招标文件获取截止时间
            String fileDeadline = getDetailValueByLabel(driver, "招标文件获取截止时间");
            announcement.setFileDeadline(parseDateTime(fileDeadline));

            // 开标时间
            String bidOpenTime = getDetailValueByLabel(driver, "开标（截标）时间");
            if (bidOpenTime.isEmpty()) {
                bidOpenTime = getDetailValueByLabel(driver, "开标时间");
            }
            announcement.setBidOpenTime(parseDateTime(bidOpenTime));

            // 开标地点
            announcement.setBidOpenLocation(getDetailValueByLabel(driver, "开标地点"));

            // 招标人
            announcement.setTenderer(getDetailValueByLabel(driver, "招标人"));

            // 联系人
            announcement.setContactPerson(getDetailValueByLabel(driver, "联系人"));

            // 备用联系人
            announcement.setBackupContactPerson(getDetailValueByLabel(driver, "备用联系人"));

            // 联系电话
            announcement.setContactPhone(getDetailValueByLabel(driver, "联系电话"));

            // 备用联系电话
            announcement.setBackupContactPhone(getDetailValueByLabel(driver, "备用联系电话"));

            // 传真
            announcement.setFax(getDetailValueByLabel(driver, "传真"));

            // 电子邮箱
            announcement.setEmail(getDetailValueByLabel(driver, "电子邮箱"));

            // 项目介绍
            announcement.setProjectIntro(getDetailValueByLabel(driver, "项目介绍"));

            // 1. 公告文件下载
//            announcement.setFileDownloadUrl(captureDownload(driver, "下载公告文件", "公告下载", "下载公告"));

            // 2. 招标文件下载
//            announcement.setBiddingFileUrl(captureDownload(driver, "获取招标文件", "下载招标文件", "招标文件"));

        } catch (Exception e) {
            log.warn("解析原公告部分失败: {}", e.getMessage());
        }
    }

    /**
     * 解析变更公告部分
     */
    private void parseChangeAnnouncement(WebDriver driver, BiddingAnnouncement announcement) {
        try {
            // 检查是否有变更公告
            try {
                driver.findElement(By.xpath("//*[contains(text(),'变更公告')]"));
            } catch (Exception e) {
                // 没有变更公告部分
                return;
            }

            // 变更公告内容
            announcement.setChangeContent(getDetailValueByLabel(driver, "变更公告内容"));

            // 3. 变更公告文件下载
//            announcement.setChangeFileUrl(captureDownload(driver, "下载变更公告文件", "下载变更公告", "变更公告文件"));

        } catch (Exception e) {
            log.debug("解析变更公告部分: {}", e.getMessage());
        }
    }
}
