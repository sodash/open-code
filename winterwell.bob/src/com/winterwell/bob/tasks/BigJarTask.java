package com.winterwell.bob.tasks;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Collection;
import java.util.HashSet;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import com.winterwell.utils.log.Log;

/**
 * Combine lots of jars into one file.
 * @author daniel
 */
public class BigJarTask extends JarTask {

	/**
	 * Combine lots of jars into one file.
	 * @param jar 
	 * @param inputJars
	 */
	public BigJarTask(File jar, Collection<File> inputJars) {
		super(jar, inputJars, new File("dummy"));
		assert inputJars.size() != 0 : inputJars+" is empty";
	}

	@Override
	public void doTask() throws Exception {		
		// Create the output!
		ZipOutputStream out = new ZipOutputStream(new FileOutputStream(jar));
		// Prevent name clashes - first entry wins
		filenames = new HashSet<String>();

		// Don't let manifests from the individual files in
		filenames.add("META-INF/MANIFEST.MF");

		// Add the files !
		for(File smallJar : files) {
			assert smallJar.exists() : smallJar;
			ZipInputStream zin = new ZipInputStream(new FileInputStream(smallJar));
			ZipEntry entry = zin.getNextEntry();
			while (entry != null) {
				String name = entry.getName();
//				if (entry.isDirectory()) {
//					entry = zin.getNextEntry();
//					continue;
//				}
				addFile2(name, smallJar, zin, out);
				entry = zin.getNextEntry();
			}			
		}
		
		// Add manifest
		addManifest(out);
		
		// Complete the ZIP file
		out.close();
		Log.d(LOGTAG, "Created: "+jar+" from "+files);
	}

	/**
	 * NA
	 */
	@Deprecated
	public void setAppend(boolean appendFlag) {
		throw new UnsupportedOperationException();
	}


}
