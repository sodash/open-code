package com.winterwell.datalog;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.regex.Pattern;

import com.winterwell.datalog.DataLog.KInterpolate;
import com.winterwell.utils.log.Log;
import com.winterwell.utils.threads.IFuture;
import com.winterwell.utils.time.Dt;
import com.winterwell.utils.time.Time;

class DummyDataLog implements IDataLog {
	static final String LOGTAG = DataLog.LOGTAG;
	int warnings = 0;
	private final String err;

	public DummyDataLog(Throwable e) {
		err = "" + e;
	}

	@Override
	public IFuture<MeanRate> getMean(Time start, Time end, String... tagBits) {
		warn();
		return new DummyFuture<MeanRate>(null);
	}

	private void warn() {
		if (warnings > 3) return;
		Log.w(LOGTAG, err);
		warnings++;
	}
	
	@Override
	public String label(String label, String... tagBits) {
		warn();
		return null;
	}

	@Override
	public void count(DataLogEvent event) {
		warn();
	}
	
	@Override
	public void set(double x, Object... tagBits) {
		warn();
	}

	@Override
	public IDataLogReq<Double> getTotal(Time start, Time end, String... tagBits) {
		warn();
		return new DummyFuture<Double>(0.0);
	}

	@Override
	public void mean(double x, Object... tag) {
		warn();
	}

	@Override
	public Dt getPeriod() {
		warn();
		return null;
	}

	@Override
	public MeanRate getMean(String... tag) {
		warn();
		return null;
	}

	@Override
	public Set<String> getLive() {
		warn();
		return null;
	}

	@Override
	public IFuture<Iterable> getData(Pattern id, Time start, Time end) {
		warn();
		return new DummyFuture<Iterable>(Collections.EMPTY_LIST);
	}

	@Override
	public IFuture<Iterable> getData(Time start, Time end, KInterpolate ifn, Dt bucketSize, String... tagBits) {
		warn();
		return new DummyFuture<Iterable>(Collections.EMPTY_LIST);
	}

	@Override
	public Collection<String> getActiveLabels() {
		warn();
		return new ArrayList();
	}

	@Override
	public Rate get(String... tag) {
		warn();
		return Rate.ZERO(DataLog.tag(tag));
	}

	@Override
	public void flush() {
		warn();
	}

	@Override
	public void count(double dx, Object... tags) {
		warn();
	}

	@Override
	public void close() {
		// you *can* safely close this
	}

	@Override
	public void setListener(IListenDataLog listener, String... tagBits) {
	}

	@Override
	public void removeListener(String... tagBits) {
	}


	@Override
	public Map<String, IListenDataLog> getListeners() {
		return Collections.EMPTY_MAP;
	}


	@Override
	public void set(Time at, double x, Object[] tags) {
		throw new UnsupportedOperationException();
	}


	@Override
	public void count(Time at, double dx, Object[] tags) {
		throw new UnsupportedOperationException();
	}


	@Override
	public DataLogConfig getConfig() {
		warn();
		return new DataLogConfig();
	}

	@Override
	public IFuture<? extends Iterable> getMeanData(Time start, Time end,
			KInterpolate fn, Dt bucketSize, String... tagBits) {
		warn();
		return new DummyFuture<Iterable>(Collections.EMPTY_LIST);
	}

	@Override
	public void setEventCount(DataLogEvent event) {
		warn();
	}

	@Override
	public IDataLogAdmin getAdmin() {
		return new DummyDataLogAdmin();
	}

	@Override
	public Object getStorage() {
		return null;
	}

	@Override
	public void init() {
		
	}
}

class DummyDataLogAdmin implements IDataLogAdmin {
//	@Override
	public void registerEventType(CharSequence dataspace, String eventType) {		
	}

	@Override
	public void registerDataspace(CharSequence dataspace) {		
	}	
}

class DummyFuture<V> implements IDataLogReq<V> {

	final V v;

	public DummyFuture(V v) {
		this.v = v;
	}

	@Override
	public boolean cancel(boolean mayInterruptIfRunning) {
		return false;
	}

	@Override
	public boolean isCancelled() {
		return false;
	}

	@Override
	public boolean isDone() {
		return true;
	}

	@Override
	public V get(long timeout, TimeUnit unit) throws InterruptedException,
			ExecutionException, TimeoutException {
		return get();
	}

	@Override
	public V get() throws RuntimeException {
		return v;
	}

	@Override
	public IDataLogReq<V> setServer(String server) {
		return this;
	}

}
