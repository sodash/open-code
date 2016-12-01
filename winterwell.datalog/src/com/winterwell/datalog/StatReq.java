package com.winterwell.datalog;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.regex.Pattern;

import com.winterwell.maths.timeseries.BucketedDataStream;
import com.winterwell.maths.timeseries.DataUtils;
import com.winterwell.maths.timeseries.Datum;
import com.winterwell.maths.timeseries.IDataStream;
import com.winterwell.maths.timeseries.ListDataStream;
import com.winterwell.utils.TodoException;
import com.winterwell.utils.Utils;
import com.winterwell.utils.time.Dt;
import com.winterwell.utils.time.Time;

import com.winterwell.utils.web.IHasJson;
import com.winterwell.datalog.Stat.KInterpolate;
import com.winterwell.depot.Desc;
import com.winterwell.utils.TimeOut;
import com.winterwell.utils.web.SimpleJson;

/**
 * The data-fetch part of StatImpl. Provides cacheing and batched-data-fetch.
 * @author daniel
 *
 * @param <X>
 */
public class StatReq<X> implements IStatReq<X>, IHasJson {		
	
	
	/**
	 * Double or ListDataStream
	 */
	protected volatile X v;

	protected static StatImpl stat;// = (StatImpl) Stat.dflt;

	@Override
	public Object toJson2() throws UnsupportedOperationException {
		X value = get();
		if (value == null) return null;
		if (value instanceof Double) {			
			return value;
		}
		if (value instanceof IDataStream) {
			String json = DataUtils.toJson((IDataStream) value);
			return new SimpleJson().fromJson(json);
		}
		throw new TodoException(value+" from "+this);
	}
	

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((cmd == null) ? 0 : cmd.hashCode());
		result = prime * result + ((end == null) ? 0 : end.hashCode());
		result = prime * result
				+ ((interpolate == null) ? 0 : interpolate.hashCode());
		result = prime * result + ((server == null) ? 0 : server.hashCode());
		result = prime * result + ((start == null) ? 0 : start.hashCode());
		result = prime * result + ((tag == null) ? 0 : tag.hashCode());
		result = prime * result
				+ ((tagMatcher == null) ? 0 : tagMatcher.pattern().hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		StatReq other = (StatReq) obj;
		if (cmd == null) {
			if (other.cmd != null)
				return false;
		} else if (!cmd.equals(other.cmd))
			return false;
		if (end == null) {
			if (other.end != null)
				return false;
		} else if (!end.equals(other.end))
			return false;
		if (interpolate != other.interpolate)
			return false;
		if (server == null) {
			if (other.server != null)
				return false;
		} else if (!server.equals(other.server))
			return false;
		if (start == null) {
			if (other.start != null)
				return false;
		} else if (!start.equals(other.start))
			return false;
		if (tag == null) {
			if (other.tag != null)
				return false;
		} else if (!tag.equals(other.tag))
			return false;
		if (tagMatcher == null) {
			if (other.tagMatcher != null)
				return false;
		} else if (!tagMatcher.pattern().equals(other.tagMatcher.pattern()))
			return false;
		return true;
	}

	protected final String cmd;

	protected String tag;	

	/**
	 * Can be null!
	 */
	protected final Time start;

	/**
	 * Can be null!
	 */
	protected final Time end;

	protected Pattern tagMatcher;

	protected KInterpolate interpolate;

	/**
	 * The cache-key. This can have a shorter period than the actual BatchGet, as it will exclude the
	 * latets bucket.
	 */
	transient StatReq key;
	
	public StatReq(String cmd, String tag, Time start, Time end, KInterpolate interpolate, Dt bucketSize) {
		this(cmd, tag, null, start, end, interpolate, bucketSize);
		Utils.check4null(cmd, tag);
	}
	
	/**
	 * null by default, which gets treated as equivalent to {@link Desc#LOCAL_SERVER}
	 */
	protected String server;

