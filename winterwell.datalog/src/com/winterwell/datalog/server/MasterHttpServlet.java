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
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.IdsQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.TermQueryBuilder;

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
import com.winterwell.web.app.WebRequest;
import com.winterwell.web.fields.AField;
import com.winterwell.web.fields.Checkbox;
import com.winterwell.web.fields.JsonField;
import com.winterwell.web.fields.SField;

import com.winterwell.datalog.Stat;
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
	private static final SField TYPE = new SField("type");

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		doGet(req, resp);
	}
	
	public MasterHttpServlet() {
	}
	
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		WebRequest request = new WebRequest(null, req, resp);
		String path = req.getServletPath();
		String rpath = req.getRealPath("");
		String ptrans = req.getPathTranslated();
		String pathu = req.getRequestURI();
		StringBuffer pathu2 = req.getRequestURL();
		String cpath = req.getContextPath();
		String pi = req.getPathInfo();
		// Log (low overhead)
		if (path.startsWith("/lg")) {
			LogServlet.fastLog(new WebRequest(null, req, resp));
			return;
		}
		// Tripwire (low overhead)
		if (path.startsWith("/trk")) {
			new Img0Servlet().doGet(req, resp);
			return;
		}

		WebUtils2.CORS(request, true);		
		try {
			// Project
			if (request.getSlug()==null) throw new WebEx.E404(request.getRequestUrl(), "You must specify a project");
			String project = request.getSlugBits()[0];
			request.put(new Key("project"), project);
			// What to do?			
			if (request.actionIs("open_file")) {
				doOpenFile(request);
				return;
			}
			if (request.getSlugBits().length>1 && "view".equals(request.getSlugBits()[1])) {
				doOpenFile(request);
				return;
			}
			if (request.getAction()!=null) {
				JsonResponse output = doAction(request);
				WebUtils2.sendJson(output, request);
				return;
			}
			// view an example?
			String id = request.getSlugBits(1);
			if (id!=null) {
				ESHttpClient ec = new ESHttpClient(DataExperimentServer.esconfig);
				Map<String, Object> obj = ec.get(project, "experiment", id);
				JsonResponse output = new JsonResponse(request, obj);
				WebUtils2.sendJson(output, request);
				return;
			}
			
			JsonResponse output = new JsonResponse(request, null);
			WebUtils2.sendJson(output, request);
		} catch(Throwable ex) {
			Log.e("web", ex);
			doSendError(request, ex);
		}
	}

	private void doSendError(WebRequest request, Throwable ex) {
		WebPage page = new WebPage();
		page.append("<h1>Error: "+ex.getClass().getSimpleName()+"</h1>");
		String sex = Printer.toString(ex, true);
		page.append("<pre>"+sex+"</pre>");
		request.setPage(page);
		request.sendPage();
	}

	private JsonResponse doAction(WebRequest request) {		
		if (request.actionIs("record")) {
			return doReport(request, true);
		}
		if (request.actionIs("upload")) {
			return doUpload(request);
		}
		if (request.actionIs("check")) {
			return doReport(request, false);
		}
		if (request.actionIs("get")) {
			return doQuery(request);
		}
		if (request.actionIs("delete")) {
			return doDelete(request);
		}
		// TODO Auto-generated method stub
		return null;
	}

	private JsonResponse doUpload(WebRequest request) {
		request.processMultipartIncoming(null);
		String id = request.getSlugBits(2);		
		Map<String, Object> pmap = request.getParameterMap();
		Object files = request.getParameterMap().get("files");
		Object filenames = request.getParameterMap().get("files-filename");		
		if (files instanceof File) {
			File dest = new File("uploads/"+request.getSlug(), filenames.toString());
			// save the file into ES??
//			if ("model.json".equals(filenames)) {
//				ESHttpClient ec = new ESHttpClient(DataExperimentServer.esconfig);
//				UpdateRequestBuilder up = ec.prepareUpdate("assist", "experiment", id);
//				up.setDoc(json)
//				IESResponse r = up.get();
//				System.out.println(r);
//			}
			dest.mkdirs();
			FileUtils.move((File)files, dest);
		}
		return new JsonResponse(request, null);
	}

	private void doOpenFile(WebRequest request) {
		String f = request.get("file");
		File file;
		if (f==null) {
			String path = request.getRequestPath();
			path = path.replace("/assist/view/", "/");
			System.out.println(path);
			f = path;
		}
		// HACK: modify old file paths
		f = f.replace("/home/daniel/winterwell/", "");
		file = new File(f).getAbsoluteFile();
		Log.d("Serve file: "+file);
		assert file.getPath().startsWith(FileUtils.getWorkingDirectory().getAbsolutePath()) : file;
		try {
			FileServlet.serveFile(file, request);
		} catch (IOException e) {
			throw Utils.runtime(e);
		}
	}

	private JsonResponse doDelete(WebRequest request) {
		String project = request.get("project");
		String id = request.get("id");
		assert id != null;
		ESHttpClient ec = new ESHttpClient(DataExperimentServer.esconfig);
		IndexRequestBuilder ir = ec.prepareIndex(project, request.get(TYPE, "experiment"), id);
		ArrayMap map = new ArrayMap("status","trash");
		ir.setSource(map);
		IESResponse resp = ir.get();
		return new JsonResponse(request, null);
	}

	private JsonResponse doQuery(WebRequest request) {
		ESHttpClient ec = new ESHttpClient(DataExperimentServer.esconfig);
		SearchRequestBuilder search = ec.prepareSearch(request.get("project"));
		search.setType(request.get(TYPE, "experiment"));
		search.setSourceInclude("name","results","storageTime", "hash", "spec.hash", "spec.output_dir");
		search.setSize(10000);
		// no trash
		if ( ! "trash".equals(request.get("status"))) {
			QueryBuilder fb = new BoolQueryBuilder().mustNot(new TermQueryBuilder("status", "trash"));
			search.setQuery(fb);
		}		
		SearchResponse results = search.get();
		
		// purge
		int delcnt=0;
		ListMap<String,Map> named2rs = new ListMap();
		List<Map> hits = results.getHits();
		for(Map hit : hits) {
			String name = SimpleJson.get(hit, "_source", "name");
			if (name==null) continue;
			named2rs.add(name, hit);
		}
		for(String name : named2rs.keySet()) {
			List<Map> rs = named2rs.get(name);
			if (rs.size() < 1) continue;
			BestOne<Map> latest = new BestOne<>();
			for (Map map : rs) {
				String st = SimpleJson.get(map, "_source", "storageTime");
				if (st==null) continue;
				Time t = new Time(st);
				double score = t.getTime();
				latest.maybeSet(map, score);
			}
			rs.remove(latest.getBest());
			for (Map map : rs) {
				DeleteRequestBuilder del = new DeleteRequestBuilder(ec);
				String id = (String) map.get("_id");
				if (id==null) continue;
				del.setIndex("assist");
				del.setType("experiment");
				del.setId(id);
				IESResponse delr = del.get();
				String j = delr.getJson();
				delcnt++;
				hits.remove(map);
			}
		}
		System.out.println("DELCNT: "+delcnt);
		
		System.out.println("JSON length: "+Printer.prettyNumber(results.getJson().length())+" Total: "+results.getTotal()+" Hits: "+results.getHits().size());
		return new JsonResponse(request, hits);
	}

	private JsonResponse doReport(WebRequest request, boolean doReport) {
		String project = request.get("project");
		assert project!=null;
		Map map = (Map) JSON.parse(request.getPostBody());
		map.put("storageTime", new Time().toISOString());
		Map spec = (Map) map.get("spec");
		Map scores = (Map) map.get("results");
		String name = (String) Utils.or(request.get("name"), spec.get("name"), "experiment");
		Desc<Experiment> desc = new Desc<>(name, Experiment.class);
		desc.setCheckValueFlag(false);
		desc.setTag(project);
		desc.putAll(spec);
		ESHttpClient ec = new ESHttpClient(DataExperimentServer.esconfig);
		String id = StrUtils.md5(desc.getId());
		String type = request.get(TYPE, "experiment");
		if (doReport) {
			// store it in ES
			IndexRequestBuilder ir = ec.prepareIndex(project, type, id);
			ir.setSource(map);
			IESResponse resp = ir.get();
			Map<String, Object> cargo = resp.getParsedJson();
			if (request.get(new Checkbox("purge"), false)) {
				doPurgeOthers(id, map);
			}
			return new JsonResponse(request, cargo);
		}
		// check for it
		Map<String, Object> obj = ec.get(project, type, id);
		JsonResponse jr = new JsonResponse(request, obj);
		jr.setSuccess(obj!=null);
		return jr;
	}

	private void doPurgeOthers(String id, Map map) {
		Object name = map.get("name");
		if (name==null) return;
		ESHttpClient ec = new ESHttpClient(DataExperimentServer.esconfig);
		DeleteByQueryRequestBuilder del = new DeleteByQueryRequestBuilder(ec, "assist");
		del.setType("experiment");
		QueryBuilder fb = 
				new BoolQueryBuilder()
					.mustNot(new IdsQueryBuilder().addIds(id))
					.must(new TermQueryBuilder("name", name));
		del.setQuery(fb);
		IESResponse response = del.get();
		return;
	}
	
}
