package com.winterwell.utils.log;

import java.util.List;

import com.winterwell.datalog.Rate;
import com.winterwell.utils.io.Option;
import com.winterwell.utils.time.Dt;
import com.winterwell.utils.time.TUnit;

/**
 * Typically loaded from config/log.properties
 * @author daniel
 *
 */
class LogConfig {

	@Option
	List<String> ignoretags;
	
	@Option
	List<String> verbosetags; 
	
	@Option(description="reports to filter out.")
	List<String> exclude;
	
	@Option(description="reports to downgrade from error or warning to just info. Uses case-sensitive keyword matching.")
	List<String> downgrade;
	
	@Option(description="Window to apply throttling (ie silently skipping reports). The first throttled report does generate a 'throttling' log message.")
	Dt throttleWindow = TUnit.MINUTE.dt;
	
	@Option(description="How much is too much? Can be null for unlimited")
	Rate throttleAt = new Rate(1000, TUnit.MINUTE);
	
	@Option(description="How big can an individual log file get? e.g. 1gb or 100mb. Setting this does have a minor performance hit. The first overflow report will generate a 'file too big' log message.")
	String fileMaxSize;
	
	@Option
	Dt fileInterval;
	
	@Option
	Integer fileHistory;
}
