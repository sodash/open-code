package com.winterwell.bob.tasks;

import static org.junit.Assert.*;

import java.util.Arrays;

import org.junit.Test;

public class TestWebsiteTaskTest {

	@Test
	public void testDoTask() {
		TestWebsiteTask twt = new TestWebsiteTask();
		twt.setBaseUrl("https://app.sogive.org/");
		twt.setTestUrls(Arrays.asList("#search"));
		twt.run();
	}

}
