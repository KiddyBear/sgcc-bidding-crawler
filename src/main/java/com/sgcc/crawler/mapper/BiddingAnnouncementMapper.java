package com.sgcc.crawler.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sgcc.crawler.entity.BiddingAnnouncement;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 招标公告Mapper接口
 */
@Mapper
public interface BiddingAnnouncementMapper extends BaseMapper<BiddingAnnouncement> {

    /**
     * 根据项目编号查询
     */
    @Select("SELECT * FROM bidding_announcement WHERE project_code = #{projectCode}")
    BiddingAnnouncement selectByProjectCode(@Param("projectCode") String projectCode);

    /**
     * 根据项目编号和公告类型查询
     */
    @Select("SELECT * FROM bidding_announcement WHERE project_code = #{projectCode} AND announcement_type = #{type}")
    BiddingAnnouncement selectByProjectCodeAndType(@Param("projectCode") String projectCode, @Param("type") String type);

    /**
     * 根据内容指纹查询（用于去重和变更检测）
     */
    @Select("SELECT * FROM bidding_announcement WHERE content_hash = #{contentHash}")
    BiddingAnnouncement selectByContentHash(@Param("contentHash") String contentHash);

    /**
     * 查询未推送的公告
     */
    @Select("SELECT * FROM bidding_announcement WHERE notified = 0")
    List<BiddingAnnouncement> selectUnnotified();

    /**
     * 根据公告类型查询
     */
    @Select("SELECT * FROM bidding_announcement WHERE announcement_type = #{type} ORDER BY publish_time DESC")
    List<BiddingAnnouncement> selectByType(@Param("type") String type);
}
