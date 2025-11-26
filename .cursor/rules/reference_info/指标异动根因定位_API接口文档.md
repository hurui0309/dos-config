## 指标异动根因定位 - API 接口文档（RESTful, Java 后端）

### 0. 约定与通用规范
- **返回包裹**（所有接口统一返回结构）：
  - `respCode`：String，业务码（成功"0000"）。
  - `respMsg`：String，提示信息。
  - `bizSeq`：String，业务流水号（服务端生成，建议 UUID 或时间戳+序列号）。
  - `data`：任意类型（泛型），本次接口的实际数据载荷。
- **日期与时间类型约定**：
  - 日期：`yyyy-MM-dd` 格式（Java `LocalDate`）
  - 时间戳：`yyyy-MM-dd HH:mm:ss` 格式（Java `LocalDateTime`）
  - 时区：建议统一使用服务端时区或 UTC（需与前端约定）
- **数值类型约定**：
  - 金额、比率、贡献度等精度敏感字段：`decimal(24, 6)`
  - 计数、排名等整数：`Integer` 或 `Long`
  - 一般浮点数（趋势展示）：`Double`
- **分页**：如需要可统一扩展 `pageNo`(Integer)、`pageSize`(Integer)、`total`(Integer)。


返回统一示例（成功）：
```json
{
  "respCode": "0000",
  "respMsg": "success",
  "bizSeq": "BIZ202511180001",
  "data": { }
}
```

返回统一示例（失败）：
```json
{
  "respCode": "A0400",
  "respMsg": "参数不合法：metricName 不能为空",
  "bizSeq": "BIZ202511180002",
  "data": null
}
```

---

### 1) 核心指标查询接口
- **描述**：按指标名称模糊搜索核心指标列表。
- **方法与路径**：`GET /attribution/metrics`
- **请求参数**（Query String）：
  - `metricName`：String，指标名称关键字（可选，支持模糊匹配；不传或为空则返回全部）
  - `limit`：Integer，返回条数上限（可选，默认 20，最大 100）
- **返回数据**（data）：`List<MetricBrief>`
  - `metricId`：String，指标唯一标识
  - `metricName`：String，指标中文名称

**请求示例**：
```http
GET /attribution/metrics?metricName=%E5%8F%91%E6%94%BE&limit=10 HTTP/1.1
Host: example.com
```

**响应示例**：
```json
{
  "respCode": "0000",
  "respMsg": "success",
  "bizSeq": "BIZ202511180101",
  "data": [
    { "metricId": "finance.disbursement_amount", "metricName": "发放金额" },
    { "metricId": "finance.repayment_amount", "metricName": "还款金额" }
  ]
}
```

---

### 2) 核心指标趋势查询
- **描述**：查询某核心指标在基准期与对比期两个时间段的趋势数据与统计对比。
- **方法与路径**：`GET /attribution/metrics/{metricId}/trend`
- **路径参数**：
  - `metricId`：String，指标唯一标识（必填）
- **请求参数**（Query String）：
  - `baselineStartDate`：String（`yyyy-MM-dd`），基准期开始日期（必填）
  - `baselineEndDate`：String（`yyyy-MM-dd`），基准期结束日期（必填）
  - `compareStartDate`：String（`yyyy-MM-dd`），对比期开始日期（必填）
  - `compareEndDate`：String（`yyyy-MM-dd`），对比期结束日期（必填）
- **返回数据**（data）：`MetricTrendVO`
  - `baselineSeries`：`List<MetricPoint>`，基准期趋势序列（按日期升序）
  - `compareSeries`：`List<MetricPoint>`，对比期趋势序列（按日期升序）
  - `baselineAvg`：decimal(24, 6)，基准期平均值
  - `compareAvg`：decimal(24, 6)，对比期平均值
  - `changeRate`：decimal(24, 6)，变化幅度（计算公式：`(compareAvg - baselineAvg) / max(ε, |baselineAvg|)`）

**结构体定义**：
- `MetricPoint`：
  - `date`：String（`yyyy-MM-dd`），日期
  - `value`：decimal(24, 6)，指标值

**请求示例**：
```http
GET /attribution/metrics/finance.disbursement_amount/trend?baselineStartDate=2025-09-01&baselineEndDate=2025-09-07&compareStartDate=2025-09-08&compareEndDate=2025-09-14 HTTP/1.1
Host: example.com
```

