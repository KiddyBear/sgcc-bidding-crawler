package com.sgcc.crawler.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.openqa.selenium.WebElement;

import java.time.LocalDateTime;

/**
 * 招标公告详情实体类
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("bidding_announcement")
public class BiddingAnnouncement {

    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 公告类型
     */
    private String announcementType;

    /**
     * 项目编号
     */
    private String projectCode;

    /**
     * 项目名称
     */
    private String projectName;

    /**
     * 采购项目名称（详情页）
     */
    private String procurementName;

    /**
     * 项目状态
     */
    private String projectStatus;

    /**
     * 采购类型
     */
    private String procurementType;

    /**
     * 详情页URL
     */
    private String detailUrl;

    /**
     * 招标文件获取截止时间
     */
    private LocalDateTime fileDeadline;

    /**
     * 开标（截标）时间
     */
    private LocalDateTime bidOpenTime;

    /**
     * 开标地点
     */
    private String bidOpenLocation;

    /**
     * 招标人
     */
    private String tenderer;

    /**
     * 联系人
     */
    private String contactPerson;

    /**
     * 备用联系人
     */
    private String backupContactPerson;

    /**
     * 联系电话
     */
    private String contactPhone;

    /**
     * 备用联系电话
     */
    private String backupContactPhone;

    /**
     * 传真
     */
    private String fax;

    /**
     * 电子邮箱
     */
    private String email;

    /**
     * 项目介绍
     */
    private String projectIntro;

    /**
     * 公告文件下载链接
     */
    private String fileDownloadUrl;

    /**
     * 招标文件下载链接
     */
    private String biddingFileUrl;

    /**
     * 变更公告内容
     */
    private String changeContent;

    /**
     * 变更公告文件下载链接
     */
    private String changeFileUrl;

    /**
     * 发布时间/创建时间（列表页显示的时间）
     */
    private LocalDateTime publishTime;

    /**
     * 内容指纹（MD5），基于 projectCode + announcementType + projectName 计算
     * 用于唯一标识一条公告，支持同项目多条公告（如公告一、公告二）
     */
    private String contentHash;

    /**
     * 是否已推送 0-否 1-是
     */
    private Integer notified;

    /**
     * 数据抓取时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    /**
     * 原始HTML内容（用于调试）
     */
    @TableField(exist = false)
    private String rawHtml;

    /**
     * 网络元素
     */
    @TableField(exist = false)
    private WebElement webElement;
}
