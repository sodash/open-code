package com.winterwell.datalog;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Holds documentation for runtime access. Status: experimental
 * 
 * @author daniel
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
public @interface Doc {

	StatTag[] value();

}
