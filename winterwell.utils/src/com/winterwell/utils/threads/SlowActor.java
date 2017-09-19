package com.winterwell.utils.threads;

import java.io.Flushable;
import java.io.IOException;
import java.util.Queue;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

import com.winterwell.depot.Desc;
import com.winterwell.utils.Utils;
import com.winterwell.utils.threads.Actor.Packet;
import com.winterwell.utils.time.Dt;
import com.winterwell.utils.time.Time;

/**
 * An actor which takes it's time about things. Useful for when things need a
 * pause. Because acting is all about the timing.
 * <p>
 * Uses {@link DelayQueue} under the hood.
 * 
 * @author daniel
 */
public class SlowActor<Msg> extends Actor<Msg> implements Flushable {

	public SlowActor() {
		super(new DelayQueue());
	}

	@Override
	public void flush() throws IOException {
		// send it all through receive
		Queue<Packet<Msg>> _q = getQ();
		try {
			for (Packet<Msg> packet : _q) {				
				consume(packet.msg, packet.from);
			}
		} catch (IOException e) {
			throw e;
		} catch (Exception e) {
			throw Utils.runtime(e);
		}
	}
	
	public final void sendDelayed(Msg msg, Actor sender, Dt delay) {
//		Log.d(getClass().getSimpleName(), "in " + delay + ": " + msg);
		sendDelayed(msg, sender, new Time().plus(delay));
	}

	public void send(Msg msg, Actor sender) {
		send(new DPacket<Msg>(msg, sender, null));
	}	

	/**
	 * Send a message to this actor!
	 * 
	 * @param msg
	 * @param sender
	 *            Can be null.
	 */
	public final void sendDelayed(Msg msg, Actor sender, Time when) {
		send(new DPacket(msg, sender, when));
	}

	static final class DPacket<Msg> extends Packet<Msg> implements Delayed {
		private static final long serialVersionUID = 1L;

		public DPacket(Msg msg, Actor sender, Time when) {
			super(msg, sender);
			this.t = when;
		}

		@Override
		public int compareTo(Delayed o) {
			if (o instanceof DPacket) {
				DPacket p = (DPacket) o;
				if (p.t == null) {
					return t == null ? 0 : 1;
				}
				if (t == null)
					return -1;
				return t.compareTo(p.t);
			} else {
				return 1;
			}
		}

		/**
		 * The time when the packet should be read.
		 */
		final Time t;

		@Override
		public long getDelay(TimeUnit unit) {
			if (t == null)
				return 0;
			return unit.convert(t.getTime() - System.currentTimeMillis(), unit);
		}

	}

}
