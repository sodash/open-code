package com.winterwell.datalog;

import com.winterwell.utils.threads.IFuture;


public interface IStatReq<V> extends IFuture<V> {

	IStatReq<V> setServer(String server);

}
