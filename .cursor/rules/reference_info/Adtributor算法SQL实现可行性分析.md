# Adtributor 算法 StarRocks SQL 实现可行性分析

## 一、可行性分析

### 1.1 算法核心逻辑分析

Adtributor 算法的核心计算包括以下几个部分：

#### 1.1.1 基础指标计算
- **f_all（预测值总和）**: `SUM(predict)`
- **a_all（实际值总和）**: `SUM(real)`
- **EP（贡献度）**: `(a - f) / (a_all - f_all)`
  - a: 维度值的实际值
  - f: 维度值的预测值

#### 1.1.2 惊奇度（Surprise）计算
```
pp = a / a_all
qq = f / f_all

if (pp != 0) and (qq != 0):
    ss = 0.5 * (pp * log10(2 * pp / (pp + qq)) + qq * log10(2 * qq / (pp + qq)))
elif pp == 0:
    ss = 0.5 * qq * log10(2)
else:
    ss = 0.5 * pp * log10(2)
```

### 1.2 StarRocks SQL 能力评估

| 功能需求 | StarRocks 支持情况 | 说明 |
|---------|------------------|------|
| 分组聚合 | 完全支持 | `GROUP BY` 和聚合函数（SUM、COUNT等） |
| 窗口函数 | 完全支持 | 可计算全局总和（`SUM() OVER()`） |
| 数学函数 | 完全支持 | **LOG10**、LOG、ABS、POWER 等（惊奇度必须使用 LOG10） |
| 条件表达式 | 完全支持 | CASE WHEN、IF、COALESCE 等 |
| WITH CTE | 完全支持 | 可构建多层查询结构 |
| 排序和筛选 | 完全支持 | ORDER BY、WHERE、HAVING 等 |

### 1.3 实现难点与解决方案

| 难点 | 解决方案 | 可行性 |
|-----|---------|-------|
| 惊奇度的条件分支计算 | 使用 CASE WHEN 嵌套实现 | 可行 |
| 按维度分别计算 | 使用 UNION ALL 合并多个维度的计算结果 | 可行 |
| 阈值筛选和排序 | WHERE + ORDER BY + LIMIT | 可行 |
| 多维度组合分析 | 需要笛卡尔积或多次 UNION，计算复杂度高 | 单维度可行，多维度需优化 |

### 1.4 可行性结论

**基于 StarRocks SQL 实现 Adtributor 算法是可行的**

**优势：**
1. StarRocks 提供了完整的 SQL 标准函数支持（包括关键的 LOG10 函数）
2. 窗口函数可以高效计算全局指标
3. CTE 可以构建清晰的计算流程
4. 支持高性能的分析查询
5. 经过 Python 验证，SQL 计算结果准确无误

**限制：**
1. SQL 实现相对于 Python 代码可读性较差
2. 多维度组合分析时 SQL 会变得复杂
3. 迭代式的根因定位逻辑在 SQL 中较难实现

**建议：**
- 适合单层、单维度或少量维度的根因分析
- 复杂的多层递归分析建议使用 Python + SQL 混合模式
- 可以将核心计算逻辑用 SQL 实现，结果处理用 Python

---

## 二、数据准备与 SQL 示例

### 2.1 测试数据插入

