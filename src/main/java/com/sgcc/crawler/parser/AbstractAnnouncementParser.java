package com.sgcc.crawler.parser;

import com.sgcc.crawler.entity.AnnouncementType;
import com.sgcc.crawler.entity.BiddingAnnouncement;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * 抽象公告解析器基类
 */
@Slf4j
public abstract class AbstractAnnouncementParser implements AnnouncementParser {

    protected static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    protected static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    protected static final Random random = new Random();

    protected int elementWaitTimeout = 10;
    protected int minDelay = 2000;
    protected int maxDelay = 5000;

    /**
     * 设置等待超时时间
     */
    public void setElementWaitTimeout(int timeout) {
        this.elementWaitTimeout = timeout;
    }

    /**
     * 设置随机延迟范围
     */
    public void setDelayRange(int min, int max) {
        this.minDelay = min;
        this.maxDelay = max;
    }

    @Override
    public List<BiddingAnnouncement> parseList(WebDriver driver) {
        List<BiddingAnnouncement> announcements = new ArrayList<>();

        try {
            // 查找列表行
            List<WebElement> rows = findListRows(driver);
            log.info("找到 {} 条记录", rows.size());

            for (WebElement row : rows) {
                try {
                    BiddingAnnouncement announcement = parseListRow(row);
                    if (announcement != null && announcement.getProjectCode() != null) {
                        announcement.setAnnouncementType(getSupportedType().name());
                        // 前后的游览器保持一致，不能重开
                        announcement.setWebElement(row);
                        announcements.add(announcement);
                    }
                } catch (StaleElementReferenceException e) {
                    log.warn("元素已过期，跳过");
                } catch (Exception e) {
                    log.warn("解析行数据失败: {}", e.getMessage());
                }
            }
        } catch (Exception e) {
            log.error("解析列表失败", e);
        }

        return announcements;
    }

    /**
     * 查找列表行元素
     */
    @Override
    public List<WebElement> findListRows(WebDriver driver) {
        String[] selectors = getListRowSelectors();
        
        for (String selector : selectors) {
            try {
                List<WebElement> rows = driver.findElements(By.cssSelector(selector));
                if (!rows.isEmpty()) {
                    log.info("使用选择器 {} 找到 {} 行", selector, rows.size());
                    return rows;
                }
            } catch (Exception e) {
                log.debug("选择器 {} 查找失败", selector);
            }
        }
        
        return new ArrayList<>();
    }

    /**
     * 获取列表行选择器 - 子类可重写
     */
    protected String[] getListRowSelectors() {
        return new String[]{
                ".el-table__body tbody tr",
                "table tbody tr",
                ".list-item",
                ".data-row",
                "[class*='row']"
        };
    }

