package com.winterwell.utils.threads;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

import org.junit.Test;

import com.winterwell.utils.Utils;

public class EqualsLockerTest {

	@Test
	public void testNeverLocked() throws InterruptedException {
		final EqualsLocker locker = new EqualsLocker();
		// Unlock without ever first locking
		locker.unlock("a"); // should be OK!
		assert !locker.isLocked("a");
		locker.lock("a");
		assert locker.isLocked("a");
		locker.unlock("a");
		assert !locker.isLocked("a");
	}


	@Test
	public void testSuddenThreadDeath() throws InterruptedException {
		final EqualsLocker locker = new EqualsLocker();
		Thread t = new Thread() {
			@Override
			public void run() {
				System.out.println("lock a...");
				locker.lock("a");
				System.out.println("...locked a");
				Utils.sleep(15000);
				System.out.println("unlock a?!");
				locker.unlock("a");
			}
		};
		t.start();
		Utils.sleep(100);
		assert locker.isLocked("a");
		assert ! locker.isHeldByCurrentThread("a");
		t.stop();
		Utils.sleep(100);
		
		assert ! t.isAlive();
		
		assert ! locker.isLocked("a");
		locker.lock("a");
		assert locker.isLocked("a");
		assert locker.isHeldByCurrentThread("a");		
	}

	@Test
	public void testLockStress() throws InterruptedException {
		final EqualsLocker locker = new EqualsLocker();
		final Map<String, Integer> map = new HashMap();
		SafeExecutor exe = new SafeExecutor(Executors.newFixedThreadPool(20));
		final String key = "key";
		map.put(key, 0);
		final AtomicInteger aok = new AtomicInteger();
		int N = 1000;
		for (int i = 0; i < N; i++) {
			exe.submit(new Runnable() {
				public void run() {
					String k = new String(key);
					assert k != key && k.equals(key);
					// Acquire the lock
					// ReentrantLock lck =
					locker.lock(k);
					// Do stuff safely
					Integer v = map.get(k);
					if (Utils.getRandomChoice(0.1))
						Utils.sleep(1);
					v = v + 1;
					if (v % 100 == 0)
						System.out.println(v);
					map.put(k, v);
					// Release the lock
					locker.unlock(k); // , lck);
					aok.incrementAndGet();
					// System.out.println(aok.get());
				}
			});
		}

		exe.throwIfException();
		exe.shutdown();
		exe.awaitTermination(100, TimeUnit.SECONDS);
		exe.throwIfException();

		System.out.println(map);
		assert map.get(key) == N;
		assert aok.get() == N : aok.get();
	}

	@Test
	public void testGetLockStress() throws InterruptedException {
		final EqualsLocker locker = new EqualsLocker();
		final Map<String, Integer> map = new HashMap();
		SafeExecutor exe = new SafeExecutor(Executors.newFixedThreadPool(10));
		final String key = "key";
		map.put(key, 0);
		final AtomicInteger aok = new AtomicInteger();
		final ReentrantLock2 _lock2 = new ReentrantLock2();
		final ReentrantLock lock = locker.getLock2(key, _lock2);
		for (int i = 0; i < 10000; i++) {
			final int j = i;
			exe.submit(new Runnable() {
				public void run() {
					String k = new String(key);
					assert k != key && k.equals(key);
					ReentrantLock lock2 = locker.getLock2(key, _lock2);
					assert lock2 == lock : j+": "+lock2+" vs "+lock;
					aok.incrementAndGet();
				}
			});
		}

		exe.throwIfException();
		exe.shutdown();
		exe.awaitTermination(100, TimeUnit.SECONDS);
		exe.throwIfException();

		assert aok.get() == 10000 : aok.get();
	}

	@Test
	public void testLock() throws InterruptedException {
		final EqualsLocker locker = new EqualsLocker();
		final Map<String, Integer> map = new HashMap();
		final String key = "key";
		map.put(key, 0);
		final AtomicInteger aok = new AtomicInteger();
		Thread a = new Thread("Alice") {
			@Override
			public void run() {
				String k = new String(key);
				assert k != key && k.equals(key);
				locker.lock(k);
				Integer v = map.get(k);
				v = v + 1;
				Utils.sleep(100);
				map.put(k, v);
				locker.unlock(k);
				aok.incrementAndGet();
			}
		};
		Thread b = new Thread("Bob") {
			@Override
			public void run() {
				String k = new String(key);
				assert k != key && k.equals(key);
				locker.lock(k);
				Integer v = map.get(k);
				v = v + 1;
				Utils.sleep(100);
				map.put(k, v);
				locker.unlock(k);
				aok.incrementAndGet();
			}
		};
		a.start();
		b.start();

		a.join();
		b.join();

		System.out.println(map);
		assert map.get(key) == 2;
		assert locker.size() == 0;
		assert aok.get() == 2;
	}

	@Test
	public void testNotEquals() throws InterruptedException {
		final EqualsLocker locker = new EqualsLocker();
		final Map<String, Integer> map = new HashMap();
		final String key = "key1";
		final String key2 = "key2";
		map.put(key, 0);
		map.put(key2, 100);
		// locking this not-equal key should have no effect
		locker.lock(key2);
		final AtomicInteger aok = new AtomicInteger();
		Thread a = new Thread("Alice") {
			@Override
			public void run() {
				String k = new String(key);
				assert k != key && k.equals(key);
				locker.lock(k);
				Integer v = map.get(k);
				v = v + 1;
				Utils.sleep(100);
				map.put(k, v);
				locker.unlock(k);
				aok.incrementAndGet();
			}
		};
		Thread b = new Thread("Bob") {
			@Override
			public void run() {
				String k = new String(key);
				assert k != key && k.equals(key);
				locker.lock(k);
				Integer v = map.get(k);
				v = v + 1;
				Utils.sleep(100);
				map.put(k, v);
				locker.unlock(k);
				aok.incrementAndGet();
			}
		};
		a.start();
		b.start();

		a.join();
		b.join();

		locker.unlock(key2);

		System.out.println(map);
		assert map.get(key) == 2;
		assert locker.size() == 0;
		assert aok.get() == 2;
	}

}
