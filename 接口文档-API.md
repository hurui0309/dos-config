# 指标异动根因定位系统 · REST API 文档（v1.1.0）

> **面向读者**：前端 / 移动端 / 第三方调用方 / 测试与运维团队  
> **对应代码分支**：`main`（2025-11-24）  
> **接口 Base URL**：`/attribution`（如有多环境，请在部署手册里维护域名）

---

## 0. 通用规范与约定

### 0.1 统一返回结构

所有接口均使用 `BaseResponse<T>` 作为响应结构：

| 字段名   | 类型   | 说明                                             |
|----------|--------|--------------------------------------------------|
| respCode | String | 业务码：成功 `"0"`，其它表示失败                |
| respMsg  | String | 人类可读提示，失败时包含原因                    |
| bizSeq   | String | 业务流水号（服务端生成，建议 UUID 或时间戳）    |
| data     | Any    | 具体业务数据（泛型），失败时可为空或null        |

**成功示例**:
```json
{
  "respCode": "0",
  "respMsg": "success",
  "bizSeq": "BIZ202511240001",
  "data": {
    "metricId": "finance.loan_increase_amount",
    "metricName": "贷款余额增量"
  }
}
```

**失败示例**:
```json
{
  "respCode": "DOS0002",
  "respMsg": "参数不合法：limit 必须在 1-100 之间",
  "bizSeq": "BIZ202511240002",
  "data": null
}
```

### 0.2 错误码对照

| 错误码   | 说明                     | 触发场景                         |
|----------|--------------------------|----------------------------------|
| 0        | 成功                     | 一切正常                         |
| DOS0001  | 鉴权失败                 | Header 缺失或签名/Token 失效     |
| DOS0002  | 参数错误                 | `Preconditions`/`@Valid` 失败    |
| DOS5000  | 系统异常                 | 未捕获异常                       |

### 0.3 日期 & 数值规范

- **日期格式**：`yyyy-MM-dd`（Java `LocalDate`）  
- **时间格式**：`yyyy-MM-dd HH:mm:ss`（Java `LocalDateTime`）  
- **数值类型**：
  - 金额/比率：`decimal(24,6)` / Java `BigDecimal`
  - 进度：`Integer`（0-100）

### 0.4 枚举说明

#### TaskStatus（任务状态枚举）

| 枚举值   | 说明         | 适用场景                  |
|----------|--------------|---------------------------|
| PENDING  | 待执行       | 任务已创建，等待调度      |
| RUNNING  | 执行中       | 任务正在执行              |
| SUCCESS  | 已成功完成   | 任务执行成功（v1.1.0 更名）|
| FAILED   | 失败         | 任务执行失败              |
| CANCELED | 已取消       | 用户取消任务              |

#### ReportStatus（AI报告状态枚举）

| 枚举值      | 说明     | 适用场景            |
|-------------|----------|---------------------|
| GENERATING  | 生成中   | AI报告正在生成      |
| COMPLETED   | 已完成   | AI报告生成完成      |
| FAILED      | 生成失败 | AI报告生成失败      |

#### DateGranularity（时间粒度枚举）

| 枚举值 | dateGran | normalFmt   | symFmt   | 说明     |
|--------|----------|-------------|----------|----------|
| DAY    | day      | yyyy-MM-dd  | %Y-%m-%d | 日粒度   |
| WEEK   | week     | yyyy-ww     | %Y-%v    | 周粒度   |
| MONTH  | month    | yyyy-MM     | %Y-%m    | 月粒度   |
| YEAR   | year     | yyyy        | %Y       | 年粒度   |

**Java 定义**:
```java
public enum DateGranularity implements EnumStringParseAble<DateGranularity> {
    DAY("day", "yyyy-MM-dd", "%Y-%m-%d"),
    WEEK("week", "yyyy-ww", "%Y-%v"),
    MONTH("month", "yyyy-MM", "%Y-%m"),
    YEAR("year", "yyyy", "%Y");
    
    private String dateGran;     // 字符串值
    private String normalFmt;    // Java 日期格式
    private String symFmt;       // SQL 日期格式
}
```

### 0.5 分页规范

| 参数名   | 类型    | 默认值 | 约束      | 说明         |
|----------|---------|--------|-----------|--------------|
| pageNo   | Integer | 1      | >= 1      | 页码         |
| pageSize | Integer | 20     | 1-100     | 每页条数     |

**分页返回结构**:
```json
{
  "total": 150,
  "pageNo": 1,
  "pageSize": 20,
  "records": [...]
}
```

---

## 1. 核心指标接口

### 1.1 查询核心指标列表

**接口地址**: `GET /attribution/metrics`

**功能描述**: 按指标名称关键字模糊检索指标列表

