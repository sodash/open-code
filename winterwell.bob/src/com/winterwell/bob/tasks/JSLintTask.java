/**
 * 
 */
package com.winterwell.bob.tasks;

import java.io.File;

import com.winterwell.utils.io.FileUtils;

/**
 * @author miles
 *
 */
public class JSLintTask extends ProcessTask {

	/**
	 * @param command
	 */
	public JSLintTask(File file) {
		super(new File(FileUtils.getWinterwellDir(), "code/middleware/jsl/jsl").getAbsolutePath());
		addArg("-process");
		addArg(file.getAbsolutePath());
	}
	
	/**
	 * Error levels:
     *  0 - Success
     *  1 - JavaScript warnings
     *  2 - Usage or configuration error
     *  3 - JavaScript error
     *  4 - File error
	 */
	@Override
	protected boolean processFailed(int code) {
		return code > 2;
	}

}