**响应示例**：
```json
{
  "respCode": "0000",
  "respMsg": "success",
  "bizSeq": "BIZ202511180102",
  "data": {
    "baselineSeries": [
      { "date": "2025-09-01", "value": 1200.00 },
      { "date": "2025-09-02", "value": 1100.00 }
    ],
    "compareSeries": [
      { "date": "2025-09-08", "value": 1500.00 },
      { "date": "2025-09-09", "value": 1400.00 }
    ],
    "baselineAvg": 1150.00,
    "compareAvg": 1450.00,
    "changeRate": 0.2609
  }
}
```

---

### 3) 归因树查询接口
- **描述**：按归因树名称模糊搜索归因树列表。
- **方法与路径**：`GET /attribution/trees`
- **请求参数**（Query String）：
  - `treeName`：String，归因树名称关键字（可选，支持模糊匹配；不传或为空则返回全部）
  - `limit`：Integer，返回条数上限（可选，默认 20，最大 100）
- **返回数据**（data）：`List<AttributionTreeBrief>`
  - `treeId`：String，归因树唯一标识
  - `treeName`：String，归因树中文名称

**请求示例**：
```http
GET /attribution/trees?treeName=%E8%B4%B7%E6%AC%BE&limit=10 HTTP/1.1
Host: example.com
```

**响应示例**：
```json
{
  "respCode": "0000",
  "respMsg": "success",
  "bizSeq": "BIZ202511180103",
  "data": [
    { "treeId": "loan_balance_increment", "treeName": "贷款余额增量归因树" }
  ]
}
```

---

### 4) 归因树配置查询接口
- **描述**：根据 `treeId` 获取归因树完整配置结构（用于前端渲染成树形展示）。
- **方法与路径**：`GET /attribution/trees/{treeId}/config`
- **路径参数**：
  - `treeId`：String，归因树唯一标识（必填）
- **返回数据**（data）：`AttributionTreeConfigVO`
  - `treeId`：String，归因树 ID
  - `version`：Integer，配置版本号
  - `root`：`MetricTreeNode`，根节点（包含递归子节点）

**数据结构定义**（与详细设计文档一致，便于前端直接递归渲染）：
- `MetricTreeNode`：
  - `nodeId`：String，节点唯一标识
  - `nodeName`：String，节点展示名（对应指标中文名）
  - `metricId`：String，关联指标 ID
  - `isRate`：Boolean，是否为比率型指标（true/false）
  - `op`：String，运算类型（`add`、`sub`、`mul`、`div`，叶子节点可为空或 null）
  - `dimensions`：`List<String>`，可做单维归因的维度列表
  - `params`：`Map<String, Object>`，节点参数（如 `epThreshold`、`epTotalThreshold` 等）
  - `children`：`List<MetricTreeNode>`，子节点列表（递归结构）

**请求示例**：
```http
GET /attribution/trees/loan_balance_increment/config HTTP/1.1
Host: example.com
```

**响应示例**：
```json
{
  "respCode": "0000",
  "respMsg": "success",
  "bizSeq": "BIZ202511180104",
  "data": {
    "treeId": "loan_balance_increment",
    "version": 1,
    "root": {
      "nodeId": "loan_inc",
      "nodeName": "贷款余额增量",
      "metricId": "finance.loan_increase_amount",
      "isRate": false,
      "op": "sub",
      "dimensions": [],
      "params": {},
      "children": [
        {
          "nodeId": "disbursement",
          "nodeName": "发放金额",
          "metricId": "finance.disbursement_amount",
          "isRate": false,
          "op": "mul",
          "dimensions": ["ent_credit_period", "usage_status", "package_type"],
          "params": { "epThreshold": 0.1, "epTotalThreshold": 0.67 },
          "children": [
            { 
              "nodeId": "drawdown_cust_cnt", 
              "nodeName": "发放户数", 
              "metricId": "finance.drawdown_cust_cnt", 
              "isRate": false, 
              "op": null, 
              "dimensions": [], 
              "params": {}, 
              "children": [] 
            },
            { 
              "nodeId": "avg_drawdown_amt", 
              "nodeName": "户均发放额", 
              "metricId": "finance.avg_drawdown_amt", 
              "isRate": false, 
              "op": null, 
              "dimensions": [], 
              "params": {}, 
              "children": [] 
            }
          ]
        },
        {
          "nodeId": "repayment",
          "nodeName": "还款金额",
          "metricId": "finance.repayment_amount",
          "isRate": false,
          "op": null,
          "dimensions": [],
          "params": {},
          "children": []
        }
      ]
    }
  }
}
```

