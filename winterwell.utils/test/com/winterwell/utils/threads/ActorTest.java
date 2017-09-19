package com.winterwell.utils.threads;

import org.junit.Test;

import com.winterwell.utils.Utils;

public class ActorTest {

	@Test
	public void testSend() {
		PingPongActor a = new PingPongActor("A", 3);
		PingPongActor b = new PingPongActor("B", 3);
		// kick them off
		a.send("ping", b);
		while (a.isAlive() || b.isAlive()) {
			Utils.sleep(50);
		}
		System.out.println(PingPongActor.log);
		assert PingPongActor.log.contains("pong");
	}

	/**
	 * The classic ping-pong example. Two actors exchange messages until they get
	 * bored & stop.
	 * 
	 * @author daniel
	 * 
	 */
	static class PingPongActor extends Actor<String> {

		int max;
		int n;
		private String name;

		public PingPongActor(String name, int n) {
			this.name = name;
			this.max = n;
		}

		@Override
		protected String getName() {
			return name;
		}

		static volatile String log = "";

		@Override
		protected void consume(String msg, Actor from) {		
			log += from.getName() + ": " + msg + "\n";
			if (msg.startsWith("STOP")) {
				pleaseStop();
				return;
			}
			if (n == max) {
				from.send("STOP", this);
				pleaseStop();
				return;
			}
			if (msg.startsWith("ping")) {
				from.send("pong " + n, this);
			} else {
				from.send("ping " + n, this);
			}
			n++;
		}

	}
}
