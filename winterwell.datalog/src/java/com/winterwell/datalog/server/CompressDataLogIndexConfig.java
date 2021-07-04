package com.winterwell.datalog.server;

import com.winterwell.datalog.DataLogConfig;
import com.winterwell.utils.io.Option;

public class CompressDataLogIndexConfig extends DataLogConfig {

	@Option(description="The new index to output into. Normally unset, defaults to {source index}_compressed")
	public String destIndex;
	
	@Option(description="Normally unset. Set to switch off the alias swap, which would normally remove the old data from datalog.{dataspace}.all and swap in the compressed data.")
	public boolean noAliasSwap;

	@Option(description="Normally unset. A filter to only process some data. HACK setting `-user:/.+@trk/`")
	public String filter;
	
}
