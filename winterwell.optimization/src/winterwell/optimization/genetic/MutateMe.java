package winterwell.optimization.genetic;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marker for fields which can be mutated.
 * @author daniel
 */
@Retention(RetentionPolicy.RUNTIME)
//Meta-annotation for "Don't throw this away during compilation"
@Target({ ElementType.FIELD })
//Meta-annotation for "Only allowed on fields"
public @interface MutateMe {

	double high() default 0;
	double low() default 0;	
	String choices() default "";	
}
