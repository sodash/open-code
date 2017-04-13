/**
 * 
 */
package com.winterwell.nlp.io;

import java.util.List;

import com.winterwell.maths.IFactory;
import com.winterwell.utils.containers.AbstractIterator;

import com.winterwell.depot.IHasDesc;

/**
 * {@link ITokenStream}s can be chained together to form a processing pipeline.
 * {@link ITokenStream}s can also act as factories for producing equivalent
 * processing pipelines.
 * 
 * 
 * @author daniel
 */
public interface ITokenStream extends Iterable<Tkn>,
		IHasDesc, IFactory<String, ITokenStream> 
{

	@Override
	public AbstractIterator<Tkn> iterator();
	
	/**
	 * Creates a new tokenizer of the same class and configuration as the
	 * invocant.
	 * <p>
	 * Be careful when implementing this that you do copy all the settings!
	 * 
	 * @param input
	 *            the input stream for the new tokenizer.
	 * @return the new tokenizer object.
	 */
	@Override
	ITokenStream factory(String input);
	
	List<Tkn> toList();
}