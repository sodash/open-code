package com.winterwell.nlp;

import com.winterwell.maths.WinterwellMaths;

public class WinterwellNLP {

	private static boolean initFlag;

	/**
	 * This should ALWAYS be called when using winterwell.nlp Make sure XStream
	 * has been poked and so on.
	 */
	public static void init() {
		if (initFlag)
			return;
		// init the dependencies
		WinterwellMaths.init();
	}

}
