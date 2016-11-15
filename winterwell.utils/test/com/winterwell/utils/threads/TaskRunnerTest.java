package com.winterwell.utils.threads;

import org.junit.Test;

import winterwell.utils.Utils;

public class TaskRunnerTest {

	@Test
	public void testTaskRunnerBoolean() {
		TaskRunner tr = new TaskRunner(true);
		final StringBuilder sb = new StringBuilder();
		tr.submit(new DummyTask(sb, "A", 100));
		tr.submit(new DummyTask(sb, "B", 100));
		tr.submit(new DummyTask(sb, "C", 100));
		assert tr.getTodo().isEmpty();
		assert sb.toString().equals("ABC");
	}

	@Test
	public void testTaskRunnerThreaded() {
		TaskRunner tr = new TaskRunner(2);
		final StringBuilder sb = new StringBuilder();
		tr.submit(new DummyTask(sb, "A", 500));
		tr.submit(new DummyTask(sb, "B", 10));
		tr.submit(new DummyTask(sb, "C", 10));
		Utils.sleep(600);
		assert tr.getTodo().isEmpty();
		assert sb.toString().equals("BCA");
	}

	class DummyTask extends ATask<String> {

		private int ms;
		private String msg;
		private StringBuilder sb;

		public DummyTask(StringBuilder sb, String string, int millisecs) {
			super("DummyTask[" + string + "]");
			this.sb = sb;
			msg = string;
			this.ms = millisecs;
		}

		@Override
		protected String run() throws Exception {
			Utils.sleep(ms);
			sb.append(msg);
			return sb.toString();
		}

	}
}