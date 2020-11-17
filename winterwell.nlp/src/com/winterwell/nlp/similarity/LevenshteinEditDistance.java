package com.winterwell.nlp.similarity;

import java.util.ArrayList;
import java.util.List;

/**
 * Damerau-Levenshtein edit distance, as copied from Wikipedia
 * 
 * @author daniel
 * @testedby  LevenshteinEditDistanceTest}
 */
public class LevenshteinEditDistance implements ICompareWords {

	protected int DELETE_COST = 1;
	protected int INSERT_COST = 1;
	protected int TRANSPOSE_COST = 1;

	public LevenshteinEditDistance() {		
	}

	/**
	 * @param ai
	 * @param bj
	 * @return the cost of an ai/bj switch. 
	 */
	protected int cost(Object ai, Object bj) {
		if (ai.equals(bj))
			return 0;
		return UNIT;
	}
	
	/**
	 * Also set {@link #DELETE_COST} {@link #INSERT_COST} {@link #TRANSPOSE_COST}! 
	 */
	protected int UNIT = 1;

	public double editDistance(List a, List b) {
		if(a.isEmpty()) return b.size();
		if(b.isEmpty()) return a.size();
		
		// d is a table with lenStr1+1 rows and lenStr2+1 columns
		// TODO only need to keep a few rows, so could save space
		// (would be important for DNA matching, probably irrelevant for normal
		// words)
		int d[][] = new int[a.size() + 1][b.size() + 1];
		// i and j are used to iterate over str1 and str2
		// for loop is inclusive, need table 1 row/column larger than string
		// length.
		for (int i = 0; i <= a.size(); i++) {
			d[i][0] = i*UNIT;
		}
		for (int j = 1; j <= b.size(); j++) {
			d[0][j] = j*UNIT;
		}
		// NB: Ported from public-source code which assumed string indices start at 1, not 0.
		for (int i = 1; i <= a.size(); i++) {
			for (int j = 1; j <= b.size(); j++) {
				Object ai = a.get(i-1);
				Object bj = b.get(j-1);				
				int dij = Math.min( d[i - 1][j] + DELETE_COST, // deletion
									d[i][j - 1] + INSERT_COST); // insertion
				int cost = ai.equals(bj)? 0 : cost(ai, bj);
				dij = Math.min(dij, d[i - 1][j - 1] + cost); // substitution
				
				// Damerau: transposition
				if (i > 1 && j > 1 && ai.equals(b.get(j - 2))
						&& a.get(i - 2).equals(bj)) 
				{
					dij = Math.min(dij, d[i - 2][j - 2] + TRANSPOSE_COST); // transposition
				}
				d[i][j] = dij;
			}
		}
		return d[a.size()][b.size()] / ((double)UNIT);
	}

	public double editDistance(String a, String b) {
		return editDistance(list(a), list(b));
	}
	
	protected List list(String a) {
		return list2(a);
	}

	private List<Character> list2(String a) {
		List list = new ArrayList(a.length());
		for (int i = 0; i < a.length(); i++) {
			list.add(a.charAt(i));
		}
		return list;
	}

	@Override
	public double similarity(String a, String b) {
		List la = list(a);
		List lb = list(b);
		double edits = editDistance(la, lb);
		// normalise to be in [0,1]
		double max = Math.max(a.length(), b.length());
		return (max - edits) / max;
	}
	

}
