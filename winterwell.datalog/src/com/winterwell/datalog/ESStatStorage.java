//package com.winterwell.datalog;
//
//import static org.elasticsearch.node.NodeBuilder.nodeBuilder;
//
//import java.sql.Connection;
//import java.sql.PreparedStatement;
//import java.sql.ResultSet;
//import java.sql.SQLException;
//import java.util.HashMap;
//import java.util.Iterator;
//import java.util.List;
//import java.util.Map;
//import java.util.concurrent.ExecutionException;
//import java.util.regex.Pattern;
//
//import org.elasticsearch.action.search.SearchType;
//import org.elasticsearch.index.query.BoolQueryBuilder;
//import org.elasticsearch.index.query.QueryBuilders;
//import org.elasticsearch.search.facet.FacetBuilders;
//import org.elasticsearch.search.facet.statistical.StatisticalFacet;
//import org.elasticsearch.search.sort.SortBuilders;
//
//import static com.winterwell.es.ESUtils.*;
//import winterwell.maths.stats.distributions.d1.MeanVar1D;
//import winterwell.maths.timeseries.IDataStream;
//
//import com.winterwell.datalog.Stat.KInterpolate;
//import com.winterwell.es.ESUtils;
//import com.winterwell.es.client.BulkRequestBuilder;
//import com.winterwell.es.client.BulkResponse;
//import com.winterwell.es.client.ESHttpClient;
//import com.winterwell.es.client.ESHttpClient.AdminClient;
//import com.winterwell.es.client.ESHttpResponse;
//import com.winterwell.es.client.IESResponse;
//import com.winterwell.es.client.IndexRequestBuilder;
//import com.winterwell.es.client.SearchRequestBuilder;
//import com.winterwell.es.client.SearchResponse;
//import com.winterwell.utils.MathUtils;
//import com.winterwell.utils.Printer;
//import com.winterwell.utils.Utils;
//import com.winterwell.utils.containers.ArrayMap;
//import com.winterwell.utils.containers.Pair2;
//import com.winterwell.utils.log.Log;
//import com.winterwell.utils.threads.IFuture;
//import com.winterwell.utils.threads.IFuture.IDeferred;
//
//import com.winterwell.utils.io.SqlUtils;
//import com.winterwell.utils.time.Dt;
//import com.winterwell.utils.time.Period;
//import com.winterwell.utils.time.Time;
//
//import com.winterwell.utils.web.WebUtils2;
//
///**
// * ElasticSearch backed stat.
// * 
// * TODO overview of how this works
// * 
// * @author Alex N, Daniel W
// *
// */
//public class ESStatStorage implements IStatStorage {
//
//	private final StatConfig config;
//	
//	public final String INDEX = "stat";
//	public final String TYPE = "stat";
//	public final String SERVER; 
//	
//	/**
//	 * FIXME: This constructor is called at sodash startup. It can't call initStatDB,
//	 * because it calls {@link SqlUtils#setDBOptions(DBOptions)}. When sodash DB is initialising
//	 * afterwards an assertion error is thrown.
//	 * @DW Move initStatDB along with other DB inits?
//	 *
//	 * @param config
//	 * @param server - stat results now get saved with a marker from their local server
//	 * 
//	 */
//	public ESStatStorage(StatConfig config, String localserver) {
//		this.SERVER = localserver;
//		this.config = config;
//		initStatDB();
//	}
//	
//	public ESStatStorage(StatConfig config) {
//		this.SERVER = WebUtils2.hostname();
//		this.config = config;
//		initStatDB();
//	}
//	
//	@Override
//	public void save(Period period, Map<String, Double> tag2count,
//			Map<String, MeanVar1D> tag2mean) {
//		initStatDB();
//		// Save as the middle of the period?! - I dunno, it's what we do at the moment.
//		Long mid = StatImpl.doSave3_time(period).getTime();
//		
//		if (!tag2count.isEmpty()) save2_tag2count(mid, tag2count);
//		if (!tag2mean.isEmpty()) save2_tag2mean(mid, tag2mean);
//	}
//
//	private void save2_tag2mean(Long mid, Map<String, MeanVar1D> tag2mean) {
//		BulkRequestBuilder bulkRequest = client.prepareBulk();
//		for (String tag : tag2mean.keySet()){
//			String enctag = SqlUtils.sqlEncode(tag);
//			MeanVar1D value = tag2mean.get(tag);
//			Log.v(Stat.LOGTAG, "saving " + tag + "=" + value + " to db");
//			
//			bulkAdd(bulkRequest, new StatEntry(SERVER, enctag, mid, value));
//		}
//		BulkResponse bulkResponse = bulkRequest.get();
//		if (bulkResponse.hasFailures()) {
//			// What in the hell do we do here?
//		   handleFailures(bulkResponse);
//		}
//	}
//
//	private void save2_tag2count(Long mid, Map<String, Double> tag2count) {
//		BulkRequestBuilder bulkRequest = client.prepareBulk();
//		for (String tag : tag2count.keySet()){
//			// might as well, not sure what insertion attacks are like on this thing.
//			
//			Double value = tag2count.get(tag);
//			// This needs sanitised, but how? SqlEncode ain't good.
//			// String enctag = SqlUtils.sqlEncode(tag);
//			Log.d(Stat.LOGTAG, "saving " + tag + "=" + value + " to db");
//			bulkAdd(bulkRequest, new StatEntry(SERVER, tag, mid, value));
//		}
//		BulkResponse bulkResponse = bulkRequest.get();
//		if (bulkResponse.hasFailures()) {
//			// What in the hell do we do here?
//		   handleFailures(bulkResponse);
//		}
//	}
//
//	private void handleFailures(BulkResponse bulkResponse) {
//		Log.e("ESStatStorage", "TODO "+bulkResponse);
//	}
//
//	private void bulkUpdateAddCount(BulkRequestBuilder bulkRequest, StatEntry statEntry){
//		HashMap<String, Object> json = statEntry.toJSON();
//		bulkRequest.add(			
//				client.prepareUpdate(INDEX, TYPE, statEntry.getId())
//		        .setUpsert(json)
//				.setScript("ctx._source." + "count_mean" + " += " + statEntry.getCount_mean()));
//		
//	}
//	
//	private void bulkAdd(BulkRequestBuilder bulkRequest, StatEntry statEntry) {
//		Map<String, Object> json = statEntry.toJSON();
//		bulkRequest.add(client.prepareIndex(INDEX, TYPE, statEntry.getId())
//		        .setSource(json)
//		        );
//	}
//
//	/**
//	 * This one's tricky, because we can't do a bulk update, as we don't know what's there.
//	 */
//	@Override
//	public void saveHistory(Map<Pair2<String, Time>, Double> tag2time2count) {
//		if (tag2time2count.isEmpty()) return;
//		BulkRequestBuilder bulkRequest = client.prepareBulk();
//		for (Pair2<String, Time> tagtime : tag2time2count.keySet()){
//			bulkUpdateAddCount(bulkRequest, new StatEntry(SERVER, tagtime.first, tagtime.second.getTime(), tag2time2count.get(tagtime)));
//		}
//
//		BulkResponse bulkResponse = bulkRequest.get();
//		if (bulkResponse.hasFailures()) {
//			// What in the hell do we do here?
//		   handleFailures(bulkResponse);
//		}
//	}
//	
//
//	/**
//	 * Replaces values at the given tag/time pairs.
//	 * @param tag2time2count
//	 */
//	public void setHistory(Map<Pair2<String, Time>, Double> tag2time2count) {
//		// We are setting values, should be okay just to do a normal push.
//		BulkRequestBuilder bulkRequest = client.prepareBulk();
//		for (Pair2<String, Time> tagtime : tag2time2count.keySet()){
//			bulkAdd(bulkRequest, new StatEntry(SERVER, tagtime.first, tagtime.second.getTime(), tag2time2count.get(tagtime)));
//		}
//		BulkResponse bulkResponse = bulkRequest.get();
//		if (bulkResponse.hasFailures()) {
//			// What in the hell do we do here?
//		   handleFailures(bulkResponse);
//		}
//	}
//
//	@Override
//	public IFuture<IDataStream> getData(Pattern id, Time start, Time end) {
//		return new ESStatReq<IDataStream>(KStatReq.DATA, id, start, end);
//	}
//
//	@Override
//	public StatReq<IDataStream> getData(String tag, Time start, Time end,
//			KInterpolate fn, Dt bucketSize) {
//		return new ESStatReq<IDataStream>(KStatReq.DATA, tag, start, end, fn, bucketSize);
//	}
//
//	@Override
//	public StatReq<Double> getTotal(String tag, Time start, Time end) {
//		return new ESStatReq<Double>(KStatReq.TOTAL, tag, start, end, null, null);
//	}
//
//	@Override
//	public Iterator<Map> getReader(String server, Time start, Time end,
//			Pattern tagMatcher, String tag) {
//		assert tagMatcher != null || tag != null : "One of tagMatcher/tag should not be null";
//		assert tagMatcher == null || tag == null : "One of tagMatcher/tag should be null";
//		if (tagMatcher != null){
//			return searchWPattern(tagMatcher, start.getTime(), end.getTime());
//		} else {
//			return search(tag, start.getTime(), end.getTime());
//		}
//	}
//
//	@Override
//	public IFuture<MeanRate> getMean(Time start, Time end, String tag) {
//		return new ESStatReq<MeanRate>(KStatReq.TOTAL, tag, start, end, null, null);
//	}
//
//	private static ESHttpClient client;
//	
//	public synchronized void initStatDB() {
//		if (client!=null) return;		
//				
//		// The elasticsearch Client object is thread safe and its lifecycle is meant to be similar to the application lifecycle itself, that's why you don't need to create an instance for each request. 
//		// A singleton client for the whole application is fine.
//		client = new ESHttpClient();		
//		AdminClient ac = client.admin();
//		
//		// Make the indices
//		IndicesAdminClient iac = ac.indices();
//		
//		try {
//			// for speed, don't waste time repeating the create
//			if ( ! iac.indexExists(INDEX)) {						
//				CreateIndexRequest cir = new CreateIndexRequest(client, INDEX);
//				IESResponse resp = cir.get();
//				Log.d("init", "Create ES index "+INDEX+": "+resp.isSuccess());
//				Utils.sleep(1500);
//			} else {
//				Log.d("init", "Already have ES index "+INDEX);
//			}
//		
//		// Mappings
//
//			init2_mappings(iac);
//		} catch(Exception ex) {
//			throw Utils.runtime(ex);
//		}		
//	}
//	
//	ESIterator search(String tag, long start, long end) {				
//		// Create the boolean query - this asks for both tag and timestamp range.
//		BoolQueryBuilder bqb = getTimeAndTagQuery(tag, start, end);
//		
//		SearchRequestBuilder search = client.prepareSearch(INDEX)
//		        .setTypes(TYPE)
//		        .setQuery(bqb)     // Query
//		        // This might be useful for scrolling. It defaults to 10, which is crap, and might cause errors if we do
//		        // it with historic searches. Currently set to something humongous to see what it does.
//		        .addSort(SortBuilders.fieldSort("timestamp"));
//		
//		return new ESIterator(search);
//	}
//	
//	/**
//	 * Unfortunately, the iterator here is for SearchHits (the whole lot, as such we may need to create some 
//	 * sort of iterator which pages over days, to avoid humungous data sets. Or we might not. We'll see.
//	 * 
//	 * @param ts
//	 * @param tag
//	 * @param start - the start of a period
//	 * @param end - the end of a period 
//	 * @return
//	 */
//	ESIterator searchWPattern(Pattern pattern, long start, long end) {				
//		// Create the boolean query - this asks for both tag and timestamp range.
//		BoolQueryBuilder bqb = getTimeAndTagQueryWPattern(pattern, start, end);
//		
//		SearchRequestBuilder search = client.prepareSearch(INDEX)
//		        .setTypes(TYPE)
//		        .setSearchType(SearchType.QUERY_THEN_FETCH)
//		        .setQuery(bqb)     // Query
//		        // This might be useful for scrolling. It defaults to 10, which is crap, and might cause errors if we do
//		        // it with historic searches. Currently set to something humongous to see what it does.
//		        .addSort(SortBuilders.fieldSort("timestamp"));
//		
//		return new ESIterator(search);
//	}
//	
//	public BoolQueryBuilder getTimeAndTagQuery(String tag, long start, long end) {
//		BoolQueryBuilder bqb = 
//				QueryBuilders
//					.boolQuery()
//						.must(QueryBuilders.rangeQuery("timestamp")
//	                    .from(start)
//	                    .to(end)
//	                    .includeLower(true)
//	                    .includeUpper(false))
//	                    
//	            .must(QueryBuilders.termQuery("tag", tag)
//	            		);
//		return bqb;
//	}
//	
//	public BoolQueryBuilder getTimeAndTagQueryWPattern(Pattern pattern, long start, long end) {
//		String patternRaw = pattern.pattern();
//		BoolQueryBuilder bqb = 
//				QueryBuilders
//					.boolQuery()
//						.must(QueryBuilders.rangeQuery("timestamp")
//	                    .from(start)
//	                    .to(end)
//	                    .includeLower(true)
//	                    .includeUpper(false))
//	                    
//	            .must(QueryBuilders.regexpQuery("tag", patternRaw)
//	            		);
//		return bqb;
//	}
//	
//	/**
//	 * @param iac
//	 * @throws InterruptedException
//	 * @throws ExecutionException
//	 */
//	/**
//	 * @param iac
//	 * @throws InterruptedException
//	 * @throws ExecutionException
//	 */
//	private void init2_mappings(IndicesAdminClient iac) throws InterruptedException, ExecutionException {
//	
//		Map<String, Object> properties = StatEntry.getDescription(); 
//		
//		// TODO
////		ClusterState cs = client.admin().cluster().prepareState().setIndices(INDEX).execute().actionGet().getState();
////		Map imd = cs.getMetaData().get(INDEX);
////		MappingMetaData mmd = imd.mapping(TYPE);
//		Map mmd = null;
//		if (mmd != null){
//			try{
//				// Checking our maps match. Evil, due to all the type-casting but how to improve?
//				Map<String, Object> existingProperties = null; //mmd.getSourceAsMap();
//				Map exProperties = (Map) existingProperties.get("properties");
//				for (String key : properties.keySet()){
//					Map value = (Map) properties.get(key);
//					Map value2 = (Map) exProperties.get(key);
//					if (!value.get("type").equals(value2.get("type"))){
//						throw new IllegalStateException("Stat is configured incorrectly");
//					}
//				}
//				// Ok, our mappings match, we don't need to do this.
//				return;
//			}
//			catch (Exception e){
//				throw new RuntimeException(e);
//			}
//			
//		
//		}
//
//		
//		
//		PutMappingRequestBuilder pm = iac.preparePutMapping(INDEX)
//											.setType(TYPE)
//			.setSource(new ArrayMap(TYPE,
//						new ArrayMap("properties", properties)
//			
//					  ));				
//		IESResponse ok = pm.execute().get();
//		Log.d("init", "desc mapping: "+ok.isAcknowledged());		
//	}
//	
//	
//	/**
//	 * backend request for get-total
//	 * @param server
//	 * @param s
//	 * @param e
//	 * @param tagMatcher
//	 * @param tag
//	 * @return the count of tags given the parameters.
//	 */
//	double selectSum(String server, Time s, Time e, Pattern tagMatcher, String tag) {
//		initStatDB();
//		assert tagMatcher != null || tag != null : "One of tagMatcher/tag should not be null";
//		assert tagMatcher == null || tag == null : "One of tagMatcher/tag should be null";
//		if (tagMatcher != null){
//			return sumWPattern(tagMatcher, s.getTime(), e.getTime());
//		} else {
//			return sum(tag, s.getTime(), e.getTime());
//		}
//		
//	}
//	
//	public final String SUMMATION_FIELD = "count_mean";
//	
//	public double sum(String tag, long start, long end){
//		BoolQueryBuilder bqb = getTimeAndTagQuery(tag, start, end);
//		SearchResponse response = client.prepareSearch(INDEX)
//		        .setTypes(TYPE)
//		        .setSearchType(SearchType.COUNT) // Count doesn't get the results back.
//		        .addFacet(FacetBuilders.statisticalFacet("total").field(SUMMATION_FIELD))
//		        .setQuery(bqb)     // Query
//		        
//		        // This might be useful for scrolling. It defaults to 10, which is crap, and might cause errors if we do
//		        // it with historic searches. Currently set to something humongous to see what it does.
//		//        .setFrom(0).setSize(10000000) don't think size needed here, it counts all!
//		//		        .addSort(SortBuilders.fieldSort("timestamp")) I think no sort needed
//		        .get();
//		
//		Map f = (Map) response.getFacets().get("total");
//		return MathUtils.toNum(f.get("total"));
//	}
//	
//	public double sumWPattern(Pattern tagPattern, long start, long end){
//		BoolQueryBuilder bqb = getTimeAndTagQueryWPattern(tagPattern, start, end);
//		SearchResponse response = client.prepareSearch(INDEX)
//		        .setTypes(TYPE)
//		        .setSearchType(SearchType.COUNT) // Count doesn't get the results back.
//		        .addFacet(FacetBuilders.statisticalFacet("total").field(SUMMATION_FIELD))
//		        .setQuery(bqb)     // Query
//		        
//		        // This might be useful for scrolling. It defaults to 10, which is crap, and might cause errors if we do
//		        // it with historic searches. Currently set to something humongous to see what it does.
//		//        .setFrom(0).setSize(10000000) don't think size needed here, it counts all!
//		//		        .addSort(SortBuilders.fieldSort("timestamp")) I think no sort needed
//		        .get();
//		
//		Map f = (Map) response.getFacets().get("total");
//		return MathUtils.toNum(f.get("total"));
//	}
//	
//	/**
//	 * Actually do the putting.
//	 * @param statEntry
//	 */
//	public void put(StatEntry statEntry) {
//		
//		IndexRequestBuilder pi = setupIndexRequestBuilder();
//		Map<String, Object> json = statEntry.toJSON();
//		pi = pi.setSource(json);
//		pi.setId(statEntry.getId());
//
//		// routing? Probably based on the StatEntry's "server" field.
////		pi = pi.setRouting(ref.oxid.toString());
//		
//		// We need to do this, or can we fire-and-forget? A possible source of speed improvement.
//		IESResponse f = pi.get();
//		// wait until done / failed
////		f.actionGet();
//	}
//
//	/**
//	 * Are these settings correct? A source of improvement perhaps.
//	 * @return
//	 */
//	public IndexRequestBuilder setupIndexRequestBuilder() {
//		IndexRequestBuilder pi = client.prepareIndex(INDEX, TYPE, null);		
////		ReplicationType replicationType = ReplicationType.ASYNC;
////		pi = pi.setReplicationType(replicationType);
////		pi = pi.setOperationThreaded(false);
////		WriteConsistencyLevel consistencyLevel = WriteConsistencyLevel.ONE;
////		pi = pi.setConsistencyLevel(consistencyLevel);
//		return pi;
//	}
//
//	@Override
//	public StatReq<IDataStream> getMeanData(String tag, Time start, Time end,
//			KInterpolate fn, Dt bucketSize) {
//		// TODO Auto-generated method stub
//		return null;
//	}
//
//}
//
//
//
///**
// * This class represents the two types of objects which get stored in Stat. 
// * @author alexn
// *
// */
//class StatEntry{
//
//	public String server;
//	public String tag;
//	public long timestamp;
//	public Double count_mean;
//	public Double variance;
//	public Double max;
//	public Double min;
//	
//	
//	/**
//	 * The simple type - tag - timestamp - count
//	 * @param tag
//	 * @param timestamp
//	 */
//	public StatEntry(String server, String tag, long timestamp, Double count){
//		this(server, tag, timestamp, count, null, null, null);
//	}
//	/**
//	 * The complex type - tag - timestamp - mean - variance - max - min 
//	 * @param tag
//	 * @param timestamp
//	 * @param count_mean
//	 * @param variance
//	 * @param max
//	 * @param min
//	 */
//	public StatEntry(String server, String tag, long timestamp, Double mean, Double variance, Double max, Double min){
//		this.server = server;
//		this.tag = tag;
//		this.timestamp = timestamp;
//		this.count_mean = mean;
//		this.variance = variance;
//		this.max = max;
//		this.min = min;
//	}
//	/**
//	 * The complex type from a meanVar1D 
//	 * @param tag
//	 * @param timestamp
//	 * @param MeanVar1D
//	 */
//	public StatEntry(String server, String tag, long timestamp, MeanVar1D meanvar){
//		this.server = server;
//		this.tag = tag;
//		this.timestamp = timestamp;
//		this.count_mean = meanvar.getMean();
//		this.variance = meanvar.getVariance();
//		this.max = meanvar.getMax();
//		this.min = meanvar.getMin();
//	}
//	
//	public String getTag() {
//		return tag;
//	}
//
//	public double getCount_mean() {
//		return count_mean;
//	}
//
//	public double getVariance() {
//		return variance;
//	}
//
//	public double getMax() {
//		return max;
//	}
//
//	public double getMin() {
//		return min;
//	}
//
//	public long getTimestamp(){
//		return timestamp;
//	}
//
//	public String getServer(){
//		return server;
//	}
//	/**
//	 * Gets a composite of timestamp_tag@server.
//	 * @return
//	 */
//	public String getId(){
//		return new String(timestamp + "_" + tag + "@" + server);
//	}
//	
//	/**
//	 * Returns object in Jsonified form.
//	 * @return
//	 */
//	public HashMap<String, Object> toJSON(){
//		HashMap<String, Object> map = new HashMap<String,Object>();
//		map.put("server", server);
//		map.put("tag", tag);
//		map.put("timestamp", timestamp);
//		map.put("count_mean", count_mean);
//		map.put("variance", variance);
//		map.put("max", max);
//		map.put("min", min);
//		map.put("@class", StatEntry.class.getName());
//		return map;
//	}
//	
//	
//	/**
//	 * Returns an ElasticSearch Description of the object. TODO make this more generic?
//	 * @return
//	 */
//	public static ArrayMap<String, Object> getDescription(){
//		return new ArrayMap<String, Object>(
//				"server", type().string().noAnalyzer(),
//				"timestamp", type().LONG(),
//				"tag", type().string().noAnalyzer(),
//				"count_mean", type().DOUBLE(),
//				"variance" , type().DOUBLE(),
//				"max", type().DOUBLE(),
//				"min", type().DOUBLE()
//				);
//	}
//	
//}
//
