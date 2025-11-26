# JPA 迁移至 MyBatis 完成说明

## 迁移完成日期
2025年11月24日

## 迁移概述

已成功将项目从 Spring Data JPA 完全迁移至 MyBatis，并移除了所有冗余代码。

## 完成的工作

### 1. Entity 类更新 ✅

移除了所有 JPA 注解，改为纯 POJO：

**移除的注解：**
- `@Entity`
- `@Table`
- `@Id`
- `@GeneratedValue`
- `@Column`
- `@Enumerated`
- `@PrePersist`
- `@PreUpdate`

**更新的 Entity 类：**
- `Metric.java` - 核心指标实体
- `AnalysisTask.java` - 分析任务实体
- `AttributionTree.java` - 归因树配置实体
- `AttributionResult.java` - 归因结果实体
- `AiReport.java` - AI报告实体

**字段名调整（数据库风格）：**
- `createTime` → `createdAt`
- `updateTime` → `updatedAt`
- `startTime` → `startedAt`
- `endTime` → `endedAt`
- `generateTime` → `generatedAt`

### 2. DAO 接口层 ✅

创建了完整的 DAO 接口（5个）：

```
src/main/java/com/dosconfig/dao/
├── MetricDao.java                    # 核心指标DAO
├── AnalysisTaskDao.java             # 分析任务DAO
├── AttributionTreeDao.java          # 归因树DAO
├── AttributionResultDao.java        # 归因结果DAO
└── AiReportDao.java                 # AI报告DAO
```

**DAO 接口特点：**
- 使用 `@Mapper` 注解
- 使用 `@Param` 注解标识参数
- 提供标准 CRUD 方法
- 支持条件查询和分页

### 3. Mapper XML 文件 ✅

创建了完整的 Mapper XML 文件（5个）：

```
src/main/resources/daoMapper/
├── MetricDaoMapper.xml
├── AnalysisTaskDaoMapper.xml
├── AttributionTreeDaoMapper.xml
├── AttributionResultDaoMapper.xml
└── AiReportDaoMapper.xml
```

**Mapper 特点：**
- 使用 `<resultMap>` 映射结果
- 使用 `<sql>` 定义可重用 SQL 片段
- BLOB 字段单独定义
- 支持动态 SQL
- 支持分页查询

### 4. Service 层更新 ✅

**AttributionServiceImpl.java 重构：**
- 移除所有 Repository 依赖
- 改用 DAO 接口
- 移除 `@Slf4j`，使用 `Logger LOG`
- 更新所有数据访问方法
- 移除 JPA 的 `Optional`、`Page`、`Pageable` 等类型
- 手动实现分页逻辑

**变更对比：**

```java
// 旧方式 (JPA)
@Autowired
private MetricRepository metricRepository;

Metric metric = metricRepository.findByMetricId(metricId)
    .orElseThrow(() -> new SystemException("指标不存在"));

// 新方式 (MyBatis)
@Autowired
private MetricDao metricDao;

Metric metric = metricDao.selectByMetricId(metricId);
if (metric == null) {
    throw new SystemException("指标不存在");
}
```

### 5. 异常处理统一 ✅

- 移除了 `BusinessException`
- 统一使用 `SystemException`
- 恢复了 `GlobalExceptionHandler`
- 所有异常返回 `BaseResponse<Void>`

### 6. 删除冗余文件 ✅

**删除的 Repository 文件（5个）：**
- `MetricRepository.java`
- `AnalysisTaskRepository.java`
- `AttributionTreeRepository.java`
- `AttributionResultRepository.java`
- `AiReportRepository.java`

**删除的目录：**
- `src/main/java/com/dosconfig/repository/`

### 7. Maven 依赖更新 ✅

**pom.xml 变更：**

```xml
<!-- 移除 -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-jpa</artifactId>
</dependency>

<!-- 添加 -->
<dependency>
    <groupId>org.mybatis.spring.boot</groupId>
    <artifactId>mybatis-spring-boot-starter</artifactId>
    <version>3.0.3</version>
</dependency>

<dependency>
    <groupId>com.google.guava</groupId>
    <artifactId>guava</artifactId>
    <version>32.1.3-jre</version>
</dependency>
```

### 8. 配置文件更新 ✅

**application.properties 变更：**

