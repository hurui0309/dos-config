-- ============================================
-- 指标异动根因定位系统 - 数据库初始化脚本
-- Version: 1.0.0
-- Date: 2025-11-20
-- ============================================

-- 创建数据库（如果不存在）
CREATE DATABASE IF NOT EXISTS metric_attribution 
DEFAULT CHARACTER SET utf8mb4 
COLLATE utf8mb4_bin;

USE metric_attribution;

-- ============================================
-- 1. 核心指标表
-- ============================================
DROP TABLE IF EXISTS `t_metric`;

CREATE TABLE `t_metric` (
  `metric_id` VARCHAR(100) NOT NULL COMMENT '指标唯一标识',
  `metric_name` VARCHAR(200) NOT NULL COMMENT '指标中文名称',
  `metric_desc` VARCHAR(500) DEFAULT NULL COMMENT '指标描述',
  `table_name` VARCHAR(100) DEFAULT NULL COMMENT '数据表名',
  `field_name` VARCHAR(100) DEFAULT NULL COMMENT '指标字段名',
  `create_time` DATETIME NOT NULL COMMENT '创建时间',
  `update_time` DATETIME NOT NULL COMMENT '更新时间',
  PRIMARY KEY (`metric_id`),
  KEY `idx_metric_name` (`metric_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin COMMENT='核心指标表';

-- ============================================
-- 2. 归因树配置表
-- ============================================
DROP TABLE IF EXISTS `t_attribution_tree`;

CREATE TABLE `t_attribution_tree` (
  `tree_id` VARCHAR(100) NOT NULL COMMENT '归因树唯一标识',
  `tree_name` VARCHAR(100) NOT NULL COMMENT '归因树中文名称',
  `metric_id` VARCHAR(100) NOT NULL COMMENT '关联指标ID',
  `metric_name` VARCHAR(200) NOT NULL COMMENT '关联指标中文名称',
  `version` INT NOT NULL DEFAULT 1 COMMENT '配置版本号',
  `global_filter` TEXT DEFAULT NULL COMMENT '全局过滤条件（JSON）',
  `tree_config` TEXT NOT NULL COMMENT '树结构配置（JSON格式）',
  `create_time` DATETIME NOT NULL COMMENT '创建时间',
  `update_time` DATETIME NOT NULL COMMENT '更新时间',
  PRIMARY KEY (`tree_id`),
  KEY `idx_tree_name` (`tree_name`),
  KEY `idx_metric_id` (`metric_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin COMMENT='归因树配置表';

-- ============================================
-- 3. 分析任务表
-- ============================================
DROP TABLE IF EXISTS `t_analysis_task`;

CREATE TABLE `t_analysis_task` (
  `task_id` VARCHAR(100) NOT NULL COMMENT '任务唯一标识',
  `tree_id` VARCHAR(100) NOT NULL COMMENT '归因树ID',
  `tree_name` VARCHAR(100) DEFAULT NULL COMMENT '归因树名称（冗余字段）',
  `list_id` VARCHAR(100) DEFAULT NULL COMMENT '名单ID（可选）',
  `list_name` VARCHAR(200) DEFAULT NULL COMMENT '名单名称（冗余字段）',
  `contribution_threshold` DECIMAL(24,6) DEFAULT NULL COMMENT '贡献度阈值',
  `time_granularity` VARCHAR(20) NOT NULL COMMENT '时间粒度：DAY-日，WEEK-周，MONTH-月，YEAR-年',
  `baseline_date` DATE NOT NULL COMMENT '基准日期',
  `compare_date` DATE NOT NULL COMMENT '对比日期',
  `status` VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT '任务状态：PENDING-待执行，RUNNING-执行中，SUCCESS-已完成，FAILED-失败，CANCELED-已取消',
  `progress` INT DEFAULT 0 COMMENT '执行进度（0-100）',
  `message` VARCHAR(500) DEFAULT NULL COMMENT '状态说明',
  `creator` VARCHAR(100) DEFAULT NULL COMMENT '创建人',
  `create_time` DATETIME NOT NULL COMMENT '创建时间',
  `start_time` DATETIME DEFAULT NULL COMMENT '开始时间',
  `end_time` DATETIME DEFAULT NULL COMMENT '结束时间',
  `update_time` DATETIME NOT NULL COMMENT '更新时间',
  PRIMARY KEY (`task_id`),
  KEY `idx_tree_id` (`tree_id`),
  KEY `idx_tree_name` (`tree_name`),
  KEY `idx_list_id` (`list_id`),
  KEY `idx_status` (`status`),
  KEY `idx_creator` (`creator`),
  KEY `idx_create_time` (`create_time`),
  KEY `idx_time_granularity` (`time_granularity`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin COMMENT='分析任务表';

-- ============================================
-- 4. 归因分析结果表
-- ============================================
DROP TABLE IF EXISTS `t_attribution_result`;

CREATE TABLE `t_attribution_result` (
  `task_id` VARCHAR(100) NOT NULL COMMENT '任务ID',
  `result_tree` LONGTEXT NOT NULL COMMENT '结果树（JSON格式）',
  `create_time` DATETIME NOT NULL COMMENT '创建时间',
  PRIMARY KEY (`task_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin COMMENT='归因分析结果表';

-- ============================================
-- 5. AI归因报告表
-- ============================================
DROP TABLE IF EXISTS `t_ai_report`;

CREATE TABLE `t_ai_report` (
  `task_id` VARCHAR(100) NOT NULL COMMENT '任务ID',
  `report_status` VARCHAR(20) NOT NULL DEFAULT 'GENERATING' COMMENT '报告生成状态：GENERATING-生成中，COMPLETED-已完成，FAILED-生成失败',
  `report_content` LONGTEXT DEFAULT NULL COMMENT '报告内容（完整报告内容，大TEXT）',
  `generate_time` DATETIME DEFAULT NULL COMMENT '生成时间',
  `create_time` DATETIME NOT NULL COMMENT '创建时间',
  PRIMARY KEY (`task_id`),
  KEY `idx_report_status` (`report_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin COMMENT='AI归因报告表';

-- ============================================
-- 初始化测试数据（可选）
-- ============================================

-- 插入测试指标数据
INSERT INTO `t_metric` (`metric_id`, `metric_name`, `metric_desc`, `table_name`, `field_name`, `create_time`, `update_time`) VALUES
('finance.loan_increase_amount', '贷款余额增量', '贷款余额增量指标', 't_finance_metric', 'loan_increase_amount', NOW(), NOW()),
('finance.disbursement_amount', '发放金额', '贷款发放金额指标', 't_finance_metric', 'disbursement_amount', NOW(), NOW()),
('finance.repayment_amount', '还款金额', '贷款还款金额指标', 't_finance_metric', 'repayment_amount', NOW(), NOW()),
('finance.drawdown_cust_cnt', '发放户数', '贷款发放户数指标', 't_finance_metric', 'drawdown_cust_cnt', NOW(), NOW()),
('finance.avg_drawdown_amt', '户均发放额', '户均贷款发放额指标', 't_finance_metric', 'avg_drawdown_amt', NOW(), NOW());

-- 插入测试归因树配置
INSERT INTO `t_attribution_tree` (`tree_id`, `tree_name`, `metric_id`, `version`, `global_filter`, `tree_config`, `create_time`, `update_time`) VALUES
('loan_balance_increment', '贷款余额增量归因树', 'finance.loan_increase_amount', 1, 
'{
  "nodeType": "relation",
  "relation": "AND",
  "subConditions": [
    {
      "nodeType": "field",
      "field": "dim_calendar_a.fmt_date",
      "op": "GTE",
      "value": "2023-01-01"
    }
  ]
}',
'{
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
}', 
NOW(), NOW());

-- ============================================
-- 查询验证
-- ============================================
SELECT '=== 指标表数据 ===' AS '';
SELECT * FROM t_metric;

SELECT '=== 归因树配置数据 ===' AS '';
SELECT tree_id, tree_name, version FROM t_attribution_tree;

SELECT '数据库初始化完成！' AS '';

