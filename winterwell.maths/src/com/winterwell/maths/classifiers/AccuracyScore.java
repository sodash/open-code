package com.winterwell.maths.classifiers;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.winterwell.utils.MathUtils;
import com.winterwell.utils.StrUtils;
import com.winterwell.utils.Utils;
import com.winterwell.utils.containers.ArrayMap;
import com.winterwell.utils.containers.ArraySet;
import com.winterwell.utils.containers.Containers;
import com.winterwell.utils.containers.Range;
import com.winterwell.utils.web.IHasJson;

/**
 * Accuracy scores: PPV (positive predictive value) & sensitivity by tag.
 * False positives
 * False negatives
 * 
 * This uses a inflationary method of counting which will tend to discount old mistakes
 * (and successes) over time.
 * 
 * Shares some methods with ConfusionMatrix, and can be used as a drop-in
 * replacement.
 * 
 * TODO: Back-end onto a full ConfustionMatrix
 * TODO: Numerical stability? (Unit tests!)
 * 
 * @author daniel
 * @testedby {@link AccuracyScoreTest}
 */
public class AccuracyScore<Tag> implements IHasJson  {
	
	private static final String NULL = "null";
	
	
	
	public Set<Tag> getTags() {
		ArraySet set = new ArraySet();
		set.addAll(correct.keySet());
		set.addAll(falseNeg.keySet());
		set.addAll(falsePos.keySet());
		return set;
	}
	
	
	/**
	 * 
	 * @param tag
	 * @param beta
	 * @return the F-Score for a single tag
	 */
	public double getTagFScore(Tag tag, double beta) {
		assert beta > 0 : beta;
		double right = 0;
		Double c = correct.get(tag);
		if (c != null) right = adjust(c);
		
		if (right == 0) return 0;
		double precision = right / getTotalPredicted(tag);
		double recall = right / getTotal(tag);
		return getFScore(beta, precision, recall);
	}

	/**
	 * Score the classifier overall.
	 * See http://en.wikipedia.org/wiki/F_score
	 * 
	 * @param beta
	 *            1 is the normal value.
	 */
	public double getFScore(double beta) {
		assert beta > 0;
		double right = 0;
		double allReturned = 0;
		double allLabelled = 0;
		for (Tag k : getTags()) {
			Double c = correct.get(k);
			if (c!=null) {
				right += adjust(c);
			}
			allReturned += getTotalPredicted(k);
			allLabelled += getTotal(k);
		}
		if (right == 0) return 0;
		double precision = right / allReturned; // correct results divided by
													// all returned results
		double recall = right / allLabelled; // results divided by results
												// that should have been
												// returned
		return getFScore(beta, precision, recall);
	}
	
	/**
	 * Does the work for {@link #getFScore(double)}
	 * @param beta 1 is the normal value.
	 * @param precision PPV
	 * @param recall sensitivity
	 * @return
	 */
	public static double getFScore(double beta, double precision, double recall) {
		assert beta > 0 : beta;
		assert MathUtils.isProb(precision);
		assert MathUtils.isProb(recall);
		if (precision==0 || recall==0) return 0;
		double b2 = beta * beta;
		return (1 + b2) * precision * recall / (b2 * (precision + recall));
	}
	
	/**
	 * Convenience for {@link #getFScore(double, double, double)}
	 * @param beta
	 * @param correct
	 * @param falsePos
	 * @param falseNeg
	 * @return
	 */
	public static double getFScore(double beta, double correct, double falsePos, double falseNeg) {
		double ppv = correct/(correct+falsePos);
		double sens = correct/(correct+falseNeg);
		return getFScore(beta, ppv, sens);
	}


	public String toString() {
		// get keys
		ArraySet<Tag> keys = new ArraySet(correct.keySet());
		keys.addAll(falsePos.keySet());
		keys.addAll(falseNeg.keySet());
		
		StringBuilder sb = new StringBuilder("AccuracyScore[F-score: "+getFScore(1)
				+ "\ntag	true-total	PPV	sensitivity\n");
		for (Tag object : keys) {
			double ppv = getPPV(object);
			double sens = getSensitivity(object);
			double total = getTotal(object);
			String total_s = StrUtils.toNSigFigs(total, 2);
			String ppv_s = StrUtils.toNSigFigs(100*ppv, 2);
			String sens_s = StrUtils.toNSigFigs(100*sens, 2);
			sb.append(StrUtils.ellipsize(String.valueOf(object), 40)+":\t"+total_s+"\t"+ppv_s+"%\t"+sens_s+"%\n");
		}		
		sb.append("]");
		return sb.toString();
	}
	
