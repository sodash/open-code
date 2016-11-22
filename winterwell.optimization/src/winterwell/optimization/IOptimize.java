package winterwell.optimization;

/**
 * Simple interface for optimization drivers.
 * Optimization parameters are set directly on the implementor.
 * 
 * TODO: Possibly we should support common things like the amount of time allotted
 * or a target utility to be taken into account.
 * 
 * @author Joe Halliwell <joe@winterwell.com>
 *
 */
public interface IOptimize<X> {
	
	/**
	 * Return a (possibly) optimal X as measured by the specified objective function
	 * @param objective
	 * @return
	 */
	public X optimize(IEvaluate<X,?> objective);
	
}