**请求参数**:
| 参数名     | 类型   | 必填 | 默认值 | 说明                        |
|------------|--------|------|--------|-----------------------------|
| metricName | String | 否   | -      | 指标名称关键字（模糊匹配）  |
| limit      | int    | 否   | 20     | 返回条数限制（1-100）       |

**返回数据**: `List<MetricBriefDTO>`

**MetricBriefDTO 结构**:
| 字段名     | 类型   | 说明           |
|------------|--------|----------------|
| metricId   | String | 指标唯一标识   |
| metricName | String | 指标中文名称   |
| metricDesc | String | 指标描述       |
| tableName  | String | 数据源表名     |
| fieldName  | String | 数据源字段名   |

**请求示例**:
```http
GET /attribution/metrics?metricName=贷款&limit=10
```

**响应示例**:
```json
{
  "respCode": "0",
  "respMsg": "success",
  "bizSeq": "BIZ202511240101",
  "data": [
    {
      "metricId": "finance.loan_increase_amount",
      "metricName": "贷款余额增量",
      "metricDesc": "贷款余额的净增加量",
      "tableName": "t_finance_daily",
      "fieldName": "loan_increase_amt"
    },
    {
      "metricId": "finance.disbursement_amount",
      "metricName": "发放金额",
      "metricDesc": "贷款发放总金额",
      "tableName": "t_finance_daily",
      "fieldName": "disbursement_amt"
    },
    {
      "metricId": "finance.repayment_amount",
      "metricName": "还款金额",
      "metricDesc": "贷款还款总金额",
      "tableName": "t_finance_daily",
      "fieldName": "repayment_amt"
    }
  ]
}
```

---

### 1.2 查询核心指标趋势

**接口地址**: `GET /attribution/metrics/{metricId}/trend`

**功能描述**: 查询指定指标在基准期与对比期的趋势数据，支持 ECharts 图表直接渲染

**路径参数**:
| 参数名   | 类型   | 必填 | 说明         |
|----------|--------|------|--------------|
| metricId | String | 是   | 指标唯一标识 |

**请求参数**:
| 参数名          | 类型            | 必填 | 说明                       |
|-----------------|-----------------|------|----------------------------|
| timeGranularity | DateGranularity | 是   | 时间粒度（DAY/WEEK/MONTH/YEAR） |
| baselineDate    | String          | 是   | 基准日期（yyyy-MM-dd）     |
| compareDate     | String          | 是   | 对比日期（yyyy-MM-dd）     |

**返回数据**: `MetricTrendDTO`

**MetricTrendDTO 结构**:
| 字段名      | 类型                   | 说明                     |
|-------------|------------------------|--------------------------|
| chartData   | ChartDataDTO           | ECharts 格式图表数据     |
| metricItems | List\<MetricItemDTO\>  | 关键指标摘要列表         |

**ChartDataDTO 结构**（对应 ECharts option）:
| 字段名 | 类型                    | 说明                          |
|--------|-------------------------|-------------------------------|
| xAxis  | Map\<String, Object\>   | X轴配置（包含 type 和 data）  |
| yAxis  | Map\<String, Object\>   | Y轴配置（包含 type）          |
| series | List\<Map\<String, Object\>\> | 系列数据数组          |

**xAxis 结构示例**:
```json
{
  "type": "category",
  "data": ["2024-01-01", "2024-01-02", "2024-01-03", ...]
}
```

**yAxis 结构示例**:
```json
{
  "type": "value"
}
```

**series 元素结构**:
| 字段名 | 类型             | 说明                                 |
|--------|------------------|--------------------------------------|
| name   | String           | 系列名称（如：基准期、对比期）       |
| type   | String           | 图表类型（line/bar/pie等）           |
| data   | List\<Number\>   | 数据点数组                           |

**MetricItemDTO 结构**:
| 字段名 | 类型   | 说明                                |
|--------|--------|-------------------------------------|
| key    | String | 指标键（如：baselineAvg）           |
| value  | String | 指标值（如：1197.43）               |
| title  | String | 展示标题（如：基准期均值）          |
| color  | String | 颜色标识（red/green/blue等）        |
| remark | String | 备注说明                            |

**请求示例**:
```http
GET /attribution/metrics/finance.loan_increase_amount/trend?timeGranularity=DAY&baselineDate=2024-01-01&compareDate=2024-01-08
```