    /**
     * 安全获取元素文本
     */
    protected String getTextSafe(WebElement element) {
        try {
            if (element == null) return "";
            return element.getText().trim();
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * 安全获取元素文本 - 通过CSS选择器
     */
    protected String getTextByCss(WebElement parent, String cssSelector) {
        try {
            WebElement element = parent.findElement(By.cssSelector(cssSelector));
            return element.getText().trim();
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * 安全获取元素文本 - 通过XPath
     */
    protected String getTextByXpath(WebElement parent, String xpath) {
        try {
            WebElement element = parent.findElement(By.xpath(xpath));
            return element.getText().trim();
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * 安全获取元素属性
     */
    protected String getAttributeSafe(WebElement element, String attrName) {
        try {
            if (element == null) return "";
            String value = element.getAttribute(attrName);
            return value != null ? value.trim() : "";
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * 解析日期时间
     * 支持 yyyy-MM-dd, yyyy-MM-dd HH:mm:ss, yyyy年MM月dd日 等多种格式
     */
    protected LocalDateTime parseDateTime(String dateStr) {
        if (dateStr == null || dateStr.trim().isEmpty()) {
            return null;
        }

        // 统一清理和标准化格式
        String cleaned = dateStr.trim()
                .replace("年", "-")
                .replace("月", "-")
                .replace("日", " ")
                .replace("/", "-")
                .replace(".", "-")
                .replaceAll("\\s+", " ") // 合并多个空格
                .trim();

        // 移除末尾可能残留的横杠（如：2026-01-03-）
        if (cleaned.endsWith("-")) {
            cleaned = cleaned.substring(0, cleaned.length() - 1).trim();
        }

        try {
            // 情况1：包含时间 (yyyy-MM-dd HH:mm:ss 或 yyyy-MM-dd HH:mm)
            if (cleaned.contains(" ") && cleaned.contains(":")) {
                // 如果只有分钟 (yyyy-MM-dd HH:mm)，补齐秒数
                if (cleaned.split(":").length == 2) {
                    cleaned += ":00";
                }
                return LocalDateTime.parse(cleaned, DATE_TIME_FORMATTER);
            } 
            
            // 情况2：仅日期 (yyyy-MM-dd)
            return java.time.LocalDate.parse(cleaned, DATE_FORMATTER).atStartOfDay();
            
        } catch (Exception e) {
            log.debug("日期解析常规尝试失败 [{}], 处理后 [{}], 尝试兜底方案: {}", dateStr, cleaned, e.getMessage());
            
            // 最后的兜底尝试：使用 ISO 标准格式
            try {
                if (cleaned.contains(" ")) {
                    return LocalDateTime.parse(cleaned.replace(" ", "T"));
                }
                return java.time.LocalDate.parse(cleaned).atStartOfDay();
            } catch (Exception ignored) {
                log.warn("日期解析完全失败: {}", dateStr);
                return null;
            }
        }
    }

    /**
     * 等待元素可见
     */
    protected WebElement waitForElement(WebDriver driver, By locator) {
        try {
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(elementWaitTimeout));
            return wait.until(ExpectedConditions.visibilityOfElementLocated(locator));
        } catch (Exception e) {
            log.debug("等待元素超时: {}", locator);
            return null;
        }
    }

    /**
     * 等待元素可点击
     */
    protected WebElement waitForClickable(WebDriver driver, By locator) {
        try {
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(elementWaitTimeout));
            return wait.until(ExpectedConditions.elementToBeClickable(locator));
        } catch (Exception e) {
            log.debug("等待元素可点击超时: {}", locator);
            return null;
        }
    }

    /**
     * 点击元素（带JavaScript备用方案）
     */
    protected boolean clickElement(WebDriver driver, WebElement element) {
        try {
            // 滚动到元素可见
            ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView(true);", element);
            randomSleep(500, 1000);
            
            try {
                element.click();
            } catch (Exception e) {
                // JavaScript点击作为备用
                ((JavascriptExecutor) driver).executeScript("arguments[0].click();", element);
            }
            return true;
        } catch (Exception e) {
            log.warn("点击元素失败: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 随机延迟
     */
    protected void randomSleep() {
        randomSleep(minDelay, maxDelay);
    }

    /**
     * 随机延迟（指定范围）
     */
    protected void randomSleep(int min, int max) {
        try {
            int delay = min + random.nextInt(max - min);
            Thread.sleep(delay);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * 获取详情页标签页内的文本值
     * @param driver WebDriver
     * @param labelText 标签文本（如"联系人"）
     * @return 对应的值
     */
    /**
     * 获取详情页标签页内的文本值
     * @param driver WebDriver
     * @param labelText 标签文本（如"联系人"）
     * @return 对应的值
     */
    protected String getDetailValueByLabel(WebDriver driver, String labelText) {
        try {
            // 尝试多种方式查找
            String[] xpaths = {
                    String.format("//*[contains(text(),'%s')]/following-sibling::*[1]", labelText),
                    String.format("//*[contains(text(),'%s')]/../following-sibling::*[1]", labelText),
                    String.format("//td[contains(text(),'%s')]/following-sibling::td[1]", labelText),
                    String.format("//th[contains(text(),'%s')]/following-sibling::td[1]", labelText),
                    String.format("//span[contains(text(),'%s')]/following-sibling::span[1]", labelText),
                    String.format("//div[contains(text(),'%s')]/following-sibling::div[1]", labelText)
            };

            for (String xpath : xpaths) {
                try {
                    WebElement element = driver.findElement(By.xpath(xpath));
                    String text = element.getText().trim();
                    if (!text.isEmpty()) {
                        return text;
                    }
                } catch (Exception ignored) {
                }
            }
        } catch (Exception e) {
            log.debug("获取详情值失败 [{}]: {}", labelText, e.getMessage());
        }
        return "";
    }

    /**
     * 增强版：尝试捕获下载链接或触发下载
     * 支持多关键词和模糊匹配
     */
    protected String captureDownload(WebDriver driver, String... keywords) {
        for (String keyword : keywords) {
            // 1. 构造多种可能的探测器 (XPath 使用 normalize-space 忽略空格和换行)
            String[] xpaths = {
                    String.format("//a[contains(normalize-space(.), '%s')]", keyword),
                    String.format("//button[contains(normalize-space(.), '%s')]", keyword),
                    String.format("//span[contains(normalize-space(.), '%s')]", keyword),
                    String.format("//div[contains(normalize-space(.), '%s')]", keyword),
                    String.format("//*[contains(@title, '%s')]", keyword),
                    String.format("//*[contains(@class, 'download') or contains(@class, 'file')]//*[contains(text(), '%s')]", keyword)
            };

            for (String xpath : xpaths) {
                try {
                    List<WebElement> elements = driver.findElements(By.xpath(xpath));
                    for (WebElement btn : elements) {
                        if (!btn.isDisplayed()) continue;

                        // A. 尝试直接获取 href
                        String href = btn.getAttribute("href");
                        if (href != null && !href.isEmpty() && !href.toLowerCase().startsWith("javascript") && !href.equals("#")) {
                            log.info("成功通过 href 捕获到链接: {}", href);
                            return href;
                        }

                        // B. 尝试从 onclick/data-url 解析
                        String[] attrs = {"onclick", "data-url", "data-href", "url"};
                        for (String attr : attrs) {
                            String val = btn.getAttribute(attr);
                            if (val != null && !val.isEmpty()) {
                                log.info("尝试从属性 {} 解析链接: {}", attr, val);
                                String parsed = parseUrlFromText(val);
                                if (parsed != null) return parsed;
                            }
                        }

                        // C. 兜底方案：模拟人工点击触发下载 (SeleniumConfig 已配置自动保存)
                        log.info("无法直接提取URL，执行拟人化点击触发下载: {}", keyword);
                        clickElement(driver, btn);
                        randomSleep(1000, 2000);
                        return "TRIGGERED_DOWNLOAD";
                    }
                } catch (Exception ignored) {}
            }
        }
        return null;
    }

    /**
     * 从复杂的 JS 字符串或属性值中尝试提取 URL
     */
    private String parseUrlFromText(String text) {
        if (text == null || text.isEmpty()) return null;
        // 正则提取引号内的路径，支持 .jsp, .do, .pdf, .zip, .doc 等常见后缀
        java.util.regex.Pattern p = java.util.regex.Pattern.compile("['\"]([^'\"]*?\\.(?:jsp|do|pdf|zip|doc|docx|rar|xls|xlsx)[^'\"]*?)['\"]");
        java.util.regex.Matcher m = p.matcher(text);
        if (m.find()) {
            return m.group(1);
        }
        return null;
    }
}
