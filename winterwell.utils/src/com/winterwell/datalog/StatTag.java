package com.winterwell.datalog;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Documentation for a Stat tag Status: experimental
 * 
 * @author daniel
 * 
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
// Meta-annotation for "Don't throw this away during compilation"
public @interface StatTag {

	String tag();

	String doc() default "";
}
