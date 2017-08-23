package com.winterwell.nlp.io;

import java.util.ArrayList;
import java.util.List;

import com.winterwell.depot.Desc;
import com.winterwell.depot.IHasDesc;
import com.winterwell.depot.ModularXML;
import com.winterwell.maths.timeseries.ADataStream;
import com.winterwell.utils.ReflectionUtils;
import com.winterwell.utils.containers.AbstractIterator;

/**
 * A stream of word/sentence tokens.
 * <p>
 * In a slightly baroque but ultimately practical design decision, this doubles
 * as a factory for token streams. Token streams are often linked into chains of
 * processing. Class constructors would not be a good way to instantiate fresh
 * chained streams.
 * <p>
 * To implement: Over-ride one of {@link #iterator()} or {@link #processFromBase(Tkn, AbstractIterator)}.
 * 
 * @author daniel
 * @see ADataStream which uses a similar design
 */
public abstract class ATokenStream implements ITokenStream {

	@Override
	public String toString() {
		return getClass().getSimpleName() +(base==null?"" : " <- " + base);
	}
	
	/**
	 * A token stream is unlikely to have sub-modules.
	 * This method checks whether base is one.
	 */
	@Override
	public IHasDesc[] getModules() {	
		if (base==null) return null;
		if (base instanceof ModularXML) {
			return new IHasDesc[]{base};
		}
		return base.getModules();
	}
	
	protected ATokenStream() {
		this.base = null;
	}
	protected ATokenStream(ITokenStream base) {
		this.base = base;
		desc.addDependency("bs", base.getDesc());
	}


	/**
	 * This is a description of the stream-as-a-factory, & not of the specific-to-this-input stream itself!
	 * Implementations must set parameters in the constructor & setter methods.
	 */
	@Override
	public final Desc<ITokenStream> getDesc() {		
		return desc;
	}
	
	protected final Desc<ITokenStream> desc = new Desc<ITokenStream>(
			ReflectionUtils.getSimpleName(getClass()), ITokenStream.class);
	
	/**
	 * null for a top-level stream.
	 */
	protected final ITokenStream base;
	
	/**
	 * Provides an iterator for chaining token-streams ONLY.
	 * Other sub-classes should override either this or {@link #processFromBase(Tkn)}.
	 */
	@Override
	public AbstractIterator<Tkn> iterator() {
		final AbstractIterator<Tkn> bit = base.iterator();
		return new AbstractIterator<Tkn>() {			
			@Override
			protected Tkn next2() throws Exception {
				while(bit.hasNext()) {			
					Tkn original = bit.next();
					Tkn token = processFromBase(original, bit);
					if (token==null) continue;				
					return token;
				}
				return null;
			}			
			
		};
	}

	
	/**
	 * Work with the default iterator implementation
	 * (which reads from {@link #base}, passing through this method).
	 * @param original
	 * @param bit 
	 * @return token to return, or null to skip this token
	 */
	protected Tkn processFromBase(Tkn original, AbstractIterator<Tkn> bit) {
		throw new UnsupportedOperationException(getClass().getName());
	}


	/**
	 * Override this if you can't be a factory!
	 */
	@Override
	public boolean isFactory() {
		return true;
	}
		

	@Override
	public List<Tkn> toList() {
		List<Tkn> list = new ArrayList<Tkn>();
		for (Tkn tok : this) {
			list.add(tok);
		}
		return list;
	}
}