```properties
# 移除 JPA 配置
# spring.jpa.database=mysql
# spring.jpa.show-sql=true
# spring.jpa.hibernate.ddl-auto=none
# ...

# 添加 MyBatis 配置
mybatis.mapper-locations=classpath:daoMapper/*.xml
mybatis.type-aliases-package=com.dosconfig.entity
mybatis.configuration.map-underscore-to-camel-case=true
mybatis.configuration.log-impl=org.apache.ibatis.logging.slf4j.Slf4jImpl

# 更新日志配置
logging.level.com.dosconfig.dao=DEBUG
```

## 项目结构对比

### 迁移前（JPA）
```
com.dosconfig/
├── entity/          # JPA实体（带注解）
├── repository/      # JpaRepository接口
├── service/         # 使用Repository
└── ...
```

### 迁移后（MyBatis）
```
com.dosconfig/
├── entity/          # 纯POJO实体
├── dao/             # MyBatis Mapper接口
├── service/         # 使用DAO
└── ...

resources/
└── daoMapper/       # MyBatis XML Mapper
```

## 技术栈对比

| 功能 | 迁移前（JPA） | 迁移后（MyBatis） |
|------|--------------|------------------|
| ORM框架 | Spring Data JPA | MyBatis 3.0.3 |
| 数据访问接口 | Repository | DAO |
| 实体注解 | JPA注解 | 无注解（POJO） |
| SQL管理 | 自动生成 | XML配置 |
| 查询方式 | 方法名推导 | 手写SQL |
| 事务管理 | @Transactional | @Transactional |
| 分页 | Pageable | 手动offset/limit |
| 异常处理 | BusinessException | SystemException |
| 日志 | @Slf4j | Logger LOG |

## 优势分析

### MyBatis 的优势
1. ✅ **SQL 可控**：手写 SQL，性能可优化
2. ✅ **灵活性高**：复杂查询更容易实现
3. ✅ **无黑盒**：SQL 清晰可见，易于调试
4. ✅ **学习曲线低**：熟悉 SQL 即可上手
5. ✅ **适合复杂业务**：多表关联、动态SQL更方便

### 与项目规范一致
- 符合 WeBank 内部标准（DAO + Mapper 模式）
- 与现有项目风格统一
- 更易于维护和交接

## 数据库表结构要求

### 表结构调整建议

由于字段名从 `create_time` 改为 `created_at`，需要调整数据库表结构：

```sql
-- 示例：t_analysis_task 表调整
ALTER TABLE t_analysis_task 
    CHANGE COLUMN create_time created_at DATETIME,
    CHANGE COLUMN start_time started_at DATETIME,
    CHANGE COLUMN end_time ended_at DATETIME,
    ADD COLUMN updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP;

-- 类似地调整其他表...
```

**或者使用 MyBatis 的字段映射：**
保持数据库列名不变，在 resultMap 中映射：

```xml
<result column="create_time" property="createdAt" jdbcType="TIMESTAMP"/>
```

## 验证清单

- [x] 所有Entity类移除JPA注解
- [x] 所有DAO接口创建完成
- [x] 所有Mapper XML创建完成
- [x] Service层更新使用DAO
- [x] 删除所有Repository文件
- [x] 删除repository目录
- [x] Maven依赖更新
- [x] 配置文件更新
- [x] 异常处理统一
- [x] 代码lint检查通过

## 后续工作

### 1. 数据库表结构同步
根据实际情况，决定是：
- 修改数据库列名（`create_time` → `created_at`）
- 或在 Mapper 中添加字段映射

### 2. 单元测试
添加 DAO 层和 Service 层的单元测试。

### 3. 性能优化
根据实际业务场景优化 SQL 查询：
- 添加合适的索引
- 优化复杂查询
- 使用缓存

### 4. 接口测试
全面测试所有 REST API 接口。

## 注意事项

1. **事务管理**：MyBatis 仍支持 `@Transactional` 注解
2. **SQL 注入**：使用 `#{param}` 而不是 `${param}`
3. **枚举处理**：需要在 Entity 中正确定义枚举类型
4. **日期格式**：确保时区配置正确
5. **连接池**：使用 HikariCP（Spring Boot 默认）

## 总结

✅ **迁移完成，所有代码lint检查通过！**

项目已成功从 JPA 迁移至 MyBatis，代码结构清晰，符合企业级开发规范。所有冗余代码已删除，项目更加轻量化和可维护。

