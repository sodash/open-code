package com.winterwell.web.app;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.junit.Test;

import com.winterwell.utils.Utils;
import com.winterwell.utils.log.Log;
import com.winterwell.utils.threads.SafeExecutor;
import com.winterwell.utils.web.WebUtils2;
import com.winterwell.web.FakeBrowser;
import com.winterwell.web.fields.Checkbox;

public class JettyLauncherTest {

	@Test
	public void testSessionVariables() {
		JettyLauncher jl = new JettyLauncher(new File("test"), 9627);
		jl.setup();		
		jl.addServlet("/*", new MyTestServlet());		
		Log.i("web", "...Launching Jetty web server on port "+jl.getPort());
		jl.run();

		FakeBrowser fb = new FakeBrowser();
		String ok = fb.getPage("http://localhost:9627/test.json?site=%%SITE%%");
		assert ok != null;
		assert ok.startsWith("OK: null") : ok;
		

		String ok2 = fb.getPage("http://localhost:9627/test.json?site=foo");
		assert ok2 != null;
		assert ok2.startsWith("OK: foo") : ok2;
		
		String ok3 = fb.getPage("http://localhost:9627/test.json?site=bar");
		assert ok3 != null;
		assert ok3.startsWith("OK: bar") : ok3;
		// test session handling
		assert ok3.contains("Previous: foo") : ok3;
		
		jl.stop();
	}
	
	
	@Test
	public void testLaunch() {
		JettyLauncher jl = new JettyLauncher(new File("test"), 9627);
		jl.setup();		
		jl.addServlet("/*", new MyTestServlet());		
		Log.i("web", "...Launching Jetty web server on port "+jl.getPort());
		jl.run();

		FakeBrowser fb = new FakeBrowser();
		String ok = fb.getPage("http://localhost:9627/test.json?site=%%SITE%%");
		assert ok != null;
		assert ok.startsWith("OK: null") : ok;
		
		String ok2 = fb.getPage("http://localhost:9627/test.json?site=foo");
		assert ok2 != null;
		assert ok2.startsWith("OK: foo") : ok2;		
		
		jl.stop();
	}
	
	/**
	 * Test that the server can handle at least a few requests simultaneously.
	 */
	@Test
	public void testMultiThreading() {
		JettyLauncher jl = new JettyLauncher(new File("test"), 9627);
		jl.setup();		
		jl.addServlet("/*", new MyTestServlet());		
		Log.i("web", "...Launching Jetty web server on port "+jl.getPort());
		jl.run();

		SafeExecutor ex = new SafeExecutor(Executors.newFixedThreadPool(5));
		AtomicInteger maxThreads = new AtomicInteger(-1);
		for(int i=0; i<5; i++) {
			final int fi = i;
			ex.submit(() -> {
				FakeBrowser fb = new FakeBrowser();
				String ok = fb.getPage("http://localhost:9627/test.json?thread="+fi);
				assert ok != null;
				int thread = Integer.parseInt(ok);
				int mt = maxThreads.get();
				while( ! maxThreads.compareAndSet(mt, Math.max(thread, mt)));
				return ok;
			});
		}		
		ex.shutdown();
		ex.awaitTermination();
		
		jl.stop();
		assert maxThreads.get() > 1 : maxThreads;
	}

	@Test
	public void testBlankLaunch() {
		JettyLauncher jl = new JettyLauncher(new File("test"), 9628);
		jl.setup();				
		Log.i("web", "...Launching Jetty web server on port "+jl.getPort());
		jl.run();

		FakeBrowser fb = new FakeBrowser();
		String ok = fb.getPage("http://localhost:9628/StaticHello.html");
		
		jl.stop();
	}

}


class MyTestServlet extends HttpServlet {
	
	private static Set<Thread> threads = new HashSet();

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		try {
			WebRequest state = new WebRequest(this, req, resp);

			// testing multi-threading?
			String thread = state.get("thread");
			if (thread!=null) {
				threads.add(Thread.currentThread());
				Utils.sleep(50);
				WebUtils2.sendText(""+threads.size(), state.getResponse());
				return;
			}

			System.out.println(state);
			String site = state.get("site");
			Checkbox cb = new Checkbox("testCheckbox");
			Boolean off = state.get(cb);
			System.out.println(off);
			
			String prev = (String) state.getSession().getAttribute("previous");
			HttpSession session = state.getSession();
			session.setAttribute("previous", site);
						
			WebUtils2.sendText("OK: "+site+"\nPrevious: "+prev, state.getResponse());;			
		} catch(Throwable ex) {
			ex.printStackTrace();
			throw Utils.runtime(ex);
		}
	}
}