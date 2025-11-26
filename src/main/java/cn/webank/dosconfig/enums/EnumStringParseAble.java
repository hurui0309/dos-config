package cn.webank.dosconfig.enums;

/**
 * 枚举字符串解析接口
 */
public interface EnumStringParseAble<E extends Enum<E>> {
    
    /**
     * 从字符串解析为枚举
     * @param value 字符串值
     * @return 枚举实例
     */
    default E parse(String value) {
        if (value == null) {
            return null;
        }
        for (E e : ((Class<E>) getClass()).getEnumConstants()) {
            if (e.toString().equalsIgnoreCase(value)) {
                return e;
            }
        }
        return null;
    }
}

