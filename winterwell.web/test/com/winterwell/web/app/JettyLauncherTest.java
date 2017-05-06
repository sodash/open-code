package com.winterwell.web.app;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.Test;

import com.winterwell.utils.Utils;
import com.winterwell.utils.log.Log;
import com.winterwell.utils.web.WebUtils2;
import com.winterwell.web.FakeBrowser;
import com.winterwell.web.fields.Checkbox;

public class JettyLauncherTest {

	@Test
	public void test() {
		JettyLauncher jl = new JettyLauncher(new File("test"), 9627);
		jl.setup();		
		jl.addServlet("/*", new MyTestServlet());		
		Log.i("web", "...Launching Jetty web server on port "+jl.getPort());
		jl.run();

		FakeBrowser fb = new FakeBrowser();
		String ok = fb.getPage("http://localhost:9627/test.json?site=%%SITE%%");
		assert ok != null;
		assert ok.equals("OK: null") : ok;
		

		String ok2 = fb.getPage("http://localhost:9627/test.json?site=foo");
		assert ok2 != null;
		assert ok2.equals("OK: foo") : ok;
	}

}


class MyTestServlet extends HttpServlet {
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		try {
			WebRequest state = new WebRequest(this, req, resp);
			System.out.println(state);
			String site = state.get("site");
			Checkbox cb = new Checkbox("testCheckbox");
			Boolean off = state.get(cb);
			System.out.println(off);
			WebUtils2.sendText("OK: "+site, state.getResponse());;
		} catch(Throwable ex) {
			ex.printStackTrace();
			throw Utils.runtime(ex);
		}
	}
}