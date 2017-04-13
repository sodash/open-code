package com.winterwell.nlp;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.winterwell.utils.containers.ArrayMap;
import com.winterwell.utils.containers.Containers;

/**
 * Guess the tense of a sentence: past, present or future.
 * <p>
 * This is just a very crude keyword based hack for now. TODO get some training
 * data and so something better.
 * 
 * @author daniel
 * 
 */
public class Tense {

	public enum KTense {
		CONDITIONAL, FUTURE, PAST, PRESENT
	}

	static String[] cond = new String[] { "would", "should", "could" };
	static String[] future = new String[] { "will", "going", "would", "can",
			"shall", "pledge", "promise" };
	static String[] past = new String[] { "have", "did", "had", "was", "were",
			"ed ", "went" };

	/**
	 * Count usage of fragments in text
	 * 
	 * @param fragments
	 * @param text
	 * @return
	 */
	private int count(String[] fragments, String text) {
		int cnt = 0;
		for (String s : fragments) {
			int from = 0;
			while (true) {
				from = text.indexOf(s, from);
				if (from == -1) {
					break;
				}
				from += s.length();
				cnt++;
			}
		}
		return cnt;
	}

	public KTense guessTense(String text) {
		int pastCounts = count(past, text);
		int futureCounts = count(future, text);
		int condCounts = count(cond, text);

		if (pastCounts == 0 && futureCounts == 0 && condCounts == 0)
			return KTense.PRESENT;

		Map<KTense, Integer> map = new ArrayMap<KTense, Integer>(KTense.PAST,
				pastCounts, KTense.FUTURE, futureCounts, KTense.CONDITIONAL,
				condCounts);
		List<KTense> keys = Containers.getSortedKeys(map);
		Collections.reverse(keys);
		KTense tense = keys.get(0);

		return tense;
	}

}
