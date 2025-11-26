package cn.webank.dosconfig.service;

import java.util.List;
import java.util.Map;

import cn.webank.dosconfig.entity.rmb.Req_04302590_01;

/**
 * 指标查询能力接口，实际实现由外部系统提供。
 */
public interface MetricService {
    /**
     * 指标数据查询。
     */
    List<Map<String, Object>> queryResultList(Req_04302590_01 req);
}

