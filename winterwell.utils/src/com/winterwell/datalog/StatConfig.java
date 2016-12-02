package com.winterwell.datalog;

import com.winterwell.utils.io.DBUtils.DBOptions;
import com.winterwell.utils.io.Option;
import com.winterwell.utils.time.Dt;
import com.winterwell.utils.time.TUnit;

/**
 * This might include DB connection options -- but it does not have to, provided those are set
	 * elsewhere.
	 * 
 * @author daniel
 *         <p>
 *         <b>Copyright & license</b>: (c) Winterwell Associates Ltd, all rights
 *         reserved. This class is NOT formally a part of the com.winterwell.utils
 *         library. In particular, licenses for the com.winterwell.utils library do
 *         not apply to this file.
 */
public class StatConfig extends DBOptions {

	/**
	 * Bucket size. Also the gap between saves.
	 */
	@Option(description = "Bucket size. Also the gap between saves.")
	public Dt interval = new Dt(15, TUnit.MINUTE);

	@Option(description = "namespace: if set, use a separate namespace (to avoid race-condition overwriting of stats with another JVM).")
	public String namespace;

	public Dt filePeriod = TUnit.DAY.dt;
	
	@Option
	public Class storageClass;

}
