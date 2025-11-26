package cn.webank.weup.validate.annotations.numbers;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * 数值范围校验占位注解。
 */
@Documented
@Target(FIELD)
@Retention(RUNTIME)
public @interface IntRange {

    int min() default Integer.MIN_VALUE;

    int max() default Integer.MAX_VALUE;

    String message() default "integer value out of range";
}

