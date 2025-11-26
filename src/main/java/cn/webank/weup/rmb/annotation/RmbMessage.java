package cn.webank.weup.rmb.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * RMB 接口定义注解占位，便于在本地编译。
 */
@Documented
@Target(TYPE)
@Retention(RUNTIME)
public @interface RmbMessage {

    String name();

    String serviceId();

    String scenarioId();

    String useDesc() default "";

    String threadPoolName() default "";
}

