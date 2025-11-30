package cn.webank.dosconfig.exception;

import java.util.stream.Collectors;

import cn.webank.dosconfig.entity.BaseResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import cn.webank.dosconfig.enums.ResponseEnum;

/**
 * 全局异常处理器
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger LOG = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * 处理系统异常
     */
    @ExceptionHandler(SystemException.class)
    public BaseResponse<Void> handleSystemException(SystemException e) {
        LOG.error("系统异常: code={}, message={}", e.getCode(), e.getMessage(), e);
        return BaseResponse.error(e.getCode(), e.getMessage());
    }

    /**
     * 处理参数校验异常
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public BaseResponse<Void> handleValidationException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));
        LOG.error("参数校验异常: {}", message, e);
        return BaseResponse.error(ResponseEnum.PARAM_ERR.getCode(), "参数不合法：" + message);
    }

    /**
     * 处理IllegalArgumentException
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public BaseResponse<Void> handleIllegalArgumentException(IllegalArgumentException e) {
        LOG.error("参数异常: {}", e.getMessage(), e);
        return BaseResponse.error(ResponseEnum.PARAM_ERR.getCode(), "参数不合法：" + e.getMessage());
    }

    /**
     * 处理其他未捕获异常
     */
    @ExceptionHandler(Exception.class)
    public BaseResponse<Void> handleException(Exception e) {
        LOG.error("系统异常", e);
        return BaseResponse.error(ResponseEnum.SYS_ERR);
    }
}

