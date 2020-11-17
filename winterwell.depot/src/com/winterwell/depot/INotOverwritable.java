package com.winterwell.depot;


/**
 * Use case: to provide an extra layer of defence for objects which
 * get stored in the depot, and where an existing object should never
 * get over-written (except by a modified version of itself).
 * <p>
 * Note: You ARE allowed to overwrite if you use {@link Desc#markForMerge()}.
 * 
 * @author daniel
 * @testedby  INotOverwritableTest}
 */
public interface INotOverwritable {

	public static final class OverWriteException extends IllegalStateException {
		public OverWriteException(String string) {
			super(string);
		}

		private static final long serialVersionUID = 1L;
		
	}
	
}