---

### 5) 发起分析任务
- **描述**：根据归因树与时间段参数发起一次指标异动归因分析任务（异步计算）。
- **方法与路径**：`POST /attribution/tasks`
- **请求体**（JSON Body）：
  - `treeId`：String，归因树唯一标识（必填）
  - `contributionThreshold`：decimal(24, 6)，贡献度阈值（必填，范围 0.0 ~ 1.0）
  - `baselineStartDate`：String（`yyyy-MM-dd`），基准期开始日期（必填）
  - `baselineEndDate`：String（`yyyy-MM-dd`），基准期结束日期（必填）
  - `compareStartDate`：String（`yyyy-MM-dd`），对比期开始日期（必填）
  - `compareEndDate`：String（`yyyy-MM-dd`），对比期结束日期（必填）
- **返回数据**（data）：`TaskCreateVO`
  - `taskId`：String，任务唯一标识

**请求示例**：
```http
POST /attribution/tasks HTTP/1.1
Host: example.com
Content-Type: application/json

{
  "treeId": "loan_balance_increment",
  "contributionThreshold": 0.05,
  "baselineStartDate": "2025-09-01",
  "baselineEndDate": "2025-09-07",
  "compareStartDate": "2025-09-08",
  "compareEndDate": "2025-09-14"
}
```

**响应示例**：
```json
{
  "respCode": "0000",
  "respMsg": "success",
  "bizSeq": "BIZ202511180105",
  "data": {
    "taskId": "TASK202511180001"
  }
}
```

---

### 6) 查询分析任务列表
- **描述**：按归因树名称与创建人进行模糊筛选，获取任务列表（支持分页）。
- **方法与路径**：`GET /attribution/tasks`
- **请求参数**（Query String）：
  - `treeName`：String，归因树名称关键字（可选，支持模糊匹配；不传或为空则不筛选）
  - `creator`：String，创建人名称关键字（可选，支持模糊匹配；不传或为空则不筛选）
  - `pageNo`：Integer，页码（可选，默认 1）
  - `pageSize`：Integer，每页条数（可选，默认 20，最大 100）
- **返回数据**（data）：`TaskListVO`
  - `total`：Integer，总记录数
  - `list`：`List<AnalysisTaskVO>`，任务列表

**结构体定义**：
- `AnalysisTaskVO`：
  - `taskId`：String，任务唯一标识
  - `treeId`：String，归因树 ID
  - `treeName`：String，归因树名称
  - `creator`：String，创建人
  - `baselineStartDate`：String（`yyyy-MM-dd`），基准期开始日期
  - `baselineEndDate`：String（`yyyy-MM-dd`），基准期结束日期
  - `compareStartDate`：String（`yyyy-MM-dd`），对比期开始日期
  - `compareEndDate`：String（`yyyy-MM-dd`），对比期结束日期
  - `status`：String，任务状态（枚举：`PENDING`、`RUNNING`、`DONE`、`FAILED`、`CANCELED`）
  - `createTime`：String（`yyyy-MM-dd HH:mm:ss`），任务创建时间
  - `endTime`：String（`yyyy-MM-dd HH:mm:ss`），任务结束时间（未结束时为 null）

**请求示例**：
```http
GET /attribution/tasks?treeName=%E8%B4%B7%E6%AC%BE&creator=zhang&pageNo=1&pageSize=20 HTTP/1.1
Host: example.com
```

**响应示例**：
```json
{
  "respCode": "0000",
  "respMsg": "success",
  "bizSeq": "BIZ202511180106",
  "data": {
    "total": 1,
    "list": [
      {
        "taskId": "TASK202511180001",
        "treeId": "loan_balance_increment",
        "treeName": "贷款余额增量归因树",
        "creator": "zhangsan",
        "baselineStartDate": "2025-09-01",
        "baselineEndDate": "2025-09-07",
        "compareStartDate": "2025-09-08",
        "compareEndDate": "2025-09-14",
        "status": "DONE",
        "createTime": "2025-11-18 10:12:30",
        "endTime": "2025-11-18 10:12:58"
      }
    ]
  }
}
```

---

### 7) 查询任务状态
- **描述**：根据任务 ID 查询分析任务的当前状态与基本信息。
- **方法与路径**：`GET /attribution/tasks/{taskId}/status`
- **路径参数**：
  - `taskId`：String，任务唯一标识（必填）
