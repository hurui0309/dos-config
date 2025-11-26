package cn.webank.dosconfig.controller;

import java.util.List;

import cn.webank.dosconfig.entity.attribution.dto.request.CreateTaskRequest;
import cn.webank.dosconfig.enums.DateGranularity;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import cn.webank.dosconfig.entity.BaseResponse;
import cn.webank.dosconfig.entity.attribution.dto.response.AiAttributionReportDTO;
import cn.webank.dosconfig.entity.attribution.dto.response.AttributionResultDTO;
import cn.webank.dosconfig.entity.attribution.dto.response.AttributionTreeBriefDTO;
import cn.webank.dosconfig.entity.attribution.dto.response.AttributionTreeConfigDTO;
import cn.webank.dosconfig.entity.attribution.dto.response.MetricBriefDTO;
import cn.webank.dosconfig.entity.attribution.dto.response.MetricTrendDTO;
import cn.webank.dosconfig.entity.attribution.dto.response.TaskCreateDTO;
import cn.webank.dosconfig.entity.attribution.dto.response.TaskListDTO;
import cn.webank.dosconfig.entity.attribution.dto.response.TaskStatusDTO;
import cn.webank.dosconfig.service.AttributionService;
import com.google.common.base.Preconditions;

/**
 * 归因分析Controller
 */
@RestController
@RequestMapping("/attribution")
public class AttributionController {

    private static final Logger LOG = LoggerFactory.getLogger(AttributionController.class);

    @Autowired
    private AttributionService attributionService;

    /**
     * 1) 核心指标查询接口
     * GET /attribution/metrics
     */
    @GetMapping("/metrics")
    public BaseResponse<List<MetricBriefDTO>> getMetrics(
            @RequestParam(required = false) String metricName,
            @RequestParam(required = false, defaultValue = "20") Integer limit) {
        // 参数校验
        Preconditions.checkArgument(limit != null && limit > 0 && limit <= 100, 
                "limit参数必须在1-100之间");
        
        LOG.debug("查询核心指标列表: metricName={}, limit={}", metricName, limit);
        List<MetricBriefDTO> metrics = attributionService.getMetrics(metricName, limit);
        return BaseResponse.ok(metrics);
    }

    /**
     * 2) 核心指标趋势查询
     * GET /attribution/metrics/{metricId}/trend
     */
    @GetMapping("/metrics/{metricId}/trend")
    public BaseResponse<MetricTrendDTO> getMetricTrend(
            @PathVariable String metricId,
            @RequestParam DateGranularity timeGranularity,
            @RequestParam String baselineDate,
            @RequestParam String compareDate) {
        // 参数校验
        Preconditions.checkArgument(StringUtils.isNotBlank(metricId), "指标ID不能为空");
        Preconditions.checkArgument(timeGranularity != null, "时间粒度不能为空");
        Preconditions.checkArgument(StringUtils.isNotBlank(baselineDate), "基准日期不能为空");
        Preconditions.checkArgument(StringUtils.isNotBlank(compareDate), "对比日期不能为空");
        Preconditions.checkArgument(baselineDate.matches("\\d{4}-\\d{2}-\\d{2}"), 
                "基准日期格式必须为yyyy-MM-dd");
        Preconditions.checkArgument(compareDate.matches("\\d{4}-\\d{2}-\\d{2}"), 
                "对比日期格式必须为yyyy-MM-dd");
        
        LOG.debug("查询核心指标趋势: metricId={}, timeGranularity={}", metricId, timeGranularity);
        MetricTrendDTO trend = attributionService.getMetricTrend(metricId, timeGranularity, baselineDate, compareDate);
        return BaseResponse.ok(trend);
    }

    /**
     * 3) 归因树查询接口
     * GET /attribution/trees
     */
    @GetMapping("/trees")
    public BaseResponse<List<AttributionTreeBriefDTO>> getTrees(
            @RequestParam(required = false) String treeName,
            @RequestParam(required = false, defaultValue = "20") Integer limit) {
        // 参数校验
        Preconditions.checkArgument(limit != null && limit > 0 && limit <= 100, 
                "limit参数必须在1-100之间");
        
        LOG.debug("查询归因树列表: treeName={}, limit={}", treeName, limit);
        List<AttributionTreeBriefDTO> trees = attributionService.getTrees(treeName, limit);
        return BaseResponse.ok(trees);
    }

