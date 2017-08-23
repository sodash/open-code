package com.winterwell.maths.stats.distributions.cond;

import java.util.Collection;
import java.util.stream.Stream;

import com.winterwell.utils.containers.AbstractIterator;
import com.winterwell.utils.containers.Containers;

/**
 * A stream/list of Sitns (which are context + outcome)
 * @author daniel
 *
 * @param <X> type of Sitn outcome -- usually String, because a Sitn is usually a word in a document
 * with some context. This is *not* the classifier outcome.
 * 
 * @see StreamClassifier
 */
public interface ISitnStream<X> extends Iterable<Sitn<X>>, IHasSignature {

	default Stream<Sitn<X>> stream() {
		return Containers.getList(this).stream();
	}
	
	/**
	 * Creates a new data stream of the same class <i>and configuration</i> as
	 * the invocant.
	 * 
	 * @param sourceSpecifier
	 *            This depends on the processing chain - it is whatever the end
	 *            of the chain expects! *Either* this is an ISitnStream to be
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
	 *             this becomes ugly as streams which wrap other streams have
	 *             trouble with this.
	 */
	ISitnStream<X> factory(Object sourceSpecifier);

	String[] getContextSignature();

	/**
	 * @return the classes which can be used in {@link #factory(Object)}. E.g.
	 *         [String], or [String,IDocument]
	 */
	Collection<Class> getFactoryTypes();

	@Override
	public AbstractIterator<Sitn<X>> iterator();

}
