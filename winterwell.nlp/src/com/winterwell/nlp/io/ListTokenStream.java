package com.winterwell.nlp.io;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import com.winterwell.utils.Printer;
import com.winterwell.utils.TodoException;
import com.winterwell.utils.containers.AbstractIterator;
import com.winterwell.utils.containers.Containers;
import com.winterwell.utils.log.Log;

/**
 * A Token stream backed by a list.
 * 
 * This class does allow you to add more Token, but this is not threadsafe. To
 * iterate through again, use {@link #copyWithReset()}.
 * 
 * @author Daniel
 */
public final class ListTokenStream extends ATokenStream implements
		Collection<Tkn> {

	private final List<Tkn> data;

	public ListTokenStream(int capacity) {
		data = new ArrayList<Tkn>(capacity);
	}

	/**
	 * Read from the input stream into this one.
	 */
	public ListTokenStream(ITokenStream stream) {
		this(Containers.getList(stream));
	}

	/**
	 * 
	 * @param Token
	 *            This will be copied. If it empty, the stream will have zero
	 *            dimensions and be pretty much unusable.
	 */
	public ListTokenStream(List<Tkn> Token) {
		// copy the list to our backing store
		Token = new ArrayList<Tkn>(Token);
		if (Token.isEmpty()) {
			Log.report("Empty Token-list: created 0D Tokenstream");
		}
		this.data = Token;
	}

	/**
	 * Create a ListTokenStream which directly reuses the backing list, but
	 * maintains its own position. This is a faster alternative to
	 * {@link #copyWithReset()}: you get the rewind but not the copy.
	 * 
	 * @param data
	 *            This will NOT be copied
	 * @param share
	 *            must be true. Used to separate from the other constructor.
	 */
	public ListTokenStream(ListTokenStream data, boolean share) {
		assert share == true;
		this.data = data.data;
	}

	/**
	 * Convenience constructor
	 * 
	 * @param splitByWhitespace
	 */
	public ListTokenStream(String splitByWhitespace) {
		this(12);
		String[] words = splitByWhitespace.split("\\s+");
		for (String word : words) {
			add(new Tkn(word));
		}
	}

	/**
	 * Add a new piece of Token to the end of the stream. This method is not
	 * threadsafe.
	 * 
	 * @param d
	 */
	@Override
	public boolean add(Tkn d) {
		return data.add(d);
	}

	@Override
	public boolean addAll(Collection<? extends Tkn> c) {
		return data.addAll(c);
	}

	@Override
	public void clear() {
		data.clear();
	}

	@Override
	public boolean contains(Object o) {
		if (o instanceof Tkn)
			return data.contains(o);
		throw new IllegalArgumentException("Not a Token: " + o.getClass());
	}

	@Override
	public boolean containsAll(Collection<?> c) {
		return data.containsAll(c);
	}

	@Override
	public ITokenStream factory(String input) {
		throw new TodoException();
	}

	public List<Tkn> getList() {
		return data;
	}

	@Override
	public boolean isEmpty() {
		return data.isEmpty();
	}

	@Override
	public boolean isFactory() {
		throw new TodoException();
	}

	@Override
	public AbstractIterator<Tkn> iterator() {
		final Iterator<Tkn> bit = data.iterator();
		return new AbstractIterator<Tkn>() {
			@Override
			protected Tkn next2() throws Exception {
				if ( ! bit.hasNext()) return null;
				return bit.next();
			}			
		};
	}
	
	@Override
	public boolean remove(Object o) {
		if (o instanceof Tkn)
			return data.remove(o);
		throw new IllegalArgumentException("Not a Token: " + o.getClass());
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
		return ListTokenStream.class.getSimpleName()
				+ ": "
				+ Printer.toString(Containers.subList(data, 0,
						Math.min(4, size()))) + " (first 4 items)";
	}

}
