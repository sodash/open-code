/**
 *
 */
package com.winterwell.utils.threads;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import com.winterwell.utils.ReflectionUtils;
import com.winterwell.utils.Utils;
import com.winterwell.utils.log.Log;
import com.winterwell.utils.time.Dt;
import com.winterwell.utils.time.TUnit;
import com.winterwell.utils.time.Time;

/**
 * HACK This code was copied out of SoDash (where it was needed for Hibernate issues).
 * In simpler contexts, do we need this?
 * 
 * Provides an in-memory JVM locking system for database-related objects.
 * <p>
 * WARNING: JVM locks are not saved to the database! They are
 * only kept in memory. If multiple processes use the database, these
 * locks will not work.
 * </p>
 * <h3>DB transactions, threads and locks</h3>
- Lock holds a thread-local list of locks-to-drop.
When higher-level code drops a lock, it does NOT get dropped then & there,
but gets added to this list.<br>
- When a transaction is saved, these locks get dropped.<br>
- If a lock is in the to-drop list, and the same thread that had the
lock last requests it again, then the request succeeds the same as if a
new lock were created.<br>
 * <p>
 * Rationale: There is a subtle issue with batching the database writes. Consider the following sequence of actions:<br>
<br>
1. Thread 1 takes a lock on object X	<br>
2. Thread 1 edits X	<br>
3. Thread 1 drops the lock	<br>
4. Thread 2 begins an edit on object X	<br>
5. Thread 1: end of servlet: transaction is committed	<br>
<br>
Thread 2 will edit an out-of-date version.<br>
<br>
So: locks need to be held until the transaction is saved.
But lock dropping code should be local.
</p>

 *
 * <h3>Usage</h3>
 * Holding onto locks is bad. So do this:
<code><pre>
Lock lock = null;
try {
	lock = Lock.(try)getLock();
	// do stuff with lock
} finally {
	Lock.release(lock);
}
</pre></code>
Note: Locks will get broken if thread-death occurs.
 *
 * @testedby {@link LockTest}
 * @author Daniel
 */
public final class Lock implements AutoCloseable {


	private Thread holder;

	/**
	 * normally 0
	 */
	private int depth;

	/**
	 * STATUS: Not currently used!
	 * 
	 * Provides the basis for holding locks until the end of a
	 * database transaction.
	 */
	private static final ThreadLocal<List<Lock>> dropMes = new ThreadLocal<List<Lock>>() {
		
		protected List<Lock> initialValue() {
			return new ArrayList<Lock>();
		}
	};
	
	
	public String toString() {
		return "Lock["+slug+" thread:"+holder+"]";
	}

	/**
	 * @param purpose2
	 * @param objects
	 * @return the lock for this lock-slug, or null if there is no lock.
	 */
	public static Lock examineLock(String ref) {
		Lock lock = locks.get(ref);
		return lock;
	}

	/** use putIfAbsent?? */
	private static final ConcurrentHashMap<String,Lock> locks = new ConcurrentHashMap<String, Lock>();

	private static final String LOGTAG = "lock";

	/**
	 * Try to acquire the lock on lockMe - returns immediately
	 * whether successful or not.
	 * <p>
	 * Locks should be used within try... finally blocks, disposing of the lock at the end.
	 *
	 * @param lockMe
	 * @param type
	 * @param time Max time to hold the lock for
	 * @return lock or null if someone else holds the lock
	 */
	public static Lock tryGetLock(String lockMe, Dt time) {
		return tryGetLock(lockMe, time, false);
	}
	
	public static Lock tryGetLock(String lockMe, Dt time, boolean reentrant)
	{
		Lock lock = examineLock(lockMe);
		if (lock == null) {
			return tryGetLock2_new(lockMe,  time);
		}
		// expired?
		if ( ! lock.isValid()) {

			Log.d(LOGTAG, "Replacing expired lock "+lockMe);
			lock.dispose2_noReally();
			return tryGetLock2_new(lockMe,  time);
		}
		// privately held by this thread? (c.f. database transaction notes)
		List<Lock> droppedHere = dropMes.get();
		if (droppedHere.contains(lock)) {
//			Log.d(LOGTAG, "reset & reuse "+lockMe); // deugging spew
			// update the time stamp?
			lock.reset(time);
			return lock;
		}

		// This probably indicates an unwanted recursion & an infinite loop
		if (lock.holder == Thread.currentThread() && lock.isValid()) {
			if (reentrant) {
				lock.depth ++;
				return lock; // NB: expiry time unchanged
			}
			// clear the lock!
			releaseNOW(lock);
			// throw an error
			throw new IllegalStateException("Dodgy recursion on lock: "+lock);
		}

		// No joy!
		return null;
	}



