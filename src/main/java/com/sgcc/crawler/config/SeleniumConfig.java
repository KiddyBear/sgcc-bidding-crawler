package com.sgcc.crawler.config;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import java.io.File;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.*;
import java.util.Random;

/**
 * Selenium配置类
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class SeleniumConfig {

    private final CrawlerConfig crawlerConfig;

    /**
     * User-Agent列表
     */
    private static final List<String> USER_AGENTS = Arrays.asList(
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36",
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/121.0.0.0 Safari/537.36",
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36",
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:123.0) Gecko/20100101 Firefox/123.0",
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.3 Safari/605.1.15"
    );

    private static final Random random = new Random();

    /**
     * 初始化ChromeDriver路径
     * 根据操作系统自动选择对应的驱动
     */
    @PostConstruct
    public void initChromeDriver() {
        String driverPath = crawlerConfig.getChromeDriverPath();
        
        // 如果配置了具体路径，直接使用
        if (StringUtils.hasText(driverPath)) {
            File driverFile = new File(driverPath);
            if (driverFile.exists()) {
                System.setProperty("webdriver.chrome.driver", driverPath);
                log.info("使用配置的ChromeDriver路径: {}", driverPath);
                return;
            }
            log.warn("配置的ChromeDriver路径不存在: {}", driverPath);
        }
        
        // 自动检测操作系统并加载对应驱动
        String autoDriverPath = detectDriverPath();
        if (autoDriverPath != null) {
            System.setProperty("webdriver.chrome.driver", autoDriverPath);
            log.info("自动检测操作系统: {}, 使用ChromeDriver: {}", getOsType(), autoDriverPath);
        } else {
            log.error("未找到ChromeDriver，请下载对应版本放到chromedriver目录");
            throw new RuntimeException("ChromeDriver not found! 请下载驱动到 chromedriver/" + getOsType() + "/ 目录");
        }
    }
    
    /**
     * 检测操作系统类型并返回对应的驱动路径
     */
    private String detectDriverPath() {
        String osType = getOsType();
        String driverName = "win".equals(osType) ? "chromedriver.exe" : "chromedriver";
        
        // Google Chrome for Testing 默认解压后的子目录结构
        String subDir;
        switch (osType) {
            case "win": subDir = "chromedriver-win64"; break;
            case "mac": subDir = "chromedriver-mac-arm64"; break; // 也可能是 chromedriver-mac-x64
            default: subDir = "chromedriver-linux64"; break;
        }
        
        // 尝试多个可能的路径
        List<String> possiblePaths = Arrays.asList(
                // 1. 带子目录的路径 (新版解压结构: chromedriver/win/chromedriver-win64/chromedriver.exe)
                Paths.get("chromedriver", osType, subDir, driverName).toAbsolutePath().toString(),
                // 2. 兼容 Mac x64
                Paths.get("chromedriver", "mac", "chromedriver-mac-x64", driverName).toAbsolutePath().toString(),
                // 3. 原有的直接路径 (旧版结构: chromedriver/win/chromedriver.exe)
                Paths.get("chromedriver", osType, driverName).toAbsolutePath().toString(),
                // 4. 当前工作目录下的路径
                Paths.get(System.getProperty("user.dir"), "chromedriver", osType, subDir, driverName).toString()
        );
        
        for (String path : possiblePaths) {
            File file = new File(path);
            if (file.exists()) {
                // Windows只检查存在，Linux/Mac检查是否存在且可执行
                if ("win".equals(osType) || file.canExecute()) {
                    return path;
                }
            }
        }
        
        log.warn("未能在以下路径找到驱动: {}", possiblePaths);
        return null;
    }
    
    /**
     * 获取操作系统类型
     * @return win / linux / mac
     */
    private String getOsType() {
        String osName = System.getProperty("os.name").toLowerCase();
        if (osName.contains("win")) {
            return "win";
        } else if (osName.contains("mac") || osName.contains("darwin")) {
            return "mac";
        } else {
            return "linux";
        }
    }

    /**
     * 创建Chrome配置选项
     */
    public ChromeOptions createChromeOptions() {
        ChromeOptions options = new ChromeOptions();

        // 随机User-Agent
        String userAgent = USER_AGENTS.get(random.nextInt(USER_AGENTS.size()));
        options.addArguments("--user-agent=" + userAgent);

        // 无头模式
        if (crawlerConfig.isHeadless()) {
            options.addArguments("--headless=new");
        }

        // 基础配置
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--disable-gpu");
        options.addArguments("--window-size=1920,1080");
        options.addArguments("--disable-blink-features=AutomationControlled");
        options.addArguments("--disable-extensions");
        options.addArguments("--disable-infobars");

        // 忽略SSL证书错误
        options.addArguments("--ignore-certificate-errors");
        options.addArguments("--ignore-ssl-errors");

        // --- 增加下载配置 ---
        Map<String, Object> prefs = new HashMap<>();
        // 设置默认下载路径（项目根目录下的 downloads 文件夹）
        // String downloadPath = Paths.get("downloads").toAbsolutePath().toString();
        String downloadPath = crawlerConfig.getDownloadFilePath();
        File downloadDir = new File(downloadPath);
        if (!downloadDir.exists()) downloadDir.mkdirs();
        
        prefs.put("download.default_directory", downloadPath);
        prefs.put("download.prompt_for_download", false); // 禁用下载提示
        prefs.put("download.directory_upgrade", true);
        prefs.put("plugins.always_open_pdf_externally", true); // PDF也直接下载而不是在浏览器打开
        options.setExperimentalOption("prefs", prefs);
        // ------------------

        // 禁用自动化检测
        options.setExperimentalOption("excludeSwitches", Arrays.asList("enable-automation"));
        options.setExperimentalOption("useAutomationExtension", false);

        // 代理配置
        if (crawlerConfig.getProxyHost() != null && crawlerConfig.getProxyPort() != null) {
            String proxy = crawlerConfig.getProxyHost() + ":" + crawlerConfig.getProxyPort();
            options.addArguments("--proxy-server=" + proxy);
            log.info("使用代理: {}", proxy);
        }

        return options;
    }

    /**
     * 创建WebDriver实例
     */
    public WebDriver createWebDriver() {
        ChromeOptions options = createChromeOptions();
        ChromeDriver driver = new ChromeDriver(options);

        // 设置超时
        driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(crawlerConfig.getPageLoadTimeout()));
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(crawlerConfig.getElementWaitTimeout()));

        // 执行CDP命令，隐藏webdriver特征
        driver.executeCdpCommand("Page.addScriptToEvaluateOnNewDocument",
                java.util.Map.of("source",
                        "Object.defineProperty(navigator, 'webdriver', {get: () => undefined})"));

        log.info("WebDriver创建成功");
        return driver;
    }

    /**
     * 获取随机延迟时间
     */
    public int getRandomDelay() {
        return crawlerConfig.getIntervalMin() +
                random.nextInt(crawlerConfig.getIntervalMax() - crawlerConfig.getIntervalMin());
    }
}