	/**
	 * normally null, unless something went wrong during a fetch
	 */
	protected transient Throwable error;

	/**
	 * Can be null for "as it is stored"
	 */
	protected Dt bucketSize;
	
	/**
	 * Set this to fetch remote data.
	 */
	@Override
	public StatReq<X> setServer(String server) {
		// just in case, given that a debugger's toString() can trigger a get
		v = null; 
		key = null;
		error = null;
		this.server = server;
		return this;
	}
	
	public StatReq(String cmd, String tag, Pattern tagMatcher, Time start, Time end, KInterpolate interpolate, Dt bucketSize) {
		this.cmd = cmd;
		this.tag = tag;
		this.tagMatcher = tagMatcher;
		this.start = start;
		this.end = end;
		this.interpolate = interpolate;
		this.bucketSize = bucketSize;
		// does not add to the todos! Used by both cache-key ops & the "real" constructors
	}

	public StatReq(String cmd, Pattern tagMatcher, Time start, Time end) {
		this(cmd, null, tagMatcher, start, end, null, null);
		Utils.check4null(cmd, tagMatcher);		
	}

	@Override
	public boolean cancel(boolean mayInterruptIfRunning) {
		// TODO Auto-generated method stub
		return false;
	}
	
	@Override
	public boolean isCancelled() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isDone() {
		return v!=null;
	}

	@Override
	public X get() {
		throw new TodoException();
	}
	
	@Override
	public X get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
		TimeOut to = new TimeOut(unit.toMillis(timeout));
		try {
			return get();
		} finally {
			to.cancel();
		}
	}
	
	/**
	 * If v is a bucket construct a bucket stream.
	 * @return v
	 */
	protected X getValue() {
		if (v == null) return null;
		
		if (bucketSize == null) return v;
		
		assert v instanceof IDataStream;
		BucketedDataStream bds = new BucketedDataStream((IDataStream) v, bucketSize);
		return (X) bds;
	}

	
	/**
	 * Initialise the value of a StatReq, based on its command. 
	 * @param sr
	 */
	static void initV(StatReq sr) {
		if (KStatReq.TOTAL.equals(sr.cmd)) {
			sr.v = 0.0;
		} else {
			assert KStatReq.DATA.equals(sr.cmd);
			sr.v = new ListDataStream(1);				
		}
	}

	/**
	 * Adds a datum to the StatReq.
	 * @param bg
	 * @param datum
	 */
	static void add(StatReq bg, Datum datum) {
		if (bg.v instanceof Double) {
			bg.v = ((Double)bg.v) + datum.x();
		} else {
			if (bg.interpolate==KInterpolate.SKIP_ZEROS && datum.isZero()) {
				return;
			}
			assert bg.v instanceof ListDataStream : bg.v.getClass();
			ListDataStream lds = (ListDataStream) bg.v;
			lds.add(datum);
		}		
	}

	/**
	 * Adds the latest bucket of data to the StatReq.
	 * @param batchGet
	 */
	static void addLatestBucket(StatReq batchGet) {
		Datum cb = stat.currentBucket(batchGet.tag, batchGet.end);
		if (cb==null) return;
		add(batchGet, cb);
	}

	/**
	 * to JSON for the value!
	 * This allows BatchGet to be dropped into e.g. String-building operations.  
	 */
	@Override
	public String toString() {
		try {
			return toJSONString();
		} catch (Exception e) {
			throw Utils.runtime(e);
		}
	}

	@Override
	public void appendJson(StringBuilder sb) {		
		X value = get();
		if (value == null) return;
		if (value instanceof Double) {
			sb.append(value);
			return;
		}
		if (value instanceof IDataStream) {
			String json = DataUtils.toJson((IDataStream) value);
			sb.append(json);
			return;
		}
		throw new TodoException(value+" from "+this);
	}
	
	@Override
	public String toJSONString() {
		StringBuilder sb = new StringBuilder();
		appendJson(sb);
		return sb.toString();
	}

}
