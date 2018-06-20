package com.winterwell.datalog;

public interface IDataLogAdmin {

	/**
	 * @deprecated This is problematic - How to maintain non-standard events across months?
	 * So it is (with ESStorage) a no-op
	 * 
	 * @param dataspace
	 * @param eventType
	 */
	void registerEventType(CharSequence dataspace, String eventType);

	void registerDataspace(CharSequence dataspace);
	
}