**响应示例**:
```json
{
  "respCode": "0",
  "respMsg": "success",
  "bizSeq": "BIZ202511240102",
  "data": {
    "chartData": {
      "xAxis": {
        "type": "category",
        "data": ["2024-01-01", "2024-01-02", "2024-01-03", "2024-01-04", "2024-01-05", "2024-01-06", "2024-01-07"]
      },
      "yAxis": {
        "type": "value"
      },
      "series": [
        {
          "name": "基准期",
          "type": "line",
          "data": [1200.50, 1150.30, 1180.20, 1210.00, 1190.80, 1220.50, 1230.70],
          "smooth": true
        },
        {
          "name": "对比期",
          "type": "line",
          "data": [1500.20, 1480.50, 1510.30, 1490.00, 1520.80, 1540.20, 1560.00],
          "smooth": true
        }
      ]
    },
    "metricItems": [
      {
        "key": "baselineAvg",
        "value": "1197.43",
        "title": "基准期均值",
        "color": "blue",
        "remark": "基准期7天平均值"
      },
      {
        "key": "compareAvg",
        "value": "1514.43",
        "title": "对比期均值",
        "color": "green",
        "remark": "对比期7天平均值"
      },
      {
        "key": "changeRate",
        "value": "+26.49%",
        "title": "变化率",
        "color": "red",
        "remark": "对比期相较基准期的变化率"
      },
      {
        "key": "deltaValue",
        "value": "317.00",
        "title": "波动量",
        "color": "orange",
        "remark": "对比期均值 - 基准期均值"
      }
    ]
  }
}
```

---

## 2. 归因树接口

### 2.1 查询归因树列表

**接口地址**: `GET /attribution/trees`

**功能描述**: 查询系统中配置的归因树列表

**请求参数**:
| 参数名   | 类型   | 必填 | 默认值 | 说明                        |
|----------|--------|------|--------|-----------------------------|
| treeName | String | 否   | -      | 归因树名称（模糊匹配）      |
| limit    | int    | 否   | 20     | 返回条数限制（1-100）       |

**返回数据**: `List<AttributionTreeBriefDTO>`

**AttributionTreeBriefDTO 结构**:
| 字段名    | 类型   | 说明             |
|-----------|--------|------------------|
| treeId    | String | 归因树唯一标识   |
| treeName  | String | 归因树中文名称   |
| metricId  | String | 关联的核心指标ID |
| metricName| String | 关联指标中文名称 |
| version   | Integer| 配置版本号       |

**请求示例**:
```http
GET /attribution/trees?treeName=贷款&limit=10
```

**响应示例**:
```json
{
  "respCode": "0",
  "respMsg": "success",
  "bizSeq": "BIZ202511240103",
  "data": [
    {
      "treeId": "loan_balance_increment",
      "treeName": "贷款余额增量归因树",
      "metricId": "finance.loan_increase_amount",
      "metricName": "贷款余额增量",
      "version": 1
    },
    {
      "treeId": "revenue_analysis",
      "treeName": "营收归因分析树",
      "metricId": "finance.total_revenue",
      "version": 2
    }
  ]
}
```

---

### 2.2 查询归因树配置

**接口地址**: `GET /attribution/trees/{treeId}/config`

**功能描述**: 查询指定归因树的完整配置结构（包含树形节点递归结构）

**路径参数**:
| 参数名 | 类型   | 必填 | 说明           |
|--------|--------|------|----------------|
| treeId | String | 是   | 归因树唯一标识 |

**返回数据**: `AttributionTreeConfigDTO`

**AttributionTreeConfigDTO 结构**:
| 字段名     | 类型           | 说明                     |
|------------|----------------|--------------------------|
| treeId     | String         | 归因树唯一标识           |
| treeName   | String         | 归因树中文名称           |
| metricId   | String         | 关联的核心指标ID         |
| metricName | String         | 关联指标中文名称         |
| version    | Integer        | 配置版本号               |
| treeConfig | MetricTreeNode | 树配置根节点（递归结构） |

**MetricTreeNode 结构**（递归）:
| 字段名     | 类型                    | 说明                                           |
|------------|-------------------------|------------------------------------------------|
| nodeId     | String                  | 节点唯一标识                                   |
| nodeName   | String                  | 节点展示名（对应指标中文名）                   |
| metricId   | String                  | 关联指标ID                                     |
| isRate     | Boolean                 | 是否为比率型指标                               |
| op         | String                  | 运算类型（add/sub/mul/div，叶子节点为null）    |
| dimensions | List\<String\>          | 可做单维归因的维度列表                         |
| params     | Map\<String, Object\>   | 节点参数（如 epThreshold、epTotalThreshold等） |
| children   | List\<MetricTreeNode\>  | 子节点列表（递归结构）                         |

**请求示例**:
```http
GET /attribution/trees/loan_balance_increment/config
```

