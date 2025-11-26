package cn.webank.weup.rmb.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * RMB RPC 超时配置占位注解。
 */
@Documented
@Target(TYPE)
@Retention(RUNTIME)
public @interface RmbCallTimeOut {

    long value();
}

