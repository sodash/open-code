
package com.winterwell.datalog;

import com.winterwell.bob.tasks.WinterwellProjectFinder;
import com.winterwell.bob.wwjobs.BuildWinterwellProject;

/**
 * Hm: if this is called BuildX, then Bob will not be able to choose between this and BuildDataLog.
 * So renamed to MakeX
 * @author daniel
 *
 */
public class MakeCompressor extends BuildWinterwellProject {

	public static void main(String[] args) throws Exception {
		MakeCompressor b = new MakeCompressor();
		b.doTask();
	}
	
	public MakeCompressor() {
		super(new WinterwellProjectFinder().apply("winterwell.datalog"), "datalog.compressor");
		setMainClass("com.winterwell.datalog.server.CompressDataLogIndexMain");
	}	

}
