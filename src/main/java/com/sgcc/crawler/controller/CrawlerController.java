package com.sgcc.crawler.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sgcc.crawler.entity.AnnouncementType;
import com.sgcc.crawler.entity.BiddingAnnouncement;
import com.sgcc.crawler.entity.BiddingProject;
import com.sgcc.crawler.mapper.BiddingAnnouncementMapper;
import com.sgcc.crawler.mapper.BiddingProjectMapper;
import com.sgcc.crawler.service.AnnouncementCrawlerService;
import com.sgcc.crawler.service.CrawlerService;
import com.sgcc.crawler.service.NotifyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 爬虫API控制器
 */
@Slf4j
@RestController
@RequestMapping("/doCrawl")
@RequiredArgsConstructor
public class CrawlerController {

    private final CrawlerService crawlerService;
    private final AnnouncementCrawlerService announcementCrawlerService;
    private final NotifyService notifyService;
    private final BiddingProjectMapper biddingProjectMapper;
    private final BiddingAnnouncementMapper announcementMapper;

    /**
     * 手动触发爬取
     */
    @PostMapping("/crawl")
    public Map<String, Object> manualCrawl() {
        Map<String, Object> result = new HashMap<>();
        try {
            log.info("手动触发爬取任务");
            int newCount = crawlerService.manualCrawl();
            result.put("success", true);
            result.put("message", "爬取完成");
            result.put("newCount", newCount);
        } catch (Exception e) {
            log.error("手动爬取失败", e);
            result.put("success", false);
            result.put("message", "爬取失败: " + e.getMessage());
        }
        return result;
    }

    /**
     * 获取项目列表
     */
    @GetMapping("/projects")
    public Map<String, Object> listProjects(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword) {

        Map<String, Object> result = new HashMap<>();

        LambdaQueryWrapper<BiddingProject> wrapper = new LambdaQueryWrapper<>();

        if (status != null && !status.isEmpty()) {
            wrapper.eq(BiddingProject::getProjectStatus, status);
        }

        if (keyword != null && !keyword.isEmpty()) {
            wrapper.and(w -> w
                    .like(BiddingProject::getProjectName, keyword)
                    .or()
                    .like(BiddingProject::getProjectCode, keyword)
            );
        }

        wrapper.orderByDesc(BiddingProject::getCreatedAt);

        Page<BiddingProject> pageResult = biddingProjectMapper.selectPage(new Page<>(page, size), wrapper);

        result.put("success", true);
        result.put("data", pageResult.getRecords());
        result.put("total", pageResult.getTotal());
        result.put("page", page);
        result.put("size", size);

        return result;
    }

    /**
     * 获取项目详情
     */
    @GetMapping("/projects/{id}")
    public Map<String, Object> getProject(@PathVariable Long id) {
        Map<String, Object> result = new HashMap<>();
        BiddingProject project = biddingProjectMapper.selectById(id);
        if (project != null) {
            result.put("success", true);
            result.put("data", project);
        } else {
            result.put("success", false);
            result.put("message", "项目不存在");
        }
        return result;
    }

