package com.winterwell.nlp.similarity;

/**
 * Simplest sane implementation of ICompareText. Two strings have similarity 1
 * if they're equal, 0 otherwise.
 * 
 * @author miles
 * 
 */
public class KroneckerDelta implements ICompareText {

	@Override
	public double similarity(String a, String b) {
		if (a.equals(b))
			return 1;
		else
			return 0;
	}

}
