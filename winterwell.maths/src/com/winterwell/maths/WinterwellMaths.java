package com.winterwell.maths;

import com.winterwell.maths.matrix.XStreamDenseMatrixConverter;
import com.winterwell.utils.web.XStreamUtils;

/**
 * This class exists purely as a convenient place for some odd code.
 * 
 * @author daniel
 * 
 */
public class WinterwellMaths {

	private static boolean initFlag;

	private static void doConfigureXStream() {
		XStreamUtils.xstream().registerConverter(
				new XStreamDenseMatrixConverter());
	}

	/**
	 * This should ALWAYS be called when using winterwell.maths Make sure
	 * XStream has been poked and so on.
	 */
	public static void init() {
		if (initFlag)
			return;
		doConfigureXStream();
	}

}
