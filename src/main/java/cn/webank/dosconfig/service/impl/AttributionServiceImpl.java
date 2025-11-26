package cn.webank.dosconfig.service.impl;

import cn.webank.dosconfig.dao.AiReportDao;
import cn.webank.dosconfig.dao.AnalysisTaskDao;
import cn.webank.dosconfig.dao.AttributionResultDao;
import cn.webank.dosconfig.dao.AttributionTreeDao;
import cn.webank.dosconfig.entity.attribution.AiReport;
import cn.webank.dosconfig.entity.attribution.AnalysisTask;
import cn.webank.dosconfig.entity.attribution.AttributionResult;
import cn.webank.dosconfig.entity.attribution.AttributionTree;
import cn.webank.dosconfig.entity.attribution.dto.request.CreateTaskRequest;
import cn.webank.dosconfig.entity.attribution.dto.response.AiAttributionReportDTO;
import cn.webank.dosconfig.entity.attribution.dto.response.AnalysisTaskDTO;
import cn.webank.dosconfig.entity.attribution.dto.response.AttributionResultDTO;
import cn.webank.dosconfig.entity.attribution.dto.response.AttributionTreeBriefDTO;
import cn.webank.dosconfig.entity.attribution.dto.response.AttributionTreeConfigDTO;
import cn.webank.dosconfig.entity.attribution.dto.response.AttributionTreeResultNodeDTO;
import cn.webank.dosconfig.entity.attribution.dto.response.ChartDataDTO;
import cn.webank.dosconfig.entity.attribution.dto.response.ChartDataDTO.SeriesDTO;
import cn.webank.dosconfig.entity.attribution.dto.response.ChartDataDTO.XAxisDTO;
import cn.webank.dosconfig.entity.attribution.dto.response.ChartDataDTO.YAxisDTO;
import cn.webank.dosconfig.entity.attribution.dto.response.MetricBriefDTO;
import cn.webank.dosconfig.entity.attribution.dto.response.MetricItemDTO;
import cn.webank.dosconfig.entity.attribution.dto.response.MetricTrendDTO;
import cn.webank.dosconfig.entity.attribution.dto.response.MetricTreeNodeDTO;
import cn.webank.dosconfig.entity.attribution.dto.response.TaskCreateDTO;
import cn.webank.dosconfig.entity.attribution.dto.response.TaskListDTO;
import cn.webank.dosconfig.entity.attribution.dto.response.TaskStatusDTO;
import cn.webank.dosconfig.enums.DateGranularity;
import cn.webank.dosconfig.enums.TaskStatus;
import cn.webank.dosconfig.exception.SystemException;
import cn.webank.dosconfig.service.AttributionService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 归因分析服务实现类
 */
@Service
public class AttributionServiceImpl implements AttributionService {

