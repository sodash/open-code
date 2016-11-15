package winterwell.utils;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

import com.winterwell.utils.threads.SafeExecutor;

public class NoisyCallableTest {

	@Test(expected = Exception.class)
	public void testLousyBehaviour() throws InterruptedException,
			ExecutionException {
		ExecutorService ex = Executors.newSingleThreadExecutor();
		Callable<Object> c = new Callable<Object>() {
			@Override
			public Object call() throws Exception {
				if (true)
					throw new Exception("foo");
				return "bar";
			}
		};
		Future<Object> f = ex.submit(c);
		ex.shutdown();
		ex.awaitTermination(10, TimeUnit.SECONDS);
		// only now does an exception get thrown
		Object v = f.get();
	}

	@Test(expected = Exception.class)
	public void testSafeBehaviour() throws InterruptedException,
			ExecutionException {
		ExecutorService ex = new SafeExecutor(
				Executors.newSingleThreadExecutor());
		Callable<Object> c = new Callable<Object>() {
			@Override
			public Object call() throws Exception {
				if (true)
					throw new Exception("foo");
				return "bar";
			}
		};
		Future<Object> f = ex.submit(c);
		// we should see some log output pretty much straight away
		ex.shutdown();
		ex.awaitTermination(10, TimeUnit.SECONDS);
		// only now does an exception get thrown
		Object v = f.get();
	}
}
