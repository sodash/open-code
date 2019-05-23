package com.winterwell.web.app;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.winterwell.utils.Printer;
import com.winterwell.utils.containers.ArrayMap;
import com.winterwell.utils.containers.Containers;
import com.winterwell.utils.io.CSVWriter;
import com.winterwell.utils.web.SimpleJson;

public class Json2Csv {

	private CSVWriter w;
	private List<String> headers;

	public Json2Csv(CSVWriter w) {
		this.w = w;
	}
	
	public Json2Csv setHeaders(List<String> headers) {
		this.headers = headers;
		return this;
	}
	
	public void run(Iterable<Map> hits2) {		
		// what headers??
		if (headers==null) {
			List<Map> list = Containers.getList(hits2);
			hits2 = list;
			headers = doSendCsv2_getHeaders(list);	
		}
		
		// write
		w.write(headers);
		for (Map hit : hits2) {
			List<Object> line = Containers.apply(headers, h -> {
				String[] p = h.split("\\.");
				Object v = SimpleJson.get(hit, p);
				if (v==null) {
					return null;
				}
				if (v.getClass().isArray()) {
					v = Printer.toString(Containers.asList(v), " ");
				}
				return v;
			});
			w.write(line);
		}
		w.close();

	}

	
	/**
	 * 
	 * @param state
	 * @param hits2 
	 * @return
	 */
	protected List<String> doSendCsv2_getHeaders(List<Map> hits2) {
		if (hits2.isEmpty()) return new ArrayList();
		Map hit = hits2.get(0);
		ArrayList map = new ArrayList();
		for(Object k : hit.keySet()) {
			map.add(""+k);
		}
		return map;
//		// TODO proper recursive
//		ObjectDistribution<String> headers = new ObjectDistribution();
//		for (Map<String,Object> hit : hits2) {
//			getHeaders(hit, new ArrayList(), headers);
//		}
//		// prune
//		if (hits2.size() >= 1) {
//			int min = (int) (hits2.size() * 0.2);
//			if (min>0) headers.pruneBelow(min);
//		}
//		// sort
//		ArrayList<String> hs = new ArrayList(headers.keySet());
//		// all the level 1 headers
//		List<String> level1 = Containers.filter(hs, h -> ! h.contains("."));
//		hs.removeAll(level1);
//		Collections.sort(hs);
//		Collections.sort(level1);		
//		// start with ID, name
//		level1.remove("name");
//		level1.remove("@id");
//		Collections.reverse(level1);
//		level1.add("name");
//		level1.add("@id");		
//		level1.forEach(h -> hs.add(0, h));
//		hs.removeIf(h -> h.contains("@type") || h.contains("value100"));
	}

}
