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
import com.winterwell.es.client.TransformRequestBuilder;
import com.winterwell.es.client.admin.CreateIndexRequest;
import com.winterwell.es.client.admin.IndicesAliasesRequest;
import com.winterwell.es.client.admin.PutMappingRequestBuilder;
import com.winterwell.es.fail.ESDocNotFoundException;
import com.winterwell.es.fail.ESIndexAlreadyExistsException;
import com.winterwell.gson.JsonArray;
import com.winterwell.gson.JsonElement;
import com.winterwell.gson.JsonObject;
import com.winterwell.gson.JsonParser;
import com.winterwell.utils.Dep;
import com.winterwell.utils.Printer;
import com.winterwell.utils.Utils;
import com.winterwell.utils.VersionString;
import com.winterwell.utils.io.FileUtils;
import com.winterwell.utils.log.Log;
import com.winterwell.utils.log.LogFile;
import com.winterwell.utils.time.TUnit;
import com.winterwell.utils.time.Time;
import com.winterwell.utils.time.TimeUtils;
import com.winterwell.web.FakeBrowser;
import com.winterwell.web.app.AMain;

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
public class CompressDataLogIndexMain extends AMain<DataLogConfig> {
	
//	public final static String ALIAS = "datalog.transformed.all";
	
	
	public static void main(String[] args) {
		CompressDataLogIndexMain amain = new CompressDataLogIndexMain();
		amain.doMain(args);		
	}

	@Override
	protected void init2(DataLogConfig config) {
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
		super("CompressDataLogIndex", DataLogConfig.class);
	}
	
	@Override
	protected void doMain2() {
		// which month? (3 letter lowercase) and year (last 2 digits)		
		Time t;
		if ( ! Utils.isEmpty(configRemainderArgs)) {
			t = TimeUtils.parseExperimental(configRemainderArgs.get(0));
		} else {
			t = new Time().minus(2, TUnit.MONTH);
		}		
		String monthYear = t.format("MMMyy").toLowerCase();
		String index = "datalog."+dataspace+"_transformed_" + monthYear;
		String source = "datalog."+dataspace+"_" + monthYear;

		// specify some terms that we want to keep
		// See DataLogEvent#COMMON_PROPS
		// TODO increase this list as our usage changes
		
		List<String> aggs = Arrays.asList(("amount dntn").split(" "));
		List<String> terms = Arrays.asList(
				("evt domain host country pub vert vertiser campaign lineitem "
				 +"cid via invalid mbl browser os"
				).split(" ")
		);

		// create index and mapping
		createIndexWithPropertiesMapping(index, null, terms);
		
		ESHttpClient esc = Dep.get(ESHttpClient.class);
		ESConfig config = esc.getConfig();
		
		// aggregate data
		String jobId = "transform_"+dataspace+"_"+monthYear;
		
		//safety mechanism -- make sure that the jobID doens't exist, if it does, delete it
		try {
			TransformRequestBuilder trb_safety = esc.prepareTransformDelete(jobId); 
			trb_safety.setDebug(true);
			trb_safety.get();
		} catch (ESDocNotFoundException e) {
			Printer.out("Safe to continue...");
		}
		
		// create transform job
		// specify source and destination and time interval
		TransformRequestBuilder trb = esc.prepareTransform(jobId);
		String version = esc.getESVersion();
		VersionString vs = new VersionString(version);
		if (vs.geq("7.10.0")) { // might have to change for future upgrades of ES
			Log.i("Using modern version of ES: more efficient transform!");
			trb.setBody(source, index, aggs, terms, "24h");
		} else {
			Log.w("Not using latest version of ES: painless script for transform...");
			trb.setBodyWithPainless(source, index, aggs, terms, "24h");
		}
		trb.setDebug(true);
		IESResponse response = trb.get(); //might take a long time for complex body
		Log.d("compress", response);
		
		//after creating transform job, start it 
		TransformRequestBuilder trb2 = esc.prepareTransformStart(jobId); 
		trb2.setDebug(true);
		IESResponse response2 = trb2.get().check();
		Log.d("compress", response2);
		System.out.println("Transforming data, please wait...");
		
		//allow transform job to be completed before deleting it
		FakeBrowser fb = new FakeBrowser();
		boolean done = false;
		while (!done) {
			// TODO as client thing
			String stats = fb.getPage(config.esUrl+"/_transform/"+jobId+"/_stats");
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
		TransformRequestBuilder trb3 = esc.prepareTransformDelete(jobId); 
		trb3.setDebug(true);
		IESResponse response3 = trb3.get();
		Log.d("compress", response3);
		
		//add datalog.gl.all alias into the newly created index and remove it from original index
		IndicesAliasesRequest iar = esc.admin().indices().prepareAliases();
		iar.addAlias(index, "datalog."+dataspace+".all");
		iar.removeAlias(source, "datalog."+dataspace+".all");
		iar.get().check();
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
//		Dep.setIfAbsent(FlexiGson.class, new FlexiGson());
//		Dep.setIfAbsent(ESConfig.class, new ESConfig());
//		if ( ! Dep.has(ESHttpClient.class)) Dep.setSupplier(ESHttpClient.class, false, ESHttpClient::new);
		
		ESHttpClient esc = Dep.get(ESHttpClient.class);
		try {
			// make the index
			CreateIndexRequest cir = esc.admin().indices().prepareCreate(idx);
			if ( ! Utils.isBlank(alias)) cir.setAlias(alias);
			cir.get().check();
			Utils.sleep(100);
			// set properties mapping
			PutMappingRequestBuilder pm = esc.admin().indices().preparePutMapping(idx);
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
	
	ESHttpClient esc = Dep.get(ESHttpClient.class);
	
}
