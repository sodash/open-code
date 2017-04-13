/**
 * 
 */
package com.winterwell.nlp.io;

import junit.framework.TestCase;

/**
 * @author Joe Halliwell <joe@winterwell.com>
 * 
 */
public class UniqueTokenStreamTest extends TestCase {

	public void testBasicUsage() {
		ITokenStream ts = new UniqueTokenStream(new WordAndPunctuationTokeniser());
		ATSTestUtils.assertTokenisation(ts.factory("hello john. hello mary"),
				"hello john . mary");
	}

}
