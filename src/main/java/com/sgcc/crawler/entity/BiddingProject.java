package com.sgcc.crawler.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

/**
 * 招标项目实体类
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("bidding_project")
public class BiddingProject {

    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 项目编号
     */
    private String projectCode;

    /**
     * 项目名称
     */
    private String projectName;

    /**
     * 项目状态
     */
    private String projectStatus;

    /**
     * 截止时间
     */
    private LocalDateTime deadline;

    /**
     * 来源链接
     */
    private String sourceUrl;

    /**
     * 是否已推送 0-否 1-是
     */
    private Integer notified;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
