/**
 *
 */
package com.winterwell.bob.tasks;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.winterwell.bob.Bob;
import com.winterwell.bob.BuildTask;
import com.winterwell.utils.io.FileUtils;
import com.winterwell.utils.log.Log;

/**
 * Copy files from one directory to another.
 * <p>
 * By default, hidden files such as .svn are copied, but this can be switched
 * off via {@link #setIncludeHiddenFiles(boolean)}.
 *
 * @see RSyncTask
 * @author Daniel
 */
public class CopyTask extends BuildTask {

	@Override
	public String toString() {
		return getClass().getSimpleName()+":"+(srcDir==null?"file-list":srcDir);
	}

	private final File destDir;
	private FileFilter filter = FileUtils.TRUE_FILTER;
	private boolean includeHiddenFiles = true;
	private boolean overwrite = true;
	/** The files that weren't copied due to overwrite issues */
	private final List<File> skipped = new ArrayList<File>();
	private final File srcDir;
	private final Collection<File> srcFiles;
	private boolean overwriteIfNewer;
	private boolean errorOnDuplicate;

	/**
	 * Copy files from one directory to another.
	 * <p>
	 * By default, hidden files such as .svn are copied, but this can be switched
	 * off via {@link #setIncludeHiddenFiles(boolean)}.
	 * @param destDir This will be created if it does not exist.
	 */
	public CopyTask(File srcDir, File destDir) {
		this.srcDir = srcDir.getAbsoluteFile();
		this.destDir = destDir.getAbsoluteFile();
		this.srcFiles = null;
	}

	/**
	 * A flat copy (no directories will be created) of srcFiles into destDir.
	 * @param srcFiles
	 * @param destDir This will be created if it does not exist.
	 */
	public CopyTask(Collection<File> srcFiles, File destDir) {
		this.srcFiles = srcFiles;
		srcDir = null;
		this.destDir = destDir.getAbsoluteFile();
	}
	

	boolean resolveSymlinks;
	
	/**
	 * If true, symlinks get resolved - the output directory will contain a copy of the file (and not a symlink).
	 * If false, symlinks are copied as symlinks.
	 * 
	 * TODO I think true would be a better default 
	 */
	public CopyTask setResolveSymLinks(boolean b) {
		this.resolveSymlinks = b;
		return this;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see winterwell.bob.BuildTask#doTask()
	 */
	@Override
	public void doTask() throws Exception {
		boolean verbose = isVerbose();
		assert srcDir==null || this.srcDir.isDirectory() : this.srcDir;
		if ( ! destDir.exists()) destDir.mkdirs();
		assert destDir.isDirectory() : destDir;
		// The files to copy
		Collection<File> files;
		if (srcDir == null) {
			assert srcFiles != null;
			if (filter==null) {
				files = srcFiles;				
			} else {
				// Filter
				files =new ArrayList();
				for (File file : srcFiles) {
					if (filter.accept(file)) {
						files.add(file);
					}
				}
			}
		} else {
			files = FileUtils.find(srcDir, filter, includeHiddenFiles);
		}
		
		// Do it
		Map copy2original = new HashMap();
		List<File> symDirs = new ArrayList();
		File prev = null;
		for (final File in : files) {
			// debug weird
			wtf();
			prev = in;
			
			assert in.exists() : in;
			String path = srcDir==null? in.getName() : FileUtils.getRelativePath(in, srcDir);
			File out = new File(destDir, path);
			// Make directory if needed
			out.getParentFile().mkdirs();
			// Overwrite?
			if (out.exists() && ! doOverwrite(in, out)) {
				skipped.add(in);
				if (verbose) {
					System.out.println("\tSkipped: "+in);
				}
				continue;
			}
			// sym link? Make a matching sym-link
			if (FileUtils.isSymLink(in)) {
				boolean symdir = in.isDirectory();
				if ( ! resolveSymlinks) {
					FileUtils.makeSymLink(in.getCanonicalFile(), out);
					// TODO avoid copying files within a symlinked dir
					if (symdir) symDirs.add(in);
					continue;
				} else {
					// carry on??
				}
			}
			// if we've sym-linked a directory, skip copying its sub-files
			for (File symDir : symDirs) {
				if (FileUtils.contains(symDir, in)) {
					continue;
				}
			}			
			// Don't copy directories - just create matching ones
			if (in.isDirectory()) {
				out.mkdir();
			} else {
				// Not part of this class's spec - but almost always a mistake
				assert ! out.getName().contains("svn") : in;
				// check for overlapping files
				if (copy2original.containsKey(out)) {
					String msg = "Duplicate file: "+in+" will overwrite "+copy2original.get(out)+" in "+this;
					if (errorOnDuplicate) {
						throw new IllegalArgumentException(msg);
					}
					Log.w(LOGTAG, msg);
				}
				// copy the file
				FileUtils.copy(in, out);
				copy2original.put(out, in);
			}
			if (verbose) {
				System.out.println("\tCopied: "+in);
			}
		}
	}
	
	public void setExceptionOnDuplicate(boolean errorOnDuplicate) {
		this.errorOnDuplicate = errorOnDuplicate;
	}

	
	
	protected boolean doOverwrite(File in, File out) {
		if (overwriteIfNewer) { 
			return in.lastModified() > out.lastModified();
		}
		return overwrite;
	}

	public void setFilter(FileFilter filter) {
		this.filter = filter;
	}

	/**
	 * This overwrites any previous filter, including negative filters.
	 *
	 * @param regexFilter
	 *            matches against the entire file path
	 * @see #setNegativeFilter(String)
	 */
	public void setFilter(String regexFilter) {
		filter = FileUtils.getRegexFilter(regexFilter);
	}

	/**
	 * If true (the default), hidden files are copied. Set to false to ignore
	 * hidden files such as .svn crap.
	 *
	 * @param include
	 *            true by default
	 * @see #setFilter(AFilter)
	 * @see #setNegativeFilter(String)
	 */
	public void setIncludeHiddenFiles(boolean include) {
		assert srcFiles == null;
		includeHiddenFiles = include;
	}

	/**
	 * Set the filter according to what it rejects. This overwrites any previous
	 * filter, including positive filters.
	 *
	 * @param regex
	 *            matches against the entire file path E.g. ".*\\.java" would
	 *            reject all .java files (and accept everything else).
	 */
	public void setNegativeFilter(String regex) {
		final FileFilter regexFilter = FileUtils.getRegexFilter(regex);
		setFilter(new FileFilter(){
			@Override
			public boolean accept(File pathname) {
				return ! regexFilter.accept(pathname);
			}
		});
	}

	/**
	 * If true (the default), files will try to overwrite existing files. If
	 * false, files that already exist will be quietly skipped over.
	 */
	public void setOverwrite(boolean overwrite) {
		this.overwrite = overwrite;
		if (overwrite) this.overwriteIfNewer = false;
	}

	public CopyTask setOverwriteIfNewer(boolean o) {
		this.overwriteIfNewer = o;
		if (o) this.overwrite = false;
		return this;
	}



}
