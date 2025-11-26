package cn.webank.dosconfig.dao;

import cn.webank.dosconfig.entity.attribution.AttributionTree;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 归因树DAO接口
 */
@Mapper
public interface AttributionTreeDao {

    /**
     * 根据归因树ID查询
     * @param treeId 归因树ID
     * @return 归因树对象
     */
    AttributionTree selectByTreeId(@Param("treeId") String treeId);

    /**
     * 模糊查询归因树列表
     * @param treeName 归因树名称（模糊匹配）
     * @param limit 查询条数限制
     * @return 归因树列表
     */
    List<AttributionTree> selectByTreeName(@Param("treeName") String treeName, @Param("limit") Integer limit);

    /**
     * 查询所有归因树
     * @param limit 查询条数限制
     * @return 归因树列表
     */
    List<AttributionTree> selectAll(@Param("limit") Integer limit);

    /**
     * 根据指标ID查询归因树
     * @param metricId 指标ID
     * @param limit 查询条数限制
     * @return 归因树列表
     */
    List<AttributionTree> selectByMetricId(@Param("metricId") String metricId, @Param("limit") Integer limit);

    /**
     * 根据指标名称模糊查询归因树
     * @param metricName 指标名称（模糊匹配）
     * @param limit 查询条数限制
     * @return 归因树列表
     */
    List<AttributionTree> selectByMetricName(@Param("metricName") String metricName, @Param("limit") Integer limit);

    /**
     * 插入归因树
     * @param tree 归因树对象
     * @return 影响行数
     */
    int insert(AttributionTree tree);

    /**
     * 更新归因树
     * @param tree 归因树对象
     * @return 影响行数
     */
    int updateByTreeId(AttributionTree tree);

    /**
     * 删除归因树
     * @param treeId 归因树ID
     * @return 影响行数
     */
    int deleteByTreeId(@Param("treeId") String treeId);
}

