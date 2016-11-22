/**
 * 
 */
package winterwell.optimization.genetic;

/**
 * Interface for mutation operators
 * @author Joe Halliwell <joe@winterwell.com>
 *
 */
public interface IMutate<X> {

	/**
	 * Return a suitable mutated copy of candidate.
	 * This MUST NOT modify the candidate itself.
	 */
	public X mutate(X candidate);
	
}
