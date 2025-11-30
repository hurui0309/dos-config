package cn.webank.dosconfig.util;

import cn.webank.weup.base.util.JSONUtil;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * 本地临时工具：读取 YAML 配置并输出 t_attribution_tree 的 INSERT 语句。
 *
 * <p>使用方式：
 * <pre>
 *   java -cp target/classes cn.webank.dosconfig.util.TreeInsertSqlGenerator config/metric_trees/loan.yml
 * </pre>
 */
public final class TreeInsertSqlGenerator {

    private static final Yaml YAML = new Yaml();
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private TreeInsertSqlGenerator() {
    }

    public static void main(String[] args) throws IOException {
        String treePath = "config/tree.yml";
        Path yamlPath = Path.of(treePath);
        generateInsertSql(yamlPath);
    }

    /**
     * 读取 YAML 并打印 INSERT SQL。
     */
    public static void generateInsertSql(Path yamlPath) throws IOException {
        if (!Files.exists(yamlPath)) {
            throw new IllegalArgumentException("文件不存在: " + yamlPath);
        }

        Map<String, Object> treeNode;
        try (Reader reader = Files.newBufferedReader(yamlPath)) {
            treeNode = YAML.load(reader);
        }
        if (treeNode == null) {
            throw new IllegalArgumentException("无法解析 YAML: " + yamlPath);
        }

        String treeId = readText(treeNode, "treeId", "tree_id");
        String treeName = defaultIfBlank(readText(treeNode, "treeName", "tree_name"), treeId);
        int version = readInt(treeNode, "version", 1);

        Object rootNodeObj = resolveRootNode(treeNode);
        if (rootNodeObj == null) {
            throw new IllegalArgumentException("YAML 中缺少 root 节点: " + yamlPath);
        }

        String metricId = readText(treeNode, "metricId", "metric_id");
        if (isBlank(metricId)) {
            metricId = readText(asMap(rootNodeObj), "metricId", "metric_id");
        }

        String metricName = readText(treeNode, "metricName", "metric_name", "nodeName");
        if (isBlank(metricName)) {
            metricName = readText(asMap(rootNodeObj), "nodeName", "metricName");
        }

        Object globalFilterObj = treeNode.get("globalFilter");
        String globalFilterJson = globalFilterObj == null ? null : toJson(globalFilterObj);

        String treeConfigJson = toJson(rootNodeObj);
        String now = LocalDateTime.now().format(DATETIME_FORMATTER);
        String globalFilterSql = globalFilterJson == null ? "NULL" : "'" + escapeSql(globalFilterJson) + "'";

        String sql = String.format(Locale.ROOT,
                "INSERT INTO t_attribution_tree (tree_id, tree_name, metric_id, metric_name, version, global_filter, tree_config, create_time, update_time) " +
                        "VALUES ('%s','%s','%s','%s',%d,%s,'%s','%s','%s');",
                escapeSql(treeId),
                escapeSql(treeName),
                escapeSql(metricId),
                escapeSql(metricName),
                version,
                globalFilterSql,
                escapeSql(treeConfigJson),
                now,
                now);

        System.out.println("-- " + yamlPath.getFileName() + " -> " + treeId + " --");
        System.out.println(sql);
    }

    private static String readText(Map<String, Object> node, String... keys) {
        if (node == null || keys == null) {
            return null;
        }
        for (String key : keys) {
            if (isBlank(key)) {
                continue;
            }
            Object value = node.get(key);
            if (value instanceof String str && !isBlank(str)) {
                return str;
            } else if (value != null && !(value instanceof Map<?, ?>)) {
                String text = value.toString();
                if (!isBlank(text)) {
                    return text;
                }
            }
        }
        return null;
    }

    private static int readInt(Map<String, Object> node, String key, int defaultValue) {
        Object value = node.get(key);
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return value != null ? Integer.parseInt(value.toString()) : defaultValue;
        } catch (NumberFormatException ex) {
            return defaultValue;
        }
    }

    private static String defaultIfBlank(String value, String defaultValue) {
        return isBlank(value) ? defaultValue : value;
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private static String escapeSql(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("'", "''");
    }

    private static String toJson(Object obj) {
        try {
            return JSONUtil.toDenseJsonStr(obj);
        } catch (Exception e) {
            throw new IllegalStateException("对象序列化失败: " + obj, e);
        }
    }

    private static Map<String, Object> asMap(Object rootNodeObj) {
        if (rootNodeObj instanceof Map<?, ?> map) {
            Map<String, Object> normalized = new LinkedHashMap<>();
            map.forEach((key, value) -> {
                if (key != null) {
                    normalized.put(key.toString(), value);
                }
            });
            return normalized;
        }
        return null;
    }

    private static Object resolveRootNode(Map<String, Object> treeNode) {
        Object root = treeNode.get("root");
        if (root == null) {
            root = treeNode.get("treeConfig");
        }
        if (root == null && treeNode.containsKey("nodeId")) {
            root = treeNode;
        }
        return normalizeRoot(root);
    }

    private static Object normalizeRoot(Object candidate) {
        if (candidate instanceof Map<?, ?>) {
            return candidate;
        }
        if (candidate instanceof Iterable<?> iterable) {
            for (Object element : iterable) {
                Object normalized = normalizeRoot(element);
                if (normalized != null) {
                    return normalized;
                }
            }
        }
        return null;
    }
}

