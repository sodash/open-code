package com.winterwell.utils.io;

import java.io.File;
import java.util.concurrent.Callable;

import com.winterwell.utils.Utils;

/**
 * Checks if the file has a modified timestamp & reloads if it has.
 * @author daniel
 */
// TODO would a java 8 file watcher make this more/less efficient?
public final class UpToDateTextFile {
	boolean autoReload = true;
	private final File file;
	private String txt;
	private long lastMod;
	private Callable onUpdate;
	
	/**
	 * @param file Does not have to exist.
	 */
	public UpToDateTextFile(File file) {
		assert ! file.exists() || file.isFile() : file.getAbsolutePath();
		this.file = file;
	}
	
	public File getFile() {
		return file;
	}

	/**
	 * Called just after a reload. 
	 * @param callable Can be null
	 */
	public void onUpdate(Callable callable) {
		this.onUpdate = callable;
	}

	public void setAutoReload(boolean autoReload) {
		this.autoReload = autoReload;		
	}
	
	/**
	 * The file's text. "" if the file does not exist. 
	 */
	public String toString() {
		if (txt==null || (autoReload && lastMod < file.lastModified())) {			
			load();			
		}
		return txt==null? "" : txt;
	}

	/**
	 * @throws RuntimeException if anything goes wrong
	 */
	private void load() {
		if ( ! file.exists()) return;
		try {
			txt = FileUtils.read(file);
			lastMod = file.lastModified();
			assert ! Utils.isBlank(txt) : file;
			if (onUpdate!=null) onUpdate.call();
		} catch (Exception e) {
			throw Utils.runtime(e);
		}
	}
}