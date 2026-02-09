package com.sgcc.crawler.service.impl;

import com.sgcc.crawler.config.CrawlerConfig;
import com.sgcc.crawler.config.SeleniumConfig;
import com.sgcc.crawler.entity.BiddingProject;
import com.sgcc.crawler.mapper.BiddingProjectMapper;
import com.sgcc.crawler.service.CrawlerService;
import com.sgcc.crawler.service.NotifyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * 爬虫服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CrawlerServiceImpl implements CrawlerService {

    private final SeleniumConfig seleniumConfig;
    private final CrawlerConfig crawlerConfig;
    private final BiddingProjectMapper biddingProjectMapper;
    private final NotifyService notifyService;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter DATE_FORMATTER_SHORT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Override
    public List<BiddingProject> crawl() {
        List<BiddingProject> projects = new ArrayList<>();
        WebDriver driver = null;

        try {
            driver = seleniumConfig.createWebDriver();
            projects = doCrawl(driver);
        } catch (Exception e) {
            log.error("爬取失败", e);
        } finally {
            if (driver != null) {
                try {
                    driver.quit();
                } catch (Exception e) {
                    log.warn("关闭WebDriver失败", e);
                }
            }
        }

        return projects;
    }

    @Override
    public int manualCrawl() {
        List<BiddingProject> projects = crawl();
        int newCount = 0;

        for (BiddingProject project : projects) {
            BiddingProject existing = biddingProjectMapper.selectByProjectCode(project.getProjectCode());
            if (existing == null) {
                project.setNotified(0);
                project.setCreatedAt(LocalDateTime.now());
                project.setUpdatedAt(LocalDateTime.now());
                biddingProjectMapper.insert(project);
                newCount++;
                log.info("新增项目: {}", project.getProjectName());

                // 推送通知
                notifyService.notifyNewProject(project);
            } else {
                // 检查状态是否变更
                if (!existing.getProjectStatus().equals(project.getProjectStatus())) {
                    existing.setProjectStatus(project.getProjectStatus());
                    existing.setUpdatedAt(LocalDateTime.now());
                    biddingProjectMapper.updateById(existing);
                    log.info("项目状态更新: {} -> {}", existing.getProjectName(), project.getProjectStatus());

                    // 推送状态变更通知
                    notifyService.notifyStatusChange(existing, project.getProjectStatus());
                }
            }
        }

        return newCount;
    }

    /**
     * 执行爬取逻辑
     */
    private List<BiddingProject> doCrawl(WebDriver driver) {
        List<BiddingProject> projects = new ArrayList<>();

        try {
            // 第一步：先访问首页
            log.info("正在访问首页...");
            driver.get(crawlerConfig.getTargetUrl());
            randomSleep();

            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(crawlerConfig.getElementWaitTimeout()));

            // 等待首页加载完成
            wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("body")));
            log.info("首页加载完成");

            // 第二步：模拟点击"招标采购"导航菜单
            log.info("正在点击招标采购导航...");
            WebElement biddingNav = findAndClickBiddingNav(driver, wait);
            
            if (biddingNav == null) {
                log.error("未找到招标采购导航链接");
                return projects;
            }

            // 等待招标采购页面加载
            randomSleep();
            log.info("等待招标采购页面加载...");

            // 等待列表加载完成
            wait.until(ExpectedConditions.or(
                    ExpectedConditions.presenceOfElementLocated(By.cssSelector(".project-list")),
                    ExpectedConditions.presenceOfElementLocated(By.cssSelector(".list-content")),
                    ExpectedConditions.presenceOfElementLocated(By.cssSelector("table tbody")),
                    ExpectedConditions.presenceOfElementLocated(By.cssSelector(".el-table__body")),
                    ExpectedConditions.presenceOfElementLocated(By.cssSelector(".zbcg-list")),
                    ExpectedConditions.presenceOfElementLocated(By.cssSelector("[class*='list']")),
                    ExpectedConditions.presenceOfElementLocated(By.cssSelector(".notice-list"))
            ));

            randomSleep();

            // 解析项目列表
            projects = parseProjectList(driver);

            log.info("成功爬取 {} 个项目", projects.size());
            for (int i = 0; i < projects.size(); i++) {
                log.info("crawl no." + (i+1) + " project: " + projects.get(i).toString());
            }


        } catch (TimeoutException e) {
            log.error("页面加载超时", e);
            retryWithDifferentStrategy(driver, projects);
        } catch (Exception e) {
            log.error("爬取过程中发生错误", e);
        }

        return projects;
    }

    /**
     * 查找并点击招标采购导航
     */
    private WebElement findAndClickBiddingNav(WebDriver driver, WebDriverWait wait) {
        // 尝试多种选择器找到"招标采购"导航
        String[] navSelectors = {
                "//a[contains(text(),'招标采购')]",
                "//span[contains(text(),'招标采购')]",
                "//div[contains(text(),'招标采购')]",
                "//li[contains(text(),'招标采购')]"
        };

        for (String xpath : navSelectors) {
            try {
                WebElement navElement = wait.until(ExpectedConditions.elementToBeClickable(By.xpath(xpath)));
                log.info("找到招标采购导航，使用选择器: {}", xpath);
                
                // 滚动到元素可见
                ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView(true);", navElement);
                randomSleep();
                
                // 尝试点击
                try {
                    navElement.click();
                } catch (Exception e) {
                    // 如果普通点击失败，使用JavaScript点击
                    ((JavascriptExecutor) driver).executeScript("arguments[0].click();", navElement);
                }
                
                log.info("成功点击招标采购导航");
                return navElement;
            } catch (Exception e) {
                log.debug("选择器 {} 未找到元素", xpath);
            }
        }

        // 尝试CSS选择器
        String[] cssSelectors = {
                "a[href*='zbcg']",
                ".nav-item:contains('招标')",
                "[class*='nav'] a",
                ".menu-item a"
        };

        for (String css : cssSelectors) {
            try {
                List<WebElement> elements = driver.findElements(By.cssSelector(css));
                for (WebElement el : elements) {
                    String text = el.getText();
                    if (text != null && text.contains("招标采购")) {
                        log.info("通过CSS选择器找到招标采购导航: {}", css);
                        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", el);
                        return el;
                    }
                }
            } catch (Exception e) {
                log.debug("CSS选择器 {} 查找失败", css);
            }
        }

        return null;
    }

    /**
     * 解析项目列表
     */
    private List<BiddingProject> parseProjectList(WebDriver driver) {
        List<BiddingProject> projects = new ArrayList<>();

        try {
            // 尝试多种可能的选择器
            List<WebElement> rows = findProjectRows(driver);

            for (WebElement row : rows) {
                try {
                    BiddingProject project = parseProjectRow(row);
                    if (project != null && project.getProjectCode() != null) {
                        projects.add(project);
                    }
                } catch (Exception e) {
                    log.warn("解析行数据失败", e);
                }
            }
        } catch (Exception e) {
            log.error("解析项目列表失败", e);
        }

        return projects;
    }

    /**
     * 查找项目行元素
     */
    private List<WebElement> findProjectRows(WebDriver driver) {
        // 尝试多种选择器
        String[] selectors = {
                "table tbody tr",
                ".el-table__body tbody tr",
                ".project-item",
                ".list-item",
                "[class*='row']"
        };

        for (String selector : selectors) {
            try {
                List<WebElement> rows = driver.findElements(By.cssSelector(selector));
                if (!rows.isEmpty()) {
                    log.info("使用选择器 {} 找到 {} 个元素", selector, rows.size());
                    return rows;
                }
            } catch (Exception e) {
                // 继续尝试下一个选择器
            }
        }

        return new ArrayList<>();
    }

    /**
     * 解析单行项目数据
     */
    private BiddingProject parseProjectRow(WebElement row) {
        BiddingProject project = new BiddingProject();

        try {
            // 获取所有列
            List<WebElement> cells = row.findElements(By.tagName("td"));

            if (cells.isEmpty()) {
                // 尝试其他方式获取数据
                cells = row.findElements(By.cssSelector(".cell, [class*='col']"));
            }

            if (cells.size() >= 4) {
                // 根据列顺序提取数据(需要根据实际页面调整)
                project.setProjectName(getTextSafe(cells.get(0)));
                project.setProjectCode(getTextSafe(cells.get(1)));
                project.setProjectStatus(getTextSafe(cells.get(2)));

                // 解析截止时间
                String deadlineStr = getTextSafe(cells.get(3));
                project.setDeadline(parseDateTime(deadlineStr));
            } else {
                // 尝试从属性或其他元素获取
                project.setProjectName(getAttributeSafe(row, "data-name", ".project-name, .name"));
                project.setProjectCode(getAttributeSafe(row, "data-code", ".project-code, .code"));
                project.setProjectStatus(getAttributeSafe(row, "data-status", ".project-status, .status"));

                String deadlineStr = getAttributeSafe(row, "data-deadline", ".deadline, .time");
                project.setDeadline(parseDateTime(deadlineStr));
            }

            // 设置来源URL
            try {
                WebElement link = row.findElement(By.tagName("a"));
                project.setSourceUrl(link.getAttribute("href"));
            } catch (Exception e) {
                project.setSourceUrl(crawlerConfig.getBiddingUrl());
            }

        } catch (Exception e) {
            log.debug("解析行数据异常: {}", e.getMessage());
        }

        return project;
    }

    /**
     * 安全获取元素文本
     */
    private String getTextSafe(WebElement element) {
        try {
            return element.getText().trim();
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * 安全获取属性或子元素文本
     */
    private String getAttributeSafe(WebElement element, String attrName, String cssSelector) {
        try {
            String value = element.getAttribute(attrName);
            if (value != null && !value.isEmpty()) {
                return value;
            }

            WebElement child = element.findElement(By.cssSelector(cssSelector));
            return child.getText().trim();
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * 解析日期时间
     */
    private LocalDateTime parseDateTime(String dateStr) {
        if (dateStr == null || dateStr.isEmpty()) {
            return null;
        }

        try {
            return LocalDateTime.parse(dateStr, DATE_FORMATTER);
        } catch (Exception e) {
            try {
                return LocalDateTime.parse(dateStr, DATE_FORMATTER_SHORT);
            } catch (Exception e2) {
                log.debug("日期解析失败: {}", dateStr);
                return null;
            }
        }
    }

    /**
     * 随机延迟
     */
    private void randomSleep() {
        try {
            int delay = seleniumConfig.getRandomDelay();
            log.debug("等待 {}ms...", delay);
            Thread.sleep(delay);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * 重试策略
     */
    private void retryWithDifferentStrategy(WebDriver driver, List<BiddingProject> projects) {
        for (int i = 0; i < crawlerConfig.getRetryTimes(); i++) {
            try {
                log.info("第 {} 次重试...", i + 1);
                randomSleep();
                driver.navigate().refresh();
                randomSleep();

                List<BiddingProject> retryProjects = parseProjectList(driver);
                if (!retryProjects.isEmpty()) {
                    projects.addAll(retryProjects);
                    break;
                }
            } catch (Exception e) {
                log.warn("重试失败", e);
            }
        }
    }
}
