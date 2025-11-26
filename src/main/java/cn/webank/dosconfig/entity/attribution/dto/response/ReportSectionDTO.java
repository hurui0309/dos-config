package cn.webank.dosconfig.entity.attribution.dto.response;

/**
 * 报告章节
 */
public record  ReportSectionDTO(
        String sectionTitle,
        String sectionContent,
        Integer sectionOrder
) {
}

