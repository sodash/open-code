/**
 * 
 */
package com.winterwell.optimization.genetic;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import com.winterwell.optimization.IEvaluate;
import com.winterwell.optimization.IOptimize;
import com.winterwell.utils.Printer;
import com.winterwell.utils.Utils;
import com.winterwell.utils.WrappedException;
import com.winterwell.utils.io.FileUtils;
import com.winterwell.utils.log.Log;
import com.winterwell.utils.threads.ATask;
import com.winterwell.utils.threads.TaskRunner;
import com.winterwell.utils.time.Dt;
import com.winterwell.utils.time.StopWatch;
import com.winterwell.utils.time.TUnit;
import com.winterwell.utils.time.TimeUtils;
import com.winterwell.utils.web.XStreamUtils;


/**
 * This implements a simple evolutionary optimization strategy.
 * The idea is to subclass it to override certain behaviours e.g. selection
 * Other behaviours e.g. mutation, generation are made pluggable because
 * they could be used elsewhere.
 * <p>
 * <h3>How to Use</h3>
 * You need to create an IBreeder and an IEvaluate<br>
 * GA ga = new GA(breeder, populationSize);<br>
 * ga.optimize(evaluator);<br>
 * <p>
 * @param <X> The things you're evolving. Can be anything. It's a good idea
 * for this to implement equals() & hashcode(), as that will make the system
 * more efficient around equivalent instances.
 * 
 * @testedby {@link GATest}
 * 
 * @author Joe Halliwell <joe@winterwell.com>
 */
public class GA<X> implements IOptimize<X> {
	
	double elite = 0.1;
	int sizeHint;
	File intFile = null;
	private StopWatch timer;

	/**
	 * number of threads to use for evaluation
	 */
	private int numThreads = 4;


	/**
	 * number of threads to use for evaluation.
	 * 4 by default. Set to 1 if evaluation is not thread-safe.
	 */
	public void setNumThreads(int numThreads) {
		this.numThreads = numThreads;
	}
	
	/**
	 * How long to run for (only 1 hour by default)
	 */
	private long timeHintMillisecs = TUnit.HOUR.getMillisecs();
	private int maxGenerations = 100;
	private int statisLimit = 3;
	protected IBreeder<X> breeder;
	private TaskRunner exec;
	
	/**
	 * Stop if the score does not improve in this many generations.
	 * @param statisLimit must be at least 1
	 */
	public void setStatisLimit(int statisLimit) {
		assert statisLimit > 0;
		this.statisLimit = statisLimit;
	}
	
	/**
	 * @param n 1 should give 1 round of mutations
	 */
	public void setMaxGenerations(int n) {
		maxGenerations = n;
	}
	
	/**
	 * How long to run for? (only 1 hour by default)
	 * This will not interrupt during a generation, so it is only a hint.
	 */
	public void setTimeoutHint(Dt maxTimeHint) {
		timeHintMillisecs = maxTimeHint.getMillisecs();
	}
	
	/**
	 * For reporting on how slow evaluations are
	 */
	private AtomicInteger evalCount = new AtomicInteger();
	private StopWatch evalStatsTimer;
	protected IEvaluate<X,?> objectiveFn;
	private double prevBest;
	private int statisCount;

	
	/**
	 * @param elite in [0,1), the top % to pass through to the next generation unchanged. 
	 * E.g. 0.1
	 * <i>The</i> best solution is always kept.
	 */
	public void setElite(double elite) {
		assert (elite >= 0.0) : "elite must be >= 0.0";
		assert (elite < 1.0) : "elite must be < 1.0";
		this.elite = elite;
	}
	
	File getIntermediateFile() {
		return intFile;
	}
	
	/**
	 * If not null, the latest generation will be saved to file. If interrupted, 
	 * the GA will resume from the file.
	 * @param file
	 */
	public void setIntermediateFile(File file) {
		intFile = file;
	}
	
	/**
	 * Construct a new Genetic Algorithm optimizer
	 * @param sizeHint suggested population size. Subclasses might choose to have a larger or smaller population.
	 * @param generator the candidate generator to use
	 * @param mutator the mutation operator to use
	 * @param crossover the crossover operator to use
	 */
	public GA(int sizeHint, IBreeder<X> breeder) {
		assert (sizeHint > 0) : "sizeHint must be greater than 0";
		this.sizeHint = sizeHint;
		this.breeder = breeder;
	}
	
	/**
	 * @see com.winterwell.optimization.IOptimize#optimize(com.winterwell.optimization.IEvaluate)
	 */
	@Override
	public X optimize(IEvaluate<X,?> objective) {
		this.objectiveFn = objective;
		timer = new StopWatch();
		evalStatsTimer = new StopWatch();		
		Log.i("ga", "Evaluate "+objective);		
		// create thread pool
		if (numThreads==1) {
			exec = new TaskRunner(true);
		} else {
			exec = new TaskRunner(numThreads);
		}
		try {			
			// First generation
			Generation current = getFirstGeneration();				
			// Search!
			X best = (X) optimize2(current);
			return best;
		} catch (Exception e) {
			throw Utils.runtime(e);
		} finally {
			// close the thread pool
			exec.shutdownNow();
		}
	}

