## 本地快速测试示例

以下示例基于 `NodeMetricComputationEngine` 与 `DimensionAttributionEngine` 当前实现，方便在 IDE 中直接创建 `main` 方法或 JUnit 测试来验证计算逻辑。

---

### 1. 节点指标计算（含加/乘节点）

```java
package cn.webank.dosconfig.sample;

import cn.webank.dosconfig.entity.attribution.dto.response.MetricTreeNodeDTO;
import cn.webank.dosconfig.service.attribution.NodeMetricComputationEngine;
import cn.webank.dosconfig.service.attribution.NodeMetricComputationEngine.MetricValue;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public class NodeMetricDemo {
    public static void main(String[] args) {
        // 1. 构造指标树：root = childA + childB * childC
        MetricTreeNodeDTO childA = new MetricTreeNodeDTO("A", "指标A", "metric_a", false, "ADD", null, null);
        MetricTreeNodeDTO childB = new MetricTreeNodeDTO("B", "指标B", "metric_b", false, "ADD", null, null);
        MetricTreeNodeDTO childC = new MetricTreeNodeDTO("C", "指标C", "metric_c", false, "ADD", null, null);
        MetricTreeNodeDTO multiplyNode = new MetricTreeNodeDTO("MUL", "乘法节点", null, false, "MUL", List.of(childB, childC), null);
        MetricTreeNodeDTO root = new MetricTreeNodeDTO("ROOT", "加法根节点", null, false, "ADD", List.of(childA, multiplyNode), null);

        // 2. 准备指标值（基准期 / 对比期）
        Map<String, MetricValue> metrics = Map.of(
                "metric_a", new MetricValue(BigDecimal.valueOf(100), BigDecimal.valueOf(120)),
                "metric_b", new MetricValue(BigDecimal.valueOf(10), BigDecimal.valueOf(15)),
                "metric_c", new MetricValue(BigDecimal.valueOf(2), BigDecimal.valueOf(3))
        );

        // 3. 计算
        NodeMetricComputationEngine engine = new NodeMetricComputationEngine(new BigDecimal("0.000001"));
        var result = engine.compute(root,
                LocalDate.parse("2024-01-01"),
                LocalDate.parse("2024-01-08"),
                null,
                metrics);

        System.out.printf("Root baseline=%s, compare=%s, delta=%s%n",
                result.baselineValue(), result.compareValue(), result.deltaValue());
    }
}
```

运行后可在控制台观察各节点日志，包括乘法节点的 LMDI 拆解。

---

### 2. 维度归因算法

```java
package cn.webank.dosconfig.sample;

import cn.webank.dosconfig.service.attribution.DimensionAttributionEngine;
import cn.webank.dosconfig.entity.attribution.dto.response.DimensionAttributionItemDTO;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public class DimensionAttributionDemo {
    public static void main(String[] args) {
        Map<String, BigDecimal> baselineValues = Map.of(
                "广州", BigDecimal.valueOf(80),
                "深圳", BigDecimal.valueOf(120)
        );
        Map<String, BigDecimal> compareValues = Map.of(
                "广州", BigDecimal.valueOf(100),
                "深圳", BigDecimal.valueOf(150),
                "佛山", BigDecimal.valueOf(30)
        );

        DimensionAttributionEngine engine = new DimensionAttributionEngine(
                new BigDecimal("0.000001"),
                20,
                new BigDecimal("0.1"),
                new BigDecimal("0.67")
        );
        List<DimensionAttributionItemDTO> items = engine.analyze("city", compareValues, baselineValues);
        items.forEach(item -> System.out.printf(
                "dimensionValue=%s, delta=%s, ep=%s, surprise=%s%n",
                item.dimensionValue(),
                item.deltaValue(),
                item.contribution(),
                item.surprise()
        ));
    }
}
```

示例中对比期新增「佛山」维度，可验证 Adtributor 会自动处理“新增/消失”维度值并输出贡献度、EP 及 Surprise。

> 注：以上示例仅依赖 `service.engine` 相关类，如需运行请确保 `MetricTreeNodeDTO` 等 DTO 可直接被引用（可从现有模块复制）。

