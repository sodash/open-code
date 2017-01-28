//package com.winterwell.datalog.server;
//
//import java.io.File;
//import java.io.IOException;
//import java.util.List;
//import java.util.Map;
//
//import org.eclipse.jetty.util.ajax.JSON;
//
//import com.winterwell.datascience.Experiment;
//import com.winterwell.depot.Desc;
//import com.winterwell.es.client.DeleteByQueryRequestBuilder;
//import com.winterwell.es.client.DeleteRequestBuilder;
//import com.winterwell.es.client.ESHttpClient;
//import com.winterwell.es.client.IESResponse;
//import com.winterwell.es.client.IndexRequestBuilder;
//import com.winterwell.es.client.SearchRequestBuilder;
//import com.winterwell.es.client.SearchResponse;
//import com.winterwell.utils.BestOne;
//import com.winterwell.utils.Printer;
//import com.winterwell.utils.StrUtils;
//import com.winterwell.utils.Utils;
//import com.winterwell.utils.containers.ArrayMap;
//import com.winterwell.utils.containers.ListMap;
//import com.winterwell.utils.io.FileUtils;
//import com.winterwell.utils.log.Log;
//import com.winterwell.utils.time.Time;
//import com.winterwell.utils.web.SimpleJson;
//import com.winterwell.utils.web.WebUtils2;
//import com.winterwell.web.WebPage;
//import com.winterwell.web.ajax.JsonResponse;
//import com.winterwell.web.app.FileServlet;
//import com.winterwell.web.app.WebRequest;
//import com.winterwell.web.fields.Checkbox;
//import com.winterwell.web.fields.SField;
//
//public class ExperimentServlet {
//
//	private static final SField TYPE = new SField("type");
//
//
//	private JsonResponse doDelete(WebRequest request) {
//		String project = request.get("project");
//		String id = request.get("id");
//		assert id != null;
//		ESHttpClient ec = new ESHttpClient(DataLogServer.esconfig);
//		IndexRequestBuilder ir = ec.prepareIndex(project, request.get(TYPE, "experiment"), id);
//		ArrayMap map = new ArrayMap("status","trash");
//		ir.setSource(map);
//		IESResponse resp = ir.get();
//		return new JsonResponse(request, null);
//	}
//
//	private JsonResponse doQuery(WebRequest request) {
//		ESHttpClient ec = new ESHttpClient(DataLogServer.esconfig);
//		SearchRequestBuilder search = ec.prepareSearch(request.get("project"));
//		search.setType(request.get(TYPE, "experiment"));
//		search.setSourceInclude("name","results","storageTime", "hash", "spec.hash", "spec.output_dir");
//		search.setSize(10000);
//		// no trash
//		if ( ! "trash".equals(request.get("status"))) {
//			QueryBuilder fb = new BoolQueryBuilder().mustNot(new TermQueryBuilder("status", "trash"));
//			search.setQuery(fb);
//		}		
//		SearchResponse results = search.get();
//		
//		// purge
//		int delcnt=0;
//		ListMap<String,Map> named2rs = new ListMap();
//		List<Map> hits = results.getHits();
//		for(Map hit : hits) {
//			String name = SimpleJson.get(hit, "_source", "name");
//			if (name==null) continue;
//			named2rs.add(name, hit);
//		}
//		for(String name : named2rs.keySet()) {
//			List<Map> rs = named2rs.get(name);
//			if (rs.size() < 1) continue;
//			BestOne<Map> latest = new BestOne<>();
//			for (Map map : rs) {
//				String st = SimpleJson.get(map, "_source", "storageTime");
//				if (st==null) continue;
//				Time t = new Time(st);
//				double score = t.getTime();
//				latest.maybeSet(map, score);
//			}
//			rs.remove(latest.getBest());
//			for (Map map : rs) {
//				DeleteRequestBuilder del = new DeleteRequestBuilder(ec);
//				String id = (String) map.get("_id");
//				if (id==null) continue;
//				del.setIndex("assist");
//				del.setType("experiment");
//				del.setId(id);
//				IESResponse delr = del.get();
//				String j = delr.getJson();
//				delcnt++;
//				hits.remove(map);
//			}
//		}
//		System.out.println("DELCNT: "+delcnt);
//		
//		System.out.println("JSON length: "+Printer.prettyNumber(results.getJson().length())+" Total: "+results.getTotal()+" Hits: "+results.getHits().size());
//		return new JsonResponse(request, hits);
//	}
//
//	private JsonResponse doReport(WebRequest request, boolean doReport) {
//		String project = request.get("project");
//		assert project!=null;
//		Map map = (Map) JSON.parse(request.getPostBody());
//		map.put("storageTime", new Time().toISOString());
//		Map spec = (Map) map.get("spec");
//		Map scores = (Map) map.get("results");
//		String name = (String) Utils.or(request.get("name"), spec.get("name"), "experiment");
//		Desc<Experiment> desc = new Desc<>(name, Experiment.class);
//		desc.setCheckValueFlag(false);
//		desc.setTag(project);
//		desc.putAll(spec);
//		ESHttpClient ec = new ESHttpClient(DataLogServer.esconfig);
//		String id = StrUtils.md5(desc.getId());
//		String type = request.get(TYPE, "experiment");
//		if (doReport) {
//			// store it in ES
//			IndexRequestBuilder ir = ec.prepareIndex(project, type, id);
//			ir.setSource(map);
//			IESResponse resp = ir.get();
//			Map<String, Object> cargo = resp.getParsedJson();
//			if (request.get(new Checkbox("purge"), false)) {
//				doPurgeOthers(id, map);
//			}
//			return new JsonResponse(request, cargo);
//		}
//		// check for it
//		Map<String, Object> obj = ec.get(project, type, id);
//		JsonResponse jr = new JsonResponse(request, obj);
//		jr.setSuccess(obj!=null);
//		return jr;
//	}
//
//	private void doPurgeOthers(String id, Map map) {
//		Object name = map.get("name");
//		if (name==null) return;
//		ESHttpClient ec = new ESHttpClient(DataLogServer.esconfig);
//		DeleteByQueryRequestBuilder del = new DeleteByQueryRequestBuilder(ec, "assist");
//		del.setType("experiment");
//		QueryBuilder fb = 
//				new BoolQueryBuilder()
//					.mustNot(new IdsQueryBuilder().addIds(id))
//					.must(new TermQueryBuilder("name", name));
//		del.setQuery(fb);
//		IESResponse response = del.get();
//		return;
//	}
//
//	
//	doGet() {
//		try {
//			// Project
//			// What to do?			
//			if (request.actionIs("open_file")) {
//				doOpenFile(request);
//				return;
//			}
//			if (request.getSlugBits().length>1 && "view".equals(request.getSlugBits()[1])) {
//				doOpenFile(request);
//				return;
//			}
//			if (request.getAction()!=null) {
//				JsonResponse output = doAction(request);
//				WebUtils2.sendJson(output, request);
//				return;
//			}
//			// view an example?
//			String id = request.getSlugBits(1);
//			if (id!=null) {
//				ESHttpClient ec = new ESHttpClient(DataLogServer.esconfig);
//				Map<String, Object> obj = ec.get(project, "experiment", id);
//				JsonResponse output = new JsonResponse(request, obj);
//				WebUtils2.sendJson(output, request);
//				return;
//			}
//			
//			JsonResponse output = new JsonResponse(request, null);
//			WebUtils2.sendJson(output, request);
//		} catch(Throwable ex) {
//			Log.e("web", ex);
//			doSendError(request, ex);
//		}
//	}
//
//	private void doSendError(WebRequest request, Throwable ex) {
//		WebPage page = new WebPage();
//		page.append("<h1>Error: "+ex.getClass().getSimpleName()+"</h1>");
//		String sex = Printer.toString(ex, true);
//		page.append("<pre>"+sex+"</pre>");
//		request.setPage(page);
//		request.sendPage();
//	}
//
//	private JsonResponse doAction(WebRequest request) {		
//		if (request.actionIs("record")) {
//			return doReport(request, true);
//		}
//		if (request.actionIs("upload")) {
//			return doUpload(request);
//		}
//		if (request.actionIs("check")) {
//			return doReport(request, false);
//		}
//		if (request.actionIs("get")) {
//			return doQuery(request);
//		}
//		if (request.actionIs("delete")) {
//			return doDelete(request);
//		}
//		// TODO Auto-generated method stub
//		return null;
//	}
//
//
//	private JsonResponse doUpload(WebRequest request) {
//		request.processMultipartIncoming(null);
//		String id = request.getSlugBits(2);		
//		Map<String, Object> pmap = request.getParameterMap();
//		Object files = request.getParameterMap().get("files");
//		Object filenames = request.getParameterMap().get("files-filename");		
//		if (files instanceof File) {
//			File dest = new File("uploads/"+request.getSlug(), filenames.toString());
//			// save the file into ES??
////			if ("model.json".equals(filenames)) {
////				ESHttpClient ec = new ESHttpClient(DataExperimentServer.esconfig);
////				UpdateRequestBuilder up = ec.prepareUpdate("assist", "experiment", id);
////				up.setDoc(json)
////				IESResponse r = up.get();
////				System.out.println(r);
////			}
//			dest.mkdirs();
//			FileUtils.move((File)files, dest);
//		}
//		return new JsonResponse(request, null);
//	}
//
//	private void doOpenFile(WebRequest request) {
//		String f = request.get("file");
//		File file;
//		if (f==null) {
//			String path = request.getRequestPath();
//			path = path.replace("/assist/view/", "/");
//			System.out.println(path);
//			f = path;
//		}
//		// HACK: modify old file paths
//		f = f.replace("/home/daniel/winterwell/", "");
//		file = new File(f).getAbsoluteFile();
//		Log.d("Serve file: "+file);
//		assert file.getPath().startsWith(FileUtils.getWorkingDirectory().getAbsolutePath()) : file;
//		try {
//			FileServlet.serveFile(file, request);
//		} catch (IOException e) {
//			throw Utils.runtime(e);
//		}
//	}
//	
//}
