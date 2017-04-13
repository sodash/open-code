package com.winterwell.maths.timeseries;

import java.io.Serializable;
import java.util.Arrays;

import com.winterwell.utils.Printer;
import com.winterwell.utils.Utils;
import com.winterwell.utils.time.Time;
import com.winterwell.utils.time.TimeUtils;

import no.uib.cipr.matrix.DenseVector;
import no.uib.cipr.matrix.Vector;

/**
 * A labelled time-stamped vector.
 * <p>
 * If you want to store a nominal attribute (e.g. a string), you should maintain
 * an index elsewhere and store the index-value.
 * <p>
 * The safety checks use <code>assert</code> rather than throwing exceptions.
 * This is done for speed, given that Datum is likely to be used in intensive
 * settings. This may be a foolish optimisation.
 * <p>
 * Comparable/sorting works by timestamp
 * 
 * @author daniel
 */
public final class Datum extends DenseVector implements Comparable<Datum>,
		Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * Convenience for testing. Test time, label and data
	 * 
	 * @param d1
	 * @param d2
	 * @return
	 */
	public static boolean equals(Datum d1, Datum d2) {
		return d1.time.equals(d2.time) && Utils.equals(d1.label, d2.label)
				&& Arrays.equals(d1.getData(), d2.getData());
	}

	private Object label;

	private boolean modifiable;

	public final Time time;

	/**
	 * Convenience constructor for a 1-D unlabelled vector. Time stamp =
	 * {@link TimeUtils#ANCIENT}
	 */
	public Datum(double x) {
		this(null, x, null);
	}

	/**
	 * Convenience constructor for an unlabelled vector. Time stamp =
	 * {@link TimeUtils#ANCIENT}
	 */
	public Datum(double[] vector) {
		this(null, vector, null);
	}

	/**
	 * Convenience for creating 1-dimensional data points.
	 * 
	 * @param time
	 * @param x
	 * @param label
	 */
	public Datum(Time time, double x, Object label) {
		this(time, new double[] { x }, label);
	}

	/**
	 * 
	 * @param date
	 *            Cannot be null
	 * @param vector
	 *            Be careful: This is used directly without copying it.
	 * @param label
	 *            Can be null
	 */
	public Datum(Time date, double[] vector, Object label) {
		this(date, vector, label, false);
	}
	
	/**
	 * 
	 * @param date
	 *            If null, defaults to ANCIENT
	 * @param vector
	 *            Be careful: This is used directly without copying it.
	 * @param label
	 *            Can be null
	 */
	public Datum(Time date, double[] vector, Object label, boolean allowNonFinite) {
		super(vector, false);
		this.time = date==null? TimeUtils.ANCIENT : date;
		this.label = label;
		// paranoia
		if (allowNonFinite) return;
		if ( ! DataUtils.isFinite(this)) {
			throw new IllegalArgumentException("Non-finite number in: "+this);
		}
	}


	/**
	 * 
	 * @param time
	 * @param v
	 *            is copied (unlike the array constructor)
	 * @param label
	 */
	public Datum(Time time, Vector v, Object label) {
		this(time, DataUtils.toArray(v), label);
	}

	public Datum(Vector vector) {
		this(DataUtils.toArray(vector));
	}

	/**
	 * @param v
	 *            is copied (unlike the array constructor)
	 * @param label
	 *            <p>
	 *            Time stamp = {@link TimeUtils#ANCIENT}
	 */
	public Datum(Vector v, Object label) {
		this(TimeUtils.ANCIENT, DataUtils.toArray(v), label);
	}

	@Override
	public void add(int index, double value) {
		assert modifiable;
		super.add(index, value);
	}
	
	@Override
    public Datum scale(double alpha) {		
        super.scale(alpha);
        return this;
    }

	/**
	 * Sorts earliest-first
	 */
	@Override
	public int compareTo(Datum o) {
		return time.compareTo(o.time);
	}

	/**
	 * Create a deep copy. The copy *is* modifiable, regardless of whether the
	 * original is.
	 */
	@Override
	public Datum copy() {
		Datum d = new Datum(time, getData().clone(), label);
		d.setModifiable(true);
		return d;
	}

	/**
	 * @return If modifiable, returns the backing array. If unmodifiable,
	 *         returns a *copy* of the backing array.
	 * @deprecated fiddling directly with the array can lead to strange data
	 *             issues do this only after you've thought it through.
	 */
	@Override
	@Deprecated
	public double[] getData() {
		if (modifiable)
			return super.getData();
		return super.getData().clone();
	}

	/**
	 * Identical to {@link #size()}, but with a more self-explanatory name. 
	 */
	public int getDim() {
		return size();
	}

	/**
	 * @return the label or null if unlabelled
	 */
	public Object getLabel() {
		return label;
	}

	/**
	 * @return never null
	 */
	public final Time getTime() {
		return time;
	}

	/**
	 * @param aLabel
	 *            Can be null
	 * @return true if this Datum has the given label (test is bby equals() and
	 *         this includes null==null).
	 */
	public boolean isLabelled(Object aLabel) {
		return Utils.equals(label, aLabel);
	}

	@Override
	public Vector set(double alpha, Vector y) {
		assert modifiable;
		return super.set(alpha, y);
	}

	@Override
	public void set(int index, double value) {
		assert modifiable;
		super.set(index, value);
	}

	public void setLabel(Object label) {
		assert modifiable;
		this.label = label;
	}

	/**
	 * If false, the Datum cannot be edited (though this is only protected by
	 * asserts!).
	 * 
	 * @param modifiable
	 */
	public void setModifiable(boolean modifiable) {
		this.modifiable = modifiable;
	}

	@Override
	public String toString() {
		if (label != null)
			return Printer.toString(label) + ": " + Printer.toString(getData()); // +": "+time;
		return Printer.toString(getData()); // +": "+time;
	}

	/**
	 * @return If this is one-dimensional, return the number. It is an error to
	 *         call this on a multi-dimensional datum.
	 */
	public double x() {
		assert getDim() == 1;
		return getData()[0];
	}

	@Override
	public DenseVector zero() {
		assert modifiable;
		return super.zero();
	}

	/**
	 * @return true if this has a real time-stamp
	 * (since {@link #getTime()} never returns null)
	 */
	public boolean isTimeStamped() {
		return ! TimeUtils.ANCIENT.equals(time);
	}

	/**
	 * 
	 * @return true if this vector is all zeroes.
	 */
	public boolean isZero() {
		double[] d = super.getData();
		for (double xi : d) {
			if (xi!=0) return false;
		}
		return true;
	}

	/**
	 * Convert x into a Datum if itisn't one already.
	 */
	public static Datum datum(Vector x) {
		return x instanceof Datum? (Datum) x : new Datum(x);
	}
}
