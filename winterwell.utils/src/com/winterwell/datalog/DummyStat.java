package com.winterwell.datalog;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.regex.Pattern;

import winterwell.utils.reporting.Log;
import winterwell.utils.time.Dt;
import winterwell.utils.time.Time;

import com.winterwell.datalog.Stat.KInterpolate;
import com.winterwell.utils.threads.IFuture;

class DummyStat implements IStat {
	static final String LOGTAG = Stat.LOGTAG;
	int warnings = 0;
	private final String err;

	public DummyStat(Exception e) {
		err = "" + e;
	}

	@Override
	public IFuture<MeanRate> getMean(Time start, Time end, String... tagBits) {
		if (warnings < 3)
			Log.w(LOGTAG, err);
		warnings++;
		return new DummyFuture<MeanRate>(null);
	}
	
	@Override
	public String label(String label, String... tagBits) {
		if (warnings < 3)
			Log.w(LOGTAG, err);
		warnings++;
		return null;
	}

	@Override
	public void set(double x, Object... tagBits) {
		if (warnings < 3)
			Log.w(LOGTAG, err);
		warnings++;
	}

	@Override
	public void setEvent(String event, String... tagBits) {
		if (warnings < 3)
			Log.w(LOGTAG, err);
		warnings++;
	}

	@Override
	public IStatReq<Double> getTotal(Time start, Time end, String... tagBits) {
		if (warnings < 3)
			Log.w(LOGTAG, err);
		warnings++;
		return new DummyFuture<Double>(0.0);
	}

	@Override
	public void mean(double x, Object... tag) {
		if (warnings < 3)
			Log.w(LOGTAG, err);
		warnings++;
	}

	@Override
	public Dt getPeriod() {
		if (warnings < 3)
			Log.w(LOGTAG, err);
		warnings++;
		return null;
	}

	@Override
	public MeanRate getMean(String... tag) {
		if (warnings < 3)
			Log.w(LOGTAG, err);
		warnings++;
		return null;
	}

	@Override
	public Set<String> getLive() {
		if (warnings < 3)
			Log.w(LOGTAG, err);
		warnings++;
		return null;
	}

	@Override
	public IFuture<Iterable> getData(Pattern id, Time start, Time end) {
		if (warnings < 3)
			Log.w(LOGTAG, err);
		warnings++;
		return new DummyFuture<Iterable>(Collections.EMPTY_LIST);
	}

	@Override
	public IFuture<Iterable> getData(Time start, Time end, KInterpolate ifn, Dt bucketSize, String... tagBits) {
		if (warnings < 3)
			Log.w(LOGTAG, err);
		warnings++;
		return new DummyFuture<Iterable>(Collections.EMPTY_LIST);
	}

	@Override
	public Collection<String> getActiveLabels() {
		if (warnings < 3)
			Log.w(LOGTAG, err);
		warnings++;
		return null;
	}

	@Override
	public Rate get(String... tag) {
		if (warnings < 3)
			Log.w(LOGTAG, err);
		warnings++;
		return Rate.ZERO(Stat.tag(tag));
	}

	@Override
	public void flush() {
		if (warnings < 3)
			Log.w(LOGTAG, err);
		warnings++;
	}

	@Override
	public void count(double dx, Object... tags) {
		if (warnings < 3)
			Log.w(LOGTAG, err);
		warnings++;
	}

	@Override
	public void close() {
		if (warnings < 3)
			Log.w(LOGTAG, err);
		warnings++;
	}

	@Override
	public void setListener(IListenStat listener, String... tagBits) {
	}

	@Override
	public void removeListener(String... tagBits) {
	}


	@Override
	public Map<String, IListenStat> getListeners() {
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
	public StatConfig getConfig() {
		return new StatConfig();
	}

	@Override
	public IFuture<? extends Iterable> getMeanData(Time start, Time end,
			KInterpolate fn, Dt bucketSize, String... tagBits) {
		if (warnings < 3)
			Log.w(LOGTAG, err);
		warnings++;
		return new DummyFuture<Iterable>(Collections.EMPTY_LIST);
	}

}

class DummyFuture<V> implements IStatReq<V> {

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
	public IStatReq<V> setServer(String server) {
		return this;
	}

}