	private Generation<X> getFirstGeneration() throws Exception {
		if (intFile!=null && intFile.exists()) {
			Log.i("ga", "Loading initial population from "+intFile);
			Generation<X> current = XStreamUtils.serialiseFromXml(FileUtils.read(intFile));
			return current;
		}
		// Create, score and sort the initial population
		Log.i("ga", "Creating initial population");
		Generation<X> current = new Generation<X>(0, sizeHint);
		// Use a set to provide some defence against dupes (if the candidate implements equals) 
		Set<X> startCandidates = new HashSet<X>();
		int cnt = 0;
		while(startCandidates.size() < sizeHint) {
			X candidate = breeder.generate();
			if (startCandidates.contains(candidate)) {
				candidate = breeder.mutate(candidate);
			}
			startCandidates.add(candidate);
			// break out of the loop?
			cnt++;
			if (cnt > sizeHint*10) break; // too many dupes -- let's move on
			if (timer.getTime() > timeHintMillisecs) break; // oh well
		}
		List<Candidate<X>> scs = evaluate(startCandidates);
		current.population.addAll(scs);		
		Collections.sort(current.population);
		// save if we are saving
		saveData(current);	
		return current;
	}
	
	/**
	 * Does the actual work of optimizing the given generation according
	 * to the given objective.
	 * @param objective
	 * @param current
	 * @return
	 * @throws ExecutionException 
	 * @throws InterruptedException 
	 */
	private X optimize2(Generation<X> current) throws Exception
	{
		timer = new StopWatch();
		evalStatsTimer = new StopWatch();
		List<X> startCandidates = new ArrayList<X>();
		for (int i = 0; i < sizeHint; i++) {
			X candidate = breeder.generate();			
			startCandidates.add(candidate);
			if (timer.getTime() > timeHintMillisecs) break; // oh well
		}
		List<Candidate<X>> scs = evaluate(startCandidates);
		current.population.addAll(scs);		
		Collections.sort(current.population);
		// save if we are saving
		saveData(current);
		// Evolve it for a bit..
		// ...until we meet suitable stop criteria		
//		int statisCount = 0;
		prevBest = Double.NEGATIVE_INFINITY;
		statisCount = 0;
		while ( ! converged(current)) 
		{
			// report
			report(current);
			
			// Create a new generation - Keeping the population sorted by fitness			
			current = getNext(current);

			// save if we are saving
			saveData(current);			
		}
		return current.population.get(0).candidate;
	}

	protected boolean converged(Generation<X> current) {
		if (current.generation >= maxGenerations) {
			Log.i("ga", "Stopping GA - reached max generation "+maxGenerations);
			return true;
		}
		if (timer.getTime() > timeHintMillisecs) {
			Log.i("ga", "Stoppping GA - reached time-out "+timeHintMillisecs);
			return true;
		}
		// stop if the score does not noticeably improve in k generations
		double bestScore = current.getBestScore();
		if (bestScore > prevBest*1.0001) {
			prevBest = bestScore;
			statisCount = 0;				
			return false;
		}
		statisCount++;
		if (statisCount >= statisLimit) {
			// give up
			Log.i("ga", "Stopping GA - reached statis");
			return true;
		}			
		return false;
	}

	/**
	 * @return the average time taken to evaluate a candidate solution.
	 */
	public Dt getEvaluationTime() {
		assert evalCount.intValue() > 0;
		long ms = evalStatsTimer.getTime() / evalCount.intValue();
		TUnit unit = TimeUtils.pickTUnit(ms);
		return new Dt(ms, TUnit.MILLISECOND).convertTo(unit);
	}
	
	
	private List<Candidate<X>> evaluate(Collection<X> candidates) throws Exception {
		evalStatsTimer.start();
		// Multi-threaded evaluation
		List<Future<Candidate>> futures = new ArrayList<Future<Candidate>>();
		for(final X c : candidates) {
			ATask<Candidate> callable = new ATask<Candidate>() {
				@Override
				protected Candidate run() throws Exception {
					double score = evaluate2(c);
					Candidate candidate = new Candidate(c, score);
					return candidate;					
				}
			};			
			Future<Candidate> f = exec.submit(callable);
			futures.add(f);
		}
		// collect up the scores
		List<Candidate<X>> scoredCandidates = new ArrayList<Candidate<X>>(candidates.size());
		Exception err = null;
		for (Future<Candidate> f : futures) {
			try {
				scoredCandidates.add(f.get());
			} catch(Exception ex) {
				Log.e("ga", ex);
				err = ex;
			}
		}
		// Did everything fail?
		if (scoredCandidates.isEmpty() && err!=null) throw err;
		evalStatsTimer.pause();
		return scoredCandidates;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName()+"["
				+"pop:"+sizeHint
				+" max-gens:"+maxGenerations
				+" time:"+TimeUtils.toString(timeHintMillisecs)+"]";
	}
	
