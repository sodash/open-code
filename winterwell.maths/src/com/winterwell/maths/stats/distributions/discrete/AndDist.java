/**
 * 
 */
package com.winterwell.maths.stats.distributions.discrete;

import java.util.Collection;
import java.util.Iterator;

import com.winterwell.utils.MathUtils;
import com.winterwell.utils.Printer;
import com.winterwell.utils.StrUtils;
import com.winterwell.utils.containers.AbstractIterator;

/**
 * For combining independent models: P(X) = P1(X).P2(X)
 * 
 * The opposite OrDist is {@link DiscreteMixtureModel}
 * @testedby AndDistTest
 * @author daniel
 */
public class AndDist<X> extends AFiniteDistribution<X> {

	private final IFiniteDistribution<X>[] parts;


	public AndDist(IFiniteDistribution<X>... parts) {
		this.parts = parts;
		assert parts.length > 0  : Printer.toString(parts);
		for (IFiniteDistribution<X> iFiniteDistribution : parts) {
			assert iFiniteDistribution != null;
		}
	}

	@Override
	public String toString() {
		if (strengths==null) {
			return Printer.toString(parts);
		}
		StringBuilder sb = new StringBuilder();
		for(int i=0; i<parts.length; i++) {
			sb.append(Printer.toStringNumber(strengths[i])+"."+parts[i]+" x ");
		}
		if (sb.length()!=0) StrUtils.pop(sb, 3);
		return sb.toString();
	}

	/**
	 * If values is set, use that.<br>
	 * Otherwise, filter the results from the first part through the other parts.
	 * NB: if strengths is set, this may miss some bits!
	 */
	@Override
	public Iterator<X> iterator() throws UnsupportedOperationException {
		if (values!=null) {
			return values.iterator();
		}
		final Iterator<X> it = parts[0].iterator();
		return new AbstractIterator<X>() {
			@Override
			protected X next2() throws Exception {
				X x = null;
				while(x==null && it.hasNext()) {
					x = it.next();
					for(int i=2; i<parts.length; i++) {
						// if a part is not full strength, then it can't veto
						if (strengths!=null && strengths[i]<1) {
							continue;
						}
						IFiniteDistribution<X> part = parts[i];
						if (part.prob(x) == 0) {
							// no good
							x = null;
							break;
						}
					}
				}
				return x;
			}			
		};
	}
	
	/**
	 * Set the possible values. This defends against a distribution not having
	 *  a value -- which is fine in pure AND mode, but a mistake if {@link #setMaxStrength(double...)}
	 *  is used. Or if the first part uses a pseudo-count!
	 * @param values
	 */
	public void setValues(Collection<X> values) {
		this.values = values;
	}
	
	Collection<X> values;


	@Override
	public int size() {		
		if (values!=null) return values.size();
		// over-estimate
		return parts[0].size();
	}

	private double norm = 1;
	private double[] strengths;

	
	/**
	 * Calculate the (current) normalising constant.
	 * <p>
	 * Unfortunately any edits to the parts could throw this off.
	 * Note: {@link #isNormalised()} always returns false, since we
	 * can't say for sure whether the normalising constant is still valid.
	 */
	@Override
	public void normalise() {
		// set the flag now, so prob will work
		normalised = true; // as long as no-one edits the parts
		// Unfortunately we can never be 100% sure that this is normalised :(
		// If someone edits then normalises a part, that changes our normalising constant.
//		for(IFiniteDistribution part : parts) {
//			part.normalise();
//		}
		// Calculate the normaliser
		double _norm = 0;
		norm = 1; // For the sum calculation below, which calls prob, which needs norm
		for(X x : this) {
			double pi = prob(x);
			assert MathUtils.isFinite(pi) : x+"="+prob(x)+" in AND["+Printer.toString(parts)+"]";
			_norm += pi;
		}
		// Avoid divide-by-zero for empty distributions
		if (_norm==0) {
			_norm = 1;
		}
		norm = _norm;
	}

	@Override
	public double prob(X x) {
		if (!isNormalised()) normalise();
		double p = 1;
		for(int i=0; i<parts.length; i++) {
			IFiniteDistribution<X> part = parts[i];
			double pi = part.prob(x);			
			
			if (strengths!=null && strengths[i]!=1) {
				// TODO test this!!!
				double s = strengths[i];			
				double tw = part.getTotalWeight();
				double dilutedPi = s*pi + (1.0-s)*tw/size();
				pi = dilutedPi;
			}			
			p *= pi;
		}		
		return p / norm;
	}

	/**
	 * Set a strength for each part.
	 * This limits the effect that the part can have on the overall distribution.
	 * The part is treated as a mixture model with a uniform distribution, with weights
	 * strength and (1-strength) respectively. 
	 * <p>
	 * You are strongly advised to {@link #setValues(Collection)} if using this, to ensure
	 * all the tags do come through.
	 * @param strengths 1 = full-on, 0 = ignore completely!
	 */
	public void setMaxStrength(double... strengths) {
		assert strengths.length == parts.length : strengths.length+" vs "+parts.length;
		assert MathUtils.max(strengths) <= 1 : strengths;
		assert MathUtils.min(strengths) >= 0 : strengths;
		this.strengths = strengths;				
		normalised = false;
	}
	
}