**响应示例**:
```json
{
  "respCode": "0",
  "respMsg": "success",
  "bizSeq": "BIZ202511240104",
  "data": {
    "treeId": "loan_balance_increment",
    "treeName": "贷款余额增量归因树",
    "metricId": "finance.loan_increase_amount",
    "metricName": "贷款余额增量",
    "version": 1,
    "treeConfig": {
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
          "params": {
            "epThreshold": 0.1,
            "epTotalThreshold": 0.67
          },
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

## 3. 分析任务接口

### 3.1 发起分析任务

**接口地址**: `POST /attribution/tasks`

**功能描述**: 创建一个新的归因分析任务（异步执行）

**请求头**:
| 参数名      | 类型   | 必填 | 说明                          |
|-------------|--------|------|-------------------------------|
| X-User-Name | String | 否   | 创建人用户名（默认：system）  |

**请求体**: `CreateTaskRequest`

**CreateTaskRequest 结构**:
| 字段名                | 类型            | 必填 | 约束           | 说明                     |
|-----------------------|-----------------|------|----------------|--------------------------|
| treeId                | String          | 是   | 非空           | 归因树ID                 |
| listId                | String          | 否   | -              | 名单ID（可选）           |
| contributionThreshold | BigDecimal      | 是   | 0.0-1.0        | 贡献度阈值               |
| timeGranularity       | DateGranularity | 是   | -              | 时间粒度                 |
| baselineDate          | String          | 是   | yyyy-MM-dd     | 基准日期                 |
| compareDate           | String          | 是   | yyyy-MM-dd     | 对比日期                 |

**返回数据**: `TaskCreateDTO`

**TaskCreateDTO 结构**:
| 字段名          | 类型            | 说明                 |
|-----------------|-----------------|----------------------|
| taskId          | String          | 任务唯一标识         |
| timeGranularity | DateGranularity | 时间粒度             |
| baselineDate    | String          | 基准日期（yyyy-MM-dd）|
| compareDate     | String          | 对比日期（yyyy-MM-dd）|

**请求示例**:
```http
POST /attribution/tasks HTTP/1.1
Content-Type: application/json
X-User-Name: zhangsan

