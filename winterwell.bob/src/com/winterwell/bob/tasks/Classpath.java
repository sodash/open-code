package com.winterwell.bob.tasks;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.winterwell.utils.StrUtils;
import com.winterwell.utils.containers.ArraySet;

/**
 * Just a list of files (but its handy to give it a wrapper class)
 * @author daniel
 *
 */
public class Classpath {

	List<File> files = new ArrayList();

	public Classpath(String classpath) {
		String[] classpathEntries = classpath.split(File.pathSeparator);
		List<File> fs = new ArrayList();
		for (String ce : classpathEntries) {
			fs.add(new File(ce));
		}
		setFiles(fs);
	}
	
	public Classpath() {	
	}

	public void setFiles(List<File> files) {
		this.files = files;
	}
	
	public List<File> getFiles() {
		return files;
	}
	
	@Override
	public String toString() {
		return StrUtils.join(files, File.pathSeparator);
	}

	public static Classpath getSystemClasspath() {
		String classpath = System.getProperty("java.class.path");		
		Classpath cp = new Classpath(classpath);
		return cp;
	}

	public Classpath add(File jarOrDir) {
		files.add(jarOrDir);
		return this;
	}

	/**
	 * 		dedupe and add
	 * @param files2
	 */
	public void addAll(List<File> files2) {
		for (File file : files2) {
			if (files.contains(file)) continue;
			files.add(file);	
		}		
	}
	
	
}
