package com.winterwell.optimization.genetic;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marker for fields which can be mutated.
 * 
 * Either set `choices` OR `high` and `low -- not both.
 * 
 * @author daniel
 * @see MutateMeBreeder
 */
@Retention(RetentionPolicy.RUNTIME)
//Meta-annotation for "Don't throw this away during compilation"
@Target({ ElementType.FIELD })
//Meta-annotation for "Only allowed on fields"
public @interface MutateMe {

	/**
	 * For numerical options -- if the field is `double`, this this is continuous.
	 * If it is `int` then the outputs will be ints. 
	 */
	double high() default 0;
	
	/**
	 * For numerical options
	 */
	double low() default 0;	
	
	/**
	 * For discrete String valued options.
	 */
	String choices() default "";	
}
