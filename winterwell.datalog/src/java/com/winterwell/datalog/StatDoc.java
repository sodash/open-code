package com.winterwell.datalog;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Status: not sure about this!
 * Would Stat.doc() be better?
 * Document your Stat calls. 
 */
@Retention(RetentionPolicy.RUNTIME)
//@Target({ ElementType.METHOD})
public @interface StatDoc {
	/**
	 * Description
	 */
	String value();
}
