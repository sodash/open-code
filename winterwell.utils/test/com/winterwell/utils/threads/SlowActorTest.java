package com.winterwell.utils.threads;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

import com.winterwell.utils.time.StopWatch;

import winterwell.utils.Utils;
import winterwell.utils.time.Dt;
import winterwell.utils.time.TUnit;
import winterwell.utils.time.Time;

public class SlowActorTest {

	@Test
	public void testDelayOrder() {
		SlowActor.DPacket a = new SlowActor.DPacket("a", null, null);
		SlowActor.DPacket b = new SlowActor.DPacket("b", null,
				new Time().minus(TUnit.MINUTE));
		SlowActor.DPacket c = new SlowActor.DPacket("c", null,
				new Time().plus(TUnit.SECOND));
		SlowActor.DPacket d = new SlowActor.DPacket("d", null,
				new Time().plus(TUnit.MINUTE));
		List<SlowActor.DPacket> list = Arrays.asList(c, d, b, a);
		Collections.sort(list);
		assert list.get(0).msg == "a";
		assert list.get(1).msg == "b";
		assert list.get(2).msg == "c";
		assert list.get(3).msg == "d";
	}

	@Test
	public void testSend() {
		SlowPingPongActor a = new SlowPingPongActor("A", 3);
		SlowPingPongActor b = new SlowPingPongActor("B", 3);
		// kick them off
		StopWatch sw = new StopWatch();
		a.sendDelayed("ping", b, new Dt(2, TUnit.SECOND));
		while (a.isAlive() || b.isAlive()) {
			Utils.sleep(50);
		}
		System.out.println(SlowPingPongActor.log);
		assert SlowPingPongActor.log.contains("pong");
		assert sw.getTime() > 6000;
	}

	/**
	 * The classic ping-pong example. Two actors exchange messages until they get
	 * bored & stop.
	 * 
	 * @author daniel
	 * 
	 */
	static class SlowPingPongActor extends SlowActor<String> {

		int max;
		int n;
		private String name;

		public SlowPingPongActor(String name, int n) {
			this.name = name;
			this.max = n;
		}

		@Override
		protected String getName() {
			return name;
		}

		static volatile String log = "";

		@Override
		protected void receive(String msg, Actor sender) {
			log += sender.getName() + ": " + msg + "\n";
			if (msg.startsWith("STOP")) {
				pleaseStop();
				return;
			}
			if (n == max) {
				sender.send("STOP", this);
				pleaseStop();
				return;
			}
			if (msg.startsWith("ping")) {
				((SlowActor) sender).sendDelayed("pong " + n, this, new Dt(2,
						TUnit.SECOND));
			} else {
				((SlowActor) sender).sendDelayed("ping " + n, this, new Dt(1,
						TUnit.SECOND));
			}
			n++;
		}

	}
}
