package com.winterwell.datalog;

public interface IDataLogAdmin {

	void registerEventType(String dataspace, String eventType);

	void registerDataspace(String dataspace);
	
}
