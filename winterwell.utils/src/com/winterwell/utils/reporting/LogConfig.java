package winterwell.utils.reporting;

import java.util.List;

import com.winterwell.utils.io.Option;

class LogConfig {

	@Option
	List<String> ignoretags;
	
	@Option
	List<String> verbosetags;
	
}
