package com.winterwell.datalog.server;

import java.io.File;
import java.io.IOException;
import java.net.URLDecoder;
import java.util.*;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.util.ajax.JSON;

import com.winterwell.utils.log.Log;
import com.winterwell.utils.time.Time;
import com.winterwell.maths.classifiers.WilsonScoreInterval;
import com.winterwell.maths.datastorage.HalfLifeMap;
import com.winterwell.maths.stats.distributions.cond.Cntxt;
import com.winterwell.maths.stats.distributions.cond.FrequencyCondDistribution;
import com.winterwell.maths.stats.distributions.discrete.ObjectDistribution;
import com.winterwell.utils.BestOne;
import com.winterwell.utils.IBuildStrings;
import com.winterwell.utils.Key;
import com.winterwell.utils.Printer;
import com.winterwell.utils.StrUtils;
import com.winterwell.utils.TodoException;
import com.winterwell.utils.Utils;
import com.winterwell.utils.containers.Range;
import com.winterwell.utils.io.FileUtils;
import com.winterwell.web.app.FileServlet;
import com.winterwell.web.app.ManifestServlet;
import com.winterwell.web.app.WebRequest;
import com.winterwell.web.fields.AField;
import com.winterwell.web.fields.Checkbox;
import com.winterwell.web.fields.JsonField;
import com.winterwell.web.fields.SField;

import com.winterwell.datalog.DataLog;
import com.winterwell.datascience.Experiment;
import com.winterwell.depot.Desc;
import com.winterwell.es.client.DeleteByQueryRequestBuilder;
import com.winterwell.es.client.DeleteRequestBuilder;
import com.winterwell.es.client.ESHttpClient;
import com.winterwell.es.client.IESResponse;
import com.winterwell.es.client.IndexRequestBuilder;
import com.winterwell.es.client.SearchRequestBuilder;
import com.winterwell.es.client.SearchResponse;
import com.winterwell.es.client.UpdateRequestBuilder;
import com.winterwell.utils.containers.ArrayMap;
import com.winterwell.utils.containers.ListMap;
import com.winterwell.utils.web.SimpleJson;
import com.winterwell.utils.web.WebUtils2;
import com.winterwell.web.WebEx;
import com.winterwell.web.WebInputException;
import com.winterwell.web.WebPage;
import com.winterwell.web.ajax.JsonResponse;

/**
 *
 */
public class MasterHttpServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		doGet(req, resp);
	}
	
	public MasterHttpServlet() {
	}
	
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		WebRequest request = null;
		try {
			request = new WebRequest(null, req, resp);			
			String path = request.getRequestPath();
	
			// Tracking pixel
			if (path.startsWith("/pxl")) {
				new TrackingPixelServlet().doGet(req, resp);
				return;
			}		
			// Log
			if (path.startsWith("/lg")) {
				LgServlet.fastLog(request);
				return;
			}
			// No favicon ??and block other common requests??
			if (path.startsWith("/favicon")) {
				WebUtils2.sendError(404, "", resp);
				return;
			}
	
			// cors on
			if (DataLogServer.settings.CORS) {
				WebUtils2.CORS(request, true);
			}
			
			// which dataspace?
			if (request.getSlug()==null) {
				throw new WebEx.E404(request.getRequestUrl(), "You must specify a project");
			}
			String project = request.getSlugBits()[0];
			request.put(new Key("project"), project);
			
			// data/stats explorer
			if (path.startsWith("/data")) {
				new DataServlet().process(request);
				return;
			}
			
			// TODO experiment reports table
			
			// TODO experiment reports details
			
			// TODO dataspace admin
			
			if (path.equals("/ping")) {
				new PingServlet(request).doGet();
				return;
			}
			if (path.equals("/manifest")) {
				new ManifestServlet().process(request);
				return;
			}
			
			WebUtils2.sendError(500, "TODO", resp);
		} catch(Throwable ex) {
			Log.e("error", ex);
			WebUtils2.sendError(500, "Server Error: "+ex, resp);
		} finally {
			WebRequest.close(req, resp);
		}
	}

}