- **返回数据**（data）：`TaskStatusVO`
  - `taskId`：String，任务唯一标识
  - `status`：String，任务状态（枚举：`PENDING`、`RUNNING`、`DONE`、`FAILED`、`CANCELED`）
  - `progress`：Integer，执行进度（百分比，0-100）
  - `message`：String，状态说明（如错误信息、当前执行步骤等）
  - `createTime`：String（`yyyy-MM-dd HH:mm:ss`），任务创建时间
  - `startTime`：String（`yyyy-MM-dd HH:mm:ss`），任务开始时间（未开始为 null）
  - `endTime`：String（`yyyy-MM-dd HH:mm:ss`），任务结束时间（未结束为 null）

**请求示例**：
```http
GET /attribution/tasks/TASK202511180001/status HTTP/1.1
Host: example.com
```

**响应示例（任务完成）**：
```json
{
  "respCode": "0000",
  "respMsg": "success",
  "bizSeq": "BIZ202511180107",
  "data": {
    "taskId": "TASK202511180001",
    "status": "DONE",
    "progress": 100,
    "message": "分析完成",
    "createTime": "2025-11-18 10:12:30",
    "startTime": "2025-11-18 10:12:31",
    "endTime": "2025-11-18 10:12:58"
  }
}
```

**响应示例（任务执行中）**：
```json
{
  "respCode": "0000",
  "respMsg": "success",
  "bizSeq": "BIZ202511180108",
  "data": {
    "taskId": "TASK202511180002",
    "status": "RUNNING",
    "progress": 65,
    "message": "正在计算节点维度归因（3/5）",
    "createTime": "2025-11-18 11:20:15",
    "startTime": "2025-11-18 11:20:16",
    "endTime": null
  }
}
```

---

### 8) 查询归因树分析结果
- **描述**：根据任务 ID 查询归因树的完整分析结果，包含每个节点的波动信息、贡献度以及维度归因明细。
- **方法与路径**：`GET /attribution/tasks/{taskId}/result`
- **路径参数**：
  - `taskId`：String，任务唯一标识（必填）
- **返回数据**（data）：`AttributionResultVO`
  - `taskId`：String，任务唯一标识
  - `treeId`：String，归因树 ID
  - `treeName`：String，归因树名称
  - `baselineStartDate`：String（`yyyy-MM-dd`），基准期开始日期
  - `baselineEndDate`：String（`yyyy-MM-dd`），基准期结束日期
  - `compareStartDate`：String（`yyyy-MM-dd`），对比期开始日期
  - `compareEndDate`：String（`yyyy-MM-dd`），对比期结束日期
  - `resultTree`：`AttributionTreeResultNode`，根节点（包含递归子节点及分析结果）

**数据结构定义**：
- `AttributionTreeResultNode`（继承配置节点，增加分析结果字段）：
  - `nodeId`：String，节点唯一标识
  - `nodeName`：String，节点展示名
  - `metricId`：String，关联指标 ID
  - `isRate`：Boolean，是否为比率型指标
  - `op`：String，运算类型（`add`、`sub`、`mul`、`div`）
  - **分析结果字段**（以下为新增）：
    - `currentValue`：decimal(24, 6)，本周期指标值
    - `baselineValue`：decimal(24, 6)，对比周期指标值
    - `deltaValue`：decimal(24, 6)，波动量（绝对值）
    - `deltaRate`：decimal(24, 6)，波动幅度（相对变化率）
    - `contributionLocal`：decimal(24, 6)，本层贡献度（相对于父节点）
    - `contributionGlobal`：decimal(24, 6)，整体贡献度（相对于根节点）
  - `dimensionAttribution`：`List<DimensionAttributionItem>`，维度归因结果列表（仅当节点有维度配置时返回）
  - `children`：`List<AttributionTreeResultNode>`，子节点列表（递归结构）

- `DimensionAttributionItem`：
  - `dimension`：String，维度名称（如 `package_type`）
  - `dimensionValue`：String，维度值（如 `随借随还`）
  - `currentValue`：decimal(24, 6)，本周期该维度值的指标值
  - `baselineValue`：decimal(24, 6)，对比周期该维度值的指标值
  - `deltaValue`：decimal(24, 6)，波动量
  - `contribution`：decimal(24, 6)，贡献度（EP 值）
  - `surprise`：decimal(24, 6)，惊奇度（Surprise 值）
  - `rank`：Integer，排序位次

