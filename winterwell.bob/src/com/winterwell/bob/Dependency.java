package com.winterwell.bob;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Place on a class to indicate files & resources that should
 * be packaged with it.
 * 
 * TODO support for this in the CopyReqClasses task
 * TODO use this instead of @Resource 
 * @author daniel
 *
 */
@Retention(RetentionPolicy.CLASS) // TODO will this work if Bob isn't on the classpath?
@Target({ElementType.TYPE})
public @interface Dependency {

	String path();
	
}
