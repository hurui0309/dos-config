package cn.webank.dosconfig.entity;

import cn.webank.dosconfig.enums.ResponseEnum;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * 统一响应结构
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BaseResponse<T> {
    
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    
    /**
     * 响应码
     */
    private String respCode;
    
    /**
     * 响应信息
     */
    private String respMsg;
    
    /**
     * 业务数据
     */
    private T data;
    
    /**
     * 业务流水号
     */
    private String bizSeq;
    
    public BaseResponse() {
    }
    
    public BaseResponse(String respCode, String respMsg) {
        this.respCode = respCode;
        this.respMsg = respMsg;
    }
    
    public BaseResponse(String respCode, String respMsg, T data) {
        this.respCode = respCode;
        this.respMsg = respMsg;
        this.data = data;
    }
    
    public BaseResponse(ResponseEnum responseEnum) {
        this(responseEnum, null);
    }
    
    public BaseResponse(ResponseEnum responseEnum, T data) {
        this.respCode = responseEnum.getCode();
        this.respMsg = responseEnum.getMessage();
        this.data = data;
    }
    
    public BaseResponse(ResponseEnum responseEnum, T data, String bizSeq) {
        this.respCode = responseEnum.getCode();
        this.respMsg = responseEnum.getMessage();
        this.data = data;
        this.bizSeq = bizSeq;
    }
    
    public static <T> BaseResponse<T> ok() {
        return new BaseResponse<>(ResponseEnum.SUCCESS, null);
    }
    
    public static <T> BaseResponse<T> ok(T data) {
        return new BaseResponse<>(ResponseEnum.SUCCESS, data);
    }
    
    public static <T> BaseResponse<T> error(ResponseEnum responseEnum) {
        return new BaseResponse<>(responseEnum, null);
    }
    
    public static <T> BaseResponse<T> error(String code, String msg) {
        return new BaseResponse<>(code, msg, null);
    }
    
    public static <T> BaseResponse<T> error(String msg) {
        return new BaseResponse<>(ResponseEnum.SYS_ERR.getCode(), msg, null);
    }
    
    public String getRespCode() {
        return respCode;
    }
    
    public void setRespCode(String respCode) {
        this.respCode = respCode;
    }
    
    public String getRespMsg() {
        return respMsg;
    }
    
    public void setRespMsg(String respMsg) {
        this.respMsg = respMsg;
    }
    
    public T getData() {
        return data;
    }
    
    public void setData(T data) {
        this.data = data;
    }
    
    public String getBizSeq() {
        return bizSeq;
    }
    
    public void setBizSeq(String bizSeq) {
        this.bizSeq = bizSeq;
    }
    
    @Override
    public String toString() {
        try {
            return OBJECT_MAPPER.writeValueAsString(this);
        } catch (JsonProcessingException e) {
            return "BaseResponse{" +
                    "respCode='" + respCode + '\'' +
                    ", respMsg='" + respMsg + '\'' +
                    ", data=" + data +
                    ", bizSeq='" + bizSeq + '\'' +
                    '}';
        }
    }
}

