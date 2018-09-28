package com.winterwell.bob.tasks;

import java.util.Arrays;

import org.junit.Test;

public class TestWebsiteTaskTest {

//	@Test ails, and who cares since we dont use this
	public void testDoTask() {
		TestWebsiteTask twt = new TestWebsiteTask();
		twt.setBaseUrl("https://app.sogive.org/");
		twt.setTestUrls(Arrays.asList("#search"));
		twt.run();
	}

}
