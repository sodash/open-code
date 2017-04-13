package com.winterwell.depot;
 
/**
 * Marks a class for using {@link ModularConverter} when used with 
 * XStreamUtil's default XStream instance.
 * The class MUST implement IHasDesc
 * 
 * NB: Changed from an annotation, so that it gets inherited by sub-classes.
 */
//@Retention(RetentionPolicy.RUNTIME)
//// Meta-annotation for "Don't throw this away during compilation"
//@Target({ ElementType.TYPE })
//// Meta-annotation for "Only allowed on classes"
public interface ModularXML {
}
