/**
 *
 */
package com.winterwell.maths.hmm;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import com.winterwell.maths.ITrainable;
import com.winterwell.maths.stats.distributions.IDistributionBase;
import com.winterwell.maths.stats.distributions.cond.ACondDistribution;
import com.winterwell.maths.stats.distributions.cond.IFiniteCondDistribution;
import com.winterwell.utils.BestOne;
import com.winterwell.utils.Printer;
import com.winterwell.utils.TodoException;
import com.winterwell.utils.containers.Containers;
import com.winterwell.utils.time.Dt;

/**
 * An HMM where the states might be continuous and the timesteps variable.
 * 
 * @author Daniel
 * @testedby {@link FlexiHMMTest}
 */
public class FlexiHMM<Observed, Hidden> extends
		ACondDistribution<Observed, Hidden> implements
		ITrainable.Seqn2Layer<Observed, Hidden> {

	private static final Object END = "END";
	private static final Object START = "START";

	private final IFiniteCondDistribution<Observed, Hidden> pEmit;

	private final IFiniteCondDistribution<Hidden, Hidden> pTrans;

	protected long timestepMillisecs;

	public FlexiHMM(IFiniteCondDistribution<Observed, Hidden> pEmit,
			IFiniteCondDistribution<Hidden, Hidden> pTrans) {
		this.pEmit = pEmit;
		this.pTrans = pTrans;
	}

	@Override
	public void finishTraining() {
		super.finishTraining();
		if (pEmit instanceof ITrainable) {
			((ITrainable) pEmit).finishTraining();
		}
	}

	@Override
	public IDistributionBase<Observed> getMarginal(Hidden context) {
		throw new TodoException();
	}

	@Override
	public boolean isReady() {
		return super.isReady();
	}

	@Override
	public double logProb(Observed outcome, Hidden context) {
		return pEmit.logProb(outcome, context);
	}

	protected Collection<Hidden> nextStates(Collection<Hidden> olds,
			Observed obs) {
		return pEmit.getPossibleCauses(obs);
	}

	@Override
	public double prob(Observed outcome, Hidden context) {
		return pEmit.prob(outcome, context);
	}

	protected double PTrans(Hidden newH, Hidden oldH, Dt dt) {
		if (oldH == null)
			// strip type safety
			return ((IFiniteCondDistribution) pTrans).prob(newH, START);
		assert newH != null : oldH;
		return pTrans.prob(newH, oldH);
	}

	@Override
	public void resetup() {
		super.resetup();
		if (pEmit instanceof ITrainable) {
			((ITrainable) pEmit).resetup();
		}
	}

	public List<Observed> sampleSeqn(int len) {
		Hidden prev = null;
		List<Observed> list = new ArrayList<Observed>(len);
		for (int i = 0; i < len; i++) {
			Hidden h = pTrans.sample(prev);
			Observed e = pEmit.sample(h);
			prev = h;
			list.add(e);
		}
		// TODO use END
		return list;
	}

	@Override
	public String toString() {
		try {
			return getClass().getSimpleName() + ":" + sampleSeqn(3);
		} catch (Exception e) {
			return getClass().getSimpleName();
		}
	}

	@Override
	public void train1(Hidden x, Observed tag, double weight) {
		((ITrainable.CondUnsupervised<Hidden, Observed>) pEmit).train1(x, tag, weight);
		// can't train transition here
	}

	@Override
	public void trainSeqn(List<Observed> observed, List<Hidden> hidden) {
		assert observed.size() == hidden.size();
		Object prev = START;
		// throw away typing - use erasure to allow START and END
		CondUnsupervised trans = (ITrainable.CondUnsupervised) pTrans;
		for (int i = 0; i < observed.size(); i++) {
			Hidden h = hidden.get(i);
			// train pEmit
			train1(h, observed.get(i), 1);
			// train pTrans
			trans.train1(prev, h, 1);
			prev = h;
		}
		trans.train1(prev, END, 1);
	}

	public List<Hidden> viterbi(List<? extends Observed> visible) {
		// FIXME use start, add in end
		int n = visible.size();
		// For each observation
		List<Path> prevPaths = new ArrayList<Path>();
		prevPaths.add(new Path());
		Collection olds = Collections.singleton(START);
		for (int i = 0; i < n; i++) {
			Observed oi = visible.get(i);
			// update best path to each hidden
			List<Path> newPaths = new ArrayList<Path>();
			Collection<Hidden> nexts = nextStates(olds, oi);
			for (Hidden newH : nexts) {
				// Find best connection from the previous paths
				BestOne<Path<Hidden>> bestPathToHi = new BestOne<Path<Hidden>>();
				for (Path<Hidden> path : prevPaths) {
					Hidden oldH = path.list.isEmpty() ? null : Containers
							.last(path.list);
					double pt = PTrans(newH, oldH, new Dt(path.timeInOldH));
					double Phi = path.logProb + Math.log(pt);
					bestPathToHi.maybeSet(path, Phi);
				}
				// Extend the path
				Path<Hidden> path = bestPathToHi.getBest();
				double lp = bestPathToHi.getBestScore();
				if (lp == 0) {
					continue;
				}
				lp += logProb(oi, newH);
				Path<Hidden> extPath = new Path<Hidden>(path, newH, lp);
				if (!path.list.isEmpty() && newH == Containers.last(path.list)) {
					extPath.timeInOldH = timestepMillisecs + extPath.timeInOldH;
				}
				newPaths.add(extPath);
			}
			assert !newPaths.isEmpty() : prevPaths + " " + oi;
			prevPaths = newPaths;
			olds = nexts;
		}

		// Take the best
		BestOne<Path<Hidden>> bestPath = new BestOne<Path<Hidden>>();
		for (Path<Hidden> path : prevPaths) {
			bestPath.maybeSet(path, path.logProb);
		}
		Path<Hidden> best = bestPath.getBest();
		return best.list;
	}

}

final class Path<Hidden> {

	final List<Hidden> list;

	final double logProb;

	/**
	 * millisecs
	 */
	public long timeInOldH;

	public Path() {
		logProb = 0;
		list = Collections.emptyList();
	}

	public Path(Path<Hidden> path, Hidden newH, double logProb) {
		this.logProb = logProb;
		list = new ArrayList<Hidden>(path.list.size() + 1);
		list.addAll(path.list);
		list.add(newH);
	}

	@Override
	public String toString() {
		if (list.size() > 5)
			return "Path[" + list.get(0) + "..." + list.get(list.size() - 1)
					+ "]";
		return "Path" + Printer.toString(list);
	}
}
