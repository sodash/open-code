/**
 * 
 */
package com.winterwell.nlp.tablesense;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * An aspect of the model, which can be physical or semantic.
 * E.g. for a piece of text, the aspects might be 
 * position (physical), text (physical), language (semantic), tag (semantic).
 * 
 * If applied to a Class, it means "all the fields are Aspects"
 * 
 * @author Daniel
 *
 */
@Retention(RetentionPolicy.RUNTIME)
// Meta-annotation for "Don't throw this away during compilation"
@Target({ ElementType.TYPE, ElementType.FIELD })
public @interface Aspect {

}
