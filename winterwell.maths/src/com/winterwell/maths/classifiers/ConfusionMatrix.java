package com.winterwell.maths.classifiers;

import java.util.HashMap;
import java.util.Map;

import com.winterwell.depot.Desc;
import com.winterwell.maths.matrix.ObjectMatrix;
import com.winterwell.maths.stats.distributions.discrete.ObjectDistribution;
import com.winterwell.utils.StrUtils;
import com.winterwell.utils.TodoException;
import com.winterwell.utils.Utils;
import com.winterwell.utils.containers.Containers;

/**
 * 
 * Rows = true values Columns = predicted values
 * 
 * Note that null *is* a valid classification.
 * 
 * TODO an AccuracyTable of tag: correct, false +ive, false -ive
 * with an inflationary unit. & use this in sodash for the thresholds.
 * 
 * @author daniel
 * 
 * @param <X>
 * @testedby  ConfusionMatrixTest}
 */
public class ConfusionMatrix<X> extends ObjectMatrix<X, X> {

	/**
	 * @return When the AI said "apples", what should it have said?
	 * <p>
	 * NB: This is a fresh Map which can be edited without affect.
	 * NB2: This _does_ include 0 values.
	 */	
	@Override
	public Map<X, Double> getColumn(X col) {
		return super.getColumn(col);
	}
	
	private static final long serialVersionUID = 1L;

	private Desc modelDesc;

	private String notes;

	/**
	 * Rows = true values Columns = predicted values
	 */
	public ConfusionMatrix() {
		// TODO Auto-generated constructor stub
	}

	/**
	 * Count a prediction, updating the relevant entry in the matrix by 1.
	 * <p>
	 * This is a convenience for using {@link #plus(Object, Object, double)}.
	 * 
	 * @param target Can be null
	 * @param predicted Can be null
	 */
	public void count(X target, X predicted) {
		{ // ensure all values are in both rows & columns
			plus(target, target, 0);
			plus(predicted, predicted, 0);
		}
		plus(target, predicted, 1);
	}

	/**
	 * @return the number of times trueValue was classified as predictedValue
	 */
	@Override
	public double get(X trueValue, X predictedValue) {
		return super.get(trueValue, predictedValue);
	}

	/**
	 * @return the overall accuracy. Note: boosting elsewhere may affect this!
	 */
	public double getAccuracy() {
		double correct = 0, incorrect = 0;
		for (X k : getRowValues()) {
			correct += getTruePos(k);
			incorrect += getFalseNeg(k);
		}
		return correct / (correct + incorrect);
	}

	public double getErrorRate() {
		return 1.0 - getAccuracy();
	}

	double getFalseNeg(X klass) {
		double falses = 0;
		for (X k : getColumnValues()) {
			if (Utils.equals(k, klass)) {
				continue;
			}
			falses += get(klass, k);
		}
		return falses;
	}

	double getFalsePos(X klass) {
		double falses = 0;
		for (X k : getRowValues()) {
			if (Utils.equals(k, klass)) {
				continue;
			}
			falses += get(k, klass);
		}
		assert falses == getTotalPredicted(klass) - getTruePos(klass);
		return falses;
	}

	/**
	 * See http://en.wikipedia.org/wiki/F_score
	 * 
	 * @param beta
	 *            1 is the normal value.
	 */
	public double getFScore(double beta) {
		assert beta > 0;
		double correct = 0;
		double allReturned = 0;
		double allLabelled = 0;
		for (X k : getRowValues()) {
			if (k == null) {
				continue;
			}
			correct += getTruePos(k);
			allReturned += getTotalPredicted(k);
			allLabelled += getRowTotal(k);
		}
		if (correct == 0)
			return 0;
		double precision = correct / allReturned; // correct results divided by
													// all returned results
		double recall = correct / allLabelled; // results divided by results
												// that should have been
												// returned
		double b2 = beta * beta;
		return (1 + b2) * precision * recall / (b2 * (precision + recall));
	}

	public Desc getModelDesc() {
		return modelDesc;
	}

	public String getNotes() {
		return notes;
	}

	/**
	 * Positive predictive value: true predictions for this class / (total
	 * predictions for this class)
	 * 
	 * @param klass
	 * @return PPV (zero if no predictions were made for this class)
	 */
	public double getPPV(X klass) {
		double tp = getTruePos(klass);
		double total = getTotalPredicted(klass);
		if (total == 0)
			return 0;
		return tp / total;
	}

	/**
	 * Sensitivity = recall
	 * 
	 * @param klass
	 * @return (true +ive for klass) / (total true for klass). 0 if there were
	 *         no instances of klass
	 */
	public double getSensitivity(X klass) {
		double total = getRowTotal(klass);
		// double total2 = getTruePos(klass)+getFalseNeg(klass);
		// assert total == total2 : total +" vs "+total2;
		if (total == 0)
			return 0;
		return getTruePos(klass) / total;
	}

	public double getSpecificity(X klass) {
		return getTrueNeg(klass) / (getTrueNeg(klass) + getFalsePos(klass));
	}

	/**
	 * @return total number of things counted
	 */
	public double getTotal() {
		double sum = 0;
		for (X klass : getRowValues()) {
			sum += getRowTotal(klass);
		}
		return sum;
	}

	
	/**
	 * @param klass
	 * @return how many did we predict? true positives + false positives
	 */
	public double getTotalPredicted(X klass) {
		return getColumnTotal(klass);
	}

	private int getTrueNeg(X klass) {
		throw new TodoException(); // TODO
	}

	double getTruePos(X klass) {
		return get(klass, klass);
	}

	/**
	 * Store a description of the model that was being tested. Convenience for a
	 * common-ish case.
	 * 
	 * @param modelDesc
	 */
	public void setModelDesc(Desc modelDesc) {
		this.modelDesc = modelDesc;
	}

	/**
	 * It can be handy to store details about the experiment with the confusion
	 * matrix.
	 * 
	 * @param notes
	 */
	public void setNotes(String notes) {
		this.notes = notes;
	}

	@Override
	public String toString() {
		return "row=true, column=predicted" + StrUtils.LINEEND
				+ super.toString(1000);
	}

	/**
	 * Status: untested!
	 * @param vector The AI's estimates
	 * @return corrected values
	 */
	public Map<X, Double> correctForConfusion(Map<X, Double> vector) {
		Map<X,Double> out = new HashMap();
		for(X tag : vector.keySet()) {
			double vi = vector.get(tag);
			if (vi==0) continue;
			Map<X, Double> col = getColumn(tag);
			ObjectDistribution<X> colDistro = new ObjectDistribution<X>(col, false);
			colDistro.normalise();
			if (colDistro.getTotalWeight()==0) {
				// No confusion data?! assume correct
				Containers.plus(out, tag, vi);
				continue;
			}
			// add to out
			for(X otag : colDistro) {
				Containers.plus(out, otag, colDistro.get(otag) * vi);
			}
		}
		return out;
	}
}
