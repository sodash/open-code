/**
 * 
 */
package winterwell.optimization;

import com.winterwell.depot.Desc;


/**
 * Interface for objective functions.
 * Evaluation may be resource-intensive and/or time-consuming.
 * @param <X> The thing being evolved.
 * @param Output An intermediate output, from which a score can be extracted. 
 * E.g. this might be a confusion matrix, or data on performance-over-time.
 *  
 * @author Joe Halliwell <joe@winterwell.com>
 */
public interface IEvaluate<X,Output> {

	/**
	 * How good is this candidate? Higher scores are better.
	 * @param candidate the candidate to be evaluated
	 * @return a score -- the higher the better
	 */
	double evaluate(X candidate) throws Exception;

	/**
	 * Suggested code: see ScoreClassifier
	 * 
	 * @param candidate
	 * @return a Desc for storing the Output object. Can be null for do-not-store.
	 */
	Desc<Output> getResultDesc(X candidate);
	
	/**
	 * @param result
	 * @return score
	 */
	double result2score(Output result);

}
