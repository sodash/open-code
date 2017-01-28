//package com.winterwell.datalog.server;
//
//import java.util.ArrayList;
//import java.util.Arrays;
//import java.util.Collections;
//import java.util.List;
//import java.util.Map;
//import java.util.concurrent.Callable;
//import java.util.regex.Pattern;
//
//import org.eclipse.jetty.util.ajax.JSON;
//
//import com.winterwell.maths.chart.Chart;
//import com.winterwell.maths.chart.CombinationChart;
//import com.winterwell.maths.chart.RenderWithFlot;
//import com.winterwell.maths.chart.TimeSeriesChart;
//import com.winterwell.maths.timeseries.DataUtils;
//import com.winterwell.maths.timeseries.Datum;
//import com.winterwell.maths.timeseries.IDataStream;
//import com.winterwell.maths.timeseries.ListDataStream;
//
//import com.winterwell.utils.Constant;
//
//import com.winterwell.utils.StrUtils;
//
//import com.winterwell.utils.containers.ArrayMap;
//
//import com.winterwell.utils.log.Log;
//import com.winterwell.utils.time.Dt;
//import com.winterwell.utils.time.TUnit;
//import com.winterwell.utils.time.Time;
//
//import com.winterwell.web.FakeBrowser;
//
//import com.winterwell.web.app.WebRequest;
//import com.winterwell.web.fields.AField;
//import com.winterwell.web.fields.Checkbox;
//import com.winterwell.web.fields.Form;
//import com.winterwell.web.fields.ListField;
//import com.winterwell.web.fields.SField;
//
//import com.winterwell.datalog.Rate;
//import com.winterwell.datalog.Stat;
//import com.winterwell.datalog.Stat.KInterpolate;
//import com.winterwell.datalog.StatImpl;
//import com.winterwell.utils.threads.IFuture;
//import com.winterwell.utils.web.WebUtils2;
//import com.winterwell.web.ajax.JsonResponse;
//
//import creole.data.Security.KAccess;
//import creole.data.events.ExceptionReporter;
//import creole.plugins.alerts.AlertsServlet;
//import creole.plugins.facebook.FacebookPlugin;
//import creole.plugins.guts.PluginUtils;
//import creole.plugins.msg.Email;
//import creole.plugins.msg.SearchManagerServlet;
//import creole.plugins.notify.Notification;
//import creole.plugins.shard.ShardPlugin;
//import creole.plugins.workspace.WorkspacePlugin;
//import creole.servletguts.APageServlet;
//import creole.servletguts.Fields;
//import creole.servletguts.PageBuilder;
//import creole.servletguts.RequestState;
//import creole.servletguts.widgets.LinkWidget;
//import creole.servletguts.widgets.filters.DateFilter;
//
///**
// * Explore the DataLog/Stat values.
// * 
// * JSON inputs: See api.txt 
// * 
// * @author daniel
// *
// */
//public class StatServlet extends APageServlet {
//
//	private static final Checkbox PREVIOUS = new Checkbox("previous");
//
//	public static Map getStatWithPrevious(String statKey, Time start, Time end) {
//		// Get two weeks (or months) to give % change
//		Dt dt = start.dt(end);
//		Time preStart = start.minus(dt);
//		assert preStart.isBefore(start) && start.isBefore(end) : preStart+" "+start+" "+end;
//		// get the data-points raw, and sum them here
//		IFuture<Iterable> stats = Stat.getData(preStart, end, null, null, statKey);
//		Map map = getStatWithPrevious2(stats, statKey, true, start);
//		return map;
//	}
//
//	private static Map getStatWithPrevious2(IFuture<Iterable> stats, String statKey, boolean sumThem, Time start) {
//		ListDataStream data = new ListDataStream((IDataStream)stats.get());
//		// Sum them		
//		double thisBucket = 0, prevBucket = 0;		
//		for (Datum datum : data) {			
//			// skip zeroes
//			if (datum.x()==0) continue;
//			if (datum.getTime().isBefore(start)) {
//				prevBucket = sumThem? prevBucket + datum.x() : datum.x();
//			} else {
//				thisBucket = sumThem? thisBucket + datum.x() : datum.x();
//			}
//		}
//		if (data.isEmpty()) return null;		
//				
//	//	Datum last = data.get(data.size()-1);
//	//	Datum prev = data.size() > 1? data.get(0) : null;
//		Map smap = new ArrayMap(
//				"value", thisBucket,
//				"previous", prevBucket,				
//				"url", StatServlet.getUrl(statKey)
//				);
//		return smap;
//	}
//
//	
//	
//	@Override
//	public void processIncoming(RequestState state) throws Exception {
//		if (state.actionIs("count")) {
//			String tag = state.get(new AField<String>("tag"));
//			String[] tagBits = tag.split(""+Stat.HIERARCHY_CHAR);
//			Stat.count(1, tagBits);
//			Log.d("stat", "Counted web request "+tag+" +1 "+state);
//			Map cargo = new ArrayMap();
//			Rate rate = Stat.get(tag);
//			cargo.put(tag, rate==null? 0 : rate.x);
//			JsonResponse jr = new JsonResponse(state, cargo);
//			WebUtils2.sendJson(jr, state);
//		}
//	}
//	
//	public StatServlet() {
//		super(KAccess.ANYUSER);
//	}
//	
//	@Override
//	protected boolean allowCORS(RequestState state) {
//		return true;
//	}
//
//	public static ListField<String> DATALABELS = new ListField<String>("labels", null);
//	
//	/**
//	 * Inputs: 
//	 * cargo is e.g.
//	 * [{data for label1}, {data for label2}]
//	 * where {data for label1} =
//	 * {
//	 *  label: ""
//	 *  total: 
//	 *  data: []
//	 * }
//	 */
//	@Override
//	protected JsonResponse displayJson(RequestState state) throws Exception {
//		// HACK: cross-workspace??
//		Object ws = state.get(new AField("workspaces"));
//		if ("children".equals(ws)) {
//			List<String> kids = WorkspacePlugin.getChildWorkspaces(state.getWorkspace());
//			state.addMessage("Stats for child workspaces: "+kids);
//			JsonResponse jr = fetchCrossWorkspace(kids, state);
//			return jr;			
//		}
//		
//		List<String> labels = state.getRequired(DATALABELS);
//				
//		// HACK: access the current bucket
//		if (state.actionIs("current") || state.actionIs("count")) {
//			Map cargo = new ArrayMap();
//			for (String label : labels) {
//				Rate rate = Stat.get(label);
//				
//				// DEBUG
//				if (FacebookPlugin.GLOBAL_FB_ALARM.equals(label) && rate!=null && rate.x!=0) {					
//					if (rate.x < 5) { // HACK enforce some buffering from smalldata card on the stable servers
//						Log.w("no-global-alarm", "Don't ring the globalFBAlarm yet for "+rate);
//						cargo.put(label, 0);
//						continue;
//					}
//					Log.w("global-alarm", "Ring the globalFBAlarm "+rate+" for request "+state);
//				}
//				
//				cargo.put(label, rate==null? 0 : rate.x);			
//			}
//			return new JsonResponse(state, cargo);
//		}
//		
//		Callable<Time> _end = state.get(DateFilter.UNTIL);
//		Time end = _end==null? new Time() : _end.call();
//		
//		Callable<Time> _start = state.get(DateFilter.SINCE);
//		// Default to 1 day
//		Time start = _start==null? end.minus(TUnit.DAY) : _start.call();
//		// Get the previous total for comparison?
//		Time preStart = null;
//		if (state.get(PREVIOUS)) {
//			Dt dt = start.dt(end);
//			preStart = start.minus(dt);
//			assert preStart.isBefore(start) && start.isBefore(end) : preStart+" "+start+" "+end;
//		}		
//
//		// Group data into buckets for efficient transfer & display? 
//		// Can be null!
//		Dt bucketSize = state.get(Fields.DT);		
//		
//		// a map per id (chart-line), with x=timestamp
//		List<IDataStream> datas = new ArrayList<IDataStream>();		
//		for (String lbl : labels) {			
////			TODO support getting mean/var data if (typeMean) {
////				datas.add(Stat.getMeanData(start, end, KInterpolate.LINEAR_1DAY, bucketSize, lbl));
////			} else {
//			IFuture<Iterable> fData = Stat.getData(preStart==null? start : preStart, end, KInterpolate.LINEAR_1DAY, bucketSize, lbl);
//			datas.add(StatImpl.exec(Collections.singletonList(fData)));
//		}
//		
//		// copy it out into chart-lines
//		List<Map> dlist = new ArrayList(labels.size());
//		
//		// TODO Do we want to support eg. Facebook likes, which we don't want to sum to give a total?
//		boolean sumThem = true;
//		
//		for(int i = 0; i < datas.size(); i++) {
//			ArrayList<double[]> vals = new ArrayList<double[]>();
//			ArrayMap data_i = new ArrayMap("data", vals, "label", labels.get(i));
//			
//			double total_i = 0, prevTotal_i=0;
//			Double last_i = null, prevLast_i = null;
//			for (Datum v : datas.get(i)) { // was just Datum v : datas - now datas is a list of 1D IDataStreams rather than a single multidimensional one 
//				double y = v.x(); //was v.get(i); - now each Datum is only 1D
//				if (v.getTime().isBefore(start)) {
//					prevTotal_i = sumThem? prevTotal_i + y : y;
//					prevLast_i = y;
//					continue;
//				}
//				double x = v.time.getTime();				
//				vals.add(new double[] { x, y });
//				total_i += y;
//				last_i = y;
//			}
//			
//			data_i.put("total", total_i);
//			data_i.put("previousTotal", prevTotal_i);
//			// last is the most-recent value -- useful for stats which don't sum (e.g. twitter-followers)
//			data_i.put("last", last_i);
//			data_i.put("previousLast", prevLast_i);
//			dlist.add(data_i);
//		}
//		
//		return new JsonResponse(state, dlist);		
//	}
//	
//	/**
//	 * 
//	 * @param kids
//	 * @param state
//	 * @return cargo is a List
//	 */
//	JsonResponse fetchCrossWorkspace(List<String> kids, WebRequest state) {
//		// TODO		
//		// make the same request to the kids
//		FakeBrowser fb = ShardPlugin.newFakeBrowser();
//		Map pmap = state.getMap();
//		pmap.put("as", "su");
//		pmap.remove("workspaces");
//		List<Map> allDataSeries = new ArrayList();
//		for (String kid : kids) {
//			try {
//				String url = ShardPlugin.getProtocol(kid)+"://"+kid+".soda.sh/report-stat.json";
//				String json = fb.getPage(url, pmap);
//				Map jobj = (Map) JSON.parse(json);
//				Object[] cargo = (Object[]) jobj.get("cargo");
//				for(Object _dataSeries : cargo) {
//					Map dataSeries = (Map) _dataSeries;
//					Object label = dataSeries.get("label");
//					dataSeries.put("label", kid+"/"+label);
//					allDataSeries.add(dataSeries);
//				}
//			} catch(Throwable ex) {
//				Notification.warning(ex, (RequestState) state);
//			}
//		}		
//		JsonResponse jr = new JsonResponse(state, allDataSeries);
//		return jr;
//	}
//
//	@Override
//	protected void display(PageBuilder page, RequestState state)
//			throws Exception {
//		page.setTitle("Stats!");
//		Form form = new Form(getPath());
//		form.setMethod("get");
//		form.startTable();
//		form.addRow("Which data-tags?", DATALABELS);
//		Callable<Time> since = state.get(DateFilter.SINCE);
//		if (since==null) since = new Constant(new Time().minus(2, TUnit.HOUR));
//		form.addRow("Start date/time", DateFilter.SINCE, since);
//		form.appendHtmlTo(page);
//		page.append("<hr>");
//		
//		List<String> labels = state.get(DATALABELS);
//		if (labels==null) {
//			// List them all
//			List<String> allLabels = new ArrayList(Stat.getActiveLabels());
//			// ...alphabetically
//			Collections.sort(allLabels);	
//			page.append("<ul>");
//			for (String string : allLabels) {
//				page.append("<li>");
//				LinkWidget lw = new LinkWidget(string, getPath(), new ArrayMap(
//						DATALABELS, Arrays.asList(string)));
//				lw.appendHtmlTo(page);
//				page.append(" "+Stat.get(string));
//				page.append("</li>");
//			}
//			page.append("</ul>");
//			return;
//		}
//		
//		if (labels.size()==1) {
//			// TODO Do we have an alert??
////			AlertsPlugin.dflt.getAlert(labels.get(0));			
//			page.append("<div><h3>Create email-to-"+Email.getEmail(state.getUser())+" alert?</h3>");
//			
//			page.append("<p>Existing alerts: See <a href='"+new AlertsServlet().getPath()+"'>/alerts-alerts</a></p>");
//			
//			Form aform = new Form(PluginUtils.getPath(AlertsServlet.class));
//			aform.setMethod("post");
//			aform.setAction(Fields.ACTION_NEW);
//			aform.startTable();
//			aform.addHidden(AlertsServlet.STAT_TAG, labels.get(0));
//			aform.addRow("Threshold (per day)", SearchManagerServlet.LIMIT);
//			aform.addRow("Trigger if ", AlertsServlet.BELOW);
//			aform.appendHtmlTo(page);
//			page.append("</div>");
//		}
//								
//		CombinationChart chart = new CombinationChart();		
//		Time end = new Time();
//		
//		int dps = 0;
//		List<IFuture<Iterable>> datas = new ArrayList();
//
//		// HACK: child workspaces
//		String ws = state.get(new SField("workspaces"));
//		if (ws!=null) {
//			List<String> kids;
//			if ("children".equals(ws)) {
//				kids = WorkspacePlugin.getChildWorkspaces(state.getWorkspace());
//			} else {
//				kids = StrUtils.split(ws);
//			}
//			state.addMessage("Stats for child workspaces: "+kids);
//			JsonResponse jr = fetchCrossWorkspace(kids, state);
//			// make data streams
//			List<Map> cargo = (List) jr.getCargo();
//			List<String> newLabels = new ArrayList();
//			for (String label : labels) {
//				for (Map ds : cargo) {
//					String dsLabel = (String) ds.get("label");
//					if (dsLabel!=null && dsLabel.endsWith(label)) {
//						// xy data
//						Object[] dsData = (Object[]) ds.get("data");
//						ListDataStream lds = new ListDataStream(2);
//						for (Object object : dsData) {
//							Object[] xy = (Object[]) object;
//							lds.add(new Datum(new double[]{(Double)xy[0], (Double)xy[1]}));
//						}						
//						IFuture<Iterable> fds = new Constant<Iterable>(lds);
//						datas.add(fds);
//						newLabels.add(dsLabel);
//					}
//				}
//			}
//			labels = newLabels;
//			// end hack
//			
//		} else { 		
//			final Time start = since.call();
//			for (String label : labels) {
//				IFuture<Iterable> fdata;
//				if (label.contains("*")) {
//					fdata = Stat.getData(Pattern.compile(label), start, end);
//				} else {
//					fdata = Stat.getData(start, end, KInterpolate.LINEAR_1DAY, null, label);
//				}
//				datas.add(fdata);
//			}
//		}
//		
//		double total = 0;
//		ArrayMap totals = new ArrayMap();
//		for (int i=0; i<labels.size(); i++) {
//			IFuture<Iterable> iFuture = datas.get(i);
//			String label = labels.get(i);
//			ListDataStream data = (ListDataStream) iFuture.get();
//			if (data.isEmpty()) {
//				page.append("<p>No data for <i>"+label+"</i></p>");
//				continue;
//			}
//			Chart lblChart = new TimeSeriesChart(label, data);
//			chart.add(lblChart);
//			dps += data.size();
//			double ttl = DataUtils.sum(data).get(0);
//			total += ttl;
//			totals.put(label, ttl);
//		}
//		if (chart.getCharts().isEmpty()) {
//			return;
//		}
//		
//		page.append(RenderWithFlot.RWF_DEPENDENCIES);
//		RenderWithFlot render = new RenderWithFlot(600, 300);
//		page.append(render.renderToHtml(chart));		
//		page.append("<div style='clear:both'>"+dps+" data points. Total: "+total+(totals.size()>1? " "+totals:"")+"</div>");
//	}
//
//	public static String getUrl(String statKey) {
//		String url = "/report-stat?start=1%20month%20ago"; 
//		url = DATALABELS.addQueryParameter(url, Arrays.asList(statKey)).toString();
//		return url;
//	}
//
//}
