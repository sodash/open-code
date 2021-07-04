package com.winterwell.datalog.server;

import java.util.Arrays;
import java.util.List;

import com.winterwell.datalog.DataLog;
import com.winterwell.datalog.DataLogConfig;
import com.winterwell.datalog.DataLogEvent;
import com.winterwell.datalog.ESStorage;
import com.winterwell.es.ESType;
import com.winterwell.es.client.ESConfig;
import com.winterwell.es.client.ESHttpClient;
import com.winterwell.es.client.IESResponse;
import com.winterwell.es.client.TransformRequest;
import com.winterwell.es.client.admin.CreateIndexRequest;
import com.winterwell.es.client.admin.IndicesAliasesRequest;
import com.winterwell.es.client.admin.PutMappingRequest;
import com.winterwell.es.client.query.BoolQueryBuilder;
import com.winterwell.es.client.query.ESQueryBuilder;
import com.winterwell.es.client.query.ESQueryBuilders;
import com.winterwell.es.fail.ESDocNotFoundException;
import com.winterwell.es.fail.ESIndexAlreadyExistsException;
import com.winterwell.gson.JsonArray;
import com.winterwell.gson.JsonElement;
import com.winterwell.gson.JsonObject;
import com.winterwell.gson.JsonParser;
import com.winterwell.nlp.query.SearchQuery;
import com.winterwell.utils.Dep;
import com.winterwell.utils.Utils;
import com.winterwell.utils.VersionString;
import com.winterwell.utils.containers.Containers;
import com.winterwell.utils.io.ConfigBuilder;
import com.winterwell.utils.io.FileUtils;
import com.winterwell.utils.log.Log;
import com.winterwell.utils.log.LogFile;
import com.winterwell.utils.time.TUnit;
import com.winterwell.utils.time.Time;
import com.winterwell.utils.time.TimeUtils;
import com.winterwell.web.FakeBrowser;
import com.winterwell.web.app.AMain;
import com.winterwell.web.app.AppUtils;

/**
 * Use Transforms to compress datalog month data
 * 
 * NB: Should we use RollUps?? - Nov 2020, rollups were not so easy to get started with
 * 
 * https://www.elastic.co/guide/en/elasticsearch/reference/7.9/data-rollup-transform.html
 * 
 * 
 * 
 * Use aggregations to compress the index for a month
 * @author Kai, Daniel
 *
 */
public class CompressDataLogIndexMain extends AMain<CompressDataLogIndexConfig> {
	
//	public final static String ALIAS = "datalog.transformed.all";
	
	@Override
	protected void launchJetty() {
		Log.d(LOGTAG, "No Jetty launch - this app doesn't need or want a web server.");
	}
	
	public static void main(String[] args) {
		CompressDataLogIndexMain amain = new CompressDataLogIndexMain();
		amain.doMain(args);		
	}

	@Override
	protected void init2(CompressDataLogIndexConfig config) {
		logFile = new LogFile(FileUtils.changeType(config.logFile, "compressor.txt"))
				// keep 1 week of log files
				.setLogRotation(TUnit.DAY.dt, 7);
		// set the config
		DataLog.init(config);
		// usual setup
		super.init2(config);
		
		init3_ES();
//		init3_gson();
//		init3_youAgain();		
	}

	
	/**
	 * Maybe get from config.namespace??
	 */
	private String dataspace = "gl";
	
	public CompressDataLogIndexMain() {
		super("CompressDataLogIndex", CompressDataLogIndexConfig.class);
	}
	

	protected void showHelp() {
		System.out.println("");
		System.out.println(appName+" v"+version);
		System.out.println("----------------------------------------");
		System.out.println("");
		ConfigBuilder cb = new ConfigBuilder(new CompressDataLogIndexConfig());
		System.out.println(cb.getOptionsMessage("source index (full name), e.g. `scrubbed.datalog.gl_jan21`"));
	}	
	
	static String version = "0.1.1"; 
	
