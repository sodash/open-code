package com.winterwell.web.app;

import java.io.File;
import java.util.List;
import java.util.Map;

import org.eclipse.jetty.util.ajax.JSON;
import org.junit.Test;

import com.winterwell.utils.containers.Containers;
import com.winterwell.utils.io.CSVWriter;
import com.winterwell.utils.io.FileUtils;
import com.winterwell.utils.web.SimpleJson;

public class Json2CsvTest {

	@Test
	public void testForSanjay() {
		File f = new File("data/sanjay.json");
		String s = FileUtils.read(f);
		Map jobj = (Map) JSON.parse(s);		
		File out = new File("artifacts/feelings.csv");
		out.getParentFile().mkdirs();
		CSVWriter w = new CSVWriter(out);
		Json2Csv j2c = new Json2Csv(w);
		List<Map> hits = SimpleJson.getList(jobj, "cargo", "examples");
		List<Map> hits2 = Containers.apply(hits, h -> {
			Object src = h.get("_source");
			return (Map) src;
		});
		
		j2c.run(hits2);
		
		w.close();
	}

}
