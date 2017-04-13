package com.winterwell.maths.timeseries;


import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.winterwell.json.JSONObject;
import com.winterwell.utils.containers.ArrayMap;
import com.winterwell.utils.log.Log;
import com.winterwell.utils.time.Period;
import com.winterwell.utils.time.TUnit;
import com.winterwell.utils.time.Time;
import com.winterwell.utils.time.TimeUtils;
import com.winterwell.utils.web.IHasJson;

/**
 * Training proceeds both historically (through the database, oldest first)
 * And with live stuff, as it comes in.
 * 
 * @author daniel
 *
 */
public class TrainingInfo implements Serializable, IHasJson {	
	
	public TrainingInfo() {	
		coverage = new TimeCoverage(slicer());
	}
		
	volatile private boolean live = true;
	
	public boolean isLive() {
		return live;
	}	
	
	/**
	 * Kill the MODEL.
	 * @WARNING Use with caution!
	 * This means the associated model/classifier is dead & must not be used anymore! 
	 */
	public void kill() {
		Log.i("ai.life", "Killed "+this);
		live = false;		
	}
	
	private static final long serialVersionUID = 1L;
	public final Time wentLive = new Time();
	
	volatile int cntLive;
	volatile int cntHistorical;
	
	/**
	 * Track per-language training.
	 * ISO 2-letter lang-code to training count.
	 */
	Map<String,Integer> cntLang = Collections.synchronizedMap(new HashMap());	
	
	@Override
	public String toString() {
		return (isLive()?"":"DEAD")+"TrainingInfo[wentLive="+ wentLive 
				+ ", cntLive=" + cntLive 
				+ ", cntHistorical="+ cntHistorical + "]";
	}
	
	public int getTrainingCntLive() {
		return cntLive;
	}
	
	public int getTrainingCntHistorical() {
		return cntHistorical;
	}
	
	/**
	 * @return total training count, live + historical
	 */
	public int getTrainingCnt() {
		return cntLive + cntHistorical;
	}

	/**
	 * @param langCode
	 * @return How much training in this language?
	 */
	public int getTrainingCntLang(String langCode) {
		// HACK update old objects
		if (cntLang==null) cntLang = Collections.synchronizedMap(new HashMap());
		
		Integer v = cntLang.get(langCode);
		return v==null? 0 : v;
	}

	/**
	 * Taking calendar months from Jan 2012, ie. Jan 2012 = 0
	 */
	Map<Integer,TrainingInfo2> month2info = new HashMap();

	private final TimeCoverage coverage;
	
	public TimeCoverage getCoverage() {
		return coverage;
	}
	
	/**
	 * @return [distant-past upto start-of-day(wentLive+1)]
	 */
	private ITimeGrid slicer() {
		// include today in the "past" which will get trained on
		Time sod = TimeUtils.getStartOfDay(wentLive.plus(TUnit.DAY));
		assert sod.isAfterOrEqualTo(wentLive) : sod+" "+wentLive;
		SimpleTimeGrid stg = new SimpleTimeGrid(sod, TUnit.DAY);
		stg.setLimit(new Period(sod.minus(3, TUnit.YEAR), sod));
		return stg;
	}

	
	private TrainingInfo2 getTI2(Time t) {
//		assert period.length(TUnit.MILLISECOND) < TUnit.MONTH.getMillisecs()*1.5 : period;
//		Time t = period.first;
		// too far future? Given that the slicer will make all those buckets
		if (t.isAfter(new Time().plus(2, TUnit.YEAR))) {
			throw new IllegalArgumentException(t+" in "+this);
		}
		
		TrainingInfo2 ti2 = getTrainingInfo2(t);
		return ti2;
	}
	private TrainingInfo2 getTrainingInfo2(Time t) {
		int bi = slicer().getBucket(t);
		TrainingInfo2 ti2 = month2info.get(bi);
		if (ti2==null) {
			ti2 = new TrainingInfo2();
			month2info.put(bi, ti2);
		}
		return ti2;
	}


	public void update(boolean online, Time trainingEvent) {		
		if (online) {			
			cntLive++;
		} else {
			cntHistorical++;
		}
		TrainingInfo2 ti2 = getTI2(trainingEvent);
		ti2.cnt ++;
	}

	
	@Override
	public String toJSONString() {
		return new JSONObject(toJson2()).toString();
	}

	@Override
	public Map toJson2() throws UnsupportedOperationException {
		return new ArrayMap(
				"wentLive", wentLive,
				"cnt", getTrainingCnt(),
				"coverage", getCoverage().getProgress()
		);
	}

	@Override
	public void appendJson(StringBuilder sb) {
		sb.append(toJSONString());		
	}
}

class TrainingInfo2 implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	@Override
	public String toString() {
		return "TI2[cnt=" + cnt+"]";
	}

//	/**
//	 * For debugging
//	 */
//	Set<XId> seen = new HashSet();
	
	public int cnt;
//	ConfusionMatrix<String> confMatrix;

}
