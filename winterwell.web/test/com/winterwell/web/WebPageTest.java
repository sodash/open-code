package com.winterwell.web;

import java.io.IOException;

import junit.framework.TestCase;

public class WebPageTest extends TestCase {

	public void testBasic1() {
		WebPage wp = new WebPage();
		assertEquals(wp.toString(),
				"<!DOCTYPE html PUBLIC \"-//W3C//DTD HTML 4.01 Transitional//EN\" \"http://www.w3.org/TR/html4/loose.dtd\">\n"
						+ "<html>\n<head>\n"
						+ "<meta http-equiv='Content-Type' content='text/html; charset=utf-8'>\n"
						+ "<title></title>\n</head>\n"
						+ "<body>\n</body>\n</html>\n");
		assert wp.getBody().equals("");
	}

	public void testBasic2() {
		WebPage wp = new WebPage();
		String body = "<br/>";
		wp.append(body);
		assertEquals(wp.toString(),
				"<!DOCTYPE html PUBLIC \"-//W3C//DTD HTML 4.01 Transitional//EN\" \"http://www.w3.org/TR/html4/loose.dtd\">\n"
						+ "<html>\n<head>\n"
						+ "<meta http-equiv='Content-Type' content='text/html; charset=utf-8'>\n"
						+ "<title></title>\n</head>\n"
						+ "<body>\n<br/></body>\n</html>\n");
		assert wp.getBody().equals("<br/>");
	}

	public void testBasic3() throws IOException {
		WebPage wp = new WebPage();
		wp.append("<br>");
		wp.append('a');
		wp.append("</br>");
		assertEquals(wp.toString(),
				"<!DOCTYPE html PUBLIC \"-//W3C//DTD HTML 4.01 Transitional//EN\" \"http://www.w3.org/TR/html4/loose.dtd\">\n"
						+ "<html>\n<head>\n"
						+ "<meta http-equiv='Content-Type' content='text/html; charset=utf-8'>\n"
						+ "<title></title>\n</head>\n"
						+ "<body>\n<br>a</br></body>\n</html>\n");
		assert wp.getBody().equals("<br>a</br>");
	}

	public void testBasic4() throws IOException {
		WebPage wp = new WebPage();
		String body = "<br><h1>a</h1></br>";
		wp.append(body, 4, 14);
		assertEquals(wp.toString(),
				"<!DOCTYPE html PUBLIC \"-//W3C//DTD HTML 4.01 Transitional//EN\" \"http://www.w3.org/TR/html4/loose.dtd\">\n"
						+ "<html>\n<head>\n"
						+ "<meta http-equiv='Content-Type' content='text/html; charset=utf-8'>\n"
						+ "<title></title>\n</head>\n"
						+ "<body>\n<h1>a</h1></body>\n</html>\n");
		assert wp.getBody().equals("<h1>a</h1>");
	}

	public void testSetTitle() {
		WebPage wp = new WebPage();
		wp.setTitle("new title");
		assertEquals(wp.toString(),
				"<!DOCTYPE html PUBLIC \"-//W3C//DTD HTML 4.01 Transitional//EN\" \"http://www.w3.org/TR/html4/loose.dtd\">\n"
						+ "<html>\n<head>\n"
						+ "<meta http-equiv='Content-Type' content='text/html; charset=utf-8'>\n"
						+ "<title>new title</title>\n</head>\n"
						+ "<body>\n</body>\n</html>\n");
		assert wp.getTitle().equals("new title");
	}
}
