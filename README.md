# 指标异动根因定位系统

## 项目简介

这是一个基于 Spring Boot 的指标异动根因定位系统，用于分析业务指标的波动原因，提供多维度归因分析和 AI 智能报告生成功能。

## 技术栈

- **框架**: Spring Boot 3.2.0
- **数据库**: MySQL 8.0+
- **ORM**: Spring Data JPA
- **构建工具**: Maven
- **Java版本**: Java 17

## 项目结构

```
src/main/java/com/rootcause/
├── MetricAttributionApplication.java    # 启动类
├── common/                              # 公共类
│   └── ApiResponse.java                 # 统一响应包装
├── config/                              # 配置类
│   └── JacksonConfig.java              # Jackson配置
├── controller/                          # 控制器层
│   └── AttributionController.java       # 归因分析接口
├── dto/                                 # 数据传输对象
│   ├── request/                         # 请求DTO
│   │   └── CreateTaskRequest.java       # 创建任务请求
│   └── response/                        # 响应DTO
│       ├── MetricBriefDTO.java          # 指标简要信息
│       ├── MetricTrendDTO.java          # 指标趋势
│       ├── AttributionTreeBriefDTO.java # 归因树简要信息
│       ├── AttributionTreeConfigDTO.java# 归因树配置
│       ├── TaskCreateDTO.java           # 任务创建响应
│       ├── TaskListDTO.java             # 任务列表
│       ├── TaskStatusDTO.java           # 任务状态
│       ├── AttributionResultDTO.java    # 归因分析结果
│       └── AiAttributionReportDTO.java  # AI归因报告
├── entity/                              # 实体类
│   ├── Metric.java                      # 核心指标
│   ├── AttributionTree.java             # 归因树配置
│   ├── AnalysisTask.java                # 分析任务
│   ├── AttributionResult.java           # 归因分析结果
│   └── AiReport.java                    # AI归因报告
├── enums/                               # 枚举类
│   ├── TaskStatus.java                  # 任务状态
│   ├── ReportStatus.java                # 报告状态
│   └── OperationType.java               # 运算类型
├── exception/                           # 异常处理
│   ├── BusinessException.java           # 业务异常
│   └── GlobalExceptionHandler.java      # 全局异常处理器
├── repository/                          # 数据访问层
│   ├── MetricRepository.java
│   ├── AttributionTreeRepository.java
│   ├── AnalysisTaskRepository.java
│   ├── AttributionResultRepository.java
│   └── AiReportRepository.java
└── service/                             # 服务层
    ├── AttributionService.java          # 服务接口
    └── impl/
        └── AttributionServiceImpl.java  # 服务实现

dbScript/                                # 数据库脚本
└── v1.0.0/
    └── init_schema.sql                  # 初始化建表脚本
```

## 核心功能

### 1. 指标管理
- 查询核心指标列表
- 查询核心指标趋势数据

### 2. 归因树管理
- 查询归因树列表
- 查询归因树配置结构

### 3. 分析任务管理
- 创建分析任务（支持名单ID）
- 查询任务列表（分页+筛选）
- 查询任务状态

### 4. 归因分析
- 查询归因分析结果
- 查询AI生成的归因报告

## API接口

所有接口统一返回格式：
```json
{
  "respCode": "0000",
  "respMsg": "success",
  "bizSeq": "BIZ20251120xxxx",
  "data": {}
}
```

### 接口列表

| 接口 | 方法 | 路径 | 说明 |
|------|------|------|------|
| 查询指标列表 | GET | `/attribution/metrics` | 支持模糊搜索 |
| 查询指标趋势 | GET | `/attribution/metrics/{metricId}/trend` | 获取指标时序数据 |
| 查询归因树列表 | GET | `/attribution/trees` | 支持模糊搜索 |
| 查询归因树配置 | GET | `/attribution/trees/{treeId}/config` | 获取完整树结构 |
| 创建分析任务 | POST | `/attribution/tasks` | 发起归因分析 |
| 查询任务列表 | GET | `/attribution/tasks` | 分页查询 |
| 查询任务状态 | GET | `/attribution/tasks/{taskId}/status` | 获取执行进度 |
| 查询归因结果 | GET | `/attribution/tasks/{taskId}/result` | 获取分析结果 |
| 查询AI报告 | GET | `/attribution/tasks/{taskId}/ai-report` | 获取AI报告 |

