package com.sgcc.crawler.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sgcc.crawler.entity.BiddingProject;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 招标项目Mapper接口
 */
@Mapper
public interface BiddingProjectMapper extends BaseMapper<BiddingProject> {

    /**
     * 根据项目编号查询
     */
    @Select("SELECT * FROM bidding_project WHERE project_code = #{projectCode}")
    BiddingProject selectByProjectCode(@Param("projectCode") String projectCode);

    /**
     * 查询未推送的项目
     */
    @Select("SELECT * FROM bidding_project WHERE notified = 0")
    List<BiddingProject> selectUnnotified();
}
