package cn.webank.dosconfig.enums;

import cn.webank.dosconfig.common.Constant;

/**
 * 响应枚举
 */
public enum ResponseEnum {
    
    /**
     * 成功
     */
    SUCCESS("0", "成功"),
    
    /**
     * 用户鉴权失败
     */
    AUTH_ERR(Constant.SYS_ID + "0001", "用户鉴权失败"),
    
    /**
     * 服务异常
     */
    SYS_ERR(Constant.SYS_ID + "5000", "服务异常!"),
    
    /**
     * 请求参数错误
     */
    PARAM_ERR(Constant.SYS_ID + "0002", "请求参数错误");
    
    private final String code;
    private final String message;
    
    ResponseEnum(String code, String message) {
        this.code = code;
        this.message = message;
    }
    
    public String getCode() {
        return code;
    }
    
    public String getMessage() {
        return message;
    }
}


