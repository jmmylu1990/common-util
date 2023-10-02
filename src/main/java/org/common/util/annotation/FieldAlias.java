package org.common.util.annotation;

import java.lang.annotation.*;


@Documented
@Target( { ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
public @interface FieldAlias {

	String[] name() default {};
}