package com.winterwell.bob.tasks;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import com.winterwell.bob.BuildTask;
import com.winterwell.utils.StrUtils;
import com.winterwell.utils.Utils;
import com.winterwell.utils.containers.Containers;
import com.winterwell.utils.io.FileUtils;
import com.winterwell.utils.log.Log;
import com.winterwell.utils.time.Time;

/**
 * Create a jar file. Can also create .zip files since these are the same thing.
 * Hidden files are ignored by default - see {@link #setIncludeHiddenFiles(boolean)}
 * <p>
 * TODO better support for jarring multiple file sets - maybe a FileSet implements List<File> object?
 * @author daniel
 */
public class JarTask extends BuildTask {

	public static Map<String, Object> getManifest(File jar) {		
		try {
			JarFile jf = new JarFile(jar);
			Manifest m = jf.getManifest();
			Map<String, Attributes> es = m.getEntries();
			Attributes ma = m.getMainAttributes();			
			Map<String, Object> map = Containers.applyToKeys(ma, k -> k.toString());
			return map;
		} catch (IOException e) {
			throw Utils.runtime(e);
		} finally {
		}
	}
	
	/**
	 * Warning: this is only the ones set here, to be written by this task
	 * @return
	 */
	public Map<String, String> getManifestProps() {
		return manifestProps;
	}
	
	/**
	 * The version number for this jar
	 */
	public static final String MANIFEST_IMPLEMENTATION_VERSION = "Implementation-Version";
	/**
	 * Set this for executable jars
	 */
	public static final String MANIFEST_MAIN_CLASS = "Main-Class";
	/**
	 * The title for this jar
	 */
	public static final String MANIFEST_TITLE = "Implementation-Title";
	private boolean appendFlag;
	private File binDir;
	transient byte[] bytes = new byte[20 * 1024]; // 20k buffer
	Set<String> filenames;
	Iterable<File> files;
	private File inputBase;

	File jar;
	private final Map<String, String> manifestProps = new HashMap<String, String>();
	private boolean incHiddenFiles;

	/**
	 * @param incHiddenFiles If false - which is the default - hidden files will not be
	 * copied into the jar. If this is an append operation then hidden files in the original
	 * jar will still be preserved.
	 */
	public void setIncludeHiddenFiles(boolean incHiddenFiles) {
		this.incHiddenFiles = incHiddenFiles;
	}
	
	/**
	 * Create a jar from the files in a directory (recursive, includes
	 * everything)
	 * 
	 * @param jar
	 * @param inputDir Typically the bin folder where your .class files are
	 */
	public JarTask(File jar, File inputDir) {
		Utils.check4null(jar, inputDir);		
		this.jar = jar;
		binDir = inputDir;
		setManifestProperty("Packaging-Date", new Time().toString());
	}

	/**
	 * 
	 * @param jar
	 *            Jar file to create
	 * @param inputFiles
	 * @param inputBase
	 *            Use this to chop file names down to size. E.g. "/mydir" will
	 *            lead to "/mydir/myfile.class" being stored as "myfile.class"
	 */
	public JarTask(File jar, Iterable<File> inputFiles, File inputBase) {
		Utils.check4null(jar, inputFiles, inputBase);
		this.jar = jar;
		files = inputFiles;
		this.inputBase = inputBase;
	}

	private void addFile(File f, ZipOutputStream out) throws IOException {
		// Suitable?
		if (f.isDirectory())
			return;
		// Add ZIP entry to output stream.
		String path = inputBase == null ? f.getPath() : FileUtils
				.getRelativePath(f, inputBase);
		FileInputStream in = new FileInputStream(f);
		addFile2(path, in, out);
		in.close();
	}

