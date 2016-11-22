package com.winterwell.depot;

import java.io.File;
import java.io.IOException;

import org.junit.Test;

import com.winterwell.utils.io.FileUtils;

public class MetaBugTest {

	/**
	 * Ah-ha -- this out of date MetaData xml was causing the badness
	 */
	public void tstLoad() throws IOException {
		File out = File.createTempFile("test", ".meta");
		FileUtils.write(out, "<com.winterwell.depot.MetaData><desc><name>stopwords.txt</name><tag>winterwell.nlp</tag><properties><entry><key><name>lang</name></key><S>en</S></entry></properties><server>ww</server><type>java.io.File</type><version>0</version></desc><accessCount>0</accessCount><file>/home/daniel/winterwell/datastore/winterwell.nlp/File/ww/lang=en/stopwords.txt</file></com.winterwell.depot.MetaData>");
		MetaData md = FileUtils.load(out);
	}
	
	@Test
	public void testMetaBug() {
		Depot depot = Depot.getDefault();
		
		// copied from NLPWorkshop
		Desc<File> ad = new Desc<File>("stopwords.txt", File.class);
		ad.setTag("winterwell.nlp");
		ad.put("lang", "en");
		ad.setServer(Desc.CENTRAL_SERVER);
		
		File file = depot.getLocalPath(ad);
		FileUtils.delete(file);
		
		File stopwords = depot.get(ad);
		
		String list = FileUtils.read(stopwords);
		assert list.contains("and") : list;
		assert ! list.startsWith("<") : list;		
	}
	
}
