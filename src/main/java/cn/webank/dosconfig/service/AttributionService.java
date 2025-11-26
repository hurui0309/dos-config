package cn.webank.dosconfig.service;

import java.util.List;

import cn.webank.dosconfig.entity.attribution.dto.request.CreateTaskRequest;
import cn.webank.dosconfig.entity.attribution.dto.response.AiAttributionReportDTO;
import cn.webank.dosconfig.entity.attribution.dto.response.AttributionResultDTO;
import cn.webank.dosconfig.entity.attribution.dto.response.AttributionTreeBriefDTO;
import cn.webank.dosconfig.entity.attribution.dto.response.AttributionTreeConfigDTO;
import cn.webank.dosconfig.entity.attribution.dto.response.MetricBriefDTO;
import cn.webank.dosconfig.entity.attribution.dto.response.MetricTrendDTO;
import cn.webank.dosconfig.entity.attribution.dto.response.TaskCreateDTO;
import cn.webank.dosconfig.entity.attribution.dto.response.TaskListDTO;
import cn.webank.dosconfig.entity.attribution.dto.response.TaskStatusDTO;
import cn.webank.dosconfig.enums.DateGranularity;

/**
 * 归因分析服务接口
 */
public interface AttributionService {

    /**
     * 查询核心指标列表
     */
    List<MetricBriefDTO> getMetrics(String metricName, Integer limit);

    /**
     * 查询核心指标趋势
     */
    MetricTrendDTO getMetricTrend(String metricId, DateGranularity timeGranularity, 
                                   String baselineDate, String compareDate);

    /**
     * 查询归因树列表
     */
    List<AttributionTreeBriefDTO> getTrees(String treeName, Integer limit);

    /**
     * 查询归因树配置
     */
    AttributionTreeConfigDTO getTreeConfig(String treeId);

    /**
     * 创建分析任务
     */
    TaskCreateDTO createTask(CreateTaskRequest request, String creator);

    /**
     * 查询任务列表
     */
    TaskListDTO getTasks(String treeName, String creator, Integer pageNo, Integer pageSize);

    /**
     * 查询任务状态
     */
    TaskStatusDTO getTaskStatus(String taskId);

    /**
     * 查询归因分析结果
     */
    AttributionResultDTO getAttributionResult(String taskId);

    /**
     * 查询AI归因报告
     */
    AiAttributionReportDTO getAiReport(String taskId);
}

