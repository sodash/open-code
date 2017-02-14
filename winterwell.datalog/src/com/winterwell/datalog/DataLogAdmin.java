package com.winterwell.datalog;

public class DataLogAdmin implements IDataLogAdmin {

	private final DataLogImpl datalog;

	public DataLogAdmin(DataLogImpl dataLogImpl) {
		this.datalog = dataLogImpl;
	}

	@Override
	public void registerEventType(String dataspace, String eventType) {
		datalog.storage.registerEventType(dataspace, eventType);		
	}

	@Override
	public void registerDataspace(String dataspace) {
		if (datalog.storage instanceof ESStorage) {
			((ESStorage)datalog.storage).registerDataspace(dataspace);
		}
	}

}
