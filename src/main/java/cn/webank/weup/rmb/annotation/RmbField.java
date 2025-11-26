package cn.webank.weup.rmb.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * RMB 字段描述注解占位。
 */
@Documented
@Target(FIELD)
@Retention(RUNTIME)
public @interface RmbField {

    int seq();

    String title();

    String remark() default "";
}

