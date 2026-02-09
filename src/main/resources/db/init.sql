-- 国网招标信息爬虫系统数据库初始化脚本

-- 创建数据库
CREATE DATABASE IF NOT EXISTS sgcc_crawler DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE sgcc_crawler;

-- 招标项目表
CREATE TABLE IF NOT EXISTS bidding_project (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    project_code VARCHAR(100) UNIQUE COMMENT '项目编号',
    project_name VARCHAR(500) COMMENT '项目名称',
    project_status VARCHAR(50) COMMENT '项目状态',
    deadline DATETIME COMMENT '截止时间',
    source_url VARCHAR(500) COMMENT '来源链接',
    notified TINYINT DEFAULT 0 COMMENT '是否已推送 0-否 1-是',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_project_code (project_code),
    INDEX idx_project_status (project_status),
    INDEX idx_deadline (deadline),
    INDEX idx_notified (notified),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='招标项目表';

-- 招标公告详情表
CREATE TABLE IF NOT EXISTS bidding_announcement (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    announcement_type VARCHAR(50) COMMENT '公告类型: PREQUALIFICATION/BIDDING_ANNOUNCEMENT/PROCUREMENT等',
    project_code VARCHAR(100) COMMENT '项目编号',
    project_name VARCHAR(500) COMMENT '项目名称',
    procurement_name VARCHAR(500) COMMENT '采购项目名称',
    project_status VARCHAR(50) COMMENT '项目状态',
    procurement_type VARCHAR(100) COMMENT '采购类型',
    detail_url VARCHAR(1000) COMMENT '详情页URL',
    file_deadline DATETIME COMMENT '招标文件获取截止时间',
    bid_open_time DATETIME COMMENT '开标（截标）时间',
    bid_open_location VARCHAR(200) COMMENT '开标地点',
    tenderer VARCHAR(200) COMMENT '招标人',
    contact_person VARCHAR(100) COMMENT '联系人',
    backup_contact_person VARCHAR(100) COMMENT '备用联系人',
    contact_phone VARCHAR(50) COMMENT '联系电话',
    backup_contact_phone VARCHAR(50) COMMENT '备用联系电话',
    fax VARCHAR(50) COMMENT '传真',
    email VARCHAR(100) COMMENT '电子邮箱',
    project_intro TEXT COMMENT '项目介绍',
    file_download_url VARCHAR(1000) COMMENT '公告文件下载链接',
    bidding_file_url VARCHAR(1000) COMMENT '招标文件下载链接',
    change_content TEXT COMMENT '变更公告内容',
    change_file_url VARCHAR(1000) COMMENT '变更公告文件下载链接',
    publish_time DATETIME COMMENT '发布时间',
    content_hash VARCHAR(64) COMMENT '内容指纹MD5，基于projectCode+announcementType+projectName计算',
    notified TINYINT DEFAULT 0 COMMENT '是否已推送 0-否 1-是',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '数据抓取时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_content_hash (content_hash),
    INDEX idx_announcement_type (announcement_type),
    INDEX idx_project_code (project_code),
    INDEX idx_project_status (project_status),
    INDEX idx_publish_time (publish_time),
    INDEX idx_notified (notified),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='招标公告详情表';

-- 增量升级脚本（已有表执行）
ALTER TABLE bidding_announcement ADD COLUMN IF NOT EXISTS bidding_file_url VARCHAR(1000) COMMENT '招标文件下载链接' AFTER file_download_url;
ALTER TABLE bidding_announcement ADD COLUMN IF NOT EXISTS content_hash VARCHAR(64) COMMENT '内容指纹MD5' AFTER publish_time;
ALTER TABLE bidding_announcement DROP INDEX IF EXISTS uk_code_type;
ALTER TABLE bidding_announcement ADD UNIQUE INDEX IF NOT EXISTS uk_content_hash (content_hash);
