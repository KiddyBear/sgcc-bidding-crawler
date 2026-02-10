package com.sgcc.crawler.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 健康检查控制器
 * 提供应用状态监控接口
 */
@RestController
@RequestMapping("/health")
public class HealthController {

    @Autowired
    private DataSource dataSource;

    /**
     * 基础健康检查接口
     */
    @GetMapping("/health")
    public Map<String, Object> health() {
        Map<String, Object> result = new HashMap<>();
        result.put("status", "UP");
        result.put("timestamp", LocalDateTime.now());
        result.put("service", "sgcc-bidding-crawler");
        
        // 检查数据库连接
        try (Connection conn = dataSource.getConnection()) {
            result.put("database", "UP");
        } catch (SQLException e) {
            result.put("database", "DOWN");
            result.put("error", e.getMessage());
        }
        
        return result;
    }

    /**
     * 详细健康检查接口
     */
    @GetMapping("/health/detail")
    public Map<String, Object> healthDetail() {
        Map<String, Object> result = new HashMap<>();
        result.put("status", "UP");
        result.put("timestamp", LocalDateTime.now());
        result.put("service", "sgcc-bidding-crawler");
        
        // 数据库健康检查
        Map<String, Object> dbInfo = new HashMap<>();
        try (Connection conn = dataSource.getConnection()) {
            dbInfo.put("status", "UP");
            dbInfo.put("url", conn.getMetaData().getURL());
            dbInfo.put("username", conn.getMetaData().getUserName());
        } catch (SQLException e) {
            dbInfo.put("status", "DOWN");
            dbInfo.put("error", e.getMessage());
        }
        result.put("database", dbInfo);
        
        // 系统信息
        Map<String, Object> systemInfo = new HashMap<>();
        systemInfo.put("javaVersion", System.getProperty("java.version"));
        systemInfo.put("osName", System.getProperty("os.name"));
        systemInfo.put("osVersion", System.getProperty("os.version"));
        systemInfo.put("availableProcessors", Runtime.getRuntime().availableProcessors());
        systemInfo.put("freeMemory", Runtime.getRuntime().freeMemory());
        systemInfo.put("totalMemory", Runtime.getRuntime().totalMemory());
        result.put("system", systemInfo);
        
        return result;
    }

    /**
     * 爬虫状态检查
     */
    @GetMapping("/crawler/status")
    public Map<String, Object> crawlerStatus() {
        Map<String, Object> result = new HashMap<>();
        result.put("status", "RUNNING");
        result.put("timestamp", LocalDateTime.now());
        // 这里可以集成实际的爬虫状态检查逻辑
        result.put("lastRun", LocalDateTime.now().minusHours(1));
        result.put("nextRun", LocalDateTime.now().plusHours(1));
        return result;
    }
}