    /**
     * 发送测试消息
     */
    @PostMapping("/test-notify")
    public Map<String, Object> testNotify(@RequestParam(defaultValue = "测试消息") String message) {
        Map<String, Object> result = new HashMap<>();
        try {
            notifyService.sendMessage("测试通知", message);
            result.put("success", true);
            result.put("message", "消息已发送");
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "发送失败: " + e.getMessage());
        }
        return result;
    }

    /**
     * 健康检查
     */
    @GetMapping("/health")
    public Map<String, Object> health() {
        Map<String, Object> result = new HashMap<>();
        result.put("status", "UP");
        result.put("timestamp", System.currentTimeMillis());
        return result;
    }

    // ==================== 公告API ====================

    /**
     * 获取公告类型列表
     */
    @GetMapping("/announcement-types")
    public Map<String, Object> getAnnouncementTypes() {
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("data", Arrays.stream(AnnouncementType.values())
                .map(t -> Map.of(
                        "name", t.name(),
                        "displayName", t.getDisplayName(),
                        "tabIndex", t.getTabIndex()
                ))
                .collect(Collectors.toList()));
        return result;
    }

    /**
     * 爬取指定类型的公告
     */
    @PostMapping("/crawl/announcements")
    public Map<String, Object> crawlAnnouncements(
            @RequestParam String type,
            @RequestParam(defaultValue = "true") boolean fetchDetail) {
        Map<String, Object> result = new HashMap<>();
        try {
            AnnouncementType announcementType = AnnouncementType.valueOf(type);
            log.info("手动触发爬取公告: {}", announcementType.getDisplayName());
            
            int newCount = announcementCrawlerService.manualCrawlAndSave(announcementType);
            
            result.put("success", true);
            result.put("message", "爬取完成");
            result.put("type", type);
            result.put("newCount", newCount);
        } catch (IllegalArgumentException e) {
            result.put("success", false);
            result.put("message", "无效的公告类型: " + type);
        } catch (Exception e) {
            log.error("爬取公告失败", e);
            result.put("success", false);
            result.put("message", "爬取失败: " + e.getMessage());
        }
        return result;
    }

    /**
     * 测试爬取公告（仅前5条）
     * 用于检验拟人化点击及详情抓取逻辑
     */
    @PostMapping("/test/crawl/announcements")
    public Map<String, Object> testCrawlAnnouncements(
            @RequestParam String type,
            @RequestParam(defaultValue = "5") int limit) {
        Map<String, Object> result = new HashMap<>();
        try {
            AnnouncementType announcementType = AnnouncementType.valueOf(type);
            log.info("测试爬取公告: {}, 限制条数: {}", announcementType.getDisplayName(), limit);

            int newCount = announcementCrawlerService.testCrawlAndSave(announcementType, limit);

            result.put("success", true);
            result.put("message", "测试爬取完成");
            result.put("type", type);
            result.put("limit", limit);
            result.put("newCount", newCount);
        } catch (IllegalArgumentException e) {
            result.put("success", false);
            result.put("message", "无效的公告类型: " + type);
        } catch (Exception e) {
            log.error("测试爬取失败", e);
            result.put("success", false);
            result.put("message", "测试失败: " + e.getMessage());
        }
        return result;
    }

    /**
     * 获取公告列表
     */
    @GetMapping("/announcements")
    public Map<String, Object> listAnnouncements(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword) {

        Map<String, Object> result = new HashMap<>();

        LambdaQueryWrapper<BiddingAnnouncement> wrapper = new LambdaQueryWrapper<>();

        if (type != null && !type.isEmpty()) {
            wrapper.eq(BiddingAnnouncement::getAnnouncementType, type);
        }

        if (status != null && !status.isEmpty()) {
            wrapper.eq(BiddingAnnouncement::getProjectStatus, status);
        }

        if (keyword != null && !keyword.isEmpty()) {
            wrapper.and(w -> w
                    .like(BiddingAnnouncement::getProjectName, keyword)
                    .or()
                    .like(BiddingAnnouncement::getProjectCode, keyword)
            );
        }

        wrapper.orderByDesc(BiddingAnnouncement::getPublishTime);

        Page<BiddingAnnouncement> pageResult = announcementMapper.selectPage(new Page<>(page, size), wrapper);

        result.put("success", true);
        result.put("data", pageResult.getRecords());
        result.put("total", pageResult.getTotal());
        result.put("page", page);
        result.put("size", size);

        return result;
    }

    /**
     * 获取公告详情
     */
    @GetMapping("/announcements/{id}")
    public Map<String, Object> getAnnouncement(@PathVariable Long id) {
        Map<String, Object> result = new HashMap<>();
        BiddingAnnouncement announcement = announcementMapper.selectById(id);
        if (announcement != null) {
            result.put("success", true);
            result.put("data", announcement);
        } else {
            result.put("success", false);
            result.put("message", "公告不存在");
        }
        return result;
    }
}
