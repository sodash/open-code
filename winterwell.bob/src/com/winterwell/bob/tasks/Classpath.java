package com.winterwell.bob.tasks;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.winterwell.utils.StrUtils;
import com.winterwell.utils.containers.Containers;

/**
 * Just a list of files and globs (e.g. "mydir/*") -- but its handy to give it a wrapper class.
 * @author daniel
 *
 */
public class Classpath {

	List<String> files = new ArrayList();

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
		// null as []
		if (files==null) files = new ArrayList();
		this.files = Containers.apply(files, f -> stringForFile(f));
	}
	
	/**
	 * Java classpath format: "file1:file2:file3"
	 */
	@Override
	public String toString() {
		return StrUtils.join(files, File.pathSeparator);
	}

	public static Classpath getSystemClasspath() {
		// Or use ClassLoader getResources META-INF and strip from that??
		// see https://docs.oracle.com/javase/tutorial/essential/environment/sysprop.html
		String classpath = System.getProperty("java.class.path");		
		Classpath cp = new Classpath(classpath);
		return cp;
	}

	public Classpath add(File jarOrDir) {
		return add(stringForFile(jarOrDir));
	}

	/**
	 * 		dedupe and add
	 * @param files2
	 */
	public void addAll(Collection<File> files2) {
		for (File file : files2) {
			String fs = stringForFile(file);
			if (files.contains(fs)) continue;
			files.add(fs);	
		}		
	}

	private String stringForFile(File file) {
		return file.toString();
	}

	public boolean isEmpty() {
		return files.isEmpty();
	}

	public Classpath add(String jarOrDir) {
		files.add(jarOrDir);
		return this;
	}

	public List<File> getFiles() {
		ArrayList<File> fs = Containers.apply(files, File::new);
		return fs;
	}
	
	
}