	@Override
	protected void doMain2() {		
		// e.g. "scrubbed.datalog."+dataspace+"_" + MMMyy;
		String sourceIndex = Containers.get(configRemainderArgs, 0);
		if (Utils.isBlank(sourceIndex)) {
			showHelp();
			throw new IllegalArgumentException("Pass in a source index.");
		}
		ESHttpClient esc = Dep.get(ESHttpClient.class);
		String destIndex = Utils.or(getConfig().destIndex, sourceIndex+"_compressed");
		Log.i(LOGTAG, "Compress "+sourceIndex+" --> "+destIndex);

		// specify some terms that we want to keep
		// See DataLogEvent#COMMON_PROPS
		// TODO increase this list as our usage changes		
		List<String> aggs = Arrays.asList(("amount dntn").split(" "));
		// This list may need updating from time to time!
		List<String> terms = getConfig().longterms;

		// create index and mapping
		createIndexWithPropertiesMapping(destIndex, null, terms);				
		ESConfig esConfig = esc.getConfig();
		
		// aggregate data
		String jobId = "transform_"+sourceIndex;
		
		//safety mechanism -- make sure that the jobID doens't exist, if it does, delete it
		try {
			TransformRequest trb_safety = esc.prepareTransformDelete(jobId); 
			trb_safety.setDebug(true);
			trb_safety.get();
		} catch (ESDocNotFoundException e) {
			Log.d(LOGTAG, "Safe to continue...");
		}
		
		// create transform job
		// specify source and destination and time interval
		TransformRequest trb = esc.prepareTransform(jobId);
		String version = esc.getESVersion();
		VersionString vs = new VersionString(version);
		if (vs.geq("7.10.0")) {
			Log.i("Using modern version of ES: more efficient transform!");
			trb.setBody(sourceIndex, destIndex, aggs, terms, "24h");
		} else {
			Log.w("Not using latest version of ES: painless script for transform...");
			trb.setBodyWithPainless(sourceIndex, destIndex, aggs, terms, "24h");
		}
		// filter?
		if ( ! Utils.isBlank(getConfig().filter)) {
			SearchQuery sq = new SearchQuery(getConfig().filter);
			BoolQueryBuilder fq = AppUtils.makeESFilterFromSearchQuery(sq, null, null);
			trb.setQuery(fq);
			Log.d("Added filter", getConfig().filter+" -> "+fq);
		}
		trb.setDebug(true);
		IESResponse response = trb.get(); //might take a long time for complex body
		Log.d("compress", response);
		
		//after creating transform job, start it 
		TransformRequest trb2 = esc.prepareTransformStart(jobId); 
		trb2.setDebug(true);
		IESResponse response2 = trb2.get().check();
		Log.d(LOGTAG, response2);
		Log.d(LOGTAG, "Transforming data, please wait...");
		Log.d(LOGTAG, "...transforming job ID: "+jobId);
		Log.d(LOGTAG, esConfig.esUrl+"/_transform/"+jobId+"/_stats");
		
		//allow transform job to be completed before deleting it
		FakeBrowser fb = new FakeBrowser();
		boolean done = false;
		while (!done) {
			// TODO as client thing
			String stats = fb.getPage(esConfig.esUrl+"/_transform/"+jobId+"/_stats");
			JsonElement jelement = new JsonParser().parse(stats);
		    JsonObject  jobject = jelement.getAsJsonObject();
		    JsonArray jarray = jobject.getAsJsonArray("transforms");
		    jelement = jarray.get(0);
		    jobject = jelement.getAsJsonObject();
		    if (jobject.get("state").getAsString().equals("stopped")) {
		    	done = true;
		    	break;
		    }
		    Utils.sleep(10000); //check state every 10 seconds
		}
		
		//delete the transform job
		Log.i("Transform job done!");
		TransformRequest trb3 = esc.prepareTransformDelete(jobId); 
		trb3.setDebug(true);
		IESResponse response3 = trb3.get();
		Log.d("compress", response3);
		
		//add datalog.gl.all alias into the newly created index and remove it from original index
		if (getConfig().noAliasSwap) {
			Log.i("NO alias swap - the new index is not yet in use.");
		} else {
			IndicesAliasesRequest iar = esc.admin().indices().prepareAliases();
			iar.addAlias(destIndex, "datalog."+dataspace+".all");
			iar.removeAlias(sourceIndex, "datalog."+dataspace+".all");
			iar.get().check();
			Log.i("Alias swap done! "+sourceIndex+" -> "+destIndex);
		}
		Log.i("All done :) Enjoy your data");
	}
	
	
	/**
	 * Can we share some code with 
	 * ESStorage.registerDataspace() which does much the same??
	 * (BUT that code is a bit messy)
	 * 
	 * @param idx
	 * @param alias Optional
	 * @param terms 
	 * @return
	 */
	private void createIndexWithPropertiesMapping(String idx, String alias, List<String> terms) {
		ESHttpClient esc = Dep.get(ESHttpClient.class);
		// already exists?
		boolean already = esc.admin().indices().indexExists(idx);
		if (already) {
			return;
		}
		try {
			// make the index
			CreateIndexRequest cir = esc.admin().indices().prepareCreate(idx);
			if ( ! Utils.isBlank(alias)) cir.setAlias(alias);
			cir.get().check();
			Utils.sleep(100);
			// set properties mapping
			PutMappingRequest pm = esc.admin().indices().preparePutMapping(idx);
			ESType mytype = new ESType();
			for(String term : terms) {
				// HACK to turn Class into ESType
				ESType est = ESType.keyword;
				Class klass = DataLogEvent.COMMON_PROPS.get(term);				
				if (klass==StringBuilder.class) {
					est = new ESType().text().norms(false);
				} else if (klass==Time.class) {
					est = new ESType().date();
				} else if (klass==Double.class) {
					est = new ESType().DOUBLE();
				} else if (klass==Integer.class) {
					est = new ESType().INTEGER();
				} else if (klass==Long.class) {
					est = new ESType().LONG();					 
				} 
				mytype.property(term, est);
			}
			// set time and count types
			mytype
				.property("time", new ESType().date())
				.property(ESStorage.count, new ESType().DOUBLE());

			pm.setMapping(mytype);
			pm.setDebug(true);
			IESResponse resp = pm.get().check();
		} catch (ESIndexAlreadyExistsException ex) {
			Log.w("compress", "Index "+idx+" already exists, proceeding...");
		}
	}	
	
	private static final String LOGTAG = "compress";
	
}
