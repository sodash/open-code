package com.winterwell.maths.datastorage;

import java.util.Iterator;

/**
 * An index maps objects to index numbers (starting at 0, with unknown = -1),
 * and vice-versa.
 *  
 * @see IPruneListener
 * @see HalfLifeIndex
 * @author Daniel
 */
// Note: the signature doesn't quite match Collection :(
public interface IIndex<T> extends Iterable<T> {

	/**
	 * ??what are the rules around this??
	 */
	public static final int UNKNOWN = -1;

	/**
	 * Add to the index. It is an error to add an object which is already in the
	 * index (behaviour undefined).
	 * 
	 * @param obj
	 * @return the index for this object
	 * @see #indexOfWithAdd(Object)
	 */
	int add(T obj);

	/**
	 * For notifying users when the index is pruned. Only some indexes perform
	 * pruning (e.g. HalfLifeIndex)! For other classes this is a no-op.
	 * <b>Warning: Listeners may be transient and held using
	 * weak-references!</b>
	 * 
	 * @param listener
	 *            A listener can be re-added without effect.
	 */
	void addListener(IPruneListener listener);

	boolean contains(T obj);

	/**
	 * @param i
	 * @return index-entry i, or null if i is an invalid index-number
	 */
	T get(int i);

	/**
	 * The index values in this index. Opposite of {@link #iterator()} (sort
	 * of).
	 */
	Iterable<Integer> getIndexValues();

	/**
	 * @param obj
	 * @return the index of object, or -1 if it is not in the index.
	 */
	int indexOf(T obj);

	/**
	 * Equivalent to:
	 * <code>return contains(obj)? indexOf(obj) : add(obj);</code> This is a
	 * convenience method for a common case.
	 * 
	 * @param obj
	 * @return the index for obj (never 0)
	 */
	int indexOfWithAdd(T obj);

	/**
	 * Note: remove will quietly screw things up!
	 */
	@Override
	Iterator<T> iterator();

	/**
	 * Number of elements in the index. Note: indexOf() can return higher values
	 * than this.
	 */
	int size();

	/**
	 * Copy out the index keys into an array. Warning: array index !=
	 * index-index
	 */
	Object[] toArray();

}