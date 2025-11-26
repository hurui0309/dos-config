package cn.webank.dosconfig.dao;

import cn.webank.dosconfig.entity.attribution.AnalysisTask;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 分析任务DAO接口

 */
@Mapper
public interface AnalysisTaskDao {

    /**
     * 根据任务ID查询
     * @param taskId 任务ID
     * @return 任务对象
     */
    AnalysisTask selectByTaskId(@Param("taskId") String taskId);

    /**
     * 条件查询任务列表（分页）
     * @param treeName 归因树名称（模糊匹配）
     * @param creator 创建人
     * @param offset 偏移量
     * @param limit 查询条数限制
     * @return 任务列表
     */
    List<AnalysisTask> selectByConditions(
            @Param("treeName") String treeName,
            @Param("creator") String creator,
            @Param("offset") Integer offset,
            @Param("limit") Integer limit
    );

    /**
     * 统计任务总数
     * @param treeName 归因树名称（模糊匹配）
     * @param creator 创建人
     * @return 任务总数
     */
    int countByConditions(
            @Param("treeName") String treeName,
            @Param("creator") String creator
    );

    /**
     * 插入任务
     * @param task 任务对象
     * @return 影响行数
     */
    int insert(AnalysisTask task);

    /**
     * 更新任务
     * @param task 任务对象
     * @return 影响行数
     */
    int updateByTaskId(AnalysisTask task);

    /**
     * 更新任务状态
     * @param taskId 任务ID
     * @param status 任务状态
     * @param progress 进度
     * @return 影响行数
     */
    int updateStatus(
            @Param("taskId") String taskId,
            @Param("status") String status,
            @Param("progress") Integer progress
    );
}

