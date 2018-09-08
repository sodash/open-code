package com.winterwell.bob;

import com.winterwell.utils.io.FileUtils;

public class TestTask extends BuildTask {

	public TestTask() {
	}
	
	@Override
	public void doTask() throws Exception {
		System.out.println("Yeh! from "+FileUtils.getWorkingDirectory());
	}

}