	/**
	 * Shared by files and zipentrys
	 * 
	 * @param path
	 * @param in
	 * @param out
	 * @throws IOException
	 */
	void addFile2(String path, InputStream in, ZipOutputStream out)
			throws IOException {
		// convert separators in path?
		// I think this causes a bug in Windows
		// Nope!
//		path = path.replace('/', File.separatorChar);		
		// Ensure only 1 file per path
		if (filenames.contains(path)) {
			Log.w("JarTask", "Jar " + jar + " tried to add duplicate entries for "+ path);
			return;
		}
		filenames.add(path);
		// Add it
		out.putNextEntry(new ZipEntry(path));
		// Transfer bytes from the file to the ZIP file
		while (true) {
			int len = in.read(bytes);
			if (len == -1)
				break;
			out.write(bytes, 0, len);
		}
		// Complete the entry
		out.closeEntry();		
	}

	/**
	 * Create a manifest file if any properties were set
	 * 
	 * @param out
	 * @throws IOException
	 */
	void addManifest(ZipOutputStream out) throws IOException {
		if (manifestProps.size() == 0)
			return;
		String manifest = "";
		for (String k : manifestProps.keySet()) {
			manifest += k + ": " + manifestProps.get(k) + StrUtils.LINEEND;
		}
		out.putNextEntry(new ZipEntry("META-INF/MANIFEST.MF"));
		out.write(manifest.getBytes());
		out.closeEntry();
	}

	@Override
	public void doTask() throws Exception {
		if (binDir != null) {
			assert files==null : files;
			files = Arrays.asList(binDir);
			this.inputBase = binDir;
		}
		// Appending? Then preserve the old
		File tempFile = null;
		if (appendFlag && ! jar.exists()) {
			appendFlag = false;
		}
		if (appendFlag) {
			// get a temp file
			tempFile = File.createTempFile(jar.getName(), "temp");
			// delete it, otherwise you cannot rename your existing zip to it.
			FileUtils.delete(tempFile);
			// Move the current jar
			String jarpath = jar.getAbsolutePath();
			File oldJar = new File(jarpath);
			FileUtils.move(oldJar, tempFile);
			jar = new File(jarpath);
		}
		// Create the output!
		ZipOutputStream out = new ZipOutputStream(new FileOutputStream(jar));
		// Prevent name clashes - first entry wins
		filenames = new HashSet<String>();
		if (appendFlag && manifestProps.size() != 0) { 
			// Don't let the old manifest in
			filenames.add("META-INF/MANIFEST.MF");
			// But do copy its properties where they don't conflict
			Map<String, Object> oldManifest = getManifest(tempFile);
			oldManifest.entrySet().forEach(
					e -> manifestProps.putIfAbsent(e.getKey(), StrUtils.str(e.getValue()))
					);
		}
		// Add the files !
		doTask2_addFiles(files, out);
		
		// Appending? Then insert the old
		if (appendFlag) {
			ZipInputStream zin = new ZipInputStream(new FileInputStream(
					tempFile));
			ZipEntry entry = zin.getNextEntry();
			while (entry != null) {
				String name = entry.getName();
				addFile2(name, zin, out);
				entry = zin.getNextEntry();
			}
			// Delete temp file
			try {
				FileUtils.delete(tempFile);
			} catch (Exception e) {
				Log.w(LOGTAG, e);
			}
		}
		// Add manifest
		addManifest(out);
		// Complete the ZIP file
		out.close();
		System.out.println("Created: "+jar);
	}

	private void doTask2_addFiles(Iterable<File> files2, ZipOutputStream out) throws IOException {
		for (File f : files2) {
			if (f.isDirectory()) {
				// this is a recursive find
				List<File> childFiles = FileUtils.find(f, FileUtils.getRegexFilter(".*"), incHiddenFiles);
				for (File file : childFiles) {
					addFile(file, out);
				}
			} else {
				addFile(f, out);
			}
		}
	}

	/**
	 * If true, will not delete any existing jar file. Old entries will be
	 * overwritten TODO test age? youngest wins
	 * 
	 * @param appendFlag
	 */
	public void setAppend(boolean appendFlag) {
		this.appendFlag = appendFlag;
	}

	/**
	 * If any manifest properties are set, then a fresh manifest will be added
	 * to the jar.
	 * 
	 * NB: Packaging-Date is auto set this class
	 * 
	 * @param key
	 * @param value
	 */
	public void setManifestProperty(String key, String value) {
		manifestProps.put(key, value);
	}

}
