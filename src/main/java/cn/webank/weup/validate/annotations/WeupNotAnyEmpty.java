package cn.webank.weup.validate.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * 字符串非空集合校验占位注解。
 */
@Documented
@Target(FIELD)
@Retention(RUNTIME)
public @interface WeupNotAnyEmpty {

    String message() default "collection must not contain blank elements";
}

