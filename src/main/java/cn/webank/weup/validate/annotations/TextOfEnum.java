package cn.webank.weup.validate.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * 校验枚举值占位注解。
 */
@Documented
@Target(FIELD)
@Retention(RUNTIME)
public @interface TextOfEnum {

    Class<? extends Enum<?>> enumClass();

    String message() default "value is not part of enum";
}

