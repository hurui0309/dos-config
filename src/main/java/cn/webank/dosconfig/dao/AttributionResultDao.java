package cn.webank.dosconfig.dao;

import cn.webank.dosconfig.entity.attribution.AttributionResult;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 归因结果DAO接口
 */
@Mapper
public interface AttributionResultDao {

    /**
     * 根据任务ID查询
     * @param taskId 任务ID
     * @return 归因结果对象
     */
    AttributionResult selectByTaskId(@Param("taskId") String taskId);

    /**
     * 插入归因结果
     * @param result 归因结果对象
     * @return 影响行数
     */
    int insert(AttributionResult result);

    /**
     * 更新归因结果
     * @param result 归因结果对象
     * @return 影响行数
     */
    int updateByTaskId(AttributionResult result);

    /**
     * 删除归因结果
     * @param taskId 任务ID
     * @return 影响行数
     */
    int deleteByTaskId(@Param("taskId") String taskId);
}