**请求示例**：
```http
GET /attribution/tasks/TASK202511180001/result HTTP/1.1
Host: example.com
```

**响应示例**：
```json
{
  "respCode": "0000",
  "respMsg": "success",
  "bizSeq": "BIZ202511180109",
  "data": {
    "taskId": "TASK202511180001",
    "treeId": "loan_balance_increment",
    "treeName": "贷款余额增量归因树",
    "baselineStartDate": "2025-09-01",
    "baselineEndDate": "2025-09-07",
    "compareStartDate": "2025-09-08",
    "compareEndDate": "2025-09-14",
    "resultTree": {
      "nodeId": "loan_inc",
      "nodeName": "贷款余额增量",
      "metricId": "finance.loan_increase_amount",
      "isRate": false,
      "op": "sub",
      "currentValue": 5000.00,
      "baselineValue": 3000.00,
      "deltaValue": 2000.00,
      "deltaRate": 0.6667,
      "contributionLocal": 1.0,
      "contributionGlobal": 1.0,
      "dimensionAttribution": [],
      "children": [
        {
          "nodeId": "disbursement",
          "nodeName": "发放金额",
          "metricId": "finance.disbursement_amount",
          "isRate": false,
          "op": "mul",
          "currentValue": 8000.00,
          "baselineValue": 5000.00,
          "deltaValue": 3000.00,
          "deltaRate": 0.6000,
          "contributionLocal": 1.5,
          "contributionGlobal": 1.5,
          "dimensionAttribution": [
            {
              "dimension": "package_type",
              "dimensionValue": "随借随还",
              "currentValue": 5000.00,
              "baselineValue": 3000.00,
              "deltaValue": 2000.00,
              "contribution": 0.6667,
              "surprise": 0.1234,
              "rank": 1
            },
            {
              "dimension": "package_type",
              "dimensionValue": "等额本息",
              "currentValue": 3000.00,
              "baselineValue": 2000.00,
              "deltaValue": 1000.00,
              "contribution": 0.3333,
              "surprise": 0.0856,
              "rank": 2
            }
          ],
          "children": [
            {
              "nodeId": "drawdown_cust_cnt",
              "nodeName": "发放户数",
              "metricId": "finance.drawdown_cust_cnt",
              "isRate": false,
              "op": null,
              "currentValue": 400.00,
              "baselineValue": 250.00,
              "deltaValue": 150.00,
              "deltaRate": 0.6000,
              "contributionLocal": 0.5,
              "contributionGlobal": 0.75,
              "dimensionAttribution": [],
              "children": []
            },
            {
              "nodeId": "avg_drawdown_amt",
              "nodeName": "户均发放额",
              "metricId": "finance.avg_drawdown_amt",
              "isRate": false,
              "op": null,
              "currentValue": 20.00,
              "baselineValue": 20.00,
              "deltaValue": 0.00,
              "deltaRate": 0.0000,
              "contributionLocal": 0.0,
              "contributionGlobal": 0.0,
              "dimensionAttribution": [],
              "children": []
            }
          ]
        },
        {
          "nodeId": "repayment",
          "nodeName": "还款金额",
          "metricId": "finance.repayment_amount",
          "isRate": false,
          "op": null,
          "currentValue": 3000.00,
          "baselineValue": 2000.00,
          "deltaValue": 1000.00,
          "deltaRate": 0.5000,
          "contributionLocal": -0.5,
          "contributionGlobal": -0.5,
          "dimensionAttribution": [],
          "children": []
        }
      ]
    }
  }
}
```

---

### 9) 查询 AI 归因报告
- **描述**：根据任务 ID 查询由 AI 生成的归因分析报告（自然语言形式）。
- **方法与路径**：`GET /attribution/tasks/{taskId}/ai-report`
- **路径参数**：
  - `taskId`：String，任务唯一标识（必填）
- **返回数据**（data）：`AiAttributionReportVO`
  - `taskId`：String，任务唯一标识
  - `reportStatus`：String，报告生成状态（`GENERATING`、`COMPLETED`、`FAILED`）
  - `summary`：String，摘要（核心结论，100-200 字）
  - `sections`：`List<ReportSection>`，报告章节列表
  - `generateTime`：String（`yyyy-MM-dd HH:mm:ss`），报告生成时间

