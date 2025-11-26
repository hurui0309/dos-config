package cn.webank.dosconfig.exception;

import cn.webank.dosconfig.enums.ResponseEnum;

/**
 * 系统异常类
 */
public class SystemException extends RuntimeException {

    private String code;

    public SystemException() {
        super(ResponseEnum.SYS_ERR.getMessage());
        this.code = ResponseEnum.SYS_ERR.getCode();
    }

    public SystemException(ResponseEnum response) {
        super(response.getMessage());
        this.code = response.getCode();
    }

    public SystemException(String message) {
        super(message);
        this.code = ResponseEnum.SYS_ERR.getCode();
    }

    public String getCode() {
        return code;
    }

    public SystemException(String code, String msg) {
        super(msg);
        if (code != null) {
            this.code = code;
        } else {
            this.code = ResponseEnum.SYS_ERR.getCode();
        }
    }
}