    private static final Logger LOG = LoggerFactory.getLogger(AttributionServiceImpl.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Autowired
    private AttributionTreeDao treeDao;

    @Autowired
    private AnalysisTaskDao taskDao;

    @Autowired
    private AttributionResultDao resultDao;

    @Autowired
    private AiReportDao reportDao;

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public List<MetricBriefDTO> getMetrics(String metricName, Integer limit) {
        int actualLimit = (limit != null && limit > 0 && limit <= 100) ? limit : 20;
        List<AttributionTree> trees = (metricName != null && !metricName.trim().isEmpty())
                ? treeDao.selectByMetricName(metricName, actualLimit)
                : treeDao.selectAll(actualLimit);

        return trees.stream()
                .map(t -> new MetricBriefDTO(
                        t.getMetricId(),
                        t.getMetricName(),
                        t.getTreeId(),
                        t.getTreeName()))
                .distinct()
                .collect(Collectors.toList());
    }

    @Override
    public MetricTrendDTO getMetricTrend(String metricId, DateGranularity timeGranularity,
                                         String baselineDate, String compareDate) {
        // 验证指标存在（通过归因树）
        List<AttributionTree> trees = treeDao.selectByMetricId(metricId, 1);
        if (trees == null || trees.isEmpty()) {
            throw new SystemException("指标不存在: " + metricId);
        }

        // TODO: 实现真实的指标趋势查询逻辑
        LOG.warn("指标趋势查询功能尚未实现，返回模拟数据。metricId={}, granularity={}, baselineDate={}, compareDate={}",
                metricId, timeGranularity, baselineDate, compareDate);

        XAxisDTO xAxis = new XAxisDTO("category",
                List.of("01-01", "01-02", "01-03", "01-04", "01-05", "01-06", "01-07"));
        YAxisDTO yAxis = new YAxisDTO("value");
        List<SeriesDTO> series = List.of(
                new SeriesDTO("基准期", "line", List.of(1200, 1150, 1180, 1210, 1190, 1220, 1230)),
                new SeriesDTO("对比期", "line", List.of(1500, 1480, 1510, 1490, 1520, 1540, 1560))
        );
        ChartDataDTO chartData = new ChartDataDTO(xAxis, yAxis, series);

        List<MetricItemDTO> metricItems = List.of(
                new MetricItemDTO("baselineAvg", "1197.43", "基准期均值", "blue", "基准期均值"),
                new MetricItemDTO("compareAvg", "1514.43", "对比期均值", "green", "对比期均值"),
                new MetricItemDTO("changeRate", "+26.49%", "变化率", "red", "对比期相较基准期的变化率"),
                new MetricItemDTO("deltaValue", "317.00", "波动量", "orange", "对比期均值 - 基准期均值")
        );

        return new MetricTrendDTO(chartData, metricItems);
    }

    @Override
    public List<AttributionTreeBriefDTO> getTrees(String treeName, Integer limit) {
        int actualLimit = (limit != null && limit > 0 && limit <= 100) ? limit : 20;
        List<AttributionTree> trees = (treeName != null && !treeName.trim().isEmpty())
                ? treeDao.selectByTreeName(treeName, actualLimit)
                : treeDao.selectAll(actualLimit);

        return trees.stream()
                .map(t -> new AttributionTreeBriefDTO(
                        t.getTreeId(),
                        t.getTreeName(),
                        t.getMetricId(),
                        t.getMetricName(),
                        t.getVersion()))
                .collect(Collectors.toList());
    }

    @Override
    public AttributionTreeConfigDTO getTreeConfig(String treeId) {
        AttributionTree tree = treeDao.selectByTreeId(treeId);
        if (tree == null) {
            throw new SystemException("归因树不存在: " + treeId);
        }

        try {
            MetricTreeNodeDTO root = objectMapper.readValue(tree.getTreeConfig(), MetricTreeNodeDTO.class);
            return new AttributionTreeConfigDTO(
                    tree.getTreeId(),
                    tree.getTreeName(),
                    tree.getMetricId(),
                    tree.getMetricName(),
                    tree.getVersion(),
                    root
            );
        } catch (JsonProcessingException e) {
            LOG.error("解析归因树配置失败: treeId={}", treeId, e);
            throw new SystemException("归因树配置格式错误");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TaskCreateDTO createTask(CreateTaskRequest request, String creator) {
        AttributionTree tree = treeDao.selectByTreeId(request.treeId());
        if (tree == null) {
            throw new SystemException("归因树不存在: " + request.treeId());
        }

        LocalDate baseline = LocalDate.parse(request.baselineDate(), DATE_FORMATTER);
        LocalDate compare = LocalDate.parse(request.compareDate(), DATE_FORMATTER);
        LocalDateTime now = LocalDateTime.now();

        AnalysisTask task = new AnalysisTask();
        task.setTaskId(generateTaskId());
        task.setTreeId(request.treeId());
        task.setTreeName(tree.getTreeName());
        task.setListId(request.listId());
        task.setListName(null);  // TODO: 根据 listId 查询名单名称
        task.setContributionThreshold(request.contributionThreshold());
        task.setTimeGranularity(request.timeGranularity());
        task.setBaselineDate(baseline);
        task.setCompareDate(compare);
        task.setStatus(TaskStatus.PENDING);
        task.setProgress(0);
        task.setCreator(creator);
        task.setCreateTime(now);
        task.setUpdateTime(now);

        taskDao.insert(task);

        // 异步执行归因分析
        executeAttributionAnalysisAsync(task.getTaskId());

        return new TaskCreateDTO(
                task.getTaskId(),
                task.getTimeGranularity(),
                formatDate(task.getBaselineDate()),
                formatDate(task.getCompareDate())
        );
    }

    @Override
    public TaskListDTO getTasks(String treeName, String creator, Integer pageNo, Integer pageSize) {
        int actualPageNo = (pageNo != null && pageNo > 0) ? pageNo : 1;
        int actualPageSize = (pageSize != null && pageSize > 0 && pageSize <= 100) ? pageSize : 20;

        int offset = (actualPageNo - 1) * actualPageSize;

        List<AnalysisTask> tasks = taskDao.selectByConditions(treeName, creator, offset, actualPageSize);
        int total = taskDao.countByConditions(treeName, creator);

        List<AnalysisTaskDTO> list = tasks.stream()
                .map(this::convertToTaskDTO)
                .collect(Collectors.toList());

        return new TaskListDTO(total, actualPageNo, actualPageSize, list);
    }

    @Override
    public TaskStatusDTO getTaskStatus(String taskId) {
        AnalysisTask task = taskDao.selectByTaskId(taskId);
        if (task == null) {
            throw new SystemException("任务不存在: " + taskId);
        }

        return new TaskStatusDTO(
                task.getTaskId(),
                task.getStatus(),
                task.getProgress(),
                task.getMessage(),
                formatDateTime(task.getCreateTime()),
                formatDateTime(task.getStartTime()),
                formatDateTime(task.getEndTime())
        );
    }

    @Override
    public AttributionResultDTO getAttributionResult(String taskId) {
        AnalysisTask task = taskDao.selectByTaskId(taskId);
        if (task == null) {
            throw new SystemException("任务不存在: " + taskId);
        }

        if (task.getStatus() != TaskStatus.SUCCESS) {
            throw new SystemException("任务尚未完成，无法获取结果");
        }

        AttributionResult result = resultDao.selectByTaskId(taskId);
        if (result == null) {
            throw new SystemException("归因结果不存在: " + taskId);
        }

        try {
            AttributionTreeResultNodeDTO resultTree = objectMapper.readValue(
                    result.getResultTree(),
                    AttributionTreeResultNodeDTO.class
            );

            return new AttributionResultDTO(
                    task.getTaskId(),
                    task.getTreeId(),
                    task.getTreeName(),
                    task.getTimeGranularity(),
                    formatDate(task.getBaselineDate()),
                    formatDate(task.getCompareDate()),
                    resultTree
            );
        } catch (JsonProcessingException e) {
            LOG.error("解析归因结果失败: taskId={}", taskId, e);
            throw new SystemException("归因结果格式错误");
        }
    }

    @Override
    public AiAttributionReportDTO getAiReport(String taskId) {
        AnalysisTask task = taskDao.selectByTaskId(taskId);
        if (task == null) {
            throw new SystemException("任务不存在: " + taskId);
        }

        AiReport report = reportDao.selectByTaskId(taskId);
        if (report == null) {
            throw new SystemException("AI报告不存在: " + taskId);
        }

        return new AiAttributionReportDTO(
                report.getTaskId(),
                report.getReportStatus(),
                report.getReportContent(),
                formatDateTime(report.getGenerateTime())
        );
    }

    /**
     * 异步执行归因分析
     */
    @Async
    public void executeAttributionAnalysisAsync(String taskId) {
        LOG.info("开始执行归因分析任务: taskId={}", taskId);

        try {
            AnalysisTask task = taskDao.selectByTaskId(taskId);
            if (task == null) {
                throw new SystemException("任务不存在: " + taskId);
            }

            // 更新任务状态为执行中
            task.setStatus(TaskStatus.RUNNING);
            task.setProgress(0);
            task.setStartTime(LocalDateTime.now());
            task.setMessage("开始执行归因分析");
            task.setUpdateTime(LocalDateTime.now());
            taskDao.updateByTaskId(task);

            // TODO: 实现具体的归因分析逻辑
            // 1. 从数据库查询指标数据
            // 2. 执行Adtributor算法计算
            // 3. 生成归因树结果
            // 4. 调用AI生成分析报告
            // 5. 保存结果到数据库

            LOG.warn("归因分析核心算法尚未实现，任务将标记为待补充。taskId={}", taskId);

            // 暂时标记为失败状态，等待后续实现
            task.setStatus(TaskStatus.FAILED);
            task.setProgress(0);
            task.setEndTime(LocalDateTime.now());
            task.setMessage("归因分析算法待实现");
            task.setUpdateTime(LocalDateTime.now());
            taskDao.updateByTaskId(task);

        } catch (Exception e) {
            LOG.error("执行归因分析任务失败: taskId={}", taskId, e);

            AnalysisTask task = taskDao.selectByTaskId(taskId);
            if (task != null) {
                task.setStatus(TaskStatus.FAILED);
                task.setProgress(0);
                task.setMessage("执行失败: " + e.getMessage());
                task.setEndTime(LocalDateTime.now());
                task.setUpdateTime(LocalDateTime.now());
                taskDao.updateByTaskId(task);
            }
        }
    }

    /**
     * 生成任务ID
     */
    private String generateTaskId() {
        return "TASK" + System.currentTimeMillis() + UUID.randomUUID().toString().substring(0, 4).toUpperCase();
    }

    /**
     * 转换任务实体为DTO
     */
    private AnalysisTaskDTO convertToTaskDTO(AnalysisTask task) {
        return new AnalysisTaskDTO(
                task.getTaskId(),
                task.getTreeId(),
                task.getTreeName(),
                task.getListId(),
                task.getListName(),
                task.getContributionThreshold(),
                task.getTimeGranularity(),
                formatDate(task.getBaselineDate()),
                formatDate(task.getCompareDate()),
                task.getStatus(),
                task.getProgress(),
                task.getMessage(),
                task.getCreator(),
                formatDateTime(task.getCreateTime()),
                formatDateTime(task.getStartTime()),
                formatDateTime(task.getEndTime())
        );
    }

    /**
     * 格式化日期
     */
    private String formatDate(LocalDate date) {
        return date != null ? date.format(DATE_FORMATTER) : null;
    }

    /**
     * 格式化日期时间
     */
    private String formatDateTime(LocalDateTime dateTime) {
        return dateTime != null ? dateTime.format(DATETIME_FORMATTER) : null;
    }
}