```sql
-- 插入 2024-10-05 的数据（作为预测基准日）
INSERT INTO dws_custom_kj_serial_guarantee_txn_da 
(loan_serial_no, op, father_serial_no, ccif, put_out_date, business_sum, actual_business_rate, due_status, business_interest_sum)
VALUES
-- 正常状态的借据
('20241005001', 'add', 'F001', 'C001', '2024-10-05', 100000.00, 0.045000, '正常', 5000.00),
('20241005002', 'add', 'F002', 'C002', '2024-10-05', 150000.00, 0.050000, '正常', 7500.00),
('20241005003', 'add', 'F003', 'C003', '2024-10-05', 200000.00, 0.048000, '正常', 9600.00),
('20241005004', 'add', 'F004', 'C004', '2024-10-05', 120000.00, 0.047000, '正常', 5640.00),
('20241005005', 'add', 'F005', 'C005', '2024-10-05', 180000.00, 0.049000, '正常', 8820.00),

-- 逾期状态的借据
('20241005006', 'add', 'F006', 'C006', '2024-10-05', 80000.00, 0.052000, '逾期', 4160.00),
('20241005007', 'add', 'F007', 'C007', '2024-10-05', 90000.00, 0.051000, '逾期', 4590.00),

-- 结清状态的借据
('20241005008', 'add', 'F008', 'C008', '2024-10-05', 50000.00, 0.046000, '结清', 2300.00),
('20241005009', 'add', 'F009', 'C009', '2024-10-05', 60000.00, 0.047000, '结清', 2820.00);


-- 插入 2024-10-12 的数据（实际观测日，逾期金额异常增加）
INSERT INTO dws_custom_kj_serial_guarantee_txn_da 
(loan_serial_no, op, father_serial_no, ccif, put_out_date, business_sum, actual_business_rate, due_status, business_interest_sum)
VALUES
-- 正常状态的借据（略有增长）
('20241012001', 'add', 'F101', 'C101', '2024-10-12', 110000.00, 0.045000, '正常', 5500.00),
('20241012002', 'add', 'F102', 'C102', '2024-10-12', 160000.00, 0.050000, '正常', 8000.00),
('20241012003', 'add', 'F103', 'C103', '2024-10-12', 210000.00, 0.048000, '正常', 10080.00),
('20241012004', 'add', 'F104', 'C104', '2024-10-12', 130000.00, 0.047000, '正常', 6110.00),

-- 逾期状态的借据（异常增加）
('20241012005', 'add', 'F105', 'C105', '2024-10-12', 250000.00, 0.055000, '逾期', 13750.00),
('20241012006', 'add', 'F106', 'C106', '2024-10-12', 300000.00, 0.056000, '逾期', 16800.00),
('20241012007', 'add', 'F107', 'C107', '2024-10-12', 280000.00, 0.054000, '逾期', 15120.00),
('20241012008', 'add', 'F108', 'C108', '2024-10-12', 220000.00, 0.053000, '逾期', 11660.00),
('20241012009', 'add', 'F109', 'C109', '2024-10-12', 190000.00, 0.052000, '逾期', 9880.00),

-- 结清状态的借据
('20241012010', 'add', 'F110', 'C110', '2024-10-12', 55000.00, 0.046000, '结清', 2530.00),
('20241012011', 'add', 'F111', 'C111', '2024-10-12', 65000.00, 0.047000, '结清', 3055.00);
```

### 2.2 完整 SQL 实现（单维度根因分析）

```sql
WITH 
-- Step 1: 准备预测值（使用 2024-10-05 的数据）
predict_data AS (
    SELECT 
        due_status,
        SUM(business_sum) AS predict_value
    FROM dws_custom_kj_serial_guarantee_txn_da
    WHERE put_out_date = '2024-10-05'
    GROUP BY due_status
),

-- Step 2: 准备实际值（使用 2024-10-12 的数据）
real_data AS (
    SELECT 
        due_status,
        SUM(business_sum) AS real_value
    FROM dws_custom_kj_serial_guarantee_txn_da
    WHERE put_out_date = '2024-10-12'
    GROUP BY due_status
),

-- Step 3: 合并预测值和实际值，计算全局总和
merged_data AS (
    SELECT 
        COALESCE(r.due_status, p.due_status) AS due_status,
        COALESCE(p.predict_value, 0) AS f,
        COALESCE(r.real_value, 0) AS a,
        SUM(COALESCE(p.predict_value, 0)) OVER() AS f_all,
        SUM(COALESCE(r.real_value, 0)) OVER() AS a_all
    FROM real_data r
    FULL OUTER JOIN predict_data p ON r.due_status = p.due_status
),

-- Step 4: 计算贡献度（EP）和惊奇度（Surprise）
contribution_surprise AS (
    SELECT 
        due_status,
        f,
        a,
        f_all,
        a_all,
        -- 计算贡献度 EP
        (a - f) / NULLIF(a_all - f_all, 0) AS ep,
        -- 计算 pp 和 qq
        a / NULLIF(a_all, 0) AS pp,
        f / NULLIF(f_all, 0) AS qq,
        -- 计算惊奇度 Surprise
        CASE 
            WHEN (a / NULLIF(a_all, 0)) != 0 AND (f / NULLIF(f_all, 0)) != 0 THEN
                0.5 * (
                    (a / NULLIF(a_all, 0)) * LOG10(2 * (a / NULLIF(a_all, 0)) / ((a / NULLIF(a_all, 0)) + (f / NULLIF(f_all, 0)))) +
                    (f / NULLIF(f_all, 0)) * LOG10(2 * (f / NULLIF(f_all, 0)) / ((a / NULLIF(a_all, 0)) + (f / NULLIF(f_all, 0))))
                )
            WHEN (a / NULLIF(a_all, 0)) = 0 THEN
                0.5 * (f / NULLIF(f_all, 0)) * LOG10(2)
            ELSE
                0.5 * (a / NULLIF(a_all, 0)) * LOG10(2)
        END AS surprise
    FROM merged_data
)

-- Step 5: 筛选和排序，输出根因分析结果
SELECT 
    due_status AS 维度值,
    f AS 预测值,
    a AS 实际值,
    ROUND(ep, 4) AS 贡献度_EP,
    ROUND(surprise, 6) AS 惊奇度_Surprise,
    ROUND(ep * surprise, 6) AS 综合得分
FROM contribution_surprise
WHERE ep > 0.1  -- T_EEP 阈值
ORDER BY surprise DESC
LIMIT 5;
```

