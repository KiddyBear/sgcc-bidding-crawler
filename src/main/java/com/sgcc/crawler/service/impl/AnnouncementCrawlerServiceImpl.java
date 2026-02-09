package com.sgcc.crawler.service.impl;

import com.sgcc.crawler.config.CrawlerConfig;
import com.sgcc.crawler.config.SeleniumConfig;
import com.sgcc.crawler.entity.AnnouncementType;
import com.sgcc.crawler.entity.BiddingAnnouncement;
import com.sgcc.crawler.mapper.BiddingAnnouncementMapper;
import com.sgcc.crawler.parser.AnnouncementParser;
import com.sgcc.crawler.parser.ParserFactory;
import com.sgcc.crawler.service.AnnouncementCrawlerService;
import com.sgcc.crawler.service.NotifyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * 公告爬虫服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AnnouncementCrawlerServiceImpl implements AnnouncementCrawlerService {

    private final SeleniumConfig seleniumConfig;
    private final CrawlerConfig crawlerConfig;
    private final ParserFactory parserFactory;
    private final BiddingAnnouncementMapper announcementMapper;
    private final NotifyService notifyService;

    private static final Random random = new Random();

    @Override
    public List<BiddingAnnouncement> crawlAnnouncements(AnnouncementType type) {
        return crawlAnnouncements(type, true, -1);
    }

    @Override
    public List<BiddingAnnouncement> crawlAnnouncements(AnnouncementType type, boolean fetchDetail, int limit) {
        List<BiddingAnnouncement> announcements = new ArrayList<>();
        WebDriver driver = null;

        try {
            driver = seleniumConfig.createWebDriver();
            announcements = doCrawl(driver, type, fetchDetail, limit);
        } catch (Exception e) {
            log.error("爬取公告失败: {}", type, e);
        } finally {
            quitDriver(driver);
        }

        return announcements;
    }

    @Override
    public BiddingAnnouncement crawlAnnouncementDetail(BiddingAnnouncement announcement) {
        if (announcement.getDetailUrl() == null || announcement.getDetailUrl().isEmpty()) {
            log.warn("公告缺少详情URL: {}", announcement.getProjectCode());
            return announcement;
        }

        WebDriver driver = null;
        try {
            driver = seleniumConfig.createWebDriver();
            
            AnnouncementParser parser = parserFactory.getParser(
                    AnnouncementType.valueOf(announcement.getAnnouncementType())
            );
            
            if (parser != null) {
                // 访问详情页
                driver.get(announcement.getDetailUrl());
                randomSleep();
                
                // 解析详情
                announcement = parser.parseDetail(driver, announcement);
            }
        } catch (Exception e) {
            log.error("爬取公告详情失败: {}", announcement.getProjectCode(), e);
        } finally {
            quitDriver(driver);
        }

        return announcement;
    }

    @Override
    public int manualCrawlAndSave(AnnouncementType type) {
        log.info("开始手动爬取[{}]", type.getDisplayName());
        List<BiddingAnnouncement> announcements = crawlAnnouncements(type, true, -1);
        return saveAnnouncementsWithDedup(announcements, type);
    }

    @Override
    public int testCrawlAndSave(AnnouncementType type, int limit) {
        log.info("开始测试爬取[{}], 限制条数: {}", type.getDisplayName(), limit);
        List<BiddingAnnouncement> announcements = crawlAnnouncements(type, true, limit);
        return saveAnnouncementsWithDedup(announcements, type);
    }

    /**
     * 统一入库方法：内容指纹去重 + 变更检测 + 分类推送
     */
    private int saveAnnouncementsWithDedup(List<BiddingAnnouncement> announcements, AnnouncementType type) {
        int newCount = 0;
        int updateCount = 0;
        int skipCount = 0;

        for (BiddingAnnouncement announcement : announcements) {
            try {
                // 校验唯一键字段
                if (announcement.getProjectCode() == null || announcement.getProjectCode().isBlank()) {
                    log.warn("跳过无项目编号的公告: {}", announcement.getProjectName());
                    continue;
                }

                // 计算身份指纹
                String hash = computeContentHash(announcement);
                announcement.setContentHash(hash);

                // 通过 contentHash 查询已有记录
                BiddingAnnouncement existing = announcementMapper.selectByContentHash(hash);

                if (existing == null) {
                    // === 新公告：插入 + 推送 ===
                    announcement.setNotified(0);
                    announcement.setCreatedAt(LocalDateTime.now());
                    announcement.setUpdatedAt(LocalDateTime.now());
                    announcementMapper.insert(announcement);
                    newCount++;
                    log.info("新增公告[{}]: {} - {}", type.getDisplayName(),
                            announcement.getProjectCode(), announcement.getProjectName());

                    try {
                        notifyService.notifyNewAnnouncement(announcement);
                        announcement.setNotified(1);
                        announcementMapper.updateById(announcement);
                    } catch (Exception e) {
                        log.warn("新公告推送失败: {}", e.getMessage());
                    }

                } else {
                    // === 已存在：检测变更 ===
                    List<String> changedFields = detectChangedFields(existing, announcement);
                    if (!changedFields.isEmpty()) {
                        mergeUpdatedFields(existing, announcement);
                        existing.setUpdatedAt(LocalDateTime.now());
                        announcementMapper.updateById(existing);
                        updateCount++;
                        log.info("更新公告[{}]: {} - {}, 变更: {}",
                                type.getDisplayName(), existing.getProjectCode(),
                                existing.getProjectName(), changedFields);

                        try {
                            notifyService.notifyAnnouncementUpdate(existing, changedFields);
                        } catch (Exception e) {
                            log.warn("公告变更推送失败: {}", e.getMessage());
                        }
                    } else {
                        skipCount++;
                        log.debug("公告未变更，跳过: {} - {}", existing.getProjectCode(), existing.getProjectName());
                    }
                }
            } catch (Exception e) {
                log.error("保存公告失败: {}", announcement.getProjectCode(), e);
            }
        }

        log.info("爬取入库完成[{}]: 新增 {} 条, 更新 {} 条, 跳过 {} 条",
                type.getDisplayName(), newCount, updateCount, skipCount);
        return newCount;
    }

    /**
     * 执行爬取逻辑
     */
    private List<BiddingAnnouncement> doCrawl(WebDriver driver, AnnouncementType type, boolean fetchDetail, int limit) {
        List<BiddingAnnouncement> announcements = new ArrayList<>();

        try {
            // 1. 访问首页
            log.info("正在访问首页...");
            driver.get(crawlerConfig.getTargetUrl());
            randomSleep();

            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(crawlerConfig.getElementWaitTimeout()));
            wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("body")));

            // 2. 点击招标采购导航
            log.info("正在点击招标采购导航...");
            if (!clickBiddingNav(driver, wait)) {
                log.error("未能进入招标采购页面");
                return announcements;
            }
            randomSleep();

            // 3. 点击对应的Tab
            log.info("正在切换到Tab: {}", type.getDisplayName());
            if (!clickTab(driver, wait, type)) {
                log.error("未能切换到Tab: {}", type.getDisplayName());
                return announcements;
            }
            randomSleep();

            // 4. 等待列表加载
            waitForListLoad(driver, wait);

            // 5. 使用对应解析器解析列表
            AnnouncementParser parser = parserFactory.getParser(type);
            if (parser == null) {
                log.error("未找到类型 {} 的解析器", type);
                return announcements;
            }

            announcements = parser.parseList(driver);
            
            // 如果有限制条数，截断列表
            if (limit > 0 && announcements.size() > limit) {
                announcements = announcements.subList(0, limit);
                log.info("截断列表，仅保留前 {} 条记录", limit);
            }
            
            log.info("待处理公告数量: {}", announcements.size());

            // 6. 如果需要获取详情
            if (fetchDetail && !announcements.isEmpty()) {
                // 此时在同一个会话中，announcements 中的 webElement 是有效的
                announcements = fetchDetails(driver, parser, announcements);
            }

        } catch (Exception e) {
            log.error("爬取过程出错", e);
        }

        return announcements;
    }

    /**
     * 点击招标采购导航
     */
    private boolean clickBiddingNav(WebDriver driver, WebDriverWait wait) {
        String[] navXpaths = {
                "//a[contains(text(),'招标采购')]",
                "//span[contains(text(),'招标采购')]",
                "//div[contains(text(),'招标采购')]"
        };

        for (String xpath : navXpaths) {
            try {
                WebElement nav = wait.until(ExpectedConditions.elementToBeClickable(By.xpath(xpath)));
                clickElement(driver, nav);
                log.info("成功点击招标采购导航");
                return true;
            } catch (Exception e) {
                log.debug("XPath {} 未找到导航", xpath);
            }
        }
        return false;
    }

    /**
     * 点击Tab切换
     */
    private boolean clickTab(WebDriver driver, WebDriverWait wait, AnnouncementType type) {
        AnnouncementParser parser = parserFactory.getParser(type);
        if (parser == null) return false;

        String tabSelector = parser.getTabSelector();
        
        // 尝试XPath
        try {
            WebElement tab = wait.until(ExpectedConditions.elementToBeClickable(By.xpath(tabSelector)));
            clickElement(driver, tab);
            log.info("成功切换到Tab: {}", type.getDisplayName());
            return true;
        } catch (Exception e) {
            log.debug("XPath {} 未找到Tab", tabSelector);
        }

        // 尝试文本匹配
        String[] textPatterns = {
                "//a[contains(text(),'" + type.getDisplayName() + "')]",
                "//span[contains(text(),'" + type.getDisplayName() + "')]",
                "//div[contains(text(),'" + type.getDisplayName() + "')]",
                "//li[contains(text(),'" + type.getDisplayName() + "')]"
        };

        for (String xpath : textPatterns) {
            try {
                WebElement tab = driver.findElement(By.xpath(xpath));
                clickElement(driver, tab);
                log.info("通过文本匹配切换到Tab: {}", type.getDisplayName());
                return true;
            } catch (Exception e) {
                log.debug("XPath {} 未找到", xpath);
            }
        }

        return false;
    }

    /**
     * 等待列表加载
     */
    private void waitForListLoad(WebDriver driver, WebDriverWait wait) {
        try {
            wait.until(ExpectedConditions.or(
                    ExpectedConditions.presenceOfElementLocated(By.cssSelector(".el-table__body tbody tr")),
                    ExpectedConditions.presenceOfElementLocated(By.cssSelector("table tbody tr")),
                    ExpectedConditions.presenceOfElementLocated(By.cssSelector(".list-item")),
                    ExpectedConditions.presenceOfElementLocated(By.cssSelector("[class*='list']"))
            ));
            randomSleep(1000, 2000);
        } catch (Exception e) {
            log.warn("等待列表加载超时");
        }
    }

    /**
     * 获取详情（拟人化点击方式）
     */
    private List<BiddingAnnouncement> fetchDetails(WebDriver driver, AnnouncementParser parser, 
                                                    List<BiddingAnnouncement> announcements) {
        List<BiddingAnnouncement> detailedAnnouncements = new ArrayList<>();
        String mainWindow = driver.getWindowHandle();

        log.info("开始通过模拟点击获取详情，共 {} 条数据", announcements.size());

        for (int i = 0; i < announcements.size(); i++) {
            BiddingAnnouncement announcement = announcements.get(i);
            WebElement row = announcement.getWebElement();

            try {
                if (row == null) {
                    log.warn("第 {} 条数据缺少 WebElement，尝试重新获取", (i + 1));
                    List<WebElement> currentRows = parser.findListRows(driver);
                    if (i < currentRows.size()) {
                        row = currentRows.get(i);
                    } else {
                        log.error("无法找回第 {} 行元素", (i + 1));
                        detailedAnnouncements.add(announcement);
                        continue;
                    }
                }

                log.info("点击第 {} 行: {}", (i + 1), announcement.getProjectName());
                
                // 查找行内可点击的元素
                WebElement clickable = findClickableElement(row);
                
                // 记录当前已打开的窗口句柄
                java.util.Set<String> oldHandles = driver.getWindowHandles();

                // 拟人化点击
                try {
                    clickElement(driver, clickable);
                } catch (StaleElementReferenceException e) {
                    log.warn("元素已失效，重新定位列表行并重试");
                    List<WebElement> refreshedRows = parser.findListRows(driver);
                    if (i < refreshedRows.size()) {
                        row = refreshedRows.get(i);
                        announcement.setWebElement(row);
                        clickable = findClickableElement(row);
                        clickElement(driver, clickable);
                    } else {
                        throw e;
                    }
                }
                
                randomSleep(1500, 2500);

                // 检查是否打开了新窗口
                java.util.Set<String> currentHandles = driver.getWindowHandles();
                String detailWindow = null;
                
                if (currentHandles.size() > oldHandles.size()) {
                    for (String handle : currentHandles) {
                        if (!oldHandles.contains(handle)) {
                            detailWindow = handle;
                            break;
                        }
                    }
                    driver.switchTo().window(detailWindow);
                }

                // 1. 实时获取详情页地址
                String actualDetailUrl = driver.getCurrentUrl();
                announcement.setDetailUrl(actualDetailUrl);
                log.info("成功获取详情页地址: {}", actualDetailUrl);

                // 2. 解析详情内容
                BiddingAnnouncement detailed = parser.parseDetail(driver, announcement);
                detailedAnnouncements.add(detailed);

                // 3. 关闭详情页并切回主窗口
                if (detailWindow != null) {
                    driver.close();
                    driver.switchTo().window(mainWindow);
                } else {
                    // 如果是在当前页跳转的，需要退回列表页
                    driver.navigate().back();
                    randomSleep(2000, 3000);
                    mainWindow = driver.getWindowHandle();
                    // 当前页跳转会导致所有 WebElement 失效，标记为 null 触发下一次迭代重新获取
                    invalidateWebElements(announcements);
                }
                
                randomSleep(800, 1500);

            } catch (Exception e) {
                log.warn("通过点击获取第 {} 行详情失败: {}", (i + 1), e.getMessage());
                detailedAnnouncements.add(announcement);
                
                try {
                    if (driver.getWindowHandles().size() > 1) {
                        driver.close();
                    }
                    driver.switchTo().window(mainWindow);
                } catch (Exception ignored) {}
            }
        }

        return detailedAnnouncements;
    }

    /**
     * 在行元素中查找可点击的子元素
     */
    private WebElement findClickableElement(WebElement row) {
        try {
            return row.findElement(By.tagName("a"));
        } catch (Exception e) {
            try {
                return row.findElement(By.cssSelector("td:first-child, .cell"));
            } catch (Exception e2) {
                return row; // 兜底：点击整行
            }
        }
    }

    /**
     * 失效所有公告中的 WebElement（通常在页面导航后执行）
     */
    private void invalidateWebElements(List<BiddingAnnouncement> announcements) {
        for (BiddingAnnouncement a : announcements) {
            a.setWebElement(null);
        }
    }

    // ==================== 去重与变更检测 ====================

    /**
     * 计算身份指纹：MD5(projectCode | announcementType | projectName)
     * 同一项目的不同分包公告（如“公告一”、“公告二”）会因 projectName 不同而产生不同 hash
     */
    private String computeContentHash(BiddingAnnouncement a) {
        String raw = nullSafe(a.getProjectCode()) + "|" +
                     nullSafe(a.getAnnouncementType()) + "|" +
                     nullSafe(a.getProjectName());
        return md5(raw);
    }

    /**
     * MD5 哈希
     */
    private String md5(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : digest) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (Exception e) {
            log.error("MD5计算失败", e);
            return String.valueOf(input.hashCode());
        }
    }

    /**
     * 精确检测变更字段，返回变更描述列表
     */
    private List<String> detectChangedFields(BiddingAnnouncement existing, BiddingAnnouncement newData) {
        List<String> changes = new ArrayList<>();
        if (!safeEquals(existing.getProjectStatus(), newData.getProjectStatus())) changes.add("项目状态");
        if (!safeEquals(existing.getBidOpenTime(), newData.getBidOpenTime())) changes.add("开标时间");
        if (!safeEquals(existing.getFileDeadline(), newData.getFileDeadline())) changes.add("文件截止时间");
        if (!safeEquals(existing.getDetailUrl(), newData.getDetailUrl())) changes.add("详情链接");
        if (!safeEquals(existing.getTenderer(), newData.getTenderer())) changes.add("招标人");
        if (!safeEquals(existing.getContactPerson(), newData.getContactPerson())) changes.add("联系人");
        if (!safeEquals(existing.getProcurementType(), newData.getProcurementType())) changes.add("采购类型");
        if (!safeEquals(existing.getBidOpenLocation(), newData.getBidOpenLocation())) changes.add("开标地点");
        return changes;
    }

    /**
     * 合并更新字段（新值非空时覆盖旧值）
     */
    private void mergeUpdatedFields(BiddingAnnouncement existing, BiddingAnnouncement newData) {
        if (newData.getProjectName() != null) existing.setProjectName(newData.getProjectName());
        if (newData.getProjectStatus() != null) existing.setProjectStatus(newData.getProjectStatus());
        if (newData.getFileDeadline() != null) existing.setFileDeadline(newData.getFileDeadline());
        if (newData.getBidOpenTime() != null) existing.setBidOpenTime(newData.getBidOpenTime());
        if (newData.getDetailUrl() != null) existing.setDetailUrl(newData.getDetailUrl());
        if (newData.getTenderer() != null) existing.setTenderer(newData.getTenderer());
        if (newData.getContactPerson() != null) existing.setContactPerson(newData.getContactPerson());
        if (newData.getBackupContactPerson() != null) existing.setBackupContactPerson(newData.getBackupContactPerson());
        if (newData.getContactPhone() != null) existing.setContactPhone(newData.getContactPhone());
        if (newData.getBackupContactPhone() != null) existing.setBackupContactPhone(newData.getBackupContactPhone());
        if (newData.getProcurementType() != null) existing.setProcurementType(newData.getProcurementType());
        if (newData.getProcurementName() != null) existing.setProcurementName(newData.getProcurementName());
        if (newData.getBidOpenLocation() != null) existing.setBidOpenLocation(newData.getBidOpenLocation());
        if (newData.getFax() != null) existing.setFax(newData.getFax());
        if (newData.getEmail() != null) existing.setEmail(newData.getEmail());
        if (newData.getProjectIntro() != null) existing.setProjectIntro(newData.getProjectIntro());
        if (newData.getFileDownloadUrl() != null) existing.setFileDownloadUrl(newData.getFileDownloadUrl());
        if (newData.getBiddingFileUrl() != null) existing.setBiddingFileUrl(newData.getBiddingFileUrl());
        if (newData.getChangeContent() != null) existing.setChangeContent(newData.getChangeContent());
        if (newData.getChangeFileUrl() != null) existing.setChangeFileUrl(newData.getChangeFileUrl());
        if (newData.getPublishTime() != null) existing.setPublishTime(newData.getPublishTime());
    }

    private String nullSafe(String value) {
        return value != null ? value : "";
    }

    /**
     * 安全比较
     */
    private boolean safeEquals(Object a, Object b) {
        if (a == null && b == null) return true;
        if (a == null || b == null) return false;
        return a.equals(b);
    }

    /**
     * 点击元素
     */
    private void clickElement(WebDriver driver, WebElement element) {
        try {
            ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView(true);", element);
            randomSleep(300, 500);
            try {
                element.click();
            } catch (Exception e) {
                ((JavascriptExecutor) driver).executeScript("arguments[0].click();", element);
            }
        } catch (Exception e) {
            log.warn("点击元素失败: {}", e.getMessage());
        }
    }

    /**
     * 随机延迟
     */
    private void randomSleep() {
        randomSleep(crawlerConfig.getIntervalMin(), crawlerConfig.getIntervalMax());
    }

    private void randomSleep(int min, int max) {
        try {
            int delay = min + random.nextInt(max - min);
            Thread.sleep(delay);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * 安全关闭WebDriver
     */
    private void quitDriver(WebDriver driver) {
        if (driver != null) {
            try {
                driver.quit();
            } catch (Exception e) {
                log.warn("关闭WebDriver失败", e);
            }
        }
    }
}
