/**
 *
 */
package com.winterwell.nlp;

import java.io.File;

import junit.framework.TestCase;

import com.winterwell.depot.Depot;
import com.winterwell.depot.Desc;
import com.winterwell.depot.MetaData;
import com.winterwell.utils.gui.GuiUtils;

/**
 * Add a file to the NLP depot. Implemented as a JUnit test so it can be run
 * from Eclipse. This will also upload to winterwell.com if it can
 * 
 * @author miles
 * 
 */
public class CacheFile extends TestCase {

	/**
	 * Select a file using a GUI dialog and add it to the depot.
	 */
	private static void cacheFile() {		
		File file = GuiUtils.selectFile("Select a file to add to the depot",
				null);
		cacheFile(file);
	}
	

	/**
	 * Put a file into the depot.
	 * @param file
	 * @return
	 */
	public static File cacheFile(File file) {
		NLPWorkshop workshop = NLPWorkshop.get("en");
		Desc<File> desc = workshop.getConfig(file.getName());
		Depot.getDefault().put(desc, file);
		MetaData md = Depot.getDefault().getMetaData(desc);
		Depot.getDefault().flush();
		return md.getFile();
	}

	public static void main(String[] args) {
		cacheFile();
	}

	public void testCacheFile() {
		cacheFile();
	}

}
