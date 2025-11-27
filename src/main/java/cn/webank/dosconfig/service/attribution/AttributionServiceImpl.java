package cn.webank.dosconfig.service.attribution;

import cn.webank.dosconfig.dao.AiReportDao;
import cn.webank.dosconfig.dao.AnalysisTaskDao;
import cn.webank.dosconfig.dao.AttributionResultDao;
import cn.webank.dosconfig.dao.AttributionTreeDao;
import cn.webank.dosconfig.entity.FieldOrder;
import cn.webank.dosconfig.entity.TimeDimension;
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
import cn.webank.dosconfig.entity.attribution.dto.response.DimensionAttributionItemDTO;
import cn.webank.dosconfig.entity.attribution.dto.response.MetricBriefDTO;
import cn.webank.dosconfig.entity.attribution.dto.response.MetricItemDTO;
import cn.webank.dosconfig.entity.attribution.dto.response.MetricPointDTO;
import cn.webank.dosconfig.entity.attribution.dto.response.MetricTrendDTO;
import cn.webank.dosconfig.entity.attribution.dto.response.MetricTreeNodeDTO;
import cn.webank.dosconfig.entity.attribution.dto.response.TaskCreateDTO;
import cn.webank.dosconfig.entity.attribution.dto.response.TaskListDTO;
import cn.webank.dosconfig.entity.attribution.dto.response.TaskStatusDTO;
import cn.webank.dosconfig.entity.rmb.Req_04302590_01;
import cn.webank.dosconfig.enums.DateGranularity;
import cn.webank.dosconfig.enums.TaskStatus;
import cn.webank.dosconfig.exception.SystemException;
import cn.webank.dosconfig.service.AttributionService;
import cn.webank.dosconfig.service.MetricService;
import cn.webank.weup.base.util.JSONUtil;
import cn.webank.weup.biz.rmb.WeupRmbUtil;
import cn.webank.weup.biz.threadpool.WeupThreadPoolTaskExecutor;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Objects;
import java.util.Optional;
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
    private static final int TREND_WINDOW_SIZE = 90;
    private static final Map<DateGranularity, String> TIME_DIMENSION_MAPPING = Map.of(
            DateGranularity.DAY, "dim_calendar_a.fmt_date",
            DateGranularity.WEEK, "dim_calendar_a.week_begin_date",
            DateGranularity.MONTH, "dim_calendar_a.month_begin_date"
    );

    @Value("${attribution.epsilon:0.000001}")
    private BigDecimal epsilon;

    @Value("${attribution.metric-query.limit:1000}")
    private int metricQueryLimit;

    @Value("${attribution.dimension.top-limit:1000}")
    private int dimensionTopLimit;

    @Value("${attribution.dimension.max-result-size:20}")
    private int dimensionMaxResult;

    @Value("${attribution.dimension.ep-threshold:0.1}")
    private BigDecimal dimensionEpThreshold;

    @Value("${attribution.dimension.ep-total-threshold:0.67}")
    private BigDecimal dimensionEpTotalThreshold;
    @Autowired
    private AttributionTreeDao treeDao;

    @Autowired
    private AnalysisTaskDao taskDao;

    @Autowired
    private AttributionResultDao resultDao;

    @Autowired
    private AiReportDao reportDao;

    @Autowired
    private MetricService metricService;

    @Autowired
    @Qualifier("TPAsync")
    private WeupThreadPoolTaskExecutor taskExecutor;

    private NodeMetricComputationEngine createNodeMetricComputationEngine() {
        return new NodeMetricComputationEngine(epsilon);
    }

    private DimensionAttributionEngine createDimensionAttributionEngine() {
        return new DimensionAttributionEngine(
                epsilon,
                dimensionMaxResult,
                dimensionEpThreshold,
                dimensionEpTotalThreshold
        );
    }

    /**
     * 查询核心指标列表（支持按名称模糊匹配）。
     *
     * @param metricName 指标名称关键字，可为空
     * @param limit      返回条数上限
     * @return 指标简要信息列表
     */
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

    /**
     * 查询指定指标在“基准期-对比期”内的近 90 日趋势，并返回核心对比指标。
     */
    @Override
    public MetricTrendDTO getMetricTrend(String metricId, DateGranularity timeGranularity,
                                         String baselineDate, String compareDate) {
        LOG.info("指标趋势查询: metricId={}, granularity={}, baseline={}, compare={}",
                metricId, timeGranularity, baselineDate, compareDate);
        List<AttributionTree> trees = treeDao.selectByMetricId(metricId, 1);
        if (trees == null || trees.isEmpty()) {
            throw new SystemException("指标不存在: " + metricId);
        }

        LocalDate compareEnd = LocalDate.parse(compareDate, DATE_FORMATTER);
        LocalDate compareStart = compareEnd.minusDays(TREND_WINDOW_SIZE - 1);

        List<MetricPointDTO> trendSeries = queryTrendSeries(metricId, compareStart, compareEnd, timeGranularity);
        Map<String, BigDecimal> trendMap = trendSeries.stream()
                .collect(Collectors.toMap(MetricPointDTO::date, MetricPointDTO::value, BigDecimal::add));

        List<String> categories = buildContinuousDateRange(compareStart, compareEnd);
        List<Object> seriesData = categories.stream()
                .map(date -> Optional.ofNullable(trendMap.get(date)).orElse(BigDecimal.ZERO))
                .collect(Collectors.toList());

        ChartDataDTO chartData = new ChartDataDTO(
                new XAxisDTO("category", categories),
                new YAxisDTO("value"),
                List.of(new SeriesDTO("指标值", "line", seriesData))
        );

        BigDecimal baselineValue = Optional.ofNullable(trendMap.get(baselineDate)).orElse(BigDecimal.ZERO);
        BigDecimal compareValue = Optional.ofNullable(trendMap.get(compareDate)).orElse(BigDecimal.ZERO);
        BigDecimal deltaValue = compareValue.subtract(baselineValue);
        BigDecimal changeRate = safeDivide(deltaValue, baselineValue.abs().max(epsilon));
        LOG.info("指标趋势结果: metricId={}, baselineValue={}, compareValue={}, delta={}",
                metricId, baselineValue, compareValue, deltaValue);

        List<MetricItemDTO> metricItems = List.of(
                new MetricItemDTO("baselineValue", formatDecimal(baselineValue), "基准期指标值", "blue", "基准期 " + baselineDate + " 指标值"),
                new MetricItemDTO("compareValue", formatDecimal(compareValue), "对比期指标值", "green", "对比期 " + compareDate + " 指标值"),
                new MetricItemDTO("deltaValue", formatDecimal(deltaValue), "波动量", "orange", "对比期指标值 - 基准期指标值"),
                new MetricItemDTO("changeRate", formatPercent(changeRate), "变化率", "red", "对比期相较基准期的变化率")
        );

        return new MetricTrendDTO(chartData, metricItems);
    }

    /**
     * 查询归因树列表。
     */
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

    /**
     * 查询归因树完整配置。
     */
    @Override
    public AttributionTreeConfigDTO getTreeConfig(String treeId) {
        AttributionTree tree = treeDao.selectByTreeId(treeId);
        if (tree == null) {
            throw new SystemException("归因树不存在: " + treeId);
        }

        MetricTreeNodeDTO root = parseJson(tree.getTreeConfig(), MetricTreeNodeDTO.class, "归因树配置格式错误");
            return new AttributionTreeConfigDTO(
                    tree.getTreeId(),
                    tree.getTreeName(),
                    tree.getMetricId(),
                    tree.getMetricName(),
                    tree.getVersion(),
                    root
            );
    }

    /**
     * 创建归因分析任务：校验归因树、初始化任务实体并提交异步执行。
     */
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
        task.setListName(null);
        task.setContributionThreshold(request.contributionThreshold());
        task.setTimeGranularity(request.timeGranularity());
        task.setBaselineDate(baseline);
        task.setCompareDate(compare);
        task.setStatus(TaskStatus.PENDING);
        task.setProgress(0);
        task.setCreator(creator);
        task.setCreateTime(now);
        task.setUpdateTime(now);
        task.setMessage("等待执行");

        taskDao.insert(task);
        submitTask(task.getTaskId());

        return new TaskCreateDTO(
                task.getTaskId(),
                task.getTimeGranularity(),
                formatDate(task.getBaselineDate()),
                formatDate(task.getCompareDate())
        );
    }

    /**
     * 分页查询任务列表，可按归因树名称/创建人过滤。
     */
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

    /**
     * 查询单个任务的状态信息（进度、阶段、时间戳）。
     */
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

    /**
     * 查询归因分析结果（仅成功任务可查询）。
     */
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

        AttributionTreeResultNodeDTO resultTree = parseJson(
                    result.getResultTree(),
                AttributionTreeResultNodeDTO.class,
                "归因结果格式错误"
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
    }

    /**
     * 查询归因报告（AI 文本）结果。
     */
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

    private void submitTask(String taskId) {
        WeupRmbUtil.asyncRunWithContext(taskExecutor, () -> runAttributionTask(taskId));
    }

    /**
     * 实际执行异步归因任务。
     * 流程概括：加载任务与树配置、查询指标值、计算节点指标、逐节点执行维度归因、持久化结果并更新状态。
     */
    private void runAttributionTask(String taskId) {
        LOG.info("开始执行归因分析任务: taskId={}", taskId);
        try {
            AnalysisTask task = taskDao.selectByTaskId(taskId);
            if (task == null) {
                throw new SystemException("任务不存在: " + taskId);
            }

            AttributionTree tree = treeDao.selectByTreeId(task.getTreeId());
            if (tree == null) {
                throw new SystemException("归因树不存在: " + task.getTreeId());
            }

            MetricTreeNodeDTO root = parseJson(tree.getTreeConfig(), MetricTreeNodeDTO.class, "归因树配置格式错误");
            markTaskRunning(task, "解析归因树成功", 5);

            Map<String, NodeMetricComputationEngine.MetricValue> metricValues = buildMetricValueMap(
                    root,
                    task.getBaselineDate(),
                    task.getCompareDate(),
                    task.getTimeGranularity()
            );
            markTaskRunning(task, "节点指标值查询完成", 30);
            NodeMetricComputationEngine nodeMetricComputationEngine = createNodeMetricComputationEngine();
            DimensionAttributionEngine dimensionAttributionEngine = createDimensionAttributionEngine();
            NodeMetricComputationEngine.NodeComputation nodeComputation = nodeMetricComputationEngine.compute(
                    root,
                    task.getBaselineDate(),
                    task.getCompareDate(),
                    task.getTimeGranularity(),
                    metricValues
            );
            markTaskRunning(task, "节点指标贡献度计算完成", 60);
            // 开始处理维度归因
            AttributionTreeResultNodeDTO resultNode = addDimAttributionResult(
                    nodeComputation,
                    nodeComputation.deltaValue(),
                    nodeComputation.deltaValue(),
                    task.getBaselineDate(),
                    task.getCompareDate(),
                    task.getTimeGranularity(),
                    dimensionAttributionEngine);
            markTaskRunning(task, "完成贡献度计算", 90);
            persistResult(taskId, resultNode);
            markTaskSuccess(task, "归因分析完成");
            LOG.info("归因分析任务完成: taskId={}", taskId);
        } catch (Exception e) {
            LOG.error("执行归因分析任务失败: taskId={}", taskId, e);
            markTaskFailed(taskId, e.getMessage());
        }
    }

    /**
     * 将归因结果持久化到结果表，若已存在则覆盖。
     */
    private void persistResult(String taskId, AttributionTreeResultNodeDTO resultNode) {
        String json = toJson(resultNode, "归因结果序列化失败");
        AttributionResult existing = resultDao.selectByTaskId(taskId);
        if (existing == null) {
            AttributionResult result = new AttributionResult();
            result.setTaskId(taskId);
            result.setResultTree(json);
            result.setCreateTime(LocalDateTime.now());
            resultDao.insert(result);
            LOG.info("归因结果已插入: taskId={}", taskId);
        } else {
            existing.setResultTree(json);
            resultDao.updateByTaskId(existing);
            LOG.info("归因结果已更新: taskId={}", taskId);
        }
    }

    /**
     * 将节点指标值与维度归因结果递归组装为最终的归因结果树。
     */
    private AttributionTreeResultNodeDTO addDimAttributionResult(NodeMetricComputationEngine.NodeComputation node,
                                                                 BigDecimal rootDelta,
                                                                 BigDecimal parentDelta,
                                                                 LocalDate baselineDate,
                                                                 LocalDate compareDate,
                                                                 DateGranularity granularity,
                                                                  DimensionAttributionEngine dimensionEngine) {
        List<AttributionTreeResultNodeDTO> childResults = node.children().stream()
                .map(child -> addDimAttributionResult(child, rootDelta, node.deltaValue(), baselineDate, compareDate, granularity, dimensionEngine))
                .collect(Collectors.toList());

        List<DimensionAttributionItemDTO> dimensionAttribution = buildDimensionAttributionForNode(
                node,
                baselineDate,
                compareDate,
                granularity,
                dimensionEngine
        );

        BigDecimal localContribution = computeContribution(node.deltaValue(), parentDelta);
        BigDecimal globalContribution = computeContribution(node.deltaValue(), rootDelta);

        return new AttributionTreeResultNodeDTO(
                node.node().nodeId(),
                node.node().nodeName(),
                node.node().metricId(),
                node.node().isRate(),
                node.node().op(),
                node.compareValue(),
                node.baselineValue(),
                node.deltaValue(),
                node.deltaRate(),
                localContribution,
                globalContribution,
                dimensionAttribution,
                childResults
        );
    }

    /**
     * 针对单个节点执行维度归因：串行查询维度取值，调用算法模块计算贡献度。
     */
    private List<DimensionAttributionItemDTO> buildDimensionAttributionForNode(NodeMetricComputationEngine.NodeComputation node,
                                                                              LocalDate baselineDate,
                                                                              LocalDate compareDate,
                                                                              DateGranularity granularity,
                                                                              DimensionAttributionEngine dimensionEngine) {
        List<String> dimensions = node.node().dimensions();
        if (dimensions == null || dimensions.isEmpty()) {
            LOG.debug("节点{} 未配置维度，跳过维度归因", node.node().nodeId());
            return Collections.emptyList();
        }
        if (node.deltaValue().compareTo(BigDecimal.ZERO) == 0) {
            LOG.debug("节点{} 对比期与基准期无差异，跳过维度归因", node.node().nodeId());
            return Collections.emptyList();
        }

        List<DimensionAttributionItemDTO> results = new ArrayList<>();
        for (String dimensionId : dimensions) {
            LOG.info("维度取数开始: nodeId={}, metricId={}, dimensionId={}, baseline={}, compare={}",
                    node.node().nodeId(), node.node().metricId(), dimensionId, baselineDate, compareDate);
            Map<String, BigDecimal> baselineValues = queryDimensionValues(
                    node.node().metricId(),
                    dimensionId,
                    baselineDate,
                    granularity
            );
            Map<String, BigDecimal> compareValues = queryDimensionValues(
                    node.node().metricId(),
                    dimensionId,
                    compareDate,
                    granularity
            );
            LOG.info("维度取数完成: nodeId={}, dimensionId={}, baselineCount={}, compareCount={}",
                    node.node().nodeId(), dimensionId, baselineValues.size(), compareValues.size());

            List<DimensionAttributionItemDTO> items = dimensionEngine.analyze(
                    dimensionId,
                    compareValues,
                    baselineValues
            );
            if (!items.isEmpty()) {
                LOG.info("维度归因输出: nodeId={}, dimensionId={}, items={}", node.node().nodeId(), dimensionId, items.size());
                results.addAll(items);
            } else {
                LOG.info("维度归因无显著结果: nodeId={}, dimensionId={}", node.node().nodeId(), dimensionId);
            }
        }
        return results;
    }

    private BigDecimal computeContribution(BigDecimal nodeDelta, BigDecimal totalDelta) {
        if (totalDelta == null || totalDelta.abs().compareTo(epsilon) < 0) {
            return BigDecimal.ZERO;
        }
        return nodeDelta.divide(totalDelta.abs(), MathContext.DECIMAL64);
    }

    private List<MetricPointDTO> queryTrendSeries(String metricId,
                                                  LocalDate start,
                                                  LocalDate end,
                                                  DateGranularity granularity) {
        List<FieldOrder> orders = List.of(new FieldOrder(resolveTimeDimensionField(granularity), "asc"));
        Req_04302590_01 req = buildTrendMetricRequest(
                metricId,
                List.of(resolveTimeDimensionField(granularity)),
                start,
                end,
                granularity,
                orders,
                TREND_WINDOW_SIZE * 5
        );
        List<Map<String, Object>> rows = metricService.queryResultList(req);
        Map<String, BigDecimal> aggregated = new HashMap<>();
        for (Map<String, Object> row : rows) {
            String dateKey = Objects.toString(row.get(resolveTimeDimensionField(granularity)));
            BigDecimal value = toBigDecimal(row.get(metricId));
            aggregated.merge(dateKey, value, BigDecimal::add);
        }
        return aggregated.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> new MetricPointDTO(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());
    }

    private Req_04302590_01 buildTrendMetricRequest(String metricId,
                                                    List<String> dimensions,
                                                    LocalDate start,
                                                    LocalDate end,
                                                    DateGranularity granularity,
                                                    List<FieldOrder> orders,
                                                    int limit) {
        Req_04302590_01 req = new Req_04302590_01();
        req.setMetrics(List.of(metricId));
        req.setDimensions(dimensions);
        TimeDimension timeDimension = buildTrendTimeDimension(granularity, start, end);
        req.setTimeDimensions(List.of(timeDimension));
        req.setSort(orders);
        req.setLimit(limit);
        return req;
    }

    private TimeDimension buildTrendTimeDimension(DateGranularity granularity, LocalDate start, LocalDate end) {
        TimeDimension timeDimension = new TimeDimension();
        String dimensionField = resolveTimeDimensionField(granularity);
        timeDimension.setDimension(dimensionField);
        timeDimension.setGranularity(granularity.name().toLowerCase());
        timeDimension.setDateRange(List.of(start.format(DATE_FORMATTER), end.format(DATE_FORMATTER)));
        return timeDimension;
    }

    /**
     * 构建节点计算所需的指标值缓存，提前查询所有涉及的指标。
     */
    private Map<String, NodeMetricComputationEngine.MetricValue> buildMetricValueMap(MetricTreeNodeDTO root,
                                                                                    LocalDate baselineDate,
                                                                                    LocalDate compareDate,
                                                                                    DateGranularity granularity) {
        Set<String> metricIds = collectMetricIds(root);
        Map<String, NodeMetricComputationEngine.MetricValue> metricValues = new HashMap<>(metricIds.size());
        for (String metricId : metricIds) {
            BigDecimal baselineValue = queryMetricValue(metricId, baselineDate, granularity);
            BigDecimal compareValue = queryMetricValue(metricId, compareDate, granularity);
            metricValues.put(metricId, new NodeMetricComputationEngine.MetricValue(baselineValue, compareValue));
        }
        LOG.info("指标取数完成: baseline={}, compare={}, count={}", baselineDate, compareDate, metricValues.size());
        return metricValues;
    }

    private Set<String> collectMetricIds(MetricTreeNodeDTO node) {
        Set<String> metricIds = new HashSet<>();
        if (node.metricId() != null && !node.metricId().isBlank()) {
            metricIds.add(node.metricId());
        }
        if (node.children() != null) {
            for (MetricTreeNodeDTO child : node.children()) {
                metricIds.addAll(collectMetricIds(child));
            }
        }
        return metricIds;
    }

    private BigDecimal queryMetricValue(String metricId, LocalDate date, DateGranularity granularity) {
        Req_04302590_01 req = buildSingleDateMetricRequest(
                metricId,
                Collections.emptyList(),
                date,
                granularity,
                Collections.emptyList(),
                metricQueryLimit
        );
        List<Map<String, Object>> rows = metricService.queryResultList(req);
        BigDecimal value = rows.stream()
                .map(row -> toBigDecimal(row.get(metricId)))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        LOG.info("单日指标取数: metricId={}, date={}, value={}, rows={}", metricId, date, value, rows.size());
        return value;
    }

    private Map<String, BigDecimal> queryDimensionValues(String metricId,
                                                         String dimensionId,
                                                         LocalDate date,
                                                         DateGranularity granularity) {
        List<FieldOrder> orders = List.of(new FieldOrder(metricId, "desc"));
        Req_04302590_01 req = buildSingleDateMetricRequest(
                metricId,
                List.of(dimensionId),
                date,
                granularity,
                orders,
                dimensionTopLimit
        );
        List<Map<String, Object>> rows = metricService.queryResultList(req);
        Map<String, BigDecimal> aggregated = new HashMap<>();
        for (Map<String, Object> row : rows) {
            String dimValue = Objects.toString(row.getOrDefault(dimensionId, "UNKNOWN"));
            BigDecimal value = toBigDecimal(row.get(metricId));
            aggregated.merge(dimValue, value, BigDecimal::add);
        }
        LOG.info("维度取数: metricId={}, dimensionId={}, date={}, rows={}", metricId, dimensionId, date, rows.size());
        return aggregated;
    }

    private Req_04302590_01 buildSingleDateMetricRequest(String metricId,
                                                         List<String> dimensions,
                                                         LocalDate date,
                                                         DateGranularity granularity,
                                                         List<FieldOrder> orders,
                                                         int limit) {
        Req_04302590_01 req = new Req_04302590_01();
        req.setMetrics(List.of(metricId));
        req.setDimensions(dimensions);
        req.setTimeDimensions(List.of(buildSingleDateTimeDimension(granularity, date)));
        req.setSort(orders);
        req.setLimit(limit);
        return req;
    }

    private TimeDimension buildSingleDateTimeDimension(DateGranularity granularity, LocalDate date) {
        TimeDimension timeDimension = new TimeDimension();
        String dimensionField = resolveTimeDimensionField(granularity);
        timeDimension.setDimension(dimensionField);
        timeDimension.setGranularity(granularity.name().toLowerCase());
        String day = date.format(DATE_FORMATTER);
        timeDimension.setDateRange(List.of(day, day));
        return timeDimension;
    }

    private String resolveTimeDimensionField(DateGranularity granularity) {
        return TIME_DIMENSION_MAPPING.getOrDefault(granularity, "dim_calendar_a.fmt_date");
    }

    private void markTaskRunning(AnalysisTask task, String message, int progress) {
        task.setStatus(TaskStatus.RUNNING);
        task.setProgress(progress);
        task.setMessage(message);
        if (task.getStartTime() == null) {
            task.setStartTime(LocalDateTime.now());
        }
        task.setUpdateTime(LocalDateTime.now());
        taskDao.updateByTaskId(task);
    }

    private void markTaskSuccess(AnalysisTask task, String message) {
        task.setStatus(TaskStatus.SUCCESS);
        task.setProgress(100);
        task.setMessage(message);
        task.setEndTime(LocalDateTime.now());
        task.setUpdateTime(LocalDateTime.now());
        taskDao.updateByTaskId(task);
    }

    private void markTaskFailed(String taskId, String message) {
        AnalysisTask task = taskDao.selectByTaskId(taskId);
        if (task == null) {
            return;
        }
        task.setStatus(TaskStatus.FAILED);
        task.setProgress(0);
        task.setMessage(StringUtils.left(message, 200));
        task.setEndTime(LocalDateTime.now());
        task.setUpdateTime(LocalDateTime.now());
        taskDao.updateByTaskId(task);
    }

    private String generateTaskId() {
        return "TASK" + System.currentTimeMillis() + UUID.randomUUID().toString().substring(0, 4).toUpperCase();
    }

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

    private String formatDate(LocalDate date) {
        return date != null ? date.format(DATE_FORMATTER) : null;
    }

    private String formatDateTime(LocalDateTime dateTime) {
        return dateTime != null ? dateTime.format(DATETIME_FORMATTER) : null;
    }

    private String formatDecimal(BigDecimal value) {
        return value.setScale(6, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString();
    }

    private String formatPercent(BigDecimal value) {
        BigDecimal percent = value.multiply(BigDecimal.valueOf(100));
        return percent.setScale(2, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString() + "%";
    }

    private <T> T parseJson(String json, Class<T> type, String errorMessage) {
        try {
            return JSONUtil.fromJsonStr(json, type);
        } catch (Exception e) {
            LOG.error(errorMessage + ": {}", json, e);
            throw new SystemException(errorMessage);
        }
    }

    private String toJson(Object obj, String errorMessage) {
        try {
            return JSONUtil.toDenseJsonStr(obj);
        } catch (Exception e) {
            LOG.error(errorMessage, e);
            throw new SystemException(errorMessage);
        }
    }

    /**
     * 构造从 start 到 end（包含）的连续日期列表，保证趋势图 X 轴完整。
     */
    private List<String> buildContinuousDateRange(LocalDate start, LocalDate end) {
        List<String> dates = new ArrayList<>();
        LocalDate cursor = start;
        while (!cursor.isAfter(end)) {
            dates.add(cursor.format(DATE_FORMATTER));
            cursor = cursor.plusDays(1);
        }
        return dates;
    }

    private BigDecimal safeDivide(BigDecimal numerator, BigDecimal denominator) {
        if (denominator == null || denominator.abs().compareTo(epsilon) < 0) {
            return BigDecimal.ZERO;
        }
        return numerator.divide(denominator, MathContext.DECIMAL64);
    }

    private BigDecimal toBigDecimal(Object value) {
        if (value == null) {
            return BigDecimal.ZERO;
        }
        if (value instanceof BigDecimal bigDecimal) {
            return bigDecimal;
        }
        if (value instanceof Number number) {
            return BigDecimal.valueOf(number.doubleValue());
        }
        try {
            String text = value.toString();
            if (text.isBlank()) {
                return BigDecimal.ZERO;
            }
            return new BigDecimal(text);
        } catch (NumberFormatException ex) {
            throw new SystemException("无法解析指标值: " + value);
        }
    }

}
