package com.winterwell.web.app;

import java.io.IOException;

public interface IServlet {

	public void process(WebRequest state) throws IOException;
	
}