	public String toHtml() {
		// get keys
		ArraySet<Tag> keys = new ArraySet(correct.keySet());
		keys.addAll(falsePos.keySet());
		keys.addAll(falseNeg.keySet());
		
		StringBuilder sb = new StringBuilder("<div class='AccuracyScore'><table class='DataTable'>");
		sb.append("<tr><th>Tag</th><th>Total</th><th><abbr title='Positive Predictive Value (accuracy)'>PPV</abbr></th><th>Sensitivity</th></tr>\n");
		for (Tag object : keys) {
			Range ppv = getPPVInterval(object, 0.1);
			Range sens = getSensitivityInterval(object, 0.1);
			double total = getTotal(object);
			if (total==0) {
				continue;	// e.g. null is not a tag, but we might have a falsePos count for it
			}
			String total_s = StrUtils.toNSigFigs(total, 2);
			String ppv_s = StrUtils.toNSigFigs(100*getPPV(object),2)+"% <span class='ConfidenceInterval'>("+StrUtils.toNSigFigs(100*ppv.low, 2) +"% - "+StrUtils.toNSigFigs(100*ppv.high, 2)+"%)</span>";
			String sens_s = StrUtils.toNSigFigs(100*getSensitivity(object),2)+"% <span class='ConfidenceInterval'>("+StrUtils.toNSigFigs(100*sens.low, 2) +"% - "+StrUtils.toNSigFigs(100*sens.high, 2)+"%)</span>";
			sb.append("<tr><td>"+StrUtils.ellipsize(String.valueOf(object), 40)+"</td><td>"
							+total_s+"</td><td>"
							+ppv_s+"</td><td>"
							+sens_s+"</td></tr>\n");
		}		
		sb.append("</table>");
		sb.append("<p>Reporting 90% confidence intervals. ");
		if (inflationRate!=1) sb.append("Unit doubles every "+StrUtils.toNSigFigs(Math.log(2)/Math.log(inflationRate), 2)+" examples.");
		sb.append("</p></div>");
		return sb.toString();
	};
	
	/**
	 * Get an *approximate* count of instances of klass seen.
	 * Why approximate? Because it depends on (precisely) when the tags came in.
	 * @param klass
	 * @return the total true instances of klass
	 */
	public double getTotal(Tag klass) {
		Double tp = correct.get(klass);		
		Double fn = falseNeg.get(klass);
		double total = (tp==null? 0 : tp) + (fn==null? 0 : fn);
		// Correct for the inflationary effect. Sortof.
		return adjust(total);
	}

	final Map<Tag, Double> correct = new HashMap();
	final Map<Tag, Double> falsePos = new HashMap();
	final Map<Tag, Double> falseNeg = new HashMap();
	
	double inflationRate = 1;
	
	public double getUnit() {
		return unit;
	}
	
	public AccuracyScore() {
	}
	
	/**
	 * FIXME: Rename to setHalfLife (or something more period-oriented)
	 * @param doublingPeriod
	 * @return
	 */
	public AccuracyScore<Tag> setInflationRate(int doublingPeriod) {
		assert doublingPeriod > 0;
		double l2 = Math.log(2);
//		double ln = Math.log(doublingPeriod);
		this.inflationRate = Math.exp(l2 / doublingPeriod);
		assert inflationRate >= 1 : inflationRate;
		return this;
	}
	
	/**
	 * Count a prediction, updating the relevant entry in the matrix by (1 * inflation)
	 * <p>
	 * This is a convenience for using {@link #plus(Object, Object, double)}.
	 * </p>
	 * @param target
	 * @param predicted
	 * @param dist 
	 * @param p 
	 */
	public void count(Tag target, Tag predicted) {
		count(target, predicted, 1);
	}
	
