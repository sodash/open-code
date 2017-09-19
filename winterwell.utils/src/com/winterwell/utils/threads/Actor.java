package com.winterwell.utils.threads;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentLinkedQueue;

import com.winterwell.utils.Utils;
import com.winterwell.utils.containers.Pair2;
import com.winterwell.utils.log.Log;
import com.winterwell.utils.threads.Actor.Packet;

/**
 * A simple pure-Java Actor implementation. Maintains a queue of messages.
 * Backed by a daemon thread which is started on the first message.
 * 
 * @param <Msg> Type of the message, e.g. String or something more structured like a custom data-class.
 * @testedby ActorTest
 * @author daniel
 */
public class Actor<Msg> {

	/**
	 * Message + sending Actor.
	 * @author daniel
	 *
	 * @param <Msg>
	 */
	public static class Packet<Msg> implements Serializable {
		private static final long serialVersionUID = 1L;

		/**
		 * Sender, can be null.
		 */
		public final Actor from;

		public final Msg msg;

		public Packet(Msg msg, Actor sender) {
			this.msg = msg;
			this.from = sender;
		}
		@Override
		public String toString() {
			return "Packet[from=" + from + ", msg=" + msg + "]";
		}
	}
	
	/**
	 * can be null
	 */
	IActorMsgConsumer<Msg> consumer;

	private Pair2<Msg, Throwable> lastEx;

	private int maxq;

	private boolean pleaseStop;
	
	final Queue<Packet<Msg>> q;

	/**
	 * one thread per actor. TODO multiple threads per actor, using possibly shared Executors
	 */
	Thread thread;

	protected Actor() {
		this(new ConcurrentLinkedQueue());
	}

	protected Actor(Queue<Packet<Msg>> queue) {
		this.q = queue;
	}
	
	/**
	 * Forwards to the consumer. 
	 * If you don't want to use a consumer, just override this method
	 * @param msg
	 * @throws Exception 
	 */
	protected void consume(Msg msg, Actor from) throws Exception {
		consumer.accept(msg, from);
	}
	
	protected void consumeBatch(ArrayList<Packet<Msg>> batch) throws Exception {
		if (consumer!=null) {
			consumer.acceptBatch(batch);
			return;
		}
		for (Packet<Msg> packet : batch) {
			consume(packet.msg, packet.from);
		}
	}

	protected String getName() {
		return getClass().getSimpleName();
	}

	/**
	 * Access the queue. Warning: at your own risk!
	 * 
	 * @return
	 */
	public final Queue<Packet<Msg>> getQ() {
		return q;
	}

	public final boolean isAlive() {
		return thread != null && thread.isAlive();
	}

	final void loop() {
		if (q instanceof BlockingQueue) {
			loop2_batch();
			return;
		}
		while ( ! pleaseStop) {
			Packet<Msg> msg = null;
			try {
				// sleep based poll
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
				consume(msg.msg, msg.from);
			} catch (Throwable e) {
				Log.e(getName(), e);
				lastEx = new Pair2<Msg, Throwable>(msg==null? null : msg.msg, e);
			}
		}

	}

	private void loop2_batch() {
		ArrayList<Packet<Msg>> batch = new ArrayList();
		int batchSize = 16;
		BlockingQueue<Packet<Msg>> bq = (BlockingQueue<Packet<Msg>>) q;
		while ( ! pleaseStop) {
			try {
				// drain the queue into batch
				if (bq.drainTo(batch, batchSize) == 0) {
					// nothing in q? q.take() will wait for something...
					batch.add(bq.take());
				}
				// receive
				consumeBatch(batch);				
				batch.clear();
			} catch (Throwable e) {
				Log.e(getName(), e);
				lastEx = new Pair2<Msg, Throwable>(null, e);
			}
		}

	}

	/**
	 * @return the most recent input-to-exception, or null. Note: only the most
	 *         recent exception is ever stored. The Msg part may be null
	 */
	public Pair2<Msg, Throwable> peekLastException() {
		return lastEx;
	}
	
	public final void pleaseStop() {
		pleaseStop = true;
	}

	/**
	 * @return the most recent input-to-exception, or null. The Msg part may be null. This clears the
	 *         exception -- a 2nd call will return null. Note: only the most
	 *         recent exception is ever stored.
	 */
	public final Pair2<Msg, Throwable> popLastException() {
		Pair2<Msg, Throwable> _lastEx = lastEx;
		lastEx = null;
		return _lastEx;
	}

	/**
	 * Send a message to this actor! Convenience for
	 * {@link #send(Object, Actor)} with sender=null
	 * 
	 * @param msg
	 */
	public final void send(Msg msg) {
		send(new Packet(msg, DeadLetterActor.dflt));
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

	protected final void send(Packet packet) {
		threadAlive();
		// check the queue
		if (maxq > 0 && q.size() > maxq) {
			throw new QueueTooLongException("Could not add "+packet);
		}
		q.add(packet);
	}

	public Actor<Msg> setConsumer(IActorMsgConsumer<Msg> consumer) {
		this.consumer = consumer;
		return this;
	}

	/**
	 * Unset by default. If set, check each send to see that the queue is not too long.
	 * @param n
	 * @return
	 */
	public Actor<Msg> setMaxQ(int n) {
		maxq = n;
		return this;
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
			thread = new Thread(() -> {
				loop();
				// Done!?
				Log.d("actor", getName() + " is now exiting");
			}, getName());
			thread.setDaemon(true);
			thread.start();
		}
	}
}
