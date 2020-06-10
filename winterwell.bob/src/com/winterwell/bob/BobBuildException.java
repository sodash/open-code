package com.winterwell.bob;

import java.util.ArrayList;
import java.util.List;

import com.winterwell.utils.StrUtils;

public class BobBuildException extends RuntimeException {

	private BuildTask task;

	public BobBuildException(BuildTask bs, Throwable ex) {
		super(bs.toString(), ex);
		task = bs;		
	}

	@Override
	public String toString() {
		List<BuildTask> tasks = getTaskStack();
		String cs = ""+getCause();
		return "BobBuildException["+StrUtils.join(tasks, " > ")+" : "+
			StrUtils.ellipsize(cs, 140)
		+"]";
	}

	private List<BuildTask> getTaskStack() {
		List<BuildTask> stack;
		if (getCause() instanceof BobBuildException) {
			stack = ((BobBuildException) getCause()).getTaskStack();
		} else {
			stack = new ArrayList();
		}
		stack.add(0, task);
		return stack;
	}
}
