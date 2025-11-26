package cn.webank.dosconfig.enums;

/**
 * 日期粒度枚举
 */
public enum DateGranularity implements EnumStringParseAble<DateGranularity> {

    DAY("day", "yyyy-MM-dd", "%Y-%m-%d"),
    WEEK("week", "yyyy-ww", "%Y-%v"),
    MONTH("month", "yyyy-MM", "%Y-%m"),
    YEAR("year", "yyyy", "%Y");

    private final String dateGran;
    private final String normalFmt;
    private final String symFmt;

    DateGranularity(String dateGran, String normalFmt, String symFmt) {
        this.dateGran = dateGran;
        this.normalFmt = normalFmt;
        this.symFmt = symFmt;
    }

    public String getDateGran() {
        return dateGran;
    }

    public String getNormalFmt() {
        return normalFmt;
    }

    public String getSymFmt() {
        return symFmt;
    }
}

