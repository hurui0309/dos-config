-- ============================================================
-- 指标异动根因定位系统 · 场景化测试数据
-- Case: 日度贷款余额增量分析（任务成功 + AI 报告完成）
-- Author: GPT-5.1 Codex
-- Date: 2025-11-25
-- 使用说明：
--   1. 请先执行 init_schema.sql 初始化库表
--   2. 再执行本脚本，插入一套完整的、可演示的测试数据
-- ============================================================

USE metric_attribution;

-- ============================================================
-- 0. 清理旧数据（仅影响本案例的主键）
-- ============================================================
DELETE FROM t_ai_report WHERE task_id = 'CASE_TASK_20241124001';
DELETE FROM t_attribution_result WHERE task_id = 'CASE_TASK_20241124001';
DELETE FROM t_analysis_task WHERE task_id = 'CASE_TASK_20241124001';
DELETE FROM t_attribution_tree WHERE tree_id = 'case_loan_balance_increment';

-- ============================================================
-- 1. 指标数据
--    为防止重复，这里优先插入若不存在的指标
-- ============================================================
INSERT INTO t_metric (metric_id, metric_name, metric_desc, table_name, field_name, create_time, update_time)
VALUES
    ('finance.loan_increase_amount', '贷款余额增量', '比较周期贷款余额之差', 't_finance_metric', 'loan_increase_amount', NOW(), NOW()),
    ('finance.disbursement_amount', '发放金额', '贷款发放总额', 't_finance_metric', 'disbursement_amount', NOW(), NOW()),
    ('finance.repayment_amount', '还款金额', '贷款还款总额', 't_finance_metric', 'repayment_amount', NOW(), NOW()),
    ('finance.drawdown_cust_cnt', '发放户数', '完成提款的客户数量', 't_finance_metric', 'drawdown_cust_cnt', NOW(), NOW()),
    ('finance.avg_drawdown_amt', '户均发放额', '单户平均发放金额', 't_finance_metric', 'avg_drawdown_amt', NOW(), NOW())
ON DUPLICATE KEY UPDATE
    metric_name = VALUES(metric_name),
    metric_desc = VALUES(metric_desc),
    update_time = NOW();

-- ============================================================
-- 2. 归因树配置（与文档示例保持一致）
-- ============================================================
INSERT INTO t_attribution_tree (
    tree_id, tree_name, metric_id, metric_name, version, global_filter, tree_config, create_time, update_time
) VALUES (
    'case_loan_balance_increment',
    '贷款余额增量归因树（演示版）',
    'finance.loan_increase_amount',
    '贷款余额增量',
    1,
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
          "dimensions": ["package_type", "ent_credit_period"],
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
    NOW(),
    NOW()
) ON DUPLICATE KEY UPDATE
    tree_name = VALUES(tree_name),
    global_filter = VALUES(global_filter),
    tree_config = VALUES(tree_config),
    update_time = NOW();

-- ============================================================
-- 3. 分析任务（成功完成的样例）
-- ============================================================
INSERT INTO t_analysis_task (
    task_id, tree_id, tree_name, list_id, list_name, contribution_threshold,
    time_granularity, baseline_date, compare_date,
    status, progress, message,
    creator, create_time, start_time, end_time, update_time
) VALUES (
    'CASE_TASK_20241124001',
    'case_loan_balance_increment',
    '贷款余额增量归因树（演示版）',
    NULL,
    NULL,
    0.100000,
    'DAY',
    '2024-01-01',
    '2024-01-08',
    'SUCCESS',
    100,
    '分析完成：发放金额贡献 150%，还款金额抵消 50%',
    'zhangsan',
    '2024-11-24 10:00:00',
    '2024-11-24 10:00:05',
    '2024-11-24 10:05:23',
    NOW()
);

-- ============================================================
-- 4. 归因分析结果（与文档示例一致的 JSON）
-- ============================================================
INSERT INTO t_attribution_result (task_id, result_tree, create_time)
VALUES (
    'CASE_TASK_20241124001',
    '{
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
    }',
    NOW()
);

-- ============================================================
-- 5. AI 归因报告
-- ============================================================
INSERT INTO t_ai_report (
    task_id, report_status, report_content, generate_time, create_time
) VALUES (
    'CASE_TASK_20241124001',
    'COMPLETED',
    '# 贷款余额增量归因分析报告\n\n## 一、总体概况\n- 基准期：2024-01-01\n- 对比期：2024-01-08\n- 指标：贷款余额增量（finance.loan_increase_amount）\n- 本期值：5000.00 万元，较基准期 3000.00 万元 增长 2000.00 万元（+66.67%）\n\n## 二、关键驱动因素\n### 2.1 发放金额（正向贡献 150%）\n- 本期 8000.00 万元 / 基准期 5000.00 万元\n- 贡献来源：\n  1. 随借随还 +2000.00 万元（贡献 66.67%）\n  2. 等额本息 +1000.00 万元（贡献 33.33%）\n  3. 先息后本 +300.00 万元（贡献 10.00%）\n- 细分因素：发放户数提升 150 户（+60%），户均发放额稳定\n\n### 2.2 还款金额（负向贡献 -50%）\n- 本期 3000.00 万元 / 基准期 2000.00 万元\n- 提示：需关注集中还款对余额的影响\n\n## 三、结论与建议\n1. 继续重点运营随借随还产品，巩固优势客群\n2. 结合名单筛选，扩大发放户数提升成果\n3. 建立还款波动预警，避免对余额增量造成过大压力\n\n## 四、任务信息\n- 任务 ID：CASE_TASK_20241124001\n- 归因树：贷款余额增量归因树（演示版）\n- 时间粒度：日（DAY）\n- 创建人：zhangsan\n- 报告生成时间：2024-11-24 10:05:30\n',
    '2024-11-24 10:05:30',
    '2024-11-24 10:00:00'
);

-- ============================================================
-- 6. 验证查询
-- ============================================================
SELECT '=== 案例任务信息 ===' AS '';
SELECT task_id, tree_id, status, progress, baseline_date, compare_date FROM t_analysis_task WHERE task_id = 'CASE_TASK_20241124001';

SELECT '=== 归因结果（摘要） ===' AS '';
SELECT task_id, JSON_EXTRACT(result_tree, '$.nodeName') AS rootName FROM t_attribution_result WHERE task_id = 'CASE_TASK_20241124001';

SELECT '=== AI 报告状态 ===' AS '';
SELECT task_id, report_status, generate_time FROM t_ai_report WHERE task_id = 'CASE_TASK_20241124001';

SELECT '测试案例数据插入完成！' AS '';