    /**
     * 4) 归因树配置查询接口
     * GET /attribution/trees/{treeId}/config
     */
    @GetMapping("/trees/{treeId}/config")
    public BaseResponse<AttributionTreeConfigDTO> getTreeConfig(
            @PathVariable String treeId) {
        // 参数校验
        Preconditions.checkArgument(StringUtils.isNotBlank(treeId), "归因树ID不能为空");
        
        LOG.debug("查询归因树配置: treeId={}", treeId);
        AttributionTreeConfigDTO config = attributionService.getTreeConfig(treeId);
        return BaseResponse.ok(config);
    }

    /**
     * 5) 发起分析任务
     * POST /attribution/tasks
     */
    @PostMapping("/tasks")
    public BaseResponse<TaskCreateDTO> createTask(
            @RequestBody CreateTaskRequest request,
            @RequestHeader(value = "X-User-Name", required = false, defaultValue = "system") String creator) {
        LOG.info("创建分析任务: treeId={}, creator={}", request.treeId(), creator);
        TaskCreateDTO result = attributionService.createTask(request, creator);
        return BaseResponse.ok(result);
    }

    /**
     * 6) 查询分析任务列表
     * GET /attribution/tasks
     */
    @GetMapping("/tasks")
    public BaseResponse<TaskListDTO> getTasks(
            @RequestParam(required = false) String treeName,
            @RequestParam(required = false) String creator,
            @RequestParam(required = false, defaultValue = "1") Integer pageNo,
            @RequestParam(required = false, defaultValue = "20") Integer pageSize) {
        // 参数校验
        Preconditions.checkArgument(pageNo != null && pageNo >= 1, "页码必须大于等于1");
        Preconditions.checkArgument(pageSize != null && pageSize > 0 && pageSize <= 100, 
                "每页条数必须在1-100之间");
        
        LOG.debug("查询任务列表: treeName={}, creator={}, pageNo={}, pageSize={}",
                treeName, creator, pageNo, pageSize);
        TaskListDTO tasks = attributionService.getTasks(treeName, creator, pageNo, pageSize);
        return BaseResponse.ok(tasks);
    }

    /**
     * 7) 查询任务状态
     * GET /attribution/tasks/{taskId}/status
     */
    @GetMapping("/tasks/{taskId}/status")
    public BaseResponse<TaskStatusDTO> getTaskStatus(
            @PathVariable String taskId) {
        // 参数校验
        Preconditions.checkArgument(StringUtils.isNotBlank(taskId), "任务ID不能为空");
        
        LOG.debug("查询任务状态: taskId={}", taskId);
        TaskStatusDTO status = attributionService.getTaskStatus(taskId);
        return BaseResponse.ok(status);
    }

    /**
     * 8) 查询归因树分析结果
     * GET /attribution/tasks/{taskId}/result
     */
    @GetMapping("/tasks/{taskId}/result")
    public BaseResponse<AttributionResultDTO> getAttributionResult(
            @PathVariable String taskId) {
        // 参数校验
        Preconditions.checkArgument(StringUtils.isNotBlank(taskId), "任务ID不能为空");
        
        LOG.debug("查询归因分析结果: taskId={}", taskId);
        AttributionResultDTO result = attributionService.getAttributionResult(taskId);
        return BaseResponse.ok(result);
    }

    /**
     * 9) 查询AI归因报告
     * GET /attribution/tasks/{taskId}/ai-report
     */
    @GetMapping("/tasks/{taskId}/ai-report")
    public BaseResponse<AiAttributionReportDTO> getAiReport(
            @PathVariable String taskId) {
        // 参数校验
        Preconditions.checkArgument(StringUtils.isNotBlank(taskId), "任务ID不能为空");
        
        LOG.debug("查询AI归因报告: taskId={}", taskId);
        AiAttributionReportDTO report = attributionService.getAiReport(taskId);
        return BaseResponse.ok(report);
    }
}

