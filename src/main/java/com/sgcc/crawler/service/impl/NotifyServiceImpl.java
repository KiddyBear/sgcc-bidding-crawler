package com.sgcc.crawler.service.impl;

import com.sgcc.crawler.config.DingTalkConfig;
import com.sgcc.crawler.entity.AnnouncementType;
import com.sgcc.crawler.entity.BiddingAnnouncement;
import com.sgcc.crawler.entity.BiddingProject;
import com.sgcc.crawler.mapper.BiddingProjectMapper;
import com.sgcc.crawler.service.NotifyService;
import com.sgcc.crawler.util.DingTalkUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 钉钉通知推送服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotifyServiceImpl implements NotifyService {

    private final DingTalkConfig dingTalkConfig;
    private final BiddingProjectMapper biddingProjectMapper;

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @Override
    public void notifyNewProject(BiddingProject project) {
        if (!dingTalkConfig.isEnabled()) {
            log.info("钉钉推送已禁用");
            return;
        }

        String title = "新招标项目";
        StringBuilder content = new StringBuilder();
        content.append("### 新招标项目通知\n\n");
        content.append("**项目名称**: ").append(project.getProjectName()).append("\n\n");
        content.append("**项目编号**: ").append(project.getProjectCode()).append("\n\n");
        content.append("**项目状态**: ").append(project.getProjectStatus()).append("\n\n");

        if (project.getDeadline() != null) {
            content.append("**截止时间**: ").append(project.getDeadline().format(FORMATTER)).append("\n\n");
        }

        if (project.getSourceUrl() != null && !project.getSourceUrl().isEmpty()) {
            content.append("[查看详情](").append(project.getSourceUrl()).append(")");
        }

        boolean success = DingTalkUtil.sendMarkdown(
                dingTalkConfig.getWebhook(),
                dingTalkConfig.getSecret(),
                title,
                content.toString()
        );

        if (success) {
            // 更新推送状态
            project.setNotified(1);
            biddingProjectMapper.updateById(project);
        }
    }

    @Override
    public void notifyStatusChange(BiddingProject project, String newStatus) {
        if (!dingTalkConfig.isEnabled()) {
            log.info("钉钉推送已禁用");
            return;
        }

        String title = "项目状态变更";
        StringBuilder content = new StringBuilder();
        content.append("### 项目状态变更通知\n\n");
        content.append("**项目名称**: ").append(project.getProjectName()).append("\n\n");
        content.append("**项目编号**: ").append(project.getProjectCode()).append("\n\n");
        content.append("**原状态**: ").append(project.getProjectStatus()).append("\n\n");
        content.append("**新状态**: ").append(newStatus).append("\n\n");

        if (project.getSourceUrl() != null && !project.getSourceUrl().isEmpty()) {
            content.append("[查看详情](").append(project.getSourceUrl()).append(")");
        }

        DingTalkUtil.sendMarkdown(
                dingTalkConfig.getWebhook(),
                dingTalkConfig.getSecret(),
                title,
                content.toString()
        );
    }

    @Override
    public void sendMessage(String title, String content) {
        if (!dingTalkConfig.isEnabled()) {
            log.info("钉钉推送已禁用");
            return;
        }

        DingTalkUtil.sendMarkdown(
                dingTalkConfig.getWebhook(),
                dingTalkConfig.getSecret(),
                title,
                content
        );
    }

    @Override
    public void notifyNewAnnouncement(BiddingAnnouncement announcement) {
        if (!dingTalkConfig.isEnabled()) {
            log.info("钉钉推送已禁用，跳过新公告推送");
            return;
        }

        String typeName = resolveTypeName(announcement);
        String title = "新公告: " + typeName;
        StringBuilder content = new StringBuilder();
        content.append("### 新招标公告\n\n");
        content.append("**公告类型**: ").append(typeName).append("\n\n");
        content.append("**项目名称**: ").append(safe(announcement.getProjectName())).append("\n\n");
        content.append("**项目编号**: ").append(safe(announcement.getProjectCode())).append("\n\n");
        content.append("**项目状态**: ").append(safe(announcement.getProjectStatus())).append("\n\n");
        if (announcement.getBidOpenTime() != null) {
            content.append("**开标时间**: ").append(announcement.getBidOpenTime().format(FORMATTER)).append("\n\n");
        }
        if (announcement.getFileDeadline() != null) {
            content.append("**文件截止**: ").append(announcement.getFileDeadline().format(FORMATTER)).append("\n\n");
        }
        if (announcement.getDetailUrl() != null) {
            content.append("[查看详情](").append(announcement.getDetailUrl()).append(")");
        }

        boolean success = DingTalkUtil.sendMarkdown(
                dingTalkConfig.getWebhook(), dingTalkConfig.getSecret(), title, content.toString());
        if (success) {
            log.info("新公告推送成功: {} - {}", announcement.getProjectCode(), announcement.getProjectName());
        }
    }

    @Override
    public void notifyAnnouncementUpdate(BiddingAnnouncement announcement, List<String> changedFields) {
        if (!dingTalkConfig.isEnabled()) {
            log.info("钉钉推送已禁用，跳过公告变更推送");
            return;
        }

        String typeName = resolveTypeName(announcement);
        String title = "公告变更: " + typeName;
        StringBuilder content = new StringBuilder();
        content.append("### 招标公告变更\n\n");
        content.append("**公告类型**: ").append(typeName).append("\n\n");
        content.append("**项目名称**: ").append(safe(announcement.getProjectName())).append("\n\n");
        content.append("**项目编号**: ").append(safe(announcement.getProjectCode())).append("\n\n");
        content.append("**变更内容**: ").append(String.join("、", changedFields)).append("\n\n");
        if (announcement.getBidOpenTime() != null) {
            content.append("**开标时间**: ").append(announcement.getBidOpenTime().format(FORMATTER)).append("\n\n");
        }
        if (announcement.getDetailUrl() != null) {
            content.append("[查看详情](").append(announcement.getDetailUrl()).append(")");
        }

        boolean success = DingTalkUtil.sendMarkdown(
                dingTalkConfig.getWebhook(), dingTalkConfig.getSecret(), title, content.toString());
        if (success) {
            log.info("公告变更推送成功: {} - {}, 变更: {}",
                    announcement.getProjectCode(), announcement.getProjectName(), changedFields);
        }
    }

    private String resolveTypeName(BiddingAnnouncement announcement) {
        try {
            return AnnouncementType.valueOf(announcement.getAnnouncementType()).getDisplayName();
        } catch (Exception e) {
            return announcement.getAnnouncementType();
        }
    }

    private String safe(String value) {
        return value != null ? value : "-";
    }
}
