## 归因报告生成流程概述

1. **任务发起**  
   - 接口：`POST /attribution/tasks`。  
   - Service 校验归因树、时间区间、名单信息后创建 `AnalysisTask`，状态置为 `PENDING`，并通过 `WeupRmbUtil.asyncRunWithContext` 提交异步执行。

2. **指标树解析与指标取数**  
   - 异步任务加载归因树 JSON 配置，构造 `MetricTreeNodeDTO`。  
   - 深度遍历树节点，收集所有 `metricId`，分别查询基准期与对比期的单日（或单粒度）指标值，构建 `Map<String, MetricValue>` 缓存。

3. **归因计算（AttributionComputationEngine）**  
   - 输入：根节点、基准/对比日期、时间粒度、指标值缓存、维度值获取器。  
   - 过程：  
     1. 递归计算每个节点的 baseline / compare / delta / deltaRate；  
     2. 依据节点 `op`（加减乘除）聚合子节点结果；  
     3. 对声明可拆解的维度调用 `DimensionValueFetcher` 获取聚合数据，交给 `AdtributorCalculator` 计算 EP / Surprise 并筛选 TopN 维度值；  
     4. 生成 `AttributionTreeResultNodeDTO`，包含本层贡献度、全局贡献度、维度归因列表。

4. **结果落库与任务状态**  
   - 计算完成后将 `AttributionTreeResultNodeDTO` 序列化为 JSON，落库 `t_attribution_result`（存在则更新）。  
   - 任务状态流转：`PENDING → RUNNING (0/30/70/90%) → SUCCESS`；失败时记录错误信息并置 `FAILED`。

5. **报告查询**  
   - 用户可通过 `GET /attribution/tasks/{taskId}/result` 获取归因树结果；  
   - 若有 AI 报告，访问 `GET /attribution/tasks/{taskId}/ai-report` 获取文本内容。

> 以上步骤覆盖了“任务创建 → 指标取数 → 归因计算 → 结果持久化 → 报告查询”的闭环，可据此排查或扩展归因流程。 

