///*
// * (c) Winterwell though written by Aethys
// * December 2008
// */
//package com.winterwell.nlp.similarity;
//
//import java.io.Serializable;
//import java.util.HashMap;
//import java.util.Iterator;
//import java.util.Map;
//import java.util.Map.Entry;
//import java.util.Set;
//
//import com.winterwell.maths.stats.distributions.discrete.AFiniteDistribution;
//import com.winterwell.nlp.Trie;
//import com.winterwell.nlp.io.ITokenStream;
//import com.winterwell.nlp.io.Tkn;
//import com.winterwell.utils.BestOne;
//
///**
// * Frequency index based on hashmap.
// * 
// * TODO compare speed of HashMap against {@link Trie}
// * 
// * @Deprecated // use ObjectDistribution or IndexedDistribution
// * @author Daniel Winterstein, Tiphaine Dalmas
// */
//@Deprecated
//// use ObjectDistribution
//public class FrequencyIndex extends AFiniteDistribution<String> implements
//		Serializable {
//	private static final long serialVersionUID = 1L;
//
//	private final HashMap<String, Double> frequencies = new HashMap<String, Double>();
//	private double totalCount;
//
//	public FrequencyIndex() {
//		this.totalCount = 0;
//	}
//
//	/**
//	 * Copy constructor
//	 */
//	public FrequencyIndex(FrequencyIndex other) {
//		this();
//		merge(other);
//	}
//
//	/**
//	 * Constructor that takes a stream of tokens and adds them all.
//	 * 
//	 * @param input
//	 */
//	public FrequencyIndex(ITokenStream input) {
//		this();
//		add(input);
//	}
//
//	/**
//	 * Constructor that takes a pre-computed map of frequencies, such as one
//	 * cached in a file.
//	 * 
//	 * @param freqs
//	 */
//	public FrequencyIndex(Map<String, Double> freqs) {
//		this();
//		merge(freqs);
//	}
//
//	/**
//	 * Add the text of every token in a stream. Ignores token properties.
//	 * 
//	 * @param input
//	 */
//	public void add(ITokenStream input) {
//		for (Tkn token : input) {
//			add(token.toString());
//		}
//	}
//
//	/**
//	 * Add an instance of a term.
//	 * 
//	 * @return ignore this (for compatibility with Collection only - do not use)
//	 */
//	@Override
//	public final boolean add(String term) {
//		add(term, 1.0);
//		return true;
//	}
//
//	/**
//	 * Add count instances of a term
//	 */
//	private final void add(String term, double count) {
//		this.totalCount += count;
//		Double fr = this.frequencies.get(term);
//		if (fr == null) {
//			fr = count;
//		} else {
//			fr += count;
//		}
//		this.frequencies.put(term, fr);
//	}
//
//	/**
//	 * Return an indicator of textual diversity.
//	 * <p>
//	 * Diversity is computed as the number of unique terms divided by the total
//	 * number of terms.
//	 */
//	public double diversity() {
//		return this.totalCount > 0 ? this.frequencies.size() / this.totalCount
//				: 0.0;
//	}
//
//	/**
//	 * @return the map behind this frequency index
//	 */
//	public HashMap<String, Double> getBackingMap() {
//		return frequencies;
//	}
//
//	@Override
//	public String getMostLikely() {
//		BestOne<String> b = new BestOne<String>();
//		for (String w : this) {
//			b.maybeSet(w, tf(w));
//		}
//		return b.getBest();
//	}
//
//	/**
//	 * TODO rename - this is a bit confusing
//	 * 
//	 * @return the number of terms we have seen. That is, training on "monkey",
//	 *         "monkey", "monkey" would give 3
//	 */
//	public final double getTotalCount() {
//		return this.totalCount;
//	}
//
//	@Override
//	public Iterator<String> iterator() {
//		return frequencies.keySet().iterator();
//	}
//
//	/**
//	 * Merge another frequency index into this one
//	 */
//	public void merge(FrequencyIndex other) {
//		merge(other.frequencies);
//	}
//
//	/**
//	 * Merge in a dictionary of terms and their frequencies
//	 */
//	public void merge(Map<String, Double> frequencies) {
//		for (Entry<String, Double> entry : frequencies.entrySet()) {
//			add(entry.getKey(), entry.getValue());
//		}
//	}
//
//	/**
//	 * Identical to true-frequency {@link #tf(String)}.
//	 */
//	@Override
//	public double prob(String x) {
//		return tf(x);
//	}
//
//	/**
//	 * Absolute frequency of the term.
//	 * 
//	 * @param term
//	 */
//	public double rawFrequency(String term) {
//		Double fr = this.frequencies.get(term);
//		if (fr == null)
//			return 0.0;
//		else
//			return fr;
//	}
//
//	/**
//	 * @return the number of terms in this index. Equivalent to {@link #terms()}
//	 *         .size() but a bit more efficient.
//	 */
//	@Override
//	public int size() {
//		return frequencies.size();
//	}
//
//	/**
//	 * Returns a Set&lt;String> containing all the terms in the index. Do not
//	 * edit.
//	 * 
//	 * @return
//	 */
//	public Set<String> terms() {
//		return this.frequencies.keySet();
//	}
//
//	/**
//	 * Relative, percentage frequency of the term.
//	 * 
//	 * @param term
//	 * @return zero if totalCount is zero. Can be zero anyway if the term is
//	 *         unknown.
//	 */
//	public double tf(String term) {
//		if (this.totalCount == 0)
//			return 0.0;
//		else
//			return rawFrequency(term) / this.totalCount;
//	}
//
//	/**
//	 * Term frequency based on log. Uses 1+rawFrequency to avoid divide-by-zero
//	 * 
//	 * @param term
//	 */
//	public double tflog(String term) {
//		double fr = rawFrequency(term);
//		if (fr == 0.0)
//			return 0.0;
//		else
//			return Math.log(1 + rawFrequency(term));
//	}
//
//	/**
//	 * total count | diversity | word-freqs for first dozen words
//	 */
//	@Override
//	public String toString() {
//		StringBuilder sb = new StringBuilder();
//		sb.append("{@FrequencyIndex ");
//		sb.append(this.totalCount);
//		sb.append(" | ");
//		sb.append(diversity());
//		sb.append(" | ");
//		int cnt = 0;
//		for (String s : this) {
//			sb.append(s);
//			sb.append(" ");
//			sb.append(rawFrequency(s));
//			sb.append(" ");
//			sb.append(tf(s));
//			sb.append("; ");
//			// only print the first dozen
//			cnt++;
//			if (cnt == 12) {
//				sb.append("...");
//				break;
//			}
//		}
//		sb.append("}");
//		return sb.toString();
//	}
//
//}
