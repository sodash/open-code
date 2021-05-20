package com.winterwell.utils.threads;

import java.io.Flushable;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

import com.winterwell.utils.Utils;
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
	public void flush() {
		// send it all through receive
		Queue<Packet<Msg>> _q = getQ();
		try {
			for (Packet<Msg> packet : _q) {				
				consume(packet.msg, packet.from);
			}
		} catch (Exception e) {
			throw Utils.runtime(e);
		}
	}
	
	public 
//	final NB: removed final to allow for mocks in testing. This should not really be over-ridden. 
	void sendDelayed(Msg msg, Actor sender, Dt delay) {
		if (delay==null) {
			// no delay!
			send(msg, sender);
			return;
		}
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

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = super.hashCode();
			result = prime * result + Objects.hash(t);
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (!super.equals(obj))
				return false;
			if (getClass() != obj.getClass())
				return false;
			DPacket other = (DPacket) obj;
			return Objects.equals(t, other.t);
		}

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