	/**
	 * Like {@link #tryGetLock(String, Dt, String)} but blocks until the lock is free.
	 * For up to 1 hour!
	 * @param time Max time to hold the lock for.
	 * @return the lock for this String
	 */
	public static Lock getLock(String lockMe, Dt time) throws LockOutFailureException {
		return getLock(lockMe, time, TUnit.HOUR.dt);
	}

	/**
	 * Like {@link #tryGetLock(String, Dt, String)} but blocks until the lock is free.
	 * @param maxHold Max time to hold the lock for.
	 * @param maxTry Max time to spend trying to get the lock.
	 * @return the lock for this String
	 * @throws LockOutFailureException if getting the lock takes too long.
	 */
	public static Lock getLock(String lockMe, Dt maxHold, Dt maxTry) throws LockOutFailureException
	{
		return getLock(lockMe, maxHold, maxTry, false);
	}
	
	public static Lock getLock(String lockMe, Dt maxHold, Dt maxTry, boolean reentrant) throws LockOutFailureException
	{
		// Note: there is no fairness policy! and hence no protection against being locked out forever
		int sleep = 4;
		boolean warning = false;
		// Do break out eventually!
		Time start = new Time();
		Time giveUp = start.plus(maxTry);
		while(true) {
			Lock lock = tryGetLock(lockMe, maxHold, reentrant);
			if (lock!=null) return lock;
			String holderName = getLockHolder(lockMe);
			Log.i(LOGTAG, Thread.currentThread().getName() + "-" + Thread.currentThread().getId() + " waiting on lock " + lockMe + " held by " + holderName
						+" caller: "+ReflectionUtils.getSomeStack(8));
			if (new Time().isAfter(giveUp)) {
				throw new LockOutFailureException("Time up: Could not get "+lockMe);
			}
			// Don't quite thrash the system.
			// Yes this is crude, but how else to do it potentially across JVMs?
			// Sleep for progressively longer periods -- x2 each time (but with a limit of 10 seconds)
			sleep = Math.min(2*sleep, 10000);
			if (sleep==10000 && ! warning) {
				warning = true;
//				DBEvent.emitException(holderName+" v "+Thread.currentThread().getName(),
//						new DummyException("Slowness: "+lockMe));
			}
			// Experimental, avoiding lock phasing?
			long msecs = (long) (sleep * (0.5 + Utils.getRandom().nextFloat()));
			Utils.sleep(msecs);
		}
	}

	/**
	 * Which thread
	 * @param lockMe
	 * @return
	 */
	public static String getLockHolder(String lockMe) {
		Lock currentLock = examineLock(lockMe);
		if (currentLock == null) return "nobody";
		Thread holder = currentLock.getHolder();
		String holderName = holder == null ? "no thread!" : holder.getName() + "-" + holder.getId();
		return holderName;
	}

	/**
	 * When this object was created. This may be a future date for delayed
	 * activity.
	 */
	protected long created = System.currentTimeMillis();

	/**
	 * Ideally you should already have tried whether this lock exists,
	 * but this is not essential for correct behaviour.
	 * @param lockMe
	 * @param type
	 * @param time
	 * @param purpose
	 * @return lock or null if someone else has the lock
	 */
	private static Lock tryGetLock2_new(String lockMe,   Dt time) {
		Lock lock = new Lock(lockMe,  time);
		// did we win the race? TODO We could move the locks.putIfAbsent here, then remove the examineLock for a minor efficiency gain.
		Lock winner = examineLock(lockMe);
		if ( ! lock.equals(winner)) {
			lock.dispose();
			return null;
		}
		winner.setHolder(Thread.currentThread());
		return winner;
	}

	String slug;

	long expiryDate;

	/**
	 * Create lock. Persist it to database if it has a purpose, otherwise add it to
	 * the in-memory cache.
	 * @param lockMe
	 * @param time Max time to hold the lock for.
	 */
	private Lock(String lockMe,   Dt time) {
		assert lockMe != null;
		assert time.getValue() > 0;
		this.slug = lockMe;
		reset(time);
		// save to short term cache
		locks.putIfAbsent(lockMe, this);
	}