### 2.3 简化版 SQL（仅计算核心指标）

```sql
-- 简化版：快速查看各借据状态的贡献度和惊奇度
WITH base_data AS (
    SELECT 
        due_status,
        SUM(CASE WHEN put_out_date = '2024-10-05' THEN business_sum ELSE 0 END) AS predict_value,
        SUM(CASE WHEN put_out_date = '2024-10-12' THEN business_sum ELSE 0 END) AS real_value
    FROM dws_custom_kj_serial_guarantee_txn_da
    WHERE put_out_date IN ('2024-10-05', '2024-10-12')
    GROUP BY due_status
)
SELECT 
    due_status AS 借据状态,
    predict_value AS 预测发放金额,
    real_value AS 实际发放金额,
    real_value - predict_value AS 绝对变化,
    ROUND((real_value - predict_value) / NULLIF(
        SUM(real_value) OVER() - SUM(predict_value) OVER(), 0
    ), 4) AS 贡献度_EP,
    ROUND(
        CASE 
            WHEN real_value > 0 AND predict_value > 0 THEN
                0.5 * (
                    (real_value / NULLIF(SUM(real_value) OVER(), 0)) * 
                    LOG10(2 * (real_value / NULLIF(SUM(real_value) OVER(), 0)) / 
                        ((real_value / NULLIF(SUM(real_value) OVER(), 0)) + 
                         (predict_value / NULLIF(SUM(predict_value) OVER(), 0))))
                    +
                    (predict_value / NULLIF(SUM(predict_value) OVER(), 0)) * 
                    LOG10(2 * (predict_value / NULLIF(SUM(predict_value) OVER(), 0)) / 
                        ((real_value / NULLIF(SUM(real_value) OVER(), 0)) + 
                         (predict_value / NULLIF(SUM(predict_value) OVER(), 0))))
                )
            ELSE 0
        END, 
    6) AS 惊奇度_Surprise
FROM base_data
ORDER BY 贡献度_EP DESC;
```

---

## 三、执行结果说明

### 3.1 实际执行结果

| 借据状态 | 预测发放金额 | 实际发放金额 | 绝对变化 | 贡献度_EP | 惊奇度_Surprise |
|---------|------------|------------|---------|----------|----------------|
| 逾期 | 170,000 | 1,240,000 | +1,070,000 | 1.1383 | 0.031433 |
| 正常 | 750,000 | 610,000 | -140,000 | -0.1489 | 0.018856 |
| 结清 | 110,000 | 120,000 | +10,000 | 0.0106 | 0.001380 |

**关键指标说明：**

1. **贡献度（EP）计算逻辑**
   - 公式：`EP = (实际值 - 预测值) / (实际总和 - 预测总和)`
   - 逾期：(1,240,000 - 170,000) / (1,970,000 - 1,030,000) = 1,070,000 / 940,000 = 1.1383
   - 解读：逾期状态贡献了总变化量的 113.83%，说明该维度值是变化的主要驱动因素

2. **惊奇度（Surprise）计算逻辑**
   - 公式：`Surprise = 0.5 * (pp * log10(2*pp/(pp+qq)) + qq * log10(2*qq/(pp+qq)))`
   - 其中 pp = 实际占比，qq = 预测占比
   - 逾期：pp = 0.6294（实际占比 62.94%），qq = 0.1650（预测占比 16.50%）
   - Surprise = 0.031433，说明实际占比与预测占比偏离较大
   - 注意：**必须使用 LOG10 函数**（以 10 为底的对数），这是 Adtributor 算法的标准定义

