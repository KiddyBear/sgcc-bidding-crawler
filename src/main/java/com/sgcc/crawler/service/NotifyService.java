package com.sgcc.crawler.service;

import com.sgcc.crawler.entity.BiddingAnnouncement;
import com.sgcc.crawler.entity.BiddingProject;

import java.util.List;

/**
 * 通知推送服务接口
 */
public interface NotifyService {

    /**
     * 推送新项目通知
     */
    void notifyNewProject(BiddingProject project);

    /**
     * 推送项目状态变更通知
     */
    void notifyStatusChange(BiddingProject project, String newStatus);

    /**
     * 发送自定义消息
     */
    void sendMessage(String title, String content);

    /**
     * 推送新公告通知
     */
    void notifyNewAnnouncement(BiddingAnnouncement announcement);

    /**
     * 推送公告变更通知
     * @param changedFields 变更的字段描述列表
     */
    void notifyAnnouncementUpdate(BiddingAnnouncement announcement, List<String> changedFields);
}