	/**
	 * Delete this lock - equivalent to releasing the lock, as other
	 * processes can now acquire a lock on the underlying string.
	 */
	public void dispose() {
		// A reentrant lock which has been locked more than once? 
		if (depth > 0) {
			depth--;
			return;
		}
		// Unlike SoDash (which has to deal with Hibernate commits) we have an explicit dispose = dispose approach. 
		dispose2_noReally();
//		// Don't dispose just yet - add to the list of to-drop and wait
//		// for a database tx-boundary		
//		dropMes.get().add(this);
	}

	
	/**
	 * Equivalent to {@link #dispose()}.
	 * NB: here for try-with-resources support
	 */
	
	public void close() {	
		dispose();
	}
	
	/**
	 * Really drop a short-term lock.
	 */
	private void dispose2_noReally() {
		// Log.d(LOGTAG, "release "+slug); // deugging spew
		locks.remove(slug, this);
		List<Lock> dropMe = dropMes.get();
		dropMe.remove(this);
	}

	/**
	 * Must be called at a database transaction boundary to actually drop
	 * the short term locks this thread holds. This is done by {@link PersistUtils}
	 */
	static void txBoundary() {
		List<Lock> dropMe = dropMes.get();
		if (dropMe.isEmpty()) return;
//		Log.d(LOGTAG, "txBoundary drop "+ReflectionUtils.getSomeStack(5)); // deugging spew
		Lock[] _dropMe = dropMe.toArray(new Lock[0]);
		for(Lock lock : _dropMe) {
			lock.dispose2_noReally();
		}
		dropMe.clear(); // should be redundant
	}

	public String getSlug() {
		return slug;
	}

	boolean isValid() {
		return System.currentTimeMillis()<expiryDate
				&& holder != null && holder.isAlive();
	}


	private void reset(Dt time) {
		expiryDate = new Time().plus(time).longValue();
	}

	/**
	 * Release this lock.
	 * <p>
	 * Just a convenience for <code>if (lock!=null) lock.dispose();</code>
	 *
	 * <p>
	 * Note: Actually this doesn't release the lock here and now. The real disposal
	 * happens at a transaction boundary (e.g. a commit).
	 * Meanwhile the lock can be reacquired by this thread but not
	 * others.
	 *
	 * @param lock Can be null - which may happen when using tryGet
	 * @see #releaseNOW(Lock)
	 */
	public static void release(Lock lock) {
		if (lock!=null) lock.dispose();
	}

	/**
	 * Obvious reasons not to use this. Useful for testing.
	 */
	@Deprecated
	public static void disposeAllLocks() {
		locks.clear();
	}
	/**
	 * @param holder the holder to set
	 */
	private void setHolder(Thread holder) {
		this.holder = holder;
	}
	/**
	 * @return the holder
	 */
	public Thread getHolder() {
		return holder;
	}

	/**
	 * Status: experimental!
	 * This is an aggressive method which should be used with care.
	 *
	 * @param lockMe
	 * @return the lock for this object. Always returns fast.
	 * Interrupts the current lock holder Thread if there is one.
	 */
	public static Lock forceGetLock(String lockMe) {
		while(true) {
			Lock lock = tryGetLock(lockMe, TUnit.DAY.dt);
			if (lock != null) return lock;
			lock = examineLock(lockMe);
			if (lock == null) continue;
			// interrupt!
			Log.d(LOGTAG, "force get "+lockMe+": interrupt "+lock.getHolder());
			if (lock.getHolder() != null) {
				lock.getHolder().interrupt();
			}
			lock.dispose2_noReally();
		}
	}
	public Time getCreated() {
		return new Time(created);
	}

	public Time getExpiryDate() {
		return new Time(expiryDate);
	}

	/**
	 * Release this lock now. Really.
	 * @param lock Can be null - which may happen when using tryGet
	 */
	public static void releaseNOW(Lock lock) {
		if (lock==null) return;
		Log.d(LOGTAG, "release now "+lock);
		lock.dispose2_noReally();
	}

	/**
	 * @return true if this thread holds a valid lock for the reference
	 */
	public static boolean hasLock(String ref) {
		Lock lck = examineLock(ref);
		if (lck==null) return false;
		if (lck.holder!=Thread.currentThread()) return false;
		return lck.isValid();
	}

}

