package com.winterwell.datalog;

import com.winterwell.utils.threads.IFuture;


public interface IDataLogReq<V> extends IFuture<V> {

	IDataLogReq<V> setServer(String server);

}
