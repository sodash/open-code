///*
// * (c) Winterwell
// * December 2008
// */
//package com.winterwell.nlp.similarity;
//
//import java.io.BufferedReader;
//import java.io.IOException;
//import java.util.HashMap;
//import java.util.HashSet;
//import java.util.Iterator;
//import java.util.Map;
//import java.util.Set;
//
//import no.uib.cipr.matrix.VectorEntry;
//import no.uib.cipr.matrix.sparse.SparseVector;
//import com.winterwell.nlp.corpus.IDocument;
//import com.winterwell.nlp.io.ITokenStream;
//import com.winterwell.nlp.io.Tkn;
//import com.winterwell.nlp.io.UniqueTokenStream;
//import com.winterwell.utils.WrappedException;
//import com.winterwell.utils.containers.Containers;
//import com.winterwell.utils.io.FileUtils;
//
///**
// * Cosine-based similarity measure.
// * <p>
// * Computes the cosine between two e-mails represented by vectors of stemmed
// * terms. Each term is weighted with TF-IDF based on training data.
// * 
// * @author Daniel Winterstein
// * @author Tiphaine Dalmas
// * @author Joe Halliwell <joe@winterwell.com>
// */
//public class TFIDFCosineSimilarity implements ICompareText {
//
//	private double corpusSize = 0;
//	private boolean debug = false;
//	FrequencyIndex docFreqIndex;
//
//	private ITokenStream tokenizer;
//
//	/**
//	 * Read in a precomputed frequency index specified in a Unixy format: The
//	 * first line contains the number of documents. Thereafter, one word per
//	 * line, in the format [word][space][occurrences of word] This can take
//	 * quite a lot of time. You should probably cache the output.
//	 * 
//	 * @param buf
//	 *            Probably a file. Will be closed after reading.
//	 * @param tokenizer
//	 *            This does not need to match the frequency index
//	 * @throws IOException
//	 */
//	public TFIDFCosineSimilarity(BufferedReader buf, ITokenStream tokenizer) {
//		this.tokenizer = tokenizer;
//		try {
//			this.corpusSize = Double.parseDouble(buf.readLine());
//			String l;
//			Map<String, Double> freqs = new HashMap<String, Double>();
//			while ((l = buf.readLine()) != null) {
//				String[] bits = l.split(" ");
//				String word = bits[0];
//				double cnt = Double.parseDouble(bits[1]);
//				// run the word through the tokenizer to get the processed form
//				ITokenStream processed = tokenizer.factory(word);
//				for (Tkn token : processed) {
//					Containers.plus(freqs, token.getText(), cnt);
//				}
//			}
//			docFreqIndex = new FrequencyIndex(freqs);
//		} catch (IOException e) {
//			throw new WrappedException(e);
//		} finally {
//			FileUtils.close(buf);
//		}
//	}
//
//	/**
//	 * Constructor, taking a corpus of documents, and an object which will
//	 * tokenize the corpus.
//	 * 
//	 * @param corpus
//	 * @param tokenizer
//	 */
//	public TFIDFCosineSimilarity(Iterable<IDocument> corpus,
//			ITokenStream tokenizer) {
//		this.corpusSize = 0;
//		this.tokenizer = tokenizer;
//		this.docFreqIndex = new FrequencyIndex();
//		UniqueTokenStream unique = new UniqueTokenStream(tokenizer);
//		for (IDocument doc : corpus) {
//			docFreqIndex.add(unique.factory(doc.getContents()));
//			this.corpusSize++;
//		}
//	}
//
//	/**
//	 * Copy constructor
//	 * 
//	 * @param other
//	 */
//	public TFIDFCosineSimilarity(TFIDFCosineSimilarity other) {
//		this.corpusSize = other.corpusSize;
//		this.docFreqIndex = new FrequencyIndex(other.docFreqIndex);
//		this.tokenizer = other.tokenizer;
//		this.debug = other.debug;
//	}
//
//	/**
//	 * Return the number of documents used in the construction of this measure
//	 */
//	public double corpusSize() {
//		return corpusSize;
//	}
//
//	/**
//	 * Return the FrequencyIndex used for computing similarities. This is the
//	 * frequency with respect to documents (not individual occurrences). I.e. if
//	 * the corpus contains the doc "monkeys monkeys monkeys", this will update
//	 * the monkeys count by 1. Intended for debugging.
//	 * 
//	 * @return
//	 */
//	public FrequencyIndex getFrequencyIndex() {
//		return docFreqIndex;
//	}
//
//	FrequencyIndex getFrequencyIndex(String a) {
//		ITokenStream stream = tokenizer.factory(a);
//		FrequencyIndex freq = new FrequencyIndex();
//		for (Tkn token : stream) {
//			freq.add(token.getText());
//		}
//		return freq;
//	}
//
//	double getSimilarity(FrequencyIndex lia, FrequencyIndex lib) {
//		Set<String> terms = new HashSet<String>();
//		terms.addAll(lia.terms());
//		terms.addAll(lib.terms());
//
//		SparseVector va = new SparseVector(terms.size());
//		SparseVector vb = new SparseVector(terms.size());
//		int i = 0;
//		for (Iterator<String> iter = terms.iterator(); iter.hasNext(); i++) {
//			String term = iter.next();
//			double idf = idf(term);
//			// if the corpus is empty, weights will correspond to TF only
//			double vta = lia.tf(term) * idf;
//			double vtb = lib.tf(term) * idf;
//			va.set(i, vta);
//			vb.set(i, vtb);
//			if (debug) {
//				System.out.println(term + " idf " + idf);
//				System.out.println(term + "\t" + lia.tf(term) + "\t"
//						+ lib.tf(term));
//				System.out.println(term + "\t" + idf * lia.tf(term) + "\t"
//						+ idf * lib.tf(term));
//			}
//		}
//		va.compact();
//		vb.compact();
//		// double norms = va.norm(Norm.Two) * vb.norm(Norm.Two);
//		double norms = Math.sqrt(myNorm2(va) * myNorm2(vb));
//		Double cos = 0.0;
//		if (norms > 0.0) {
//			cos = va.dot(vb) / norms;
//		}
//		return cos;
//	}
//
//	/**
//	 * The inverse document frequency, using the safe formula:
//	 * <p>
//	 * <code>IDF(term) = log (countDocuments +1) / (countDocuments(term) + 1)</code>
//	 * <p>
//	 * The higher IDF the fewer documents the term covers. A very high-coverage
//	 * term (low idf) is likely to be a stop word or a query word. A low
//	 * coverage (high idf) term is more likely to be discriminating.
//	 * <p>
//	 * <tt>IDF(term) = log (50+1 / 50+1) = 0.00</tt><br/>
//	 * <tt>IDF(term) = log (50+1 / 40+1) = 0.21</tt><br/>
//	 * <tt>IDF(term) = log (50+1 / 25+1) = 0.67</tt><br/>
//	 * <tt>IDF(term) = log (50+1 / 0+1) = 3.93</tt><br/>
//	 * <p>
//	 * If the corpus is empty, the value returned is 1.
//	 */
//	private double idf(String term) {
//		double tn = docFreqIndex.rawFrequency(term);
//		double idf = (1 + corpusSize) / (1 + tn);
//		assert idf >= 0 : idf;
//		return Math.log(idf);
//	}
//
//	/**
//	 * Merge another TFIDF measure with this one
//	 */
//	public void merge(TFIDFCosineSimilarity other) {
//		this.corpusSize += other.corpusSize;
//		this.docFreqIndex.merge(other.docFreqIndex);
//	}
//
//	/**
//	 * Returns the square of the norm of a sparse vector. Local fix for MTJ
//	 * rounding issue. For some reason, the MTJ library has a rounding problem
//	 * when computing the norm of vectors.
//	 * 
//	 * @param vector
//	 */
//	private double myNorm2(SparseVector vector) {
//		double norm = 0.0;
//		for (Iterator<VectorEntry> iter = vector.iterator(); iter.hasNext();) {
//			VectorEntry ve = iter.next();
//			norm += Math.pow(ve.get(), 2);
//		}
//		return norm;
//	}
//
//	/**
//	 * Return a similarity score between two strings. The score is a double
//	 * between 0 and 1 where 1 is a perfect similarity match.
//	 * <p>
//	 * This is a facility to test similarity on strings. Note that no caching is
//	 * used here.
//	 * 
//	 * @param a
//	 * @param b
//	 */
//	@Override
//	public double similarity(String a, String b) {
//		if (debug) {
//			System.out.println("Comparing '" + a + "' and '" + b + "'");
//			System.out.println("Corpus size is " + corpusSize);
//		}
//		FrequencyIndex lia = getFrequencyIndex(a);
//		FrequencyIndex lib = getFrequencyIndex(b);
//		return getSimilarity(lia, lib);
//	}
//}
