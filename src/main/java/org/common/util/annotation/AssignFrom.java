package org.common.util.annotation;

import java.lang.annotation.*;

/**
 * @author Van
 *
 */
@Target({ ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface AssignFrom {
	
	String[] name() default {};
}
