package com.winterwell.bob;

import java.io.File;

import com.winterwell.utils.io.FileUtils;
import com.winterwell.utils.log.Log;

/**
 * TODO move more logging here, for better clarity of logging
 * @author daniel
 *
 */
public class BobLog {

	
	public static File getHistoryFile() {
		File bobwarehouse = getSettings().bobwarehouse;
		if ( ! bobwarehouse.exists()) {
			doMakeBobWarehouse();
		}
		File csvfile = new File(bobwarehouse, "bobhistory.csv");
		return csvfile;
	}

	private static void doMakeBobWarehouse() {
		File bobwarehouse = getSettings().bobwarehouse;
		if (bobwarehouse.exists()) return;
		Log.d("Bob", "Make bobwarehouse folder");
		bobwarehouse.mkdirs();
		FileUtils.write(new File(bobwarehouse, "README.md"),
				"# Bob Warehouse\n\nThis folder is auto-generated and managed by Bob. It contains the run history (bobhistory.csv), call-graph info (calls.dot), and git clones of dependency projects.\n\nIt can safely be deleted, although that will make your next bob run rather slow as it will get rebuilt.");
	}

	public static void logDot(String string) {
		File df = getSettings().dotFile;
		if ( ! df.getParentFile().exists()) {
			df.getParentFile().mkdirs();
		}
		
		FileUtils.append(string, df);
	}

	private static BobConfig getSettings() {
		// Dep.get(BobSettings.class) not set in JUnit runs! ??fix that
		return Bob.getSingleton().getConfig();
	}

}
