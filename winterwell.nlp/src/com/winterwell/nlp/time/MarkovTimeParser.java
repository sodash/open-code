package com.winterwell.nlp.time;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.winterwell.utils.time.TimeFragment;

import com.winterwell.maths.hmm.ObjectHMM;
import com.winterwell.maths.matrix.ObjectMatrix;
import com.winterwell.utils.StrUtils;

/**
 * Can learn time formats. This is a HiddenMarkovModel, but the output is
 * done by a method-call rather than a matrix lookup.
 * @author daniel
 * Tested By {@link MarkovTimeParserTest}
 */
public class MarkovTimeParser {

	TimeObjectHMM hmm = new TimeObjectHMM();
	
	public MarkovTimeParser() {
		// TODO Auto-generated constructor stub
	}

	
	public TimeFragment parse(String string) throws ParseException {
		string = toCanonical(string);
		
		String[] tokens = string.split("\\b");
		ArrayList<String> observed = new ArrayList(Arrays.asList(tokens));
		// the regex puts in an empty initial entry
		observed.remove(0);
		List<String> hidden = hmm.viterbi(observed);
		
		System.out.println(hidden);
		TimeFragment tf = new TimeFragment();
		for(int i=0; i<observed.size(); i++) {
			String token = observed.get(i); 
			String pattern = hidden.get(i);
			int field = getPattern2Field(pattern);
			if (field==-1) continue;
			Integer value = getObs2Value(token);
			if (value==null) {
				continue; 
			}
			tf.put(field, value);
		}
		
		return tf;
	}

/**
Canonical format: lowercase, all words shrunk to 3 letters (eg "mon", "jun", not "Monday", "June")
 * @param string
 * @return
 */
	String toCanonical(String string) {
		string = string.toLowerCase();
		string = string.replaceAll("\\b([a-zA-Z]{3})[a-zA-Z]+", "$1");
		return string;
	}


	Integer getObs2Value(String token) {
		Integer v = hmm.obs2value.get(token);
		if (v!=null) return v;
		if (StrUtils.isInteger(token)) {
			return Integer.valueOf(token);
		}
		return null;
	}


	private int getPattern2Field(String pattern) {
		Integer f = hmm.pattern2field.get(pattern);
		return f==null? -1 : f;
	}
}

class TimeObjectHMM extends ObjectHMM<String,String> {
	
	public Map<String,Integer> pattern2field = new HashMap();
	public Map<String,Integer> obs2value= new HashMap();

	public TimeObjectHMM() {
		ObjectMatrix<String, String> emit = getEmissionMatrix();
		
		List<String> hidden = new ArrayList();
		// Day of Week
		hidden.add("E");
		pattern2field.put("E", Calendar.DAY_OF_WEEK);
		String[] days = "sun mon tue wed thu fri sat".split(" ");
		for(int i=0; i<days.length; i++) {
			String day = days[i];
			emit.set(day, "E", 1);
			emit.set(day.substring(0,3), "E", 1);
			obs2value.put(day, i+1);
//			obs2value.put(day.substring(0,3), i+1);
		}
		// Let's assume all words have been shrunk to 3 letters
//		emit.set("tues", "E", 1);
//		obs2value.put("tues", Calendar.TUESDAY);
//		emit.set("thurs", "E", 1);
//		obs2value.put("thur", Calendar.THURSDAY);
//		emit.set("thur", "E", 1);
//		obs2value.put("thur", Calendar.THURSDAY);
		
		// Day of month
		hidden.add("d");
		pattern2field.put("d", Calendar.DAY_OF_MONTH);
		for(int i=1; i<32; i++) {
			emit.set(""+i, "d", 1);
			// also the "5th" version
			String tail = "th";
			if (i==1 || i==21 || i==31) tail = "st";
			else if (i==2 || i==22) tail = "nd";
			else if (i==3 || i==23) tail = "rd";
			emit.set(i+tail, "d", 1);
			obs2value.put(i+tail, i);
		}
		
		// month
		hidden.add("M");
		hidden.add("MM");
		hidden.add("MMM");
		pattern2field.put("M", Calendar.MONTH);
		pattern2field.put("MM", Calendar.MONTH);
		pattern2field.put("MMM", Calendar.MONTH);
		String[] mons = "jan feb mar apr may jun jul aug sep oct nov dec".split(" ");
		for(int i=1; i<=mons.length; i++) {
			addEmission(emit, ""+i, "M", i);
			String mm = i<10? "0"+i : ""+i;
			addEmission(emit, mm, "MM", i);
			addEmission(emit, mons[i-1], "MMM", i);
		}
		
		// Whitespace
		hidden.add("whitespace");
		emit.set(" ", "whitespace", 1);
		emit.set("\t", "whitespace", 1);

		// Punctuation
		
		// everything is equal
		ObjectMatrix<String, String> trans = getTransitionMatrix();
		for(String h : hidden) {
			for (String h2 : hidden) {
				trans.set(h, h2, 1);
			}
		}
		setHiddenStates(hidden);
	}

	private void addEmission(ObjectMatrix<String, String> emit, String obs, String pattern, int value) {
		emit.set(obs, pattern, 1);
		obs2value.put(obs, value);
	}
	
}