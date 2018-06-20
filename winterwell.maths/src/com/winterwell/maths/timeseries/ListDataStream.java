package com.winterwell.maths.timeseries;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.ListIterator;

import com.winterwell.utils.Printer;
import com.winterwell.utils.TodoException;
import com.winterwell.utils.containers.AbstractIterator;
import com.winterwell.utils.containers.Containers;
import com.winterwell.utils.io.XStreamBinaryConverter.BinaryXML;
import com.winterwell.utils.log.Log;
import com.winterwell.utils.time.Dt;
import com.winterwell.utils.time.Time;

import no.uib.cipr.matrix.Vector;

/**
 * A data stream backed by a list.
 * 
 * This class does allow you to add more data, but this is not threadsafe. To
 * iterate through again, use {@link #copyWithReset()}.
 * <p>
 * Use with {@link TimeSlicer} to build histograms over time. See
 * {@link #ListDataStream(int, TimeSlicer)}
 * 
 * @testedby {@link ListDataStreamTest}
 * @author Daniel
 */
@BinaryXML
public final class ListDataStream extends ADataStream implements
		Collection<Datum>, Serializable, List<Datum> {
	private static final long serialVersionUID = 1L;

	/**
	 * Labels for each *column*, ie dimension names.
	 * For a list of points on a 2d chart, this might be ["x-axis-name", "y-axis-name"].
	 * For a list of time-stamped multi-dimension points (e.g. breakdown-by-tagset), this might be ["positive", "negative", "neutral"]
	 *  -- in which case it hasn't told you what the y-axis units are! 
	 * @return often null
	 */
	public List<String> getLabels() {		
		return labels;
	}
	
	/**
	 * Labels for each column, ie dimension names
	 * @param labels
	 */
	public void setLabels(List<String> labels) {
		assert labels==null || labels.size() == getDim() : getDim()+" vs "+labels.size()+" "+labels;
		this.labels = labels;
	}
	
	private List<String> labels;
	
	private final List<Datum> data;

	/**
	 * For de-serialisation ONLY
	 */
	@Deprecated
	ListDataStream() {
		super();
		data = new ArrayList();
	}

	/**
	 * Create a 1D data stream (with ancient timestamps)
	 * 
	 * @param xs
	 */
	public ListDataStream(double[] xs) {
		super(1);
		data = new ArrayList<Datum>(xs.length);
		for (double x : xs) {
			add(new Datum(x));
		}
	}

	public ListDataStream(double[] xs, Time start, Dt step) {
		super(1);
		data = new ArrayList<Datum>(xs.length);
		Time t = start;
		for (double x : xs) {
			add(new Datum(t, x, null));
			t = t.plus(step);
		}
	}


	/**
	 * @param dim
	 *            Dimensions
	 */
	public ListDataStream(int dim) {
		super(dim);
		data = new ArrayList<Datum>();
	}

	/**
	 * @param dim
	 *            Dimensions
	 * @param initialCapacity
	 *            How much storage to allocate at the start.
	 */
	public ListDataStream(int dim, int initialCapacity) {
		super(dim);
		data = new ArrayList<Datum>(initialCapacity);
	}

	/**
	 * Create a list of data, where the datums have zero-values and timestamps
	 * from the bucket END-points, and are modifiable. 
	 * Usage: to build-up time
	 * based streams using {@link #get(int)}. E.g. you might use
	 * <code>dataStream.get(bucketer.getBucket(time)).add(0, 1);</code> to count
	 * time-stamped entries.
	 * 
	 * @param dim
	 * @param bucketer
	 */
	public ListDataStream(int dim, TimeSlicer bucketer) {
		this(dim, bucketer, true);
	}
	
	/**
	 * Create a list of data, where the datums have zero-values and timestamps
	 * from the bucket END-points, and are modifiable. 
	 * Usage: to build-up time
	 * based streams using {@link #get(int)}. E.g. you might use
	 * <code>dataStream.get(bucketer.getBucket(time)).add(0, 1);</code> to count
	 * time-stamped entries.
	 * 
	 * @param dim
	 * @param bucketer
	 * @param useBucketMiddleAsTime If true, use the middle of the bucket as the Datum timestamp.
	 * If false, use the end of the bucket.
	 */
	public ListDataStream(int dim, TimeSlicer bucketer, boolean useBucketMiddleAsTime) {
		this(dim, bucketer.size());
		// create blank entries for each time-slice
		for (int b = 0; b < bucketer.size(); b++) {
			Time mid = useBucketMiddleAsTime? bucketer.getBucketMiddleTime(b) : bucketer.getBucketEnd(b);
			Datum d = new Datum(mid, new double[dim], null);
			d.setModifiable(true);
			add(d);
		}
	}

	public ListDataStream(List<Datum> data) {
		this(data, data.isEmpty() ? 0 : data.get(0).getDim());
	}
	/**
	 * 
	 * @param data
	 *            This will be shallow-copied. If it empty, the stream will have
	 *            zero dimensions and be pretty much unusable. Must be in the
	 *            correct time order.
	 */
	public ListDataStream(List<Datum> data, int dim) {
		super(dim);
		// check time-order
		if (!data.isEmpty()) {
			Datum prev = data.get(0);
			for (Datum datum : data) {
				assert !prev.time.isAfter(datum.time) : "out-of-order: "
						+ prev.time + " v " + datum.time;
				prev = datum;
			}
		}
		// copy the list to our backing store
		data = new ArrayList<Datum>(data);
		if (data.isEmpty()) {
			Log.report("Empty data-list: created 0D datastream");
		}
		this.data = data;
	}

	/**
	 * Create a ListDataStream which directly reuses the backing list, but
	 * maintains its own position. This is a faster alternative to
	 * {@link #copyWithReset()}: you get the rewind but not the copy.
	 * 
	 * @param data
	 *            This will NOT be copied
	 * @param share
	 *            must be true. Used to separate from the other constructor.
	 */
	private ListDataStream(ListDataStream data, boolean share) {
		super(data.getDim());
		assert share == true;
		this.data = data.data;
	}

	public ListDataStream(Iterable<? extends Vector> data) {
		this(data, false);
	}
	
	public ListDataStream(Iterable<? extends Vector> data, boolean allowNonFinite) {
		super();
		List<? extends Vector> list = Containers.getList(data);
		this.data = new ArrayList<Datum>(list.size());
		if (list.size()==0) {
			return; // empty!
		}
		setDim(list.get(0).size());		
		for (Vector x : data) {
			if (x instanceof Datum) {
				add((Datum) x);
			} else {
				add(new Datum(null, DataUtils.toArray(x), null, allowNonFinite));
			}
		}
	}

	/**
	 * Add a new piece of data to the end of the stream. This method is not
	 * threadsafe.
	 * 
	 * @param d MUST be dated later than previous data in the list
	 */
	@Override
	public boolean add(Datum d) throws IllegalArgumentException {
		// safety check ordering
		if ( ! data.isEmpty() 
			&& Containers.last(data).getTime().isAfter(d.getTime())) {
			throw new IllegalArgumentException("Data sort order! "		
				+ data.get(data.size() - 1).getTime() + " to " + d.getTime());
		}
		assert d.getDim() == getDim() : "List-dim " + getDim() + " vs "
				+ d.getDim() + " with " + d;
		return data.add(d);
	}

	@Override
	public boolean addAll(Collection<? extends Datum> c) {
		return data.addAll(c);
	}

	@Override
	public void clear() {
		data.clear();
	}

	/**
	 * Shallow copy with reset. Shares the same list with this List (so this is
	 * very fast).
	 */
	@Deprecated
	// the use case for this was when these were 1-shot iterables
	@Override
	public ListDataStream clone() {
		ListDataStream lds = new ListDataStream(this, true);
		lds.setLabels(labels);
		return lds;
	}

	@Override
	public boolean contains(Object o) {
		if (o instanceof Datum)
			return data.contains(o);
		throw new IllegalArgumentException("Not a Datum: " + o.getClass());
	}

	@Override
	public boolean containsAll(Collection<?> c) {
		return data.containsAll(c);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * winterwell.maths.timeseries.ADataStream#instantiate(java.lang.Object)
	 */
	@Override
	public IDataStream factory(Object sourceSpecifier)
			throws ClassCastException {
		return new ListDataStream((IDataStream) sourceSpecifier);
	}

	/**
	 * @see List#get(int)
	 */
	public Datum get(int index) throws IndexOutOfBoundsException {
		return data.get(index);
	}

	/**
	 * Search through the list for a datum with the given time stamp. This
	 * ignores the current position in the stream!
	 * 
	 * @param time
	 * @param tolerance
	 *            How much difference to allow (e.g. you rarely need to the
	 *            millisecond). Can be null for zero tolerance
	 * @return the (first) datum with this time stamp or null if none.
	 * Prefers exact matches if possible.
	 * 
	 * @see DataUtils#subStream(IDataStream, Time, Time)
	 */
	public Datum get(Time time, Dt tolerance) {
		// TODO binary search for speed
		// Try for an exact match
		for (Datum d : data) {
			if (d.time.equals(time)) return d;
		}
		// Trya gaian with tolerance
		Time min = tolerance == null ? time : time.minus(tolerance);
		Time max = tolerance == null ? time : time.plus(tolerance);
		for (Datum d : data) {
			if (d.time.getTime() >= min.getTime()
					&& d.time.getTime() <= max.getTime())
				return d;
		}
		return null;
	}

	public Time getEnd() {
		return data.get(data.size() - 1).getTime();
	}

	/**
	 * @return the actual backing list. It is probably a bad idea to edit this
	 *         directly.
	 */
	public List<Datum> getList() {
		return data;
	}

	public Time getStart() {
		return data.get(0).getTime();
	}

	@Override
	public boolean isEmpty() {
		return data.isEmpty();
	}

	@Override
	public boolean isFactory() {
		return true;
	}

	@Override
	public AbstractIterator<Datum> iterator() {
		return new AbstractIterator<Datum>() {
			private int i;

			@Override
			protected Datum next2() throws Exception {
				if (i == data.size())
					return null;
				Datum d = data.get(i);
				i++;
				return d;
			}
		};
	}

	/**
	 * Use {@link #clone()} instead. Or just this!
	 */
	@Override
	@Deprecated
	public ListDataStream list() {
		return clone();
	}

	@Override
	public boolean remove(Object o) {
		if (o instanceof Datum)
			return data.remove(o);
		throw new IllegalArgumentException("Not a Datum: " + o.getClass());
	}

	@Override
	public boolean removeAll(Collection<?> c) {
		return data.removeAll(c);
	}

	@Override
	public boolean retainAll(Collection<?> c) {
		return data.retainAll(c);
	}

	@Override
	public int size() {
		return data.size();
	}

	@Override
	public Object[] toArray() {
		return data.toArray();
	}

	@Override
	public <T> T[] toArray(T[] a) {
		return data.toArray(a);
	}

	@Override
	public String toString() {
		int n = Math.min(4, size());
		return ListDataStream.class.getSimpleName()
				+ ": "
				+ Printer.toString(Containers.subList(data, 0, n))
				+ " (first "+n+" items)";
	}

	/**
	 * TODO Insert by time order, or at the end if all times are equal.
	 * currently just adds at the end! 
	 * @param x
	 */
	public void insert(Datum x) {
		add(x);
//		// count back from the end
//		for(int i=data.size()-1; i>-1; i--) {
//			Datum di = data.get(i);
//			if ( ! di.isTimeStamped()) {
//				data.add(index, element);
//			}
//		}
	}

	@Override
	public void add(int arg0, Datum arg1) {
		throw new TodoException();
	}

	@Override
	public boolean addAll(int arg0, Collection<? extends Datum> arg1) {
		throw new TodoException();
	}

	@Override
	public int indexOf(Object item) {
		return data.indexOf(item);
	}

	@Override
	public int lastIndexOf(Object element) {
		return data.lastIndexOf(element);
	}

	@Override
	public ListIterator<Datum> listIterator() {
		return data.listIterator();
	}

	@Override
	public ListIterator<Datum> listIterator(int index) {
		return data.listIterator(index);
	}

	@Override
	public Datum remove(int index) {
		return data.remove(index);
	}

	@Override
	public Datum set(int index, Datum element) {
		return data.set(index, element);
	}

	@Override
	public List<Datum> subList(int fromIndex, int toIndex) {
		return data.subList(fromIndex, toIndex);
	}

}
