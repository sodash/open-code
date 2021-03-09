package com.winterwell.utils.log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.winterwell.datalog.Rate;
import com.winterwell.utils.containers.ArrayMap;
import com.winterwell.utils.io.Option;
import com.winterwell.utils.time.Dt;
import com.winterwell.utils.time.TUnit;
import com.winterwell.utils.time.Time;

/**
 * Typically loaded from config/log.properties
 * @author daniel
 *
 */
public class LogConfig {

	@Option
	List<String> ignoretags;
	
	public LogConfig addIgnoreTag(String tag) {
		assert tag != null;
		if (ignoretags==null) ignoretags = new ArrayList();
		ignoretags.add(tag);
		return this;
	}
	
	@Option
	public List<String> verbosetags; 
	
	@Option(description="reports to filter out.")
	public List<String> exclude;
	
	@Option(description="reports to downgrade from error or warning to just info. Uses case-sensitive keyword matching.")
	List<String> downgrade;
	
	@Option(description="Window to apply throttling (ie silently skipping reports). The first throttled report does generate a 'throttling' log message.")
	public Dt throttleWindow = TUnit.MINUTE.dt;
	
	@Option(description="How much is too much? Can be null for unlimited")
	public Rate throttleAt = new Rate(1000, TUnit.MINUTE);
	
	public Rate getThrottleAt(String tag) {
		if (throttleAtForTag!=null) {
			Number taft = throttleAtForTag.get(tag);
			if (taft!=null) return new Rate(taft.doubleValue(), throttleWindow);
		}
		return throttleAt;
	}
	
	@Option
	Map<String,Number> throttleAtForTag = new ArrayMap<>(
		// let's make Time parsing less noisy
		Time.LOGTAG, 2	
	);	
	
	public void setThrottleAtForTag(String tag, Rate tagThrottleAt) {
		if (throttleAtForTag == null) throttleAtForTag = new HashMap();
		double n = tagThrottleAt.per(throttleWindow);
		throttleAtForTag.put(tag, n);
	}
	
	@Option(description="How big can an individual log file get? e.g. 1gb or 100mb. Setting this does have a minor performance hit. The first overflow report will generate a 'file too big' log message.")
	public String fileMaxSize;
	
	/**
	 * TODO How can we make this configurable at the LogListener level? So you could have e.g. a 1 week audit log, and a sampled log for feeding Kibana?? 
	 */
	@Option(description="[0,1] What fraction of log messages to keep. If set, this leads to a sampling approach -- stochastically dropping messages, which reduces log size. E.g. keep=0.1 is a good way to handle giant logs, provided a full audit trail is not needed.")
	public double keep = 1;
	
	@Option
	public Dt fileInterval;
	
	@Option
	public Integer fileHistory;
}