3. **结果解读**
   - **逾期状态**：贡献度 1.1383（最高），惊奇度 0.031433（最高），是发放金额异常增长的根本原因
   - **正常状态**：贡献度 -0.1489（负值），说明该维度值实际下降，抵消了部分增长
   - **结清状态**：贡献度 0.0106（接近 0），惊奇度 0.001380（极低），基本无影响

### 3.2 Python 验证与 SQL 对比

为验证 SQL 计算的准确性，我们使用 Python 进行了对照计算：

**Python 计算结果：**

| 借据状态 | 贡献度_EP | 惊奇度_Surprise |
|---------|----------|----------------|
| 逾期 | 1.1383 | 0.031433 |
| 正常 | -0.1489 | 0.018856 |
| 结清 | 0.0106 | 0.001380 |

**对比结论：**
- Python 计算结果与 SQL 完全一致（精确到小数点后 6 位）
- 验证了 SQL 实现的正确性，特别是 LOG10 函数的使用
- 验证了窗口函数计算全局总和的准确性

**验证总量平衡：**

```sql
-- 验证总量变化
SELECT 
    SUM(CASE WHEN put_out_date = '2024-10-05' THEN business_sum ELSE 0 END) AS 预测总额,
    SUM(CASE WHEN put_out_date = '2024-10-12' THEN business_sum ELSE 0 END) AS 实际总额,
    SUM(CASE WHEN put_out_date = '2024-10-12' THEN business_sum ELSE 0 END) - 
    SUM(CASE WHEN put_out_date = '2024-10-05' THEN business_sum ELSE 0 END) AS 总变化量
FROM dws_custom_kj_serial_guarantee_txn_da
WHERE put_out_date IN ('2024-10-05', '2024-10-12');

-- 结果：预测总额 = 1,030,000，实际总额 = 1,970,000，总变化量 = 940,000
```

---

## 四、多维度分析扩展

如果需要分析多个维度（如借据状态 + 利率区间），可以使用 UNION ALL 合并：

```sql
WITH base_calc AS (
    -- 维度1：借据状态
    SELECT 
        '借据状态' AS dimension_name,
        due_status AS dimension_value,
        -- ... 计算 EP 和 Surprise
    FROM ...
    
    UNION ALL
    
    -- 维度2：利率区间
    SELECT 
        '利率区间' AS dimension_name,
        CASE 
            WHEN actual_business_rate < 0.05 THEN '低利率'
            WHEN actual_business_rate < 0.055 THEN '中利率'
            ELSE '高利率'
        END AS dimension_value,
        -- ... 计算 EP 和 Surprise
    FROM ...
)
SELECT * FROM base_calc
ORDER BY dimension_name, surprise DESC;
```

---

## 五、性能优化建议

1. **创建物化视图**：对于频繁查询的日期范围，可以预先聚合
2. **分区表优化**：按 `put_out_date` 分区，加速时间范围查询
3. **索引优化**：在 `due_status`、`put_out_date` 上创建索引
4. **定时任务**：每日计算并存储到结果表，避免实时计算

---

## 六、总结

### 可行性总结
- **技术可行**：StarRocks SQL 完全可以实现 Adtributor 的核心算法
- **计算准确**：经过 Python 验证，SQL 计算结果精确一致
- **性能可控**：利用窗口函数和 CTE，查询性能良好
- **易于维护**：SQL 逻辑清晰，便于后续调整参数

### 注意事项
- **必须使用 LOG10**：惊奇度计算必须使用以 10 为底的对数（LOG10），这是 Adtributor 算法的标准定义
- SQL 实现适合单层、固定维度的分析
- 多层递归分析建议采用 Python + SQL 混合模式
- 需要处理除零等边界情况（使用 NULLIF）

### 推荐方案
1. **日常分析**：使用 SQL 实现，定时任务计算并存储结果
2. **复杂场景**：使用 Python 调用 SQL 获取数据，再进行深度分析
3. **可视化**：将 SQL 结果接入 BI 工具，实现自动化根因监控
4. **验证机制**：定期使用 Python 验证 SQL 计算结果，确保准确性