	public synchronized void count(Tag target, Tag predicted, double n) {
		// HACK: throw away type-safety info
		Object _target = target==null? NULL : target;
		Object _predicted = predicted==null? NULL : predicted;
		if (Utils.equals(_target, _predicted)) {
			Containers.plus((Map)correct, _target, n*unit);
		} else {
			Containers.plus((Map)falsePos, _predicted, n*unit);
			Containers.plus((Map)falseNeg, _target, n*unit);
		}
		incUnit();	
		count++;
	}
		
	long count = 0;
	double unit = 1;

	private void incUnit() {
		unit *= inflationRate;
	}

	
	
	/**
	 * Positive predictive value: true predictions for this class / (total
	 * predictions for this class)
	 * Also known as `precision`.
	 * @param klass
	 * @return PPV (zero if no predictions were made for this class)
	 */
	public double getPPV(Tag klass) {
		Double tp = correct.get(klass);
		if (tp==null || tp==0) return 0;
		Double fp = falsePos.get(klass);
		if (fp==null) return 1;
		return tp / (tp+fp);
	}
	
	/**
	 * Computes a Wilson score interval
	 * See http://en.wikipedia.org/wiki/Binomial_proportion_confidence_interval#Wilson_score_interval
	 * 
	 * TODO where should we place this?? In StatsUtils? a new BinomialDistro classs, 
	 * or a WilsonScore class?
	 * 
	 * TODO with continuity correction, c.f. 
	 * Newcombe, Robert G. "Two-Sided Confidence Intervals for the Single Proportion: Comparison of Seven Methods," Statistics in Medicine, 17, 857-872 (1998).
	 * 
	 * @param error The "width" of the interval. For example, for a 95% confidence level the error is 5% = 0.05
	 * @param p % success
	 * @param n Number of trials
	 * @return a range within [0, 1]. E.g. error=0.5 -> 95% confidence band for n.p successes out of n trials  
	 */
	public Range getInterval(double error, double p, double n) {
		return WilsonScoreInterval.getInterval(error, p, n);
	}
	
	
	public Range getPPVInterval(Tag tag, double error) {
		double ppv = getPPV(tag);
		double n = getTotalPredicted(tag);
		return getInterval(error, ppv, n);
	}
	
	/**
	 * Get the *approximate* number of predictions seen.
	 * See comments on {@link #getTotal(Object)}
	 * @param klass
	 * @return how many did we predict? true positives + false positives,
	 */
	double getTotalPredicted(Tag klass) {
		Double tp = correct.get(klass);		
		Double fp = falsePos.get(klass);
		double total = (tp==null? 0 : tp) + (fp==null? 0 : fp);
		return adjust(total);
	}
	
	/**
	 * Uses average unit, defined by "integrate unit over n, then divide by n".
	 * @param total
	 * @return
	 */
	private double adjust(double total) {
		if (inflationRate==1 || total==0 || unit==1) return total;
		double log_r = Math.log(inflationRate);
		double area = (unit-1)/(inflationRate - 1); // Sum of geometric progression
		double n = Math.log(unit) / log_r;
		double div = area/n;
		assert div < unit : div+" v "+unit;
		double b = total / div;
//		double a = total / unit; // FIXME this is bogus, though I'm not sure what we should do
		return b;
	}

	public Range getSensitivityInterval(Tag tag, double error) {
		double ppv = getSensitivity(tag);
		double n = getTotal(tag);
		return getInterval(error, ppv, n);
	}

	/**
	 * Sensitivity = recall
	 * 
	 * @param klass
	 * @return (true +ive for klass) / (total true for klass). 0 if there were
	 *         no instances of klass
	 */
	public double getSensitivity(Tag klass) {
		Double tp = correct.get(klass);
		if (tp==null || tp==0) return 0;
		Double fn = falseNeg.get(klass);
		if (fn==null) return 1;
		return tp / (tp+fn);		
	}


	@Override
	public Map toJson2() throws UnsupportedOperationException {
		Map<String, Double> ppv = new ArrayMap();
		Map<String, Double> sensitivity = new ArrayMap();
		for(Tag tag : getTags()) {
			ppv.put(tag.toString(), getPPV(tag));
			sensitivity.put(tag.toString(), getSensitivity(tag));
		}
		return new ArrayMap(
				"correct", correct,
				"ppv", ppv,
				"sensitivity", sensitivity,
				"count", count
				);
	}	
	
}