{
  "treeId": "loan_balance_increment",
  "listId": null,
  "contributionThreshold": 0.1,
  "timeGranularity": "DAY",
  "baselineDate": "2024-01-01",
  "compareDate": "2024-01-08"
}
```

**响应示例**:
```json
{
  "respCode": "0",
  "respMsg": "success",
  "bizSeq": "BIZ202511240105",
  "data": {
    "taskId": "task_20241124_001",
    "timeGranularity": "DAY",
    "baselineDate": "2024-01-01",
    "compareDate": "2024-01-08"
  }
}
```

---

### 3.2 查询分析任务列表

**接口地址**: `GET /attribution/tasks`

**功能描述**: 按归因树名称与创建人模糊筛选任务列表，支持分页

**请求参数**:
| 参数名   | 类型    | 必填 | 默认值 | 说明                          |
|----------|---------|------|--------|-------------------------------|
| treeName | String  | 否   | -      | 归因树名称（模糊查询）        |
| creator  | String  | 否   | -      | 创建人                        |
| pageNo   | Integer | 否   | 1      | 页码（>=1）                   |
| pageSize | Integer | 否   | 20     | 每页条数（1-100）             |

**返回数据**: `TaskListDTO`

**TaskListDTO 结构**:
| 字段名   | 类型                    | 说明         |
|----------|-------------------------|--------------|
| total    | Integer                 | 总记录数     |
| pageNo   | Integer                 | 当前页码     |
| pageSize | Integer                 | 每页条数     |
| tasks    | List\<AnalysisTaskDTO\> | 任务列表     |

**AnalysisTaskDTO 结构**:
| 字段名                | 类型            | 说明                     |
|-----------------------|-----------------|--------------------------|
| taskId                | String          | 任务唯一标识             |
| treeId                | String          | 归因树ID                 |
| treeName              | String          | 归因树名称               |
| listId                | String          | 名单ID（可为null）       |
| listName              | String          | 名单名称（冗余字段）     |
| contributionThreshold | BigDecimal      | 贡献度阈值               |
| timeGranularity       | DateGranularity | 时间粒度                 |
| baselineDate          | String          | 基准日期（yyyy-MM-dd）   |
| compareDate           | String          | 对比日期（yyyy-MM-dd）   |
| status                | TaskStatus      | 任务状态                 |
| progress              | Integer         | 执行进度（0-100）        |
| message               | String          | 状态说明                 |
| creator               | String          | 创建人                   |
| createTime            | String          | 创建时间（yyyy-MM-dd HH:mm:ss） |
| startTime             | String          | 开始时间（可为null）     |
| endTime               | String          | 结束时间（可为null）     |

**请求示例**:
```http
GET /attribution/tasks?creator=zhangsan&pageNo=1&pageSize=20
```

**响应示例**:
```json
{
  "respCode": "0",
  "respMsg": "success",
  "bizSeq": "BIZ202511240106",
  "data": {
    "total": 50,
    "pageNo": 1,
    "pageSize": 20,
    "tasks": [
      {
        "taskId": "task_20241124_001",
        "treeId": "loan_balance_increment",
        "treeName": "贷款余额增量归因树",
        "listId": null,
        "contributionThreshold": 0.1,
        "timeGranularity": "DAY",
        "baselineDate": "2024-01-01",
        "compareDate": "2024-01-08",
        "status": "SUCCESS",
        "progress": 100,
        "message": "分析完成",
        "creator": "zhangsan",
        "createTime": "2024-11-24 10:00:00",
        "startTime": "2024-11-24 10:00:05",
        "endTime": "2024-11-24 10:05:23"
      },
      {
        "taskId": "task_20241124_002",
        "treeId": "loan_balance_increment",
        "treeName": "贷款余额增量归因树",
        "listId": "roster_001",
        "listName": "测试名单001",
        "contributionThreshold": 0.05,
        "timeGranularity": "WEEK",
        "baselineDate": "2024-01-01",
        "compareDate": "2024-01-08",
        "status": "RUNNING",
        "progress": 65,
        "message": "正在计算维度归因（3/5）",
        "creator": "zhangsan",
        "createTime": "2024-11-24 11:30:00",
        "startTime": "2024-11-24 11:30:05",
        "endTime": null
      },
      {
        "taskId": "task_20241124_003",
        "treeId": "revenue_analysis",
        "treeName": "营收归因分析树",
        "listId": null,
        "contributionThreshold": 0.08,
        "timeGranularity": "MONTH",
        "baselineDate": "2024-01-01",
        "compareDate": "2024-02-01",
        "status": "FAILED",
        "progress": 45,
        "message": "数据查询超时",
        "creator": "lisi",
        "createTime": "2024-11-24 09:00:00",
        "startTime": "2024-11-24 09:00:05",
        "endTime": "2024-11-24 09:03:45"
      }
    ]
  }
}
```

---

### 3.3 查询任务状态

**接口地址**: `GET /attribution/tasks/{taskId}/status`

**功能描述**: 查询指定任务的当前执行状态与基本信息（用于轮询监控）

**路径参数**:
| 参数名 | 类型   | 必填 | 说明     |
|--------|--------|------|----------|
| taskId | String | 是   | 任务ID   |

**返回数据**: `TaskStatusDTO`

**TaskStatusDTO 结构**:
| 字段名     | 类型       | 说明                              |
|------------|------------|-----------------------------------|
| taskId     | String     | 任务唯一标识                      |
| status     | TaskStatus | 任务状态枚举                      |
| progress   | Integer    | 执行进度（百分比，0-100）         |
| message    | String     | 状态说明（如错误信息、当前执行步骤等） |
| createTime | String     | 任务创建时间（yyyy-MM-dd HH:mm:ss）|
| startTime  | String     | 任务开始时间（未开始为null）      |
| endTime    | String     | 任务结束时间（未结束为null）      |

**请求示例**:
```http
GET /attribution/tasks/task_20241124_001/status
```

**响应示例（任务完成）**:
```json
{
  "respCode": "0",
  "respMsg": "success",
  "bizSeq": "BIZ202511240107",
  "data": {
    "taskId": "task_20241124_001",
    "status": "SUCCESS",
    "progress": 100,
    "message": "分析完成",
    "createTime": "2024-11-24 10:00:00",
    "startTime": "2024-11-24 10:00:05",
    "endTime": "2024-11-24 10:05:23"
  }
}
```

**响应示例（任务执行中）**:
```json
{
  "respCode": "0",
  "respMsg": "success",
  "bizSeq": "BIZ202511240108",
  "data": {
    "taskId": "task_20241124_002",
    "status": "RUNNING",
    "progress": 65,
    "message": "正在计算节点维度归因（3/5）",
    "createTime": "2024-11-24 11:30:00",
    "startTime": "2024-11-24 11:30:05",
    "endTime": null
  }
}
```

**响应示例（任务失败）**:
```json
{
  "respCode": "0",
  "respMsg": "success",
  "bizSeq": "BIZ202511240109",
  "data": {
    "taskId": "task_20241124_003",
    "status": "FAILED",
    "progress": 45,
    "message": "数据查询超时：查询指标 finance.disbursement_amount 失败",
    "createTime": "2024-11-24 12:00:00",
    "startTime": "2024-11-24 12:00:05",
    "endTime": "2024-11-24 12:03:45"
  }
}
```

---

### 3.4 查询归因树分析结果

**接口地址**: `GET /attribution/tasks/{taskId}/result`

**功能描述**: 查询归因树的完整分析结果，包含每个节点的波动信息、贡献度及维度归因明细

**路径参数**:
| 参数名 | 类型   | 必填 | 说明     |
|--------|--------|------|----------|
| taskId | String | 是   | 任务ID   |

**返回数据**: `AttributionResultDTO`

**AttributionResultDTO 结构**:
| 字段名          | 类型                       | 说明                       |
|-----------------|----------------------------|----------------------------|
| taskId          | String                     | 任务唯一标识               |
| treeId          | String                     | 归因树ID                   |
| treeName        | String                     | 归因树名称                 |
| timeGranularity | DateGranularity            | 时间粒度                   |
| baselineDate    | String                     | 基准日期（yyyy-MM-dd）     |
| compareDate     | String                     | 对比日期（yyyy-MM-dd）     |
| resultTree      | AttributionTreeResultNode  | 根节点（递归结构，包含分析结果） |

**AttributionTreeResultNode 结构**（递归）:
| 字段名               | 类型                                | 说明                                     |
|----------------------|-------------------------------------|------------------------------------------|
| nodeId               | String                              | 节点唯一标识                             |
| nodeName             | String                              | 节点展示名                               |
| metricId             | String                              | 关联指标ID                               |
| isRate               | Boolean                             | 是否为比率型指标                         |
| op                   | String                              | 运算类型（add/sub/mul/div）              |
| currentValue         | BigDecimal                          | 本周期指标值                             |
| baselineValue        | BigDecimal                          | 对比周期指标值                           |
| deltaValue           | BigDecimal                          | 波动量（绝对值）                         |
| deltaRate            | BigDecimal                          | 波动幅度（相对变化率）                   |
| contributionLocal    | BigDecimal                          | 本层贡献度（相对于父节点）               |
| contributionGlobal   | BigDecimal                          | 整体贡献度（相对于根节点）               |
| dimensionAttribution | List\<DimensionAttributionItem\>    | 维度归因结果列表                         |
| children             | List\<AttributionTreeResultNode\>   | 子节点列表（递归结构）                   |

**DimensionAttributionItem 结构**:
| 字段名         | 类型       | 说明                               |
|----------------|------------|------------------------------------|
| dimension      | String     | 维度名称（如：package_type）       |
| dimensionValue | String     | 维度值（如：随借随还）             |
| currentValue   | BigDecimal | 本周期该维度值的指标值             |
| baselineValue  | BigDecimal | 对比周期该维度值的指标值           |
| deltaValue     | BigDecimal | 波动量                             |
| contribution   | BigDecimal | 贡献度（EP值）                     |
| surprise       | BigDecimal | 惊奇度（Surprise值）               |
| rank           | Integer    | 排序位次                           |

**请求示例**:
```http
GET /attribution/tasks/task_20241124_001/result
```

**响应示例**:
```json
{
  "respCode": "0",
  "respMsg": "success",
  "bizSeq": "BIZ202511240110",
  "data": {
    "taskId": "task_20241124_001",
    "treeId": "loan_balance_increment",
    "treeName": "贷款余额增量归因树",
    "timeGranularity": "DAY",
    "baselineDate": "2024-01-01",
    "compareDate": "2024-01-08",
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
            },
            {
              "dimension": "package_type",
              "dimensionValue": "先息后本",
              "currentValue": 1500.00,
              "baselineValue": 1200.00,
              "deltaValue": 300.00,
              "contribution": 0.1000,
              "surprise": 0.0234,
              "rank": 3
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

### 3.5 查询AI归因报告

**接口地址**: `GET /attribution/tasks/{taskId}/ai-report`

**功能描述**: 查询由 AI 生成的归因分析报告（Markdown 格式文本）

**路径参数**:
| 参数名 | 类型   | 必填 | 说明     |
|--------|--------|------|----------|
| taskId | String | 是   | 任务ID   |

**返回数据**: `AiAttributionReportDTO`

**AiAttributionReportDTO 结构**:
| 字段名        | 类型         | 说明                                    |
|---------------|--------------|-----------------------------------------|
| taskId        | String       | 任务唯一标识                            |
| reportStatus  | ReportStatus | 报告生成状态（GENERATING/COMPLETED/FAILED） |
| reportContent | String       | 完整报告内容（Markdown 格式，大文本字段）|
| generateTime  | String       | 报告生成时间（yyyy-MM-dd HH:mm:ss，生成中可能为null） |

**请求示例**:
```http
GET /attribution/tasks/task_20241124_001/ai-report
```

**响应示例（报告已完成）**:
```json
{
  "respCode": "0",
  "respMsg": "success",
  "bizSeq": "BIZ202511240111",
  "data": {
    "taskId": "task_20241124_001",
    "reportStatus": "COMPLETED",
    "reportContent": "# 贷款余额增量归因分析报告\n\n## 一、总体概况\n\n本期（2024-01-08）贷款余额增量为 **5000.00 万元**，相比基准期（2024-01-01）的 **3000.00 万元** 增加了 **2000.00 万元**，增幅为 **66.67%**。\n\n## 二、关键驱动因素\n\n### 2.1 发放金额（主要正向贡献）\n\n发放金额本期为 **8000.00 万元**，较基准期 **5000.00 万元** 增加了 **3000.00 万元**，贡献度达到 **150%**，是本次增长的主要驱动力。\n\n#### 2.1.1 维度归因详情\n\n按产品类型拆解，贡献度排名前三的维度值为：\n\n1. **随借随还**：贡献度 66.67%，本期 5000.00 万元，基准期 3000.00 万元，增加 2000.00 万元\n2. **等额本息**：贡献度 33.33%，本期 3000.00 万元，基准期 2000.00 万元，增加 1000.00 万元\n3. **先息后本**：贡献度 10.00%，本期 1500.00 万元，基准期 1200.00 万元，增加 300.00 万元\n\n#### 2.1.2 子因素分解\n\n发放金额 = 发放户数 × 户均发放额\n\n- **发放户数**：本期 400 户，基准期 250 户，增加 150 户（+60%），贡献度 50%\n- **户均发放额**：本期与基准期均为 20.00 万元/户，无变化，贡献度 0%\n\n**结论**：发放金额的增长主要由发放户数增加驱动，户均发放额保持稳定。\n\n### 2.2 还款金额（负向影响）\n\n还款金额本期为 **3000.00 万元**，较基准期 **2000.00 万元** 增加了 **1000.00 万元**（+50%），对余额增量形成 **-50%** 的负向贡献。\n\n## 三、建议与展望\n\n1. **持续发力随借随还产品**：该产品类型贡献度最高，建议加大营销投入\n2. **提升户均发放额**：当前户均额度稳定但未增长,可考虑适度提升授信额度\n3. **关注还款趋势**：还款金额增长虽属正常,但需监控是否影响余额规模增长\n\n## 四、附录\n\n- **分析任务ID**：task_20241124_001\n- **归因树**：贷款余额增量归因树\n- **时间粒度**：日（DAY）\n- **基准日期**：2024-01-01\n- **对比日期**：2024-01-08\n- **报告生成时间**：2024-11-24 10:05:30\n\n---\n\n*本报告由 DOS Config AI 分析引擎自动生成*",
    "generateTime": "2024-11-24 10:05:30"
  }
}
```

**响应示例（报告生成中）**:
```json
{
  "respCode": "0",
  "respMsg": "success",
  "bizSeq": "BIZ202511240112",
  "data": {
    "taskId": "task_20241124_002",
    "reportStatus": "GENERATING",
    "reportContent": null,
    "generateTime": null
  }
}
```

**响应示例（报告生成失败）**:
```json
{
  "respCode": "0",
  "respMsg": "success",
  "bizSeq": "BIZ202511240113",
  "data": {
    "taskId": "task_20241124_003",
    "reportStatus": "FAILED",
    "reportContent": null,
    "generateTime": "2024-11-24 12:03:50"
  }
}
```

---

## 4. 完整接口调用流程示例

### 4.1 典型业务流程

```bash
# 步骤1：查询指标列表
curl -G "http://localhost:8080/attribution/metrics" \
  --data-urlencode "metricName=贷款" \
  --data-urlencode "limit=10"

# 步骤2：查看指标趋势
curl -G "http://localhost:8080/attribution/metrics/finance.loan_increase_amount/trend" \
  --data-urlencode "timeGranularity=DAY" \
  --data-urlencode "baselineDate=2024-01-01" \
  --data-urlencode "compareDate=2024-01-08"

# 步骤3：查询归因树列表
curl -G "http://localhost:8080/attribution/trees" \
  --data-urlencode "treeName=贷款"

# 步骤4：查看归因树配置
curl "http://localhost:8080/attribution/trees/loan_balance_increment/config"

# 步骤5：创建分析任务
curl -X POST "http://localhost:8080/attribution/tasks" \
  -H "Content-Type: application/json" \
  -H "X-User-Name: zhangsan" \
  -d '{
    "treeId": "loan_balance_increment",
    "contributionThreshold": 0.1,
    "timeGranularity": "DAY",
    "baselineDate": "2024-01-01",
    "compareDate": "2024-01-08"
  }'

# 步骤6：轮询查询任务状态（每3秒查询一次）
curl "http://localhost:8080/attribution/tasks/task_20241124_001/status"

# 步骤7：任务完成后查询分析结果
curl "http://localhost:8080/attribution/tasks/task_20241124_001/result"

# 步骤8：查询AI归因报告
curl "http://localhost:8080/attribution/tasks/task_20241124_001/ai-report"

# 步骤9：查询任务列表（历史记录）
curl -G "http://localhost:8080/attribution/tasks" \
  --data-urlencode "creator=zhangsan" \
  --data-urlencode "pageNo=1" \
  --data-urlencode "pageSize=20"
```

---

## 5. 数据结构汇总

### 5.1 DTO 速查表

| DTO 类名                          | 用途                     | 关联接口                     |
|-----------------------------------|--------------------------|------------------------------|
| MetricBriefDTO                    | 指标列表条目             | GET /metrics                 |
| MetricTrendDTO                    | 指标趋势数据             | GET /metrics/{id}/trend      |
| ChartDataDTO                      | ECharts 图表数据         | MetricTrendDTO 内嵌          |
| MetricItemDTO                     | 关键指标摘要             | MetricTrendDTO 内嵌          |
| AttributionTreeBriefDTO           | 归因树列表条目           | GET /trees                   |
| AttributionTreeConfigDTO          | 归因树完整配置           | GET /trees/{id}/config       |
| MetricTreeNode                    | 树节点（递归）           | AttributionTreeConfigDTO 内嵌|
| CreateTaskRequest                 | 创建任务请求体           | POST /tasks                  |
| TaskCreateDTO                     | 创建任务响应             | POST /tasks                  |
| TaskListDTO                       | 任务列表（分页）         | GET /tasks                   |
| AnalysisTaskDTO                   | 任务列表条目             | TaskListDTO 内嵌             |
| TaskStatusDTO                     | 任务状态信息             | GET /tasks/{id}/status       |
| AttributionResultDTO              | 归因分析结果             | GET /tasks/{id}/result       |
| AttributionTreeResultNode         | 结果树节点（递归）       | AttributionResultDTO 内嵌    |
| DimensionAttributionItem          | 维度归因明细             | AttributionTreeResultNode 内嵌|
| AiAttributionReportDTO            | AI 报告                  | GET /tasks/{id}/ai-report    |

### 5.2 枚举类速查表

| 枚举类名        | 枚举值                                 | 说明         |
|-----------------|----------------------------------------|--------------|
| TaskStatus      | PENDING / RUNNING / SUCCESS / FAILED / CANCELED | 任务状态     |
| ReportStatus    | GENERATING / COMPLETED / FAILED        | AI 报告状态  |
| DateGranularity | DAY / WEEK / MONTH / YEAR              | 时间粒度     |

---

## 6. 变更记录

| 版本   | 日期       | 变更摘要                                                                                                                       |
|--------|------------|--------------------------------------------------------------------------------------------------------------------------------|
| v1.0.0 | 2024-11-01 | 初始版本                                                                                                                       |
| v1.1.0 | 2024-11-24 | ① 新增 DateGranularity 枚举<br>② 指标趋势接口入参/出参升级（支持时间粒度、ChartDataDTO、MetricItemDTO）<br>③ 创建任务新增 contributionThreshold 参数<br>④ TaskStatus.DONE 改名为 SUCCESS<br>⑤ AI 报告改为 reportContent 大文本字段<br>⑥ 数据库建表及 Mapper 对应调整<br>⑦ 文档结构全面优化，新增详细返回示例和结构体说明 |

---

## 7. 注意事项

1. **异步任务处理**：
   - 创建任务后立即返回 `taskId`，任务在后台异步执行
   - 建议前端每 3-5 秒轮询任务状态接口，直到状态变为 `SUCCESS`/`FAILED`/CANCELED`
   - 任务完成后再调用结果和报告接口

2. **时间粒度选择**：
   - `DAY`：适合日度监控，数据点密集
   - `WEEK`：适合周度对比，减少波动干扰
   - `MONTH`：适合月度分析，趋势更明显
   - `YEAR`：适合年度回顾

3. **贡献度阈值**：
   - 建议范围：0.05 ~ 0.2
   - 过小（<0.05）：会产生大量低贡献度维度值，影响分析效率
   - 过大（>0.3）：可能遗漏重要归因维度

4. **错误处理**：
   - 所有接口失败时 `respCode != "0"`
   - 前端应根据 `respMsg` 提示用户具体错误原因
   - 系统异常（DOS5000）建议联系运维人员

5. **性能优化**：
   - 指标列表和归因树列表建议客户端缓存
   - 避免频繁创建相同参数的分析任务
   - 大规模数据分析任务可能耗时较长（5-10分钟）

---

**文档维护人**：DOS Config 后端组  
**最后更新时间**：2025-11-24 14:30:00  
**联系方式**：dosconfig-backend@example.com
