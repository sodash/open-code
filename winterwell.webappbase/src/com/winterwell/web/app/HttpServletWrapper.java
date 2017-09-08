package com.winterwell.web.app;

import java.io.IOException;
import java.util.function.Supplier;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.winterwell.utils.Utils;

public class HttpServletWrapper extends HttpServlet {
	private static final long serialVersionUID = 1L;
	
	Supplier<IServlet> factory;
	
	public HttpServletWrapper(Supplier<IServlet> factory) {
		this.factory = factory;
	}

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		try {
			WebRequest state = new WebRequest(req, resp);
			factory.get().process(state);
		} catch (IOException | ServletException e) {
			throw e;
		} catch (Exception e) {
			throw Utils.runtime(e);
		} finally {
			WebRequest.close(req, resp);
		}
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		doGet(req, resp);
	}
	
	@Override
	protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		doGet(req, resp);
	}
	
	@Override
	protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		doGet(req, resp);
	}
	
//	@Override
//	protected void doHead(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
//		// TODO Auto-generated method stub
//		super.doHead(req, resp);
//	}
}
