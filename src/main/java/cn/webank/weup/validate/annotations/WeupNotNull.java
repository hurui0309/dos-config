package cn.webank.weup.validate.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * 非空校验占位注解。
 */
@Documented
@Target(FIELD)
@Retention(RUNTIME)
public @interface WeupNotNull {

    String message() default "must not be null";
}

