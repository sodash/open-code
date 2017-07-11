package com.winterwell.utils.threads;

import java.io.Serializable;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import com.winterwell.utils.Utils;
import com.winterwell.utils.containers.Pair2;
import com.winterwell.utils.log.Log;

/**
 * A simple pure-Java Actor implementation. Maintains a queue of messages.
 * Backed by a daemon thread which is started on the first message.
 * 
 * @param <Msg> Type of the message, e.g. String or something more structured like a custom data-class.
 * @testedby ActorTest
 * @author daniel
 */
public abstract class Actor<Msg> {

	final Queue<Packet<Msg>> q;

	/**
	 * one thread per actor. TODO multiple threads per actor, using possibly shared Executors
	 */
	Thread thread;

	private int maxq;

	/**
	 * Unset by default. If set, check each send to see that the queue is not too long.
	 * @param n
	 * @return
	 */
	public Actor<Msg> setMaxQ(int n) {
		maxq = n;
		return this;
	}

	
	protected Actor() {
		this(new ConcurrentLinkedQueue());
	}

	protected Actor(Queue<Packet<Msg>> queue) {
		this.q = queue;
	}

	public final boolean isAlive() {
		return thread != null && thread.isAlive();
	}

	private boolean pleaseStop;

	private Pair2<Msg, Throwable> lastEx;

	/**
	 * @return the most recent input-to-exception, or null. Note: only the most
	 *         recent exception is ever stored.
	 */
	public Pair2<Msg, Throwable> peekLastException() {
		return lastEx;
	}

	/**
	 * @return the most recent input-to-exception, or null. This clears the
	 *         exception -- a 2nd call will return null. Note: only the most
	 *         recent exception is ever stored.
	 */
	public final Pair2<Msg, Throwable> popLastException() {
		Pair2<Msg, Throwable> _lastEx = lastEx;
		lastEx = null;
		return _lastEx;
	}

	/**
	 * ensure we have an alive thread
	 */
	private void threadAlive() {
		if (thread != null && thread.isAlive()) {
			return;
		}
		synchronized (this) {
			if (thread != null && thread.isAlive()) {
				return;
			}
			thread = new Thread(new Runnable() {
				@Override
				public void run() {					
					loop();
					
					// Done!
					Log.d("actor", getName() + " done");
				}
			}, getName());
			thread.setDaemon(true);
			thread.start();
		}
	}

	protected String getName() {
		return getClass().getSimpleName();
	}

	final void loop() {
		while ( ! pleaseStop) {
			Packet<Msg> msg = null;
			try {
				if (q.isEmpty()) {
					Utils.sleep(10);
				}
				msg = q.poll();
				if (msg == null) {
					// NB: if a DelayQueue is used, then this (not isEmpty)
					// is where the loop gets a break from spinning.
					Utils.sleep(10);
					continue;
				}
				receive(msg.msg, msg.from);
			} catch (Throwable e) {
				Log.e(getName(), e);
				lastEx = new Pair2<Msg, Throwable>(msg.msg, e);
			}
		}
	}

	/**
	 * Send a message to this actor!
	 * 
	 * @param msg
	 * @param sender
	 *            Can be null.
	 */
	public void send(Msg msg, Actor sender) {
		send(new Packet(msg, sender));
	}

	/**
	 * Send a message to this actor! Convenience for
	 * {@link #send(Object, Actor)} with sender=null
	 * 
	 * @param msg
	 */
	public void send(Msg msg) {
		send(msg, null);
	}

	void send(Packet packet) {
		threadAlive();
		// check the queue
		if (maxq > 0 && q.size() > maxq) {
			throw new QueueTooLongException("Could not add "+packet);
		}
		q.add(packet);
	}

	/**
	 * Access the queue. Warning: at your own risk!
	 * 
	 * @return
	 */
	public final Queue<Packet<Msg>> getQ() {
		return q;
	}

	/**
	 * Receive a message! This is where the actor does it's thing.
	 * 
	 * @param msg
	 * @param sender
	 *            Can be null (e.g. if the message was sent by a non-Actor
	 *            class)
	 * @throws Exception Exceptions are caught and swallowed.
	 */
	protected abstract void receive(Msg msg, Actor sender) throws Exception;

	public final void pleaseStop() {
		pleaseStop = true;
	}

	/**
	 * Message + sending Actor.
	 * @author daniel
	 *
	 * @param <Msg>
	 */
	public static class Packet<Msg> implements Serializable {
		private static final long serialVersionUID = 1L;

		@Override
		public String toString() {
			return "Packet[from=" + from + ", msg=" + msg + "]";
		}

		public Packet(Msg msg, Actor sender) {
			this.msg = msg;
			this.from = sender;
		}

		/**
		 * Sender, can be null.
		 */
		public final Actor from;
		public final Msg msg;
	}
}
