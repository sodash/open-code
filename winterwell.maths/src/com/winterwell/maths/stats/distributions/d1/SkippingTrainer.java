package com.winterwell.maths.stats.distributions.d1;

import com.winterwell.maths.ITrainable;
import com.winterwell.utils.TodoException;
import com.winterwell.utils.Utils;

/**
 * Work with large (size unknown) data sets, and reduce costly calculations
 * by sampling.
 * 
 * Usage:
 * 
 * <pre><code>
 * for(data) {
 * 	if (skip()) continue;
 * 	X x = compute_a_value();
 *  train1(x);
 * }
 * </code></pre>
 * 
 * The amount trained is logarithmic:
 * 10 items = all 10 trained.
 * 100 items = ~50 trained.
 * 1000 items = ~100 trained.
 * 10,000 items = ~500 trained.
 * 
 * @author daniel
 * @testedby  SkippingTrainerTest}
 */
public class SkippingTrainer<X> implements
ITrainable.Unsupervised.Weighted<X>  
{

	private ITrainable.Unsupervised<X> base;

	public SkippingTrainer(ITrainable.Unsupervised<X> base) {
		this.base = base;
	}
	
	@Override
	public void finishTraining() {
		((ITrainable) base).finishTraining();
	}

	@Override
	public void train1(X data, double weight) {
		throw new TodoException();
//		if (base instanceof ITrainable.Unsupervised.Weighted) {
//			((ITrainable.Unsupervised.Weighted) base).train1(data, weight);
//		} else {
//			ITrainable.Unsupervised _base = (ITrainable.Unsupervised) base;
//			for(int i=0; i<)
//		}
	}

	/**
	 * @deprecated Why use this method?
	 * This does NOT itself skip!
	 */
	@Override
	public void train(Iterable<? extends X> data) {
		for (X d : data) {
			train1(d);
		}		
	}

	int cnt;
	
	/**
	 * @return true => skip this bit of data!, false for call train1() with it.
	 */
	public boolean skip() {
		double p = pSkip();
		if (Utils.getRandomChoice(p)) {
			return true;
		}
		return false;
	}
	
	public double pSkip() {
		return 1 - Math.min(10.0/cnt, 1);
	}
	
	/**
	 * @return number of accepted items.
	 */
	public int getCnt() {
		return cnt;
	}

	/**
	 * This does NOT itself skip! But it DOES weight the data assuming
	 * skip() is being used.
	 */
	@Override
	public void train1(X data) {
		double ps1 = 1 / (1 - pSkip());
		cnt++;
		if (base instanceof ITrainable.Unsupervised.Weighted) {
			((ITrainable.Unsupervised.Weighted<X>)base).train1(data, ps1);
		} else {			
			// use a loop instead
			int n = (int) Math.round(ps1);
			for(int i=0; i<n; i++) {
				base.train1(data);
			}
		}
	}

	@Override
	public boolean isReady() {
		return ((ITrainable) base).isReady();
	}

	@Override
	public void resetup() {
		((ITrainable) base).resetup();
	}

	@Override
	public void train(double[] weights, Iterable<? extends X> data) {
		throw new TodoException();
	}


}
