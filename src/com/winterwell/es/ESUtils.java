/**
 * 
 */
package com.winterwell.es;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.Map;

import com.google.gson.FlexiGson;
import com.google.gson.Gson;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.winterwell.es.client.ESConfig;
import com.winterwell.es.client.ESHttpClient;
import com.winterwell.utils.StrUtils;
import com.winterwell.utils.Utils;
import com.winterwell.utils.containers.ArrayMap;
import com.winterwell.utils.io.ArgsParser;
import com.winterwell.utils.log.Log;
import com.winterwell.utils.time.Time;
import com.winterwell.utils.web.IHasJson;
import com.winterwell.web.data.XId;

import org.elasticsearch.common.io.stream.StreamOutput;
import org.elasticsearch.common.io.stream.Writeable;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.ToXContent;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.node.Node;
//import org.elasticsearch.node.NodeBuilder;
import org.elasticsearch.client.Client;
//import org.elasticsearch.common.settings.ImmutableSettings;
//import org.elasticsearch.common.settings.ImmutableSettings.Builder;

/**
 * Elastic Search utils
 * 
 * @author daniel
 *
 */
public class ESUtils {

	
//	public static Node startLocalES() {
//		return startLocalES(9200, false, new File("tmp-data"));
//	}
//	
//	public static Node startLocalES(int port, boolean persistToDisk, File dataDir) {
//		// broken in 5.1 :(
//		Settings.Builder esSettings = Settings.builder()
//				.put("http.enabled", "true")
//				.put("http.port", port)
//				
//				// what is needed here??
//				.put("http.type", "local")	
//				
//				// not allowed in v5.1?!
////				.put("index.store.type", persistToDisk? "fs" : "memory")
////				.put("index.number_of_shards", 1)
////				.put("index.number_of_replicas", 0)
//
//				.put("transport.type","local")
////				.put("discovery.zen.ping.multicast.enabled", "false")
//
////				.put("store.compress.stored", "true")
//
//				.put("path.home", dataDir.toString())
//				.put("path.data", dataDir.toString())
//				;
//		try {
//			File logFile = File.createTempFile("elasticsearch", ".log");
//			esSettings = esSettings.put("path.logs", logFile.toString());
//			Log.d("ES", "local node with log-file: "+logFile.getAbsolutePath()+" data-dir: "+dataDir.getAbsolutePath());
//		} catch (IOException e) {
//			throw Utils.runtime(e);
//		}				
//		if ( ! persistToDisk) {
//			esSettings = esSettings.put("gateway.type", "none");
//		}
//		
//		Settings esSet = esSettings.build();
//		Node node = new Node(esSet);
//		return node;
//	}
	
	/**
	 * See http://www.elasticsearch.org/guide/en/elasticsearch/reference/1.x/
	 * mapping-core-types.html
	 * 
	 * @return an ElasticSearch type builder, which (being a Map) can be
	 *         submitted to PutMapping. This is just a convenience for <code>new ESType()</code>.
	 */
	public static ESType type() {
		return new ESType();
	}

	/**
	See http://www.elasticsearch.org/guide/en/elasticsearch/reference/current/query-dsl-query-string-query.html#_reserved_characters
		 */
	public static String escapeWord(String s) {
		if (s==null) return null;
		String chars = "+-&|!(){}[]^\"~*?:\\/";
		String s2 = StrUtils.escape(s, chars, '\\');
		// Note "&" will also have to be url-encoded!
		return s2;
	}
	
	/**
	 * HACK convert an ES class into a json object.
	 * TODO replace these ES classes with something nicer.
	 * Preferably auto-generated from the ES source
	 * @param qb
	 * @return
	 */
	public static Object jobj(ToXContent qb) {
		String s = qb.toString();
		return FlexiGson.fromJSON(s);
	}

}

