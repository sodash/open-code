package com.winterwell.bob;

public class TestTask extends BuildTask {

	public TestTask() {
	}
	
	@Override
	public void doTask() throws Exception {
		System.out.println("Yeh!");
	}

}