**数据结构定义**：
- `ReportSection`：
  - `sectionTitle`：String，章节标题（如"整体变化分析"、"关键驱动因素"、"维度归因详情"）
  - `sectionContent`：String，章节内容（Markdown 格式文本）
  - `sectionOrder`：Integer，章节顺序

**请求示例**：
```http
GET /attribution/tasks/TASK202511180001/ai-report HTTP/1.1
Host: example.com
```

**响应示例**：
```json
{
  "respCode": "0000",
  "respMsg": "success",
  "bizSeq": "BIZ202511180110",
  "data": {
    "taskId": "TASK202511180001",
    "reportStatus": "COMPLETED",
    "summary": "2025-09-08 至 2025-09-14 期间，贷款余额增量相比上周（2025-09-01 至 2025-09-07）增长了 2000 万元（+66.67%）。主要驱动因素为发放金额增长 3000 万元（贡献度 150%），其中发放户数增长 150 户是核心原因。还款金额增长 1000 万元对增量产生负向贡献（-50%）。在产品维度，随借随还产品贡献了 66.7% 的发放增量，惊奇度较高。",
    "sections": [
      {
        "sectionTitle": "一、整体变化分析",
        "sectionContent": "### 指标波动概况\n\n- **本期值**：5000.00 万元\n- **对比期值**：3000.00 万元\n- **波动量**：+2000.00 万元\n- **波动幅度**：+66.67%\n\n本期贷款余额增量相较对比期呈现显著上升趋势，增幅接近七成。",
        "sectionOrder": 1
      },
      {
        "sectionTitle": "二、关键驱动因素",
        "sectionContent": "### 节点贡献度分析\n\n1. **发放金额**（整体贡献度：150%）\n   - 本期值：8000.00 万元\n   - 波动量：+3000.00 万元（+60%）\n   - 该节点是本次增量的主要正向驱动因素，贡献超过整体增量的 150%\n\n2. **还款金额**（整体贡献度：-50%）\n   - 本期值：3000.00 万元\n   - 波动量：+1000.00 万元（+50%）\n   - 还款增长对余额增量产生负向影响，抵消了部分发放增量的效果\n\n### 细分节点分析\n\n发放金额的增长主要来自：\n- **发放户数**：增长 150 户（+60%），整体贡献度 75%\n- **户均发放额**：持平，无显著变化",
        "sectionOrder": 2
      },
      {
        "sectionTitle": "三、维度归因详情",
        "sectionContent": "### 发放金额 - 产品类型维度\n\n根据 EP 与 Surprise 分析，产品类型维度的归因结果如下：\n\n| 排名 | 维度值 | 本期值（万元） | 对比期值（万元） | 波动量（万元） | 贡献度（EP） | 惊奇度（Surprise） |\n|------|--------|---------------|----------------|--------------|-------------|-------------------|\n| 1    | 随借随还 | 5000.00      | 3000.00        | +2000.00     | 66.67%      | 0.1234            |\n| 2    | 等额本息 | 3000.00      | 2000.00        | +1000.00     | 33.33%      | 0.0856            |\n\n**关键发现**：\n- 随借随还产品贡献了发放增量的三分之二，且惊奇度较高，表明该产品的增长超出历史预期\n- 等额本息产品虽有增长，但贡献相对有限",
        "sectionOrder": 3
      },
      {
        "sectionTitle": "四、建议与行动项",
        "sectionContent": "基于上述分析，建议关注以下方向：\n\n1. **持续关注随借随还产品**：该产品增长强劲且超出预期，建议深入分析客群特征与营销策略有效性\n2. **优化还款管理**：还款金额增长抵消了部分发放效果，可考虑调整还款引导策略或优化贷款期限结构\n3. **扩大发放户数覆盖**：发放户数增长是核心驱动力，建议继续推进获客策略\n4. **提升户均发放额**：当前户均发放额无显著变化，可探索提升单客价值的产品设计或额度策略",
        "sectionOrder": 4
      }
    ],
    "generateTime": "2025-11-18 10:13:15"
  }
}
```

---

### 附录：数据模型汇总

#### Java VO/DTO 类型映射建议

