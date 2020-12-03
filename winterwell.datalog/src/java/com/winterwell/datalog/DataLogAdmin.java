package com.winterwell.datalog;

public class DataLogAdmin implements IDataLogAdmin {

	private final DataLogImpl datalog;

	public DataLogAdmin(DataLogImpl dataLogImpl) {
		this.datalog = dataLogImpl;
	}

//	@Override
//	public void registerEventType(CharSequence dataspace, String eventType) {
//		datalog.storage.registerEventType(new Dataspace(dataspace), eventType);		
//	}

	@Override
	public void registerDataspace(CharSequence dataspace) {
		if (datalog.storage instanceof ESStorage) {
			((ESStorage)datalog.storage).registerDataspace(new Dataspace(dataspace));
		}
	}

}
