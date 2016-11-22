/**
 * 
 */
package winterwell.optimization.genetic;

import java.util.Random;

/**
 * Interface combining generator, mutator and crossover operators.
 * @author Joe Halliwell <joe@winterwell.com>
 *
 */
public interface IBreeder<X> extends IMutate<X> {

	/**
	 * Generate a new candidate, at random or otherwise.
	 * Suggested implementation: use 1 original + mutate.
	 * @return
	 */
	public X generate();
	

	/**
	 * Return a suitable mutated copy of candidate.
	 * This MUST NOT modify the candidate itself.
	 */
	public X mutate(X candidate);


	/**
	 * Crossover two candidates to produce their combined offspring
	 */
	public X crossover(X a, X b);


	/**
	 * Set the underlying random number generator.
	 * If the same seed is set, and other GA parameters held constant,
	 * the GA should output the same sequence of candidates.
	 */
	public void setRandomSource(Random seed);
}
