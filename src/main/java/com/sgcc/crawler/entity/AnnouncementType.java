package com.sgcc.crawler.entity;

import lombok.Getter;

/**
 * 公告类型枚举
 */
@Getter
public enum AnnouncementType {

    PREQUALIFICATION("资格预审公告", "zgysgg", 1),
    BIDDING_ANNOUNCEMENT("招标公告及投标邀请书", "zbgg", 2),
    PROCUREMENT("采购公告", "cggg", 3),
    CANDIDATE_PUBLICITY("推荐中标候选人公示", "tjzbhxrgs", 4),
    RESULT_ANNOUNCEMENT("中标（成交）结果公告", "zbjggg", 5);

    /**
     * 显示名称
     */
    private final String displayName;

    /**
     * URL标识
     */
    private final String urlKey;

    /**
     * Tab索引（从1开始）
     */
    private final int tabIndex;

    AnnouncementType(String displayName, String urlKey, int tabIndex) {
        this.displayName = displayName;
        this.urlKey = urlKey;
        this.tabIndex = tabIndex;
    }

    /**
     * 根据显示名称获取枚举
     */
    public static AnnouncementType fromDisplayName(String displayName) {
        for (AnnouncementType type : values()) {
            if (type.displayName.equals(displayName)) {
                return type;
            }
        }
        return null;
    }

    /**
     * 根据Tab索引获取枚举
     */
    public static AnnouncementType fromTabIndex(int index) {
        for (AnnouncementType type : values()) {
            if (type.tabIndex == index) {
                return type;
            }
        }
        return null;
    }
}
