package com.winterwell.datalog;

import java.io.File;
import java.util.Iterator;
import java.util.Map;
import java.util.regex.Pattern;

import com.google.common.util.concurrent.ListenableFuture;
import com.winterwell.datalog.DataLog.KInterpolate;
import com.winterwell.datalog.server.DataLogSettings;
import com.winterwell.depot.Desc;
import com.winterwell.es.ESType;
import com.winterwell.es.client.ESConfig;
import com.winterwell.es.client.ESHttpClient;
import com.winterwell.es.client.ESHttpResponse;
import com.winterwell.es.client.IESResponse;
import com.winterwell.es.client.IndexRequestBuilder;
import com.winterwell.es.client.admin.CreateIndexRequest;
import com.winterwell.es.client.admin.CreateIndexRequest.Analyzer;
import com.winterwell.es.client.admin.PutMappingRequestBuilder;
import com.winterwell.maths.stats.distributions.d1.MeanVar1D;
import com.winterwell.maths.timeseries.IDataStream;
import com.winterwell.utils.TodoException;
import com.winterwell.utils.Utils;
import com.winterwell.utils.containers.ArrayMap;
import com.winterwell.utils.containers.Pair2;
import com.winterwell.utils.io.ArgsParser;
import com.winterwell.utils.threads.IFuture;
import com.winterwell.utils.time.Dt;
import com.winterwell.utils.time.Period;
import com.winterwell.utils.time.Time;

public class ESStorage implements IStatStorage {

	private ESConfig settings;
	private File configFile;
	private ESHttpClient client;
	DataLogSettings dlsettings;
	
	@Override
	public IStatStorage setSettings(DataLogSettings settings) {
		this.dlsettings = settings;
		return this;
	}

	@Override
	public void save(Period period, Map<String, Double> tag2count, Map<String, MeanVar1D> tag2mean) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void saveHistory(Map<Pair2<String, Time>, Double> tag2time2count) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public IFuture<IDataStream> getData(Pattern id, Time start, Time end) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public StatReq<IDataStream> getData(String tag, Time start, Time end, KInterpolate fn, Dt bucketSize) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public StatReq<Double> getTotal(String tag, Time start, Time end) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Iterator getReader(String server, Time start, Time end, Pattern tagMatcher, String tag) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public IFuture<MeanRate> getMean(Time start, Time end, String tag) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public StatReq<IDataStream> getMeanData(String tag, Time start, Time end, KInterpolate fn, Dt bucketSize) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setHistory(Map<Pair2<String, Time>, Double> tagTime2set) {
		// TODO Auto-generated method stub
		
	}

	public void init() {
		if (settings == null) {
			if (configFile!=null) {
				settings = ArgsParser.getConfig(new ESConfig(), configFile);
			} else {
				settings = new ESConfig();
			}
		}
		client = new ESHttpClient(settings);
		initDataspace("gen");
	}

	private void initDataspace(String dataspace) {
		String index = indexFromDataspace(dataspace);
		if (client.admin().indices().indexExists(index)) {
			return;
		}
		// make it
		CreateIndexRequest pc = client.admin().indices().prepareCreate(index);
		pc.setDefaultAnalyzer(Analyzer.keyword);
		IESResponse res = pc.get();
		res.check();
//		// TODO map it
//		PutMappingRequestBuilder pm = client.admin().indices().preparePutMapping(index);
//		Map mapping = new ArrayMap();
//		pm.setSource(mapping);
	}

	private String indexFromDataspace(String dataspace) {
		assert ! Utils.isBlank(dataspace);
		return "datalog."+dataspace;
	}

	/**
	 * 
	 * 
	 * @param cnt
	 * @param dataspace
	 * @param event
	 * @return 
	 */
	public ListenableFuture<ESHttpResponse> saveEvent(double cnt, String dataspace, Map event) {
		String index = indexFromDataspace(dataspace);
		String type = "event";
		String id = new Desc("event", Map.class).putAll(event).getId();
		IndexRequestBuilder prepIndex = client.prepareIndex(index, type, id);
		event.put("count", cnt);
		prepIndex.setSource(event);
		return prepIndex.execute();
	}

	public double getEventTotal(Time start, Time end, String string) {
		// TODO Auto-generated method stub
		throw new TodoException();
	}
	
}