| 业务对象 | 类名 | 字段说明 |
|---------|------|---------|
| 指标简要信息 | `MetricBrief` | `metricId: String`, `metricName: String` |
| 指标时序点 | `MetricPoint` | `date: String (yyyy-MM-dd)`, `value: decimal(24, 6)` |
| 指标趋势响应 | `MetricTrendVO` | `baselineSeries: List<MetricPoint>`, `compareSeries: List<MetricPoint>`, `baselineAvg: decimal(24, 6)`, `compareAvg: decimal(24, 6)`, `changeRate: decimal(24, 6)` |
| 归因树简要信息 | `AttributionTreeBrief` | `treeId: String`, `treeName: String` |
| 归因树配置 | `AttributionTreeConfigVO` | `treeId: String`, `version: Integer`, `root: MetricTreeNode` |
| 指标树节点（配置） | `MetricTreeNode` | `nodeId: String`, `nodeName: String`, `metricId: String`, `isRate: Boolean`, `op: String`, `dimensions: List<String>`, `params: Map<String, Object>`, `children: List<MetricTreeNode>` |
| 任务创建响应 | `TaskCreateVO` | `taskId: String` |
| 任务列表响应 | `TaskListVO` | `total: Integer`, `list: List<AnalysisTaskVO>` |
| 任务详情 | `AnalysisTaskVO` | `taskId: String`, `treeId: String`, `treeName: String`, `creator: String`, `baselineStartDate: String`, `baselineEndDate: String`, `compareStartDate: String`, `compareEndDate: String`, `status: String`, `createTime: String`, `endTime: String` |
| 任务状态 | `TaskStatusVO` | `taskId: String`, `status: String`, `progress: Integer`, `message: String`, `createTime: String`, `startTime: String`, `endTime: String` |
| 归因分析结果 | `AttributionResultVO` | `taskId: String`, `treeId: String`, `treeName: String`, `baselineStartDate: String`, `baselineEndDate: String`, `compareStartDate: String`, `compareEndDate: String`, `resultTree: AttributionTreeResultNode` |
| 归因树结果节点（递归） | `AttributionTreeResultNode` | `nodeId: String`, `nodeName: String`, `metricId: String`, `isRate: Boolean`, `op: String`, `currentValue: decimal(24, 6)`, `baselineValue: decimal(24, 6)`, `deltaValue: decimal(24, 6)`, `deltaRate: decimal(24, 6)`, `contributionLocal: decimal(24, 6)`, `contributionGlobal: decimal(24, 6)`, `dimensionAttribution: List<DimensionAttributionItem>`, `children: List<AttributionTreeResultNode>` |
| 维度归因明细 | `DimensionAttributionItem` | `dimension: String`, `dimensionValue: String`, `currentValue: decimal(24, 6)`, `baselineValue: decimal(24, 6)`, `deltaValue: decimal(24, 6)`, `contribution: decimal(24, 6)`, `surprise: decimal(24, 6)`, `rank: Integer` |
| AI 归因报告 | `AiAttributionReportVO` | `taskId: String`, `reportStatus: String`, `summary: String`, `sections: List<ReportSection>`, `generateTime: String` |
| 报告章节 | `ReportSection` | `sectionTitle: String`, `sectionContent: String`, `sectionOrder: Integer` |

#### 枚举类型建议

- **任务状态枚举** `TaskStatusEnum`：
  - `PENDING`：待执行
  - `RUNNING`：执行中
  - `DONE`：已完成
  - `FAILED`：失败
  - `CANCELED`：已取消

- **报告状态枚举** `ReportStatusEnum`：
  - `GENERATING`：生成中
  - `COMPLETED`：已完成
  - `FAILED`：生成失败

- **运算类型枚举** `opEnum`：
  - `add`：加法
  - `sub`：减法
  - `mul`：乘法
  - `div`：除法

#### 通用说明

- **返回结构**：所有接口统一返回 `{ respCode, respMsg, bizSeq, data }`，业务数据均在 `data` 字段。
- **日期格式**：请求/响应均使用字符串，格式为 `yyyy-MM-dd` 或 `yyyy-MM-dd HH:mm:ss`（Java 后端可使用 `LocalDate`/`LocalDateTime` 序列化）。
- **数值精度**：金额、比率等使用 `decimal(24, 6)` 保证精度；计数使用 `Integer`/`Integer`；趋势展示可用 `Double`。
- **可扩展性**：如需国际化、多租户、审计字段等，可在请求头（如 `X-Language`、`X-Tenant-Id`）或统一参数中扩展。
- **版本管理**：建议在路径中添加版本前缀（如 `/v1/attribution/...`）以便后续迭代。


