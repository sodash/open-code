package com.winterwell.utils.log;

import java.util.List;

import com.winterwell.utils.io.Option;

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
	
	@Option(description="reports to downgrade from error or warning to just info.")
	List<String> downgrade;
}
