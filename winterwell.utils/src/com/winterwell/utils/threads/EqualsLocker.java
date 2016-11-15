package com.winterwell.utils.threads;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

import winterwell.utils.reporting.Log;

/**
 * Lock equals() things.
 * 
 * <h3>Example</h3> <code><pre>
 * String a = "Hello";
 * String b = new String("Hello");
 * assert a.equals(b) && a != b;
 * 
 * EqualsLocker locker = new EqualsLocker();
 * locker.lock(a);
 * try {
 * 	// do stuff...
 *  assert locker.isLocked(b); 
 * } finally {
 * 	locker.unlock(a);
 * }
 * </pre></code>
 * 
 * @param <X>
 *            The type of thing you want to lock (not really important).
 * @author daniel
 * @testedby {@link EqualsLockerTest}
 */
public final class EqualsLocker<X> {

	final ConcurrentHashMap<X, ReentrantLock2> locker = new ConcurrentHashMap();

	/**
	 * Lock the item. Will block until it gets the lock.
	 * @param x
	 */
	public void lock(X x) {
		ReentrantLock2 lock;
		while (true) {
			lock = getLock(x);
			// OK, we have the lock and we locked it
			// But did we win? There is a race here -- if the previous lock-holder
			// removed the mapping while we were locking.
			// Also: re-put to make sure the x-> lock mapping is there.
			ReentrantLock2 old = locker.putIfAbsent(x, lock);
			if (old == null || old == lock) {
				break;
			}
			// We lost! Discard our unattached lock & try again...
			assert locker.get(x) != lock : lock;
			lock.unlock();
		}
		assert isLocked(x) : x;
		assert isHeldByCurrentThread(x) : lock + " not "
				+ Thread.currentThread().getName();
	}

	/**
	 * @deprecated For debugging use only
	 * @param x
	 * @return lock or null
	 */
	public String getLockInfo(X x) {
		ReentrantLock2 lck = locker.get(x);
		if (lck==null) return "no lock";
		Thread owner = lck.getOwner();
		return "locked by "+owner;
	}
	
	/**
	 * Get/create lock
	 */
	private ReentrantLock2 getLock(X x) {
		final ReentrantLock2 lock = new ReentrantLock2();
		// give the lock an owner, to avoid a race condition in the isAlive test below
		lock.lock();
		ReentrantLock2 realLock = getLock2(x, lock);
		if (realLock!=lock) {
			realLock.lock();
			lock.unlock();
		} 
		assert realLock.isHeldByCurrentThread();
		return realLock;
	}

	/* private not so for testing */ 
	ReentrantLock2 getLock2(X x, final ReentrantLock2 lock) {
		while(true) {
			ReentrantLock2 old = locker.putIfAbsent(x, lock);
			if (old == null) {
				// success -- our new lock is in place in the locker
				return lock;
			}
			// defend against thread death
			Thread owner = old.getOwner();
			if (owner==null || owner.isAlive()) {
					// already has a lock
				return old;
			}
			// old is bogus (it has a dead owner) -- remove it and try again
			Log.d("locker", "Removing orphan lock "+old+" for "+x);
			locker.remove(x, old);			
		}
	}

	/**
	 * Queries if this object us locked by any thread. 
	 * <br>
	 * This method is designed for use in monitoring of the system state, not for synchronization control.
	 * @param x
	 * @return
	 */
	public boolean isLocked(X x) {
		ReentrantLock2 lock = locker.get(x);
		if (lock == null)
			return false;
		Thread o = lock.getOwner();
		// dead owner?
		if (o==null || ! o.isAlive()) return false;
		assert lock.isLocked() : x;
		return true;
	}
	
	

	/**
	 * 
	 * @param x
	 * @return true if x is locked, and that lock is held by the current thread
	 */
	public boolean isHeldByCurrentThread(X x) { // , Lock expected) {
		ReentrantLock2 lock = locker.get(x);
		// assert lock == expected;
		return lock != null && lock.isHeldByCurrentThread();
	}

	// public void unlock(Object x) {
	// unlock(x, null);
	// }

	public void unlock(X x) {
		// System.out.println("unlock "+x+" by "+Thread.currentThread().getName());
		ReentrantLock2 lock = locker.get(x);
		// assert expected==null || lock == expected : lock+"\n v "+expected;
		if (lock == null) {
			return; // unlikely but possible
		}
		// assert lock != null : x;
		assert lock.isLocked() : x;
		assert lock.isHeldByCurrentThread() : x + " " + lock + " not "
				+ Thread.currentThread().getName();
		// Shall we remove? I.e. is this thread done with the lock?
		int cnt = lock.getHoldCount();
		if (cnt == 1) {
			// We're the last one out -- remove the mapping to avoid a memory
			// leak
			locker.remove(x, lock);
			// An overlapping lock could now get created, but that's OK -- this
			// lock is finished
		}
		lock.unlock();
	}

	/**
	 * @return how many locks do we hold?
	 */
	public int size() {
		return locker.size();
	}

}
/**
 * Expose the thread-owner from {@link ReentrantLock}.
 * {@inheritDoc}
 * @author daniel
 */
final class ReentrantLock2 extends ReentrantLock {
	private static final long serialVersionUID = 1L;

	@Override
	public Thread getOwner() {
		return super.getOwner();
	}
}