详细API文档请参考：`.cursor/rules/reference_info/指标异动根因定位_API接口文档.md`

## 快速开始

### 1. 数据库初始化

```bash
# 创建数据库并执行初始化脚本
mysql -u root -p < dbScript/v1.0.0/init_schema.sql
```

### 2. 配置数据库连接

修改 `src/main/resources/application.properties`：

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/metric_attribution?useUnicode=true&characterEncoding=utf8mb4&serverTimezone=Asia/Shanghai
spring.datasource.username=your_username
spring.datasource.password=your_password
```

### 3. 构建项目

```bash
mvn clean install
```

### 4. 启动应用

```bash
mvn spring-boot:run
```

应用默认在 `http://localhost:8080` 启动。

### 5. 测试接口

```bash
# 查询指标列表
curl -X GET "http://localhost:8080/attribution/metrics?metricName=发放"

# 查询归因树列表
curl -X GET "http://localhost:8080/attribution/trees"

# 创建分析任务
curl -X POST "http://localhost:8080/attribution/tasks" \
  -H "Content-Type: application/json" \
  -H "X-User-Name: admin" \
  -d '{
    "treeId": "loan_balance_increment",
    "rosterId": "roster_001",
    "contributionThreshold": 0.05,
    "baselineStartDate": "2025-09-01",
    "baselineEndDate": "2025-09-07",
    "compareStartDate": "2025-09-08",
    "compareEndDate": "2025-09-14"
  }'
```

## 数据库表结构

### 主要数据表

1. **t_metric** - 核心指标表
   - 存储指标基础信息和数据源配置

2. **t_attribution_tree** - 归因树配置表
   - 存储归因树的层次结构（JSON格式）

3. **t_analysis_task** - 分析任务表
   - 记录每次分析任务的参数和状态
   - **包含roster_id字段用于记录名单ID**

4. **t_attribution_result** - 归因分析结果表
   - 存储归因分析的完整结果（JSON格式）

5. **t_ai_report** - AI归因报告表
   - 存储AI生成的分析报告

## 待实现功能

### 1. 指标趋势查询（TODO）
位置：`AttributionServiceImpl.getMetricTrend()`

需要实现：
- 从实际数据表查询指标时序数据
- 计算基准期和对比期的统计指标

### 2. 归因分析核心算法（TODO）
位置：`AttributionServiceImpl.executeAttributionAnalysisAsync()`

需要实现：
- **Adtributor算法实现**
  - 多维归因分析
  - EP值和Surprise值计算
  - 贡献度计算
- **数据查询逻辑**
  - 从业务数据表查询指标值
  - 按维度聚合数据
- **结果树构建**
  - 递归计算每个节点的指标值
  - 计算本层和整体贡献度
- **AI报告生成**
  - 调用大模型API
  - 生成分析报告和建议

参考文档：
- `.cursor/rules/reference_info/Adtributor算法SQL实现可行性分析.md`
- `.cursor/rules/reference_info/adtributor.py`
- `.cursor/rules/reference_info/指标异动根因定位_详细设计.md`

## 开发规范

项目遵循以下开发规范（详见 `.cursor/rules/` 目录）：

- DTO规范：使用record类型，包含参数校验
- Entity规范：使用JPA注解，ID自增策略
- Repository规范：继承JpaRepository，使用JPQL
- Service规范：接口+实现类模式，返回DTO
- Controller规范：统一异常处理，返回ApiResponse

## 配置说明

### 异步任务配置

```properties
spring.task.execution.pool.core-size=5
spring.task.execution.pool.max-size=10
spring.task.execution.pool.queue-capacity=100
```

### Jackson配置

- 日期格式：`yyyy-MM-dd HH:mm:ss`
- 时区：Asia/Shanghai
- LocalDateTime自动序列化

## 测试数据

数据库初始化脚本包含以下测试数据：

### 指标数据
- finance.loan_increase_amount - 贷款余额增量
- finance.disbursement_amount - 发放金额
- finance.repayment_amount - 还款金额
- finance.drawdown_cust_cnt - 发放户数
- finance.avg_drawdown_amt - 户均发放额

### 归因树配置
- loan_balance_increment - 贷款余额增量归因树

## 许可证

Copyright © 2025 All rights reserved.

"# dos-config" 
