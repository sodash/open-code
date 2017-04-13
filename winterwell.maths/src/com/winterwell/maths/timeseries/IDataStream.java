/**
 * 
 */
package com.winterwell.maths.timeseries;

import java.io.Closeable;

import com.winterwell.maths.IFactory;
import com.winterwell.maths.datastorage.IIndex;
import com.winterwell.maths.datastorage.Vectoriser;
import com.winterwell.utils.containers.AbstractIterator;
import com.winterwell.utils.time.Dt;

/**
 * TODO make closer to List?? - separate objects for streams / iterators - you
 * can try to reuse a data-stream, but it may throw an exception.
 * 
 * <h3>Using data streams with non-numerical data</h3> You have three choices:
 * <p>
 * 1. Assign a dimension to each value (e.g. using an {@link IIndex}), and
 * convert sequences of items into vectors, e.g. using a
 * {@link Vectoriser#toVector(Iterable)}. This makes sense for e.g. words in a
 * sentence.
 * <p>
 * 2. Assign an index to each value (e.g. using an IIndex), and generate a 1-D
 * stream of index values e.g. using {@link Vectoriser#toIndexStream(Iterable)}.
 * This makes sense for e.g. values in a set.
 * <p>
 * 3. Set all vectors to 0, and use the label of the {@link Datum}s to carry the
 * values. This is for e.g. using some IDataStream time handling machinery
 * without vectorising the objects.
 * 
 * @author daniel
 */
public interface IDataStream extends Iterable<Datum>,
		IFactory<Object, IDataStream>, 
		Closeable {

	/**
	 * Since data streams can be linked to system resources, it's best to close
	 * them when done. Data-streams should also be self closing - ie. they close
	 * when empty.
	 */
	@Override
	void close();

	/**
	 * Creates a new data stream of the same class <i>and configuration</i> as
	 * the invocant. Presumably this data stream currently draws from a base
	 * data stream. It should recursively call instantiate() on the base stream
	 * in order to recreate the pipeline. E.g. <code><pre>
// recurse
IDataStream source = base instanceof IDataStream2?
				((IDataStream2)base).instantiate(sourceSpecifier)
				: (IDataStream) sourceSpecifier;
// clone
clone = new MyStream(source);
clone.setConfigStuff(myConfig);
return clone;
		</pre></code>
	 * 
	 * @param sourceSpecifier
	 *            This depends on the processing chain - it is whatever the end
	 *            of the chain expects! *Either* this is an IDataStream to be
	 *            used, *or* it provides the information for the lowest-level
	 *            stream to create a new data stream (e.g. it might specify a
	 *            source file).
	 * 
	 * @return the new data stream object.
	 * @throws ClassCastException
	 *             if sourceSpecifier was not of the right type.
	 * 
	 *             <p>
	 *             Note: I tried to add a generic type to sourceSpecifier, but
	 *             this becomes ugly as data streams which wrap other data
	 *             streams have trouble with this.
	 */
	@Override
	IDataStream factory(Object sourceSpecifier);

	/**
	 * @return the stream dimension
	 */
	int getDim();

	/**
	 * @return the sampling frequency for this stream, or null if completely
	 *         unknown. It is acceptable for there to be gaps in the samples,
	 *         ie. for the dt between two Datums to be greater than the sampling
	 *         frequency.
	 */
	Dt getSampleFrequency();

	/**
	 * @return the source specifier for the current pipeline setup.
	 */
	Object getSourceSpecifier();

	/**
	 * 
	 * @return true if this is a stream of 1-dimensional data (i.e. numbers).
	 *         Note: returns false for empty streams.
	 */
	boolean is1D();

	/**
	 * This is more reliable than size() for testing whether there is any
	 * content.
	 * <p>
	 * Note: Will call {@link AbstractIterator#peekNext()} to poll the stream.
	 * 
	 * @return true if empty
	 */
	boolean isEmpty();

	/**
	 * @return true if {@link #factory(Object)} can be used.
	 */
	@Override
	boolean isFactory();

	// TODO IProgress
	@Override
	public AbstractIterator<Datum> iterator();

	/**
	 * Read the rest of this datastream into a {@link ListDataStream}.
	 * <p>
	 * Warning: will hang if this is an infinite stream!
	 */
	// Rational: ListDataStreams have a fast copy for reusing them -- which is
	// very handy
	ListDataStream list();

	/**
	 * @return the number of data-points in the stream, or -1 if unknown.
	 *         WARNING: Only some streams can judge their size! ?? should this
	 *         be the total number or the number remaining?
	 * @see #isEmpty()
	 */
	int size();

}