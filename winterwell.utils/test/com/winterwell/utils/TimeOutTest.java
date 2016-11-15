package winterwell.utils;

import org.junit.Test;

import com.winterwell.utils.TimeOut;

public class TimeOutTest {

	@Test
	public void testTimeOutFires() {
		TimeOut to = new TimeOut(250);
		try {
			to.canThrow();
			Utils.sleep(500);
			assert false : "Wot no interrupt?";
		} catch (InterruptedException e) {
			System.out.println(e);
			// all good
		} catch (Throwable e) {
			assert Utils.getRootCause(e) instanceof InterruptedException : e;
		} finally {
			to.cancel();
		}
	}

	@Test
	public void testTimeOutCancelled() {
		TimeOut to = new TimeOut(250);
		to.cancel();
		Utils.sleep(600);
	}

}
