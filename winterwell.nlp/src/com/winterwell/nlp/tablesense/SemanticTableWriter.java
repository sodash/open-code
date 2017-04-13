/**
 * 
 */
package com.winterwell.nlp.tablesense;

import java.io.File;

import com.winterwell.utils.io.FileUtils;
import com.winterwell.web.WebPage;

/**
 * Write our semantic table html format.
 * See https://docs.google.com/document/d/12DbLRm7BWvnHAFcTbIKpMFgWr_oXnQVSoj3gNJhdlSM
 * @author Daniel
 *
 */
public class SemanticTableWriter {

	public SemanticTableWriter() {
	}
	
	public void write(PTable table, File file) {
		WebPage page = new WebPage();
		
		page.append("<table>");
		// TODO
//		List<Row> rows = table.getMLE().getRows();
		page.append("</table>");
		
		FileUtils.write(file, page.toString());
	}
}