	/**
	 * Wrap evaluate calls so we can count them - and catch any exceptions
	 * @param candidate
	 * @return
	 */
	private double evaluate2(X candidate) {
		try {
			double s = objectiveFn.evaluate(candidate);			
			evalCount.incrementAndGet();
			return s;
			
		} catch (Exception e) {
			handleException(candidate, e);
			return Double.NEGATIVE_INFINITY;
		} catch (AssertionError e) {
			handleException(candidate, e);
			return Double.NEGATIVE_INFINITY;
		}
	}

	private void handleException(X candidate, Throwable e) {
		Log.e("ga", Printer.toString(candidate)+" caused exception "+Printer.toString(e, true));
	}

	/**
	 * Over-ride if you want better reporting.
	 * @param current
	 */
	protected void report(Generation<X> current) {
		Log.i("ga", "Generation "+current.generation+": "+current.getBestScore()+"\n"			
					+"Mean evaluation time: "+ getEvaluationTime()+" ("+evalCount+"/"+evalStatsTimer+")\n"
					+"Best solution: "+current.getBest()+"\n");
	}

	private void saveData(Generation current) throws WrappedException {
		if (intFile == null) return;
		Log.i("ga", "Saving generation "+current.generation);
		// protect the current file until we've updated
		String intFileName = intFile.getAbsolutePath();
		File backup = new File(intFileName + ".old");
		if (intFile.exists()) {
			FileUtils.move(intFile, backup);
			intFile = new File(intFileName);
		}
		// save
		FileUtils.write(intFile, XStreamUtils.serialiseToXml(current));
		// no longer need the backup
		FileUtils.delete(backup);
		Log.d("ga", "...saved");
	}
	
	/**
	 * Create a new generation from an old one by passing through
	 * an elite and breeding and mutating the rest.
	 * Population size is conserved. The resulting generation is sorted.
	 * @param current the current generation. Must be sorted in *descending* order of fitness.
	 * @return the next generation. Must be sorted in descending order of fitness (i.e. best first)
	 * @throws ExecutionException 
	 * @throws InterruptedException 
	 */
	Generation getNext(Generation<X> current) throws Exception {		
		int totalSize = current.population.size();
		Generation<X> nextGen = new Generation<X>(current.generation + 1, totalSize);
		Log.d("ga","Creating generation "+nextGen.generation);
		
		int count = 0;
		
		// 1. Pass through an elite (never zero)
		int numElite = (int) Math.round(elite * current.population.size());
		numElite = Math.max(1, numElite);
		while (count < numElite) {
			nextGen.population.add(current.population.get(count));
			count++;
		}
		assert nextGen.population.size() == numElite;
		
		// 2. Make up the rest from crossover and mutation
		Set<X> offspring = new HashSet<X>();
		while (offspring.size() < totalSize - numElite && count < totalSize*3) {
			Candidate<X> a = select(current);
			Candidate<X> b = select(current);
			X c;
			c = breeder.crossover(a.candidate, b.candidate);
			c = breeder.mutate(c);
			// being a set this avoid duplicates if X has an appropriate equals method
			offspring.add(c);
			// if we keep hitting dupes, then this will quit out eventually
			count++;
		}
		
		// 3. Evaluate the new candidates, inserting into the new generation
		// Multi-threaded, in the hope that this may give speed-ups
		List<Candidate<X>> cs = evaluate(offspring);
		nextGen.population.addAll(cs);
		// sort
		Collections.sort(nextGen.population);
		// TODO add in a diversity bias? go through the population,
		// adjust score based on similarity to higher-rated candidates
		return nextGen;
	}
	
	/**
	 * Select a member of a generation for breeding. Default implementation does
	 * tournament selection with a tournament size of 2.
	 * I.e. it picks the best of two random members. 
	 * Override if you want to change it.
	 */
	Candidate select(Generation<X> gen) {
		Candidate a = Utils.getRandomMember(gen.population);
		Candidate b = Utils.getRandomMember(gen.population);
		return (a.score > b.score) ? a : b;
	}
	
	/**
	 * Sorted set of {@link Candidate}s.
	 * <p>
	 * Note: this is static so it can be saved independently of the GA.
	 */
	public static final class Generation<X> {
		final int generation;
		/**
		 * This should always be sorted best (highest scoring) first
		 */
		final List<Candidate<X>> population;
		
		Generation(int generation, int sizeHint) {
			this.generation = generation;
			this.population = new ArrayList<Candidate<X>>(sizeHint);
		}

		public double getBestScore() {
			return population.get(0).score;
		}

		public X getBest() {
			return population.get(0).candidate;
		}
	}
	/**
	 * A candidate with a score.
	 */
	public static final class Candidate<X> implements Comparable<Candidate<X>> {
		final X candidate;
		final double score;		
		
		Candidate(X candidate, double score) {
			this.candidate = candidate;
			this.score = score;
		}

		/**
		 * Sort Highest scoring first
		 * @see java.lang.Comparable#compareTo(java.lang.Object)
		 */
		@Override
		public int compareTo(Candidate other) {
			return - Double.compare(this.score, other.score);
		}
		
		@Override
		public String toString() {
			return candidate.toString() + " (" + score + ")";
		}
	}
	
}
