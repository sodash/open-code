package com.winterwell.depot;
 
/**
 * Marks a class for using {@link ModularConverter} when used with 
 * XStreamUtil's default XStream instance.
 *
 * Warning: works with XStream xml via ModularConverter -- which will use Depot.getDefault().
 * 
 * NB: Changed from an annotation, so that it gets inherited by sub-classes.
 */
//@Retention(RetentionPolicy.RUNTIME)
//// Meta-annotation for "Don't throw this away during compilation"
//@Target({ ElementType.TYPE })
//// Meta-annotation for "Only allowed on classes"
public interface ModularXML extends IHasDesc {


	/**
	 * @return the sub-modules (those parts which implement @ModularXML), or
	 *         null (the default). 
	 *         This should not recursively collect the sub-sub-modules;
	 *         the Depot will do that. It is VITAL that this includes all fields
	 *         which implement @ModularXML, otherwise these parts will not get
	 *         saved.
	 * @see @ModularXML
	 */
	default IHasDesc[] getModules() {
		return null;
	}
	
}
