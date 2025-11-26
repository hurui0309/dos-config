package cn.webank.dosconfig.dao;

import cn.webank.dosconfig.entity.attribution.AiReport;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * AI报告DAO接口
 */
@Mapper
public interface AiReportDao {

    /**
     * 根据任务ID查询
     * @param taskId 任务ID
     * @return AI报告对象
     */
    AiReport selectByTaskId(@Param("taskId") String taskId);

    /**
     * 插入AI报告
     * @param report AI报告对象
     * @return 影响行数
     */
    int insert(AiReport report);

    /**
     * 更新AI报告
     * @param report AI报告对象
     * @return 影响行数
     */
    int updateByTaskId(AiReport report);

    /**
     * 删除AI报告
     * @param taskId 任务ID
     * @return 影响行数
     */
    int deleteByTaskId(@Param("taskId") String taskId);
}

