/**
 *
 */
package com.winterwell.utils.io;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.Flushable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.file.FileSystems;
import java.nio.file.PathMatcher;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.sql.Connection;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import com.winterwell.utils.FailureException;
import com.winterwell.utils.IFilter;
import com.winterwell.utils.Printer;
import com.winterwell.utils.Proc;
import com.winterwell.utils.ShellScript;
import com.winterwell.utils.StrUtils;
import com.winterwell.utils.TodoException;
import com.winterwell.utils.Utils;
import com.winterwell.utils.WrappedException;
import com.winterwell.utils.containers.Pair2;
import com.winterwell.utils.log.Log;
import com.winterwell.utils.web.WebUtils;
import com.winterwell.utils.web.XStreamUtils;


/**
 * Static file-related utility functions.
 *
 * @author Daniel Winterstein
 * @testedby {@link FileUtilsTest}
 */
public class FileUtils {

	public static final File[] ARRAY = new File[0];
	static final String ASCII = "ISO8859_1";

	/**
	 * A directory for storing Winterwell data
	 */
	private final static File dataDir = setDataDir();

	/**
	 * Officially 255 on Linux, though apparently it's sometimes lower. Windows
	 * may go down as low as 8! TODO: if we care, detect this at runtime.
	 */
	private static final int MAX_FILENAME_LENGTH = 240;

	public static final FileFilter NO_HIDDEN_FILES = new FileFilter() {
		@Override
		public boolean accept(File f) {
			return !f.isHidden();
		}

		@Override
		public String toString() {
			return "FileFilter[no hidden files]";
		};
	};
	/**
	 * Says yes to everything
	 */
	public static final FileFilter TRUE_FILTER = new FileFilter() {
		@Override
		public boolean accept(File pathname) {
			return true;
		}

		@Override
		public String toString() {
			return "true";
		};
	};

	/**
	 * Use this instead of the String for efficiency.
	 * NB: This leads to a slightly lenient decoder.
	 *
	 * @ASSUME UTF8 is supported!
	 */
	private static final Charset UTF_8 = Charset.forName(StrUtils.ENCODING_UTF8);

	/**
	 * FEFF because this is the Unicode char represented by the UTF-8 byte order
	 * mark (EF BB BF). See
	 * http://www.rgagnon.com/javadetails/java-handle-utf8-file-with-bom.html
	 */
	private static final char UTF8_BOM = '\uFEFF';
	/**
	 * e.g. "png"
	 * lowercase
	 */
	public static final List<String> IMAGE_TYPES = Arrays.asList("png", "jpg", "jpeg", "gif", "bmp", "tiff", "svg");

	/**
	 * Append a string to a file. Creates the file if necessary (the parent
	 * directory must already exist though).
	 */
	public static void append(String string, File file) {
		try {
			BufferedWriter w = getWriter(new FileOutputStream(file, true));
			w.write(string);
			close(w);
		} catch (IOException e) {
			throw new WrappedException(e);
		}
	}

	/**
	 *
	 * @param file
	 * @param type
	 *            E.g. "txt" Must not be null. If "", we remove the last type -
	 *            e.g. foo.bar.txt would be converted to foo.bar
	 *
	 * @return A file which is the same as file except for the type. E.g.
	 *         "mydir/myfile.html" to "mydir/myfile.txt"
	 */
	public static File changeType(File file, String type) {
		String fName = file.getName();
		int i = fName.lastIndexOf('.');
		if (type.length() == 0) {
			// pop last type
			if (i == -1)
				return file;
			fName = fName.substring(0, i);
			return new File(file.getParentFile(), fName);
		}
		// pop lead . if present
		if (type.charAt(0) == '.') {
			type = type.substring(1);
		}
		assert type.length() > 0;
		if (i == -1) {
			fName = fName + "." + type;
		} else {
			fName = fName.substring(0, i + 1) + type;
		}
		return new File(file.getParentFile(), fName);
	}

	/**
	 * Flush (if Flushable) and Close, swallowing any exceptions.
	 *
	 * @param io
	 *            Can be null
	 */
	public static void close(Closeable io) {
		if (io == null)
			return;
		// Flush first
		if (io instanceof Flushable) {
			try {
				((Flushable) io).flush();
			} catch (Exception e) {
				// Swallow
				System.err.println("FileUtils.close(): " + e); // e.printStackTrace()
			}
		}

		try {
			io.close();
		} catch (IOException e) {
			// Already closed?
			if (e.getMessage() != null && e.getMessage().contains("Closed"))
				return;
			// Swallow!
			// Log.report(e); - bad idea: this can cause an infinite loop if
			// report throws an IOExecption
			System.err.println("FileUtils.close(): " + e); // e.printStackTrace();
		}
	}

	/**
	 * @param io
	 * @return A closure which closes io.
	 */
	public static Runnable closeFn(final Closeable io) {
		return new Runnable() {
			@Override
			public void run() {
				close(io);
			}
		};
	}

	/**
	 * @param io
	 * @return A closure which closes io.
	 */
	public static Runnable closeFn(final Connection io) {
		return new Runnable() {
			@Override
			public void run() {
				SqlUtils.close(io);
			}
		};
	}

	/**
	 * @param io
	 * @return A closure which closes io.
	 */
	public static Runnable closeFn(final Statement io) {
		return new Runnable() {
			@Override
			public void run() {
				SqlUtils.close(io);
			}
		};
	}

	/**
	 * Convenience method for {@link #copy(File, File, boolean)} with
	 * overwrite=true
	 *
	 *
	 * @param in
	 *            A file or directory. Copying a directory will lead to a merge
	 *            with the target directory, where existing files are left alone
	 *            unless a copied file overwrites them. Note that copying a
	 *            directory *will* copy hidden files such as .svn files.
	 * @param out
	 *            Can be a target file or a directory. Parent directories must
	 *            already exist. If /in/ is a file and /out/ is a directory, the
	 *            name of in will be used to create a target file inside out.
	 * @return the file/directory copied to
	 */
	public static File copy(File in, File out) {
		return copy(in, out, true);
	}

	/**
	 * Copy from in to out.
	 *
	 * @param in
	 *            A file or directory. Copying a directory will lead to a merge
	 *            with the target directory, where existing files are left alone
	 *            unless a copied file overwrites them. Note that copying a
	 *            directory *will* copy hidden files such as .svn files.
	 * @param out
	 *            Can be a target file or a directory. Parent directories must
	 *            already exist. If in is a file and out is a directory, the
	 *            name of in will be used to create a target file inside out.
	 * @param overwrite
	 *            if true existing files will be overwritten. If false, existing
	 *            files will lead to an IORException
	 * @return the file/directory copied to (so out, or a file in out)
	 * @throws IORException
	 *             If copying a directory, this is thrown at the end of the
	 *             operation. As many files as possible are copied, then the
	 *             exception is thrown.
	 *
	 */
	public static File copy(File in, File out, boolean overwrite)
			throws WrappedException {
		if ( ! in.exists()) {
			throw Utils.runtime(new FileNotFoundException(in.getAbsolutePath()));
		}
		assert !in.equals(out) : in + " = " + out + " can cause a delete!";
		// recursively copy directories
		if (in.isDirectory()) {
			ArrayList<File> failed = new ArrayList<File>();
			copyDir(in, out, overwrite, failed);
			// Failed any?
			if (failed.size() != 0)
				throw new FailureException("Could not copy files: "
						+ Printer.toString(failed));
			return out;
		}
		if (out.isDirectory()) {
			out = new File(out, in.getName());
		}
		try {
			if (out.exists() && !overwrite)
				throw new FailureException("Copy failed: " + out
						+ " already exists.");
			// TODO use NIO for efficiency!
			copy(new FileInputStream(in), out);
			return out;
		} catch (IOException e) {
			throw new FailureException(e.getMessage() + " copying "
					+ in.getAbsolutePath() + " to " + out.getAbsolutePath());
		}
	}

	/**
	 * Copy from in to out. Closes both streams when done.
	 *
	 * @param in
	 * @param out
	 */
	public static void copy(InputStream in, File out) {
		assert in != null && out != null;
		// resolve local files to abs paths
		out = out.getAbsoluteFile();
		if ( ! out.getParentFile().isDirectory())
			throw new FailureException("Directory does not exist: "
					+ out.getParentFile());
		try {
			FileOutputStream outStream = new FileOutputStream(out);
			copy(in, outStream);
		} catch (IOException e) {
			throw new WrappedException(e);
		}
	}

	/**
	 * Copy from in to out. Closes both streams when done.
	 *
	 * @param in
	 * @param out
	 */
	public static void copy(InputStream in, OutputStream out) {
		try {
			byte[] bytes = new byte[20 * 1024]; // 20k buffer
			while (true) {
				int len = in.read(bytes);
				if (len == -1) {
					break;
				}
				out.write(bytes, 0, len);
			}
		} catch (IOException e) {
			throw new WrappedException(e);
		} finally {
			close(in);
			close(out);
		}
	}

	/**
	 * Recursively copy a directory
	 *
	 * @param in
	 * @param out
	 * @param overwrite
	 *            applies to files, not directories. Directories are merged. Use
	 *            delete then copy if you want a true overwrite
	 * @throws IORException
	 *             if a file or directory cannot be copied. This is thrown at
	 *             the end of the operation. As many files as possible are
	 *             copied, then the exception is thrown.
	 */
	private static void copyDir(File in, File out, boolean overwrite,
			List<File> failed) {
		assert in.isDirectory() : in;
		// Create out?
		if (!out.exists()) {
			boolean ok = out.mkdirs();
			if (!ok) {
				failed.add(in);
				return;
			}
		}
		assert out.isDirectory() : out;
		for (File f : in.listFiles()) {
			// recurse on dirs
			if (f.isDirectory()) {
				File subOut = new File(out, f.getName());
				copyDir(f, subOut, overwrite, failed);
				continue;
			}
			try {
				copy(f, out, overwrite);
			} catch (WrappedException e) {
				failed.add(f);
			}
		}
	}

	/**
	 * @return a shiny new directory in temp space
	 */
	public static File createTempDir() {
		try {
			File f = File.createTempFile("tmp", "dir");
			if (f.exists()) {
				delete(f);
			}
			f.mkdirs();
			return f;
		} catch (Exception e) {
			throw Utils.runtime(e);
		}
	}

	/**
	 * Runtime exception wrapper for {@link File#createTempFile(String, String)}
	 * .
	 *
	 * @param prefix
	 * @param suffix
	 */
	public static File createTempFile(String prefix, String suffix) {
		try {
			return File.createTempFile(prefix, suffix);
		} catch (IOException e) {
			throw new WrappedException(e);
		}
	}

	/**
	 * This is a workaround for bugs under Windows.
	 *
	 * @param file
	 *            Delete this file. Returns quietly if the file does not exist.
	 *            If file is null - does nothing.
	 */
	public static void delete(File file) {
		if (file == null || ! file.exists())
			return;
		boolean ok = file.delete();
		if (ok)
			return;
		System.gc();
		ok = file.delete();
		if (ok)
			return;
		try {
			Thread.sleep(50);
		} catch (InterruptedException e) {
			//
		}
		ok = file.delete();
		if (!file.exists())
			return;
		// Hm: Is it a sym-linked directory?
		if (file.isDirectory() && isSymLink(file)) {
			// try an OS call
			if (Utils.getOperatingSystem().contains("linux")
					|| Utils.getOperatingSystem().contains("unix")) {
				String path = file.getAbsolutePath();
				Proc p = new Proc("rm -f " + path);
				p.run();
				p.waitFor(1000);
				if (!file.exists())
					return;
				throw new WrappedException(new IOException(
						"Could not delete file " + file + "; " + p.getError()));
			}
		}
		throw new WrappedException(new IOException("Could not delete file "
				+ file));
	}

	/**
	 * @param file
	 *            Delete this directory & everything in it (recurses through
	 *            directories).
	 */
	public static void deleteDir(File file) {
		if (!file.isDirectory())
			throw new FailureException(file + " is not a directory");
		if (isSymLink(file)) {
			// Just delete the link, not the contents
			delete(file);
			return;
		}
		for (File f : file.listFiles()) {
			if (f.isDirectory()) {
				deleteDir(f);
			} else {
				delete(f);
			}
		}
		delete(file);
	}

	private static void deleteNative(File out) {
		if (!Utils.OSisUnix())
			throw new TodoException("" + out);
		Proc p = new Proc("rm -f " + out.getAbsolutePath());
		p.run();
		int ok = p.waitFor();
		if (ok != 0)
			throw new FailureException(p.getError());
	}

	public static String filenameDecode(String name) {
		// remove extra //s
		name = name.replace("//", "");
		// switch % for _
		name = name.replace("_", "%");
		name = name.replace("%%", "_");
		// TODO how to remove the extra /s??
		String original = WebUtils.urlDecode(name);
		original = original.replace("%2E", ".");
		original = original.replace("%3B", ";");
		original = original.replace("%2F", "/");
		return original;
	}

	/**
	 * Convert a string so that it can safely be used as a filename. one-to-one
	 * mapping
	 *
	 * @param name
	 * @param if false, will remove /s and \s
	 */
	public static String filenameEncode(final String name) {
		// Use url encoding
		String url = WebUtils.urlEncode(name);
		// extra clarity -- don't encode if we can avoid it
		url = url.replace("%3D", "=");
		// safety
		// url-code for . would be %2E
		url = url.replace("..", ".%2E"); // no jumping up a directory
		url = url.replace(";", "%3B"); // This one is redundant paranoia
		// put the /s back in, but avoid // in case of overlap with the added /
		// markers below
		url = url.replace("%2F", "/");
		url = url.replace("//", "/%2F");

		// switch _ for %
		url = url.replace("_", "__");
		url = url.replace("%", "_");
		// Split sections down to a max length
		String[] bits = url.split("/");
		StringBuilder path = new StringBuilder(url.length());
		boolean dbl = false;
		for (String bit : bits) {
			if (bit.length() == 0) {
				// TODO
				System.out.println(path);
			}
			for (int i = 0; i < bit.length(); i += MAX_FILENAME_LENGTH) {
				int e = Math.min(bit.length(), i + MAX_FILENAME_LENGTH);
				path.append(bit.substring(i, e));
				if (e == bit.length()) {
					path.append("/");
					dbl = false;
				} else {
					// add a marker to let us know this / was added
					path.append("//");
					dbl = true;
				}
			}
		}

		// was it a // we added?
		StrUtils.pop(path, dbl ? 2 : 1);

		// trailing /
		if (url.endsWith("/")) {
			path.append('/');
		}

		return path.toString();
	}

	/**
	 * Recursively search for files by filter. Convenience method for
	 * {@link #find(File, FileFilter, boolean)} with includeHiddenFiles = true
	 *
	 * @param baseDir
	 * @param filter
	 * @return Can be empty, never null
	 */
	public static List<File> find(File baseDir, FileFilter filter) {
		return find(baseDir, filter, true);
	}

	/**
	 * Recursively search for files by filter
	 *
	 * @param baseDir
	 *            Must exist and be a directory
	 * @param filter
	 * @param includeHiddenFiles
	 *            If false, hidden files are ignored, as are hidden
	 *            sub-directories. A file is considered hidden if:
	 *            {@link File#isHidden()} returns true or the file name begins
	 *            with a .
	 * @return Can be empty, never null
	 */
	public static List<File> find(File baseDir, FileFilter filter,
			boolean includeHiddenFiles) {
		if (!baseDir.isDirectory())
			throw new IllegalArgumentException(baseDir.getAbsolutePath()
					+ " is not a directory");
		List<File> files = new ArrayList<File>();
		find2(baseDir, filter, files, includeHiddenFiles);
		return files;
	}

	/**
	 * Recursively search for files by filename. Includes hidden files if they
	 * match.
	 *
	 * @param baseDir
	 * @param regex
	 *            A regex matching against the <b>entire absolute file-path</b>.
	 *            E.g. ".*\\.txt" for .txt files or ".*mydir/.*\\.txt" for files
	 *            in mydir -- "*.txt" would not be valid.
	 * @return list of files, with paths relative to the baseDir but including
	 *         baseDir ??change this?
	 */
	public static List<File> find(File baseDir, String regex) {
		return find(baseDir, new RegexFileFilter(regex));
	}

	private static void find2(File baseDir, FileFilter filter,
			List<File> files, boolean includeHiddenFiles) {
		assert baseDir != null && filter != null && files != null;
		for (File f : baseDir.listFiles()) {
			if (f.equals(baseDir)) {
				continue;
			}
			// Hidden?
			if (!includeHiddenFiles && f.isHidden()) {
				continue;
			}
			assert includeHiddenFiles || !f.getName().startsWith(".") : f;
			// Add?
			if (filter.accept(f)) {
				if (f.exists()) {
					files.add(f);
				} else {
					// a broken symlink -- skip it
				}
			}
			// Recurse
			if (f.isDirectory()) {
				find2(f, filter, files, includeHiddenFiles);
			}
		}
	}

	/**
	 * Retrieve all classes from the specified path.
	 *
	 * @param root
	 *            Root of directory of where to search for classes.
	 * @return List of classes on the form "com.company.ClassName".
	 *
	 * @author Jacob Dreyer, released as public with permission to edit and use
	 *         on
	 *         http://www.velocityreviews.com/forums/t149403-junit-html-report
	 *         .html Some modifications by Daniel Winterstein
	 */
	private static List<String> getAllClasses(File root) throws IOException {
		assert root != null : "Root cannot be null";

		// Prepare the return array
		List<String> classNames = new ArrayList<String>();

		// Get all classes recursively
		String path = root.getCanonicalPath();
		getAllClasses(root, path.length() + 1, classNames);

		return classNames;
	}

	/**
	 * Retrive all classes from the specified path.
	 *
	 * @param root
	 *            Root of directory of where to search for classes.
	 * @param prefixLength
	 *            Index into root path name of path considered.
	 * @param result
	 *            Array to add classes found
	 */
	private static void getAllClasses(File root, int prefixLength,
			List<String> result) throws IOException {
		assert root != null : "Root cannot be null";
		assert prefixLength >= 0 : "Illegal index specifier";
		assert result != null : "Missing return array";

		// Scan all entries in the directory
		for (File entry : root.listFiles()) {

			// If the entry is a directory, get classes recursively
			if (entry.isDirectory()) {
				if (entry.canRead()) {
					getAllClasses(entry, prefixLength, result);
				}
				continue;
			}
			// Entry is a file. Filter out non-classes and inner classes
			String path = entry.getPath();
			boolean isClass = path.endsWith(".class") && path.indexOf("$") < 0;
			if (!isClass) {
				continue;
			}
			String name = entry.getCanonicalPath().substring(prefixLength);
			String className = name.replace(File.separatorChar, '.').substring(
					0, name.length() - 6);
			result.add(className);
		}
	}

	/**
	 *
	 * @param filen
	 * @return file name without last .suffix E.g. foo.bar to "foo" Note:
	 *         <i>does</i> strip out directories, so /dir/foo.bar would go to
	 *         "foo"
	 */
	public static String getBasename(File file) {
		return getBasename(file.getName());
	}

	/**
	 *
	 * @param filename
	 * @return filename without last .suffix E.g. "foo.foo.bar" to "foo.foo"
	 *         Note: does not strip out directories, so "/dir/foo.bar" would go
	 *         to "/dir/foo"
	 */
	public static String getBasename(String filename) {
		int i = filename.lastIndexOf('.');
		if (i == -1)
			return filename;
		return filename.substring(0, i);
	}

	/**
	 * Like {@link #getBasename(String)}, except this will ignore endings that
	 * are one char, longer than 4 characters, or not a-zA-Z0-9.
	 *
	 * @param filename
	 * @return e.g. "mybase" from "mybase.html", but
	 *         "winterwell.utils.FileUtils" will be unchanged!
	 *
	 * @testedby {@link FileUtilsTest#testGetBasenameCautious()}
	 */
	public static String getBasenameCautious(String filename) {
		int i = filename.lastIndexOf('.');
		if (i == -1)
			return filename;
		if (filename.length() - i > 5)
			return filename;
		if (filename.length() - i < 3) {
			return filename;
		}
		String type = filename.substring(i+1, filename.length());
		Pattern az09 = Pattern.compile("[a-zA-Z0-9]+");
		if ( ! az09.matcher(type).matches()) {
			// the end was something odder than a file-type
			return filename;
		}
		return filename.substring(0, i);
	}

	/**
	 * @param relativePath
	 * @return file in the Winterwell data directory
	 * @deprecated Use winterwell.storage instead
	 */
	@Deprecated
	public static File getDataFile(String relativePath) {
		File f = new File(dataDir, relativePath);
		if (!f.getParentFile().exists()) {
			f.getParentFile().mkdirs();
		}
		return f;
	}

	/**
	 * @deprecated Generally prefer {@link #getType(File)}.
	 * Return the full extension of the given file. This includes the leading
	 * period. Always lower case. Can be "", never null.
	 * <p>
	 * This is identical to {@link #getType(String)} but with the "." included.
	 */
	public static String getExtension(File f) {
		String ftype = getType(f);
		return ftype.length()==0? "" : "."+ftype;
	}

	/**
	 * @deprecated Generally prefer {@link #getType(File)}.
	 * This is identical to {@link #getType(String)} but with the "." included. 
	 */
	public static String getExtension(String filename) {
		String ftype = getType(filename);
		return ftype.length()==0? "" : "."+ftype;
	}

	/**
	 * @param file
	 * @return a non-existent file based on the input file. E.g. given
	 *         /home/myfile.txt this might return /home/myfile2.txt. Will return
	 *         file if it doesn't exist.
	 * @throws IORException
	 *             if you reach >10000 files of the same base name. This being a
	 *             strong sign that perhaps either a clean-up or some other
	 *             storage mechanism should be considered.
	 */
	public static File getNewFile(File file) {
		if (!file.exists())
			return file;
		String path = file.getParent();
		String name = file.getName();
		int dotI = name.lastIndexOf('.');
		String preType;
		String dotType = "";
		if (dotI == -1) {
			preType = name;
		} else {
			preType = name.substring(0, dotI);
			dotType = name.substring(dotI);
		}
		for (int i = 2; i < 10000; i++) {
			File f = new File(path, preType + i + dotType);
			if (!f.exists())
				return f;
		}
		throw new FailureException(
				"Could not find a non-existing file name for " + file);
	}

	public static BufferedReader getReader(File file) {
		if (file==null) throw new NullPointerException("No file for getReader()");
		try {
			// TODO handle .gz here??
			return getReader(new FileInputStream(file));
		} catch (FileNotFoundException e) {
			throw new WrappedException(e);
		}
	}

	/**
	 * UTF8 and buffered
	 *
	 * @param in
	 * @return
	 */
	public static BufferedReader getReader(InputStream in) {
		assert in != null;
		try {
			InputStreamReader reader = new InputStreamReader(in, UTF_8);
			// \uFFFD
			return new BufferedReader(reader);
		} catch (Exception e) {
			throw new WrappedException(e);
		}
	}

	/**
	 * @param regex
	 * @return A regex filter that must match <i>all of</i> the <i>absolute file
	 *         path</i>. The regex must usually be happy accepting an initial .*
	 *         portion!
	 */
	public static FileFilter getRegexFilter(String regex) {
		return new RegexFileFilter(regex);
	}

	/**
	 *
	 * @param f
	 * @param base
	 * @return the path of f, relative to base. e.g. "/a/b/c.txt" relative to
	 *         "/a" is "b/c.txt" This method uses absolute paths.
	 * @throws IllegalArgumentException
	 *             if f is not a sub path of base
	 * @testedby {@link FileUtilsTest#testGetRelativePath()}
	 */
	public static String getRelativePath(File f, File base) throws IllegalArgumentException
	{
		String fp = f.getAbsolutePath();
		String bp = base.getAbsolutePath();
		if ( ! fp.startsWith(bp)) {
			if (f.equals(base))
				return ""; // Is this what we want?
			throw new IllegalArgumentException(f + "=" + fp
					+ " is not a sub-file of " + base + "=" + bp);
		}
		String rp = fp.substring(bp.length());
		if (rp.isEmpty()) {
			return rp; // f = base
		}
		char ec = rp.charAt(0); // TODO a bit more efficient
		if (ec == '\\' || ec == '/') {
			rp = rp.substring(1);
		}
		return rp;

	}

	/**
	 * @param f
	 * @return "txt", or "". Never null. Always lowercase. Does not include the "."
	 */
	public static String getType(File f) {
		String fs = f.getName();
		return getType(fs);
	}

	/**
	 * @param filename
	 * @return E.g. "txt" Maybe "", never null. Always lowercase
	 */
	public static String getType(String filename) {
		int i = filename.lastIndexOf(".");
		int slashi = Math.max(filename.lastIndexOf("/"), filename.lastIndexOf("\\"));
		// e.g. foo.bar/wibble
		if (i < slashi) return "";
		if (i == -1 || i == filename.length() - 1)
			return "";
		return filename.substring(i + 1).toLowerCase();
	}

	/**
	 * @deprecated Try to avoid this, as it bakes in Winterwell specific folder structure.
	 * 
	 * WINTERWELL_HOME if defined. This is the directory which contains code,
	 * companies, business, etc.
	 * It's /home/winterwell on servers.
	 * 
	 * @see WinterwellProjectFinder
	 */
	public static File getWinterwellDir() throws FailureException {
		try {
			String dd = System.getenv("WINTERWELL_HOME");
			if (!Utils.isBlank(dd)) {
				if (dd.startsWith("~")) {
					// ~ goes awry in Windows at least
					String home = System.getProperty("user.home");
					if (home != null) {
						dd = home + "/" + dd.substring(1);
					}
				}
				File f = new File(dd).getCanonicalFile();
				if (!f.exists())
					throw new FailureException(
							"Path does not exist: WINTERWELL_HOME = " + f);
				return f;
			}

			// (home)/winterwell?
			String home = System.getProperty("user.home");
			// No home? try /home/winterwell?
			if (Utils.isBlank(home)) {
				home = "/home";
			}
			File ddf = new File(home, "winterwell").getCanonicalFile();
			if (ddf.exists() && ddf.isDirectory())
				return ddf;

			File hardcoded = new File("/home/winterwell").getCanonicalFile();
			if (hardcoded.exists() && hardcoded.isDirectory())
				return hardcoded;
			
			// Does the local folder or its parent or grandparent look OK? 
			// Let's test for the presence of the open-code repo.
			File wd = getWorkingDirectory();
			for(int i=0; i<3; i++) {
				if (new File(wd, "open-code").isDirectory()) {
					// yeh :)
					return wd;
				}
				wd = wd.getParentFile();
				if (wd==null) break;
			}
			
			// Give up
			throw new FailureException(
					"Could not find directory - environment variable WINTERWELL_HOME is not set, and "+ddf+" is not valid.");
		} catch (IOException e) {
			throw Utils.runtime(e);
		}
	}

	/**
	 * @return The application directory, in canonical form.
	 */
	public static File getWorkingDirectory() {
		// String prop = System.getProperty("user.dir"); //??why not use this??
		try {
			return new File(".").getCanonicalFile();
		} catch (IOException e) {
			throw new WrappedException(e);
		}
	}
	
	public static File getUserDirectory() {
		String uh = System.getProperty("user.home");
		if (uh==null) throw new IllegalStateException("user.home property not set");
		File f = new File(uh);
		return f;
	}

	/**
	 * @param file
	 * @return a buffered file writer, using UTF8 encoding if possible.
	 */
	public static BufferedWriter getWriter(File file) {
		try {
//			try {
			return new BufferedWriter(new OutputStreamWriter(
					new FileOutputStream(file), UTF_8));
//			} catch (UnsupportedEncodingException e) {
//				return new BufferedWriter(new OutputStreamWriter(
//						new FileOutputStream(file)));
//			}
		} catch (IOException ex) {
			throw new WrappedException(ex);
		}
	}

	/**
	 * @param out
	 * @return a buffered UTF8 encoded writer.
	 */
	public static BufferedWriter getWriter(OutputStream out) {
//		try {
		OutputStreamWriter writer = new OutputStreamWriter(out, UTF_8);
		return new BufferedWriter(writer);
//		} catch (UnsupportedEncodingException e) {
//			throw new WrappedException(e);
//		}
	}

	/**
	 * @param file .gz
	 * @see ZipFile for .zip files
	 */
	public static BufferedReader getGZIPReader(File file) {				
		try {
			FileInputStream fos = new FileInputStream(file);
			GZIPInputStream zos = new GZIPInputStream(fos);
			return getReader(zos);
		} catch (IOException ex) {
			throw Utils.runtime(ex);
		}
	}
	
	/**
	 * @param file
	 * @return a writer which will output compressed. Uses gzip format.
	 */
	public static BufferedWriter getGZIPWriter(File file, boolean append) {
		try {
			FileOutputStream fos = new FileOutputStream(file, append);
			GZIPOutputStream zos = new GZIPOutputStream(fos);
			return getWriter(zos);
		} catch (IOException ex) {
			throw Utils.runtime(ex);
		}
	}

	/**
	 * Sort of a bit like grep
	 *
	 * @param baseDir
	 * @param regex
	 *            search for this within a text line
	 * @param fileNameRegex
	 * @return
	 */
	public static Iterable<Pair2<String, File>> grep(File baseDir,
			String regex, String fileNameRegex) {
		List<File> files = find(baseDir, fileNameRegex);
		Pattern p = Pattern.compile(regex);
		List<Pair2<String, File>> found = new ArrayList<Pair2<String, File>>();
		for (File file : files) {
			String[] lines = StrUtils.splitLines(FileUtils.read(file));
			for (String line : lines) {
				if (!p.matcher(line).find()) {
					continue;
				}
				found.add(new Pair2<String, File>(line, file));
			}
		}
		return found;
	}

	/**
	 * Checks for the presence of potentially dangerous characters in a
	 * filename. I.e. which could be used for a hacking attack. This includes
	 * checking for the use of ".." to access higher directories.
	 *
	 * @param filename
	 * @return true if this filename is kosher
	 */
	public static boolean isSafe(String filename) {
		if (Utils.isBlank(filename))
			return false;
		if (filename.contains(".."))
			return false;
		if (filename.contains(";"))
			return false;
		if (filename.contains("|"))
			return false;
		if (filename.contains(">"))
			return false;
		if (filename.contains("<"))
			return false;
		return true;
	}

	/**
	 * Covers the most common video file types -- but this is NOT a complete list or a rigourous test.
	 * @param attachment
	 * @return
	 */
	public static boolean isVideo(File attachment) {		
		String ftype = getType(attachment);
		// NB: Facebook supports a longer list: https://developers.facebook.com/docs/graph-api/video-uploads
		// np4 is ambiguous :(
		return Arrays.asList("mpg", "mpeg", "mpeg4", "divx", "mov", "wmv", "m4v", "mp4", "avi").contains(ftype);
	}	

	/**
	 * Covers the most common image file types -- but this is NOT a complete list or a rigourous test.
	 */
	public static boolean isImage(File file) {
		String ftype = getType(file);
		return IMAGE_TYPES.contains(ftype);
	}
	
	/**
	 * @param f
	 * @return true if f is a sym-link. Note: returns false if f is not itself a
	 *         sym-link, but has a sym-linked directory in it's path.
	 * @testedby {@link FileUtilsTest#testIsSymLink()}
	 */
	public static boolean isSymLink(File f) {
		try {
			// TODO use `ls -l` or `stat` on Linux & check for "->"??
			// TODO use Java 7 classes if available??
			File canon = f.getCanonicalFile();
			if (!canon.getName().equals(f.getName()))
				return true;
			// same name, different dirs?
			File parent = f.getParentFile();
			if (parent == null) {
				parent = f.getAbsoluteFile().getParentFile();
			}
			if (parent == null)
				// no parent => a file system root
				return false;
			parent = parent.getCanonicalFile();
			File canonParent = canon.getParentFile();
			if (!parent.equals(canonParent))
				return true;
			return false;
		} catch (IOException e) {
			throw Utils.runtime(e);
		}
	}

	/**
	 * Inverse to {@link #save(Object, File)}.
	 *
	 * @param file
	 */
	public static <X> X load(File file) {
		BufferedReader reader = null;
		try {
			reader = getReader(file);
			X obj = (X) XStreamUtils.serialiseFromXml(reader);
			return obj;
		} finally {
			FileUtils.close(reader);
		}
	}

	/**
	 * Read in a java .properties config file
	 *
	 * @param propsFile
	 * @return
	 */
	public static Properties loadProperties(File propsFile) {
		InputStream stream = null;
		try {
			stream = new FileInputStream(propsFile);
			Properties props = new Properties();
			props.load(stream);
			return props;
		} catch (IOException e) {
			throw Utils.runtime(e);
		} finally {
			close(stream);
		}
	}

	/**
	 * @param dir
	 * @param fileNameRegex
	 *            ??Should this be a glob pattern e.g. "*.txt" instead of
	 *            ".*\\.txt"?
	 * @return files in dir matching the regex pattern
	 * @see #find(File, String) which is recursive
	 */
	public static File[] ls(File dir, String fileNameRegex) {
		if (!dir.isDirectory())
			throw new FailureException(dir + " is not a valid directory");
		return dir.listFiles(getRegexFilter(".*" + fileNameRegex));
	}

	/**
	 * Convenience for {@link #makeSymLink(File, File, boolean)} with overwrite
	 * = true
	 *
	 * @param original
	 * @param out
	 */
	public static void makeSymLink(File original, File out) {
		makeSymLink(original, out, true);
	}

	/**
	 * Make a symlink. Only works on Linux!
	 *
	 * @param original
	 *            the target of the link NB if this itself is a link, it will be
	 *            dereferenced
	 * @param origin
	 *            the symlink itself
	 * @throws IORException
	 *             if overwrite is false and out already exists
	 * @testedby {@link FileUtilsTest#testMakeSymLink()}
	 */
	// TODO? Java 7 has sym-link support via Path
	public static void makeSymLink(File original, File out, boolean overwrite) {
		if (!Utils.getOperatingSystem().contains("linux"))
			throw new TodoException();
		// no links to self
		if (original.getAbsolutePath().equals(out.getAbsolutePath()))
			throw new IllegalArgumentException("Cannot sym-link to self: "
					+ original + " = " + out);
		// the source must exist
		if (!original.exists())
			throw new WrappedException(new FileNotFoundException(
					original.getPath()));
		if (!original.isDirectory() && !original.isFile())
			throw new FailureException("Weird: " + original);
		if (out.exists()) {
			if (overwrite) {
				FileUtils.delete(out);
			} else
				throw new FailureException("Creating symlink failed: " + out
						+ " already exists.");
		}
		try {
			original = original.getCanonicalFile();
			ShellScript ss = new ShellScript("ln -s " + original + " " + out);
			ss.run();
			ss.waitFor();
			String err = ss.getError();
			if (!Utils.isBlank(err)) {
				if (overwrite && err.contains("File exists")) {
					// this can happen if the sym-link is to a non-existent file
					// File.exists() returns false for sym-links to non-existent
					// files (Java 6 on Ubuntu).
					// Or it could be a race condition.
					// However: if overwrite is true then sod the race
					// condition.
					// Trash!
					deleteNative(out);
					// try again (infinite loop risk should be very minor)
					makeSymLink(original, out, overwrite);
					return;
				}
				throw new RuntimeException(err);
			}
		} catch (Exception e) {
			throw Utils.runtime(e);
		}
	}

	/**
	 * Move a file. More robust than {@link File#renameTo(File)}, which can
	 * silently fail where a copy+delete would succeed. This will overwrite old
	 * files.
	 *
	 * @param src
	 * @param dest
	 * @throws IORException
	 * @return dest TODO @testedby {@link FileUtilsTest#testMove()} TODO test
	 *         this works properly with relative Files
	 */
	public static File move(File src, File dest) {
		if ( ! src.exists()) throw Utils.runtime(new FileNotFoundException(src.getAbsolutePath()));
		// protect the path of the src object from being modified
		File src2 = new File(src.getPath());
		assert src2.equals(src);
		boolean ok = src2.renameTo(dest);
		if (ok)
			return dest;
		// oh well: copy+delete
		FileUtils.copy(src, dest);
		FileUtils.delete(src);
		return dest;
	}

	/**
	 * Count the number of lines in a file.
	 *
	 * @param file
	 * @return
	 */
	public static int numLines(File file) {
		int cnt = 0;
		try {
			BufferedReader r = FileUtils.getReader(file);
			while (true) {
				String line = r.readLine();
				if (line == null) {
					break;
				}
				cnt++;
			}
			return cnt;
		} catch (IOException e) {
			throw new WrappedException(e);
		}
	}

	/**
	 * Like append but adds to the start of a file. Not terribly efficient -
	 * involves copying the whole file twice.
	 *
	 * @param file
	 *            If this does not exist it will be created
	 * @param string
	 *            Must not be null
	 */
	public static void prepend(File file, String string) {
		assert !file.isDirectory() && string != null;
		// Does the file exist?
		if (!file.exists() || file.length() == 0) {
			write(file, string);
			return;
		}
		try {
			File temp = File.createTempFile("prepend", "");
			write(temp, string);
			FileInputStream in = new FileInputStream(file);
			FileOutputStream out = new FileOutputStream(temp, true);
			copy(in, out);
			move(temp, file);
		} catch (IOException e) {
			throw new WrappedException(e);
		}
	}

	/**
	 * @param file
	 *            Will be opened, read and closed
	 * @return The contents of file
	 */
	public static String read(File file) throws WrappedException {
		try {
			return read(new FileInputStream(file));
		} catch (IOException e) {
			throw Utils.runtime(e);
		}
	}

	/**
	 * @param input
	 *            Will be read and closed
	 * @return The contents of input
	 */
	public static String read(InputStream in) {
		return read(getReader(in));
	}

	/**
	 * @param r
	 *            Will be read and closed
	 * @return The contents of input
	 */
	public static String read(Reader r) {
		try {
			BufferedReader reader = r instanceof BufferedReader ? (BufferedReader) r
					: new BufferedReader(r);
			final int bufSize = 8192; // this is the default BufferredReader
			// buffer size
			StringBuilder sb = new StringBuilder(bufSize);
			char[] cbuf = new char[bufSize];
			while (true) {
				int chars = reader.read(cbuf);
				if (chars == -1) {
					break;
				}
				// Workaround: ignore the byte-order-mark (BOM), if present.
				// If left in, this can upset e.g. Xerces
				if (sb.length() == 0 && cbuf[0] == UTF8_BOM) {
					sb.append(cbuf, 1, chars - 1);
				} else {
					// normal case
					sb.append(cbuf, 0, chars);
				}
			}
			return sb.toString();
		} catch (IOException e) {
			throw new WrappedException(e);
		} finally {
			FileUtils.close(r);
		}
	}

	/**
	 * @param raw
	 * @return The bytes from raw, just as they come.
	 */
	public static byte[] readRaw(InputStream raw) {
		// Troves' TByteArrayList would be nice here
		try {
			byte[] all = new byte[10240]; // ~10k
			int offset = 0;
			while (true) {
				int space = all.length - offset;
				// double the space if near the end
				if (space < all.length / 8) {
					all = Arrays.copyOf(all, all.length * 2);
					space = all.length - offset;
				}
				// read
				int r = raw.read(all, offset, space);
				if (r == -1) {
					break;
				}
				offset += r;
			}
			byte[] trimmed = Arrays.copyOf(all, offset);
			return trimmed;
		} catch (Exception e) {
			throw new WrappedException(e);
		}
	}

	/**
	 * Use File.getCanonicalPath to resolve any .. sections
	 * @param absolutePath
	 * @return given a/b/../c.txt resolve to a/c.txt
	 */
	public static String resolveDotDot(String absolutePath) {
		// getAbsolutePath will leave in ..s!
		try {
			return new File(absolutePath).getCanonicalPath();
		} catch (IOException e) {
			throw Utils.runtime(e);
		}
	}

	/**
	 * Convert a string so that it can safely be used as a filename. Does not
	 * remove /s or \s. Does allow sub-dirs
	 *
	 * @param name
	 * @return
	 */
	public static String safeFilename(String name) {
		return safeFilename(name, true);
	}

	/**
	 * Convert a string so that it can safely be used as a filename. WARNING:
	 * Many to one mapping! TODO make this a one-to-one mapping
	 *
	 * @param name
	 *            Limited to 5000 chars
	 * @param if false, will remove /s and \s. Note: .. is never allowed (it gets converted to a safer form)
	 * @see isSafe
	 * @see filenameEncode
	 */
	public static String safeFilename(String name, boolean allowSubDirs) {
		if (name == null)
			return "null";
		name = name.trim();
		if (name.equals("")) {
			name = "empty";
		}
		if (name.length() > 5000)
			throw new IllegalArgumentException("Name is too long: " + name);

		// Use _ as a sort of escape character

		// // TODO Use url encoding
		// String url = WebUtils.urlEncode(name);
		// url = url.replace("_", "__");
		// url = url.replace("%", "_");
		// return url;

		name = name.replace("_", "__");
		name = name.replace("..", "_.");
		name = name.replaceAll("[^ a-zA-Z0-9-_.~/\\\\]", "");
		name = name.trim();
		name = name.replaceAll("\\s+", "_");
		if (!allowSubDirs) {
			name = name.replace("/", "_");
			name = name.replace("\\", "_");
		}
		// chars not good for the end of a name
		while ("./-\\".indexOf(name.charAt(name.length() - 1)) != -1) {
			name = name.substring(0, name.length() - 1);
		}
		// impose a max length TODO on a per directory basis
		// 12345678901234567890123456789012345678901234567890
		if (name.length() > 100) {
			name = name.substring(0, 10) + name.hashCode()
					+ name.substring(name.length() - 10);
		}
		return name;
	}

	/**
	 * Returns an MD5 Hash as String
	 * @param file
	 * @return
	 */
	public static String getMD5HashString(File file) {

		byte[] bytes = getMD5Hash(file);
		 StringBuffer sb = new StringBuffer();
	     for (int i = 0; i < bytes.length; ++i) {
	       sb.append(Integer.toHexString((bytes[i] & 0xFF) | 0x100).substring(1,3));
	    }
	    return sb.toString();
	}
	
	
	public static byte[] getMD5Hash(File file) {
		try {
			MessageDigest md;
			md = MessageDigest.getInstance("MD5");
			InputStream fis = new FileInputStream(file);
			DigestInputStream dis = new DigestInputStream(fis, md);
			byte[] buffer = new byte[1024];
			int numRead;
			do {
		           numRead = dis.read(buffer);
		           if (numRead > 0) {
		               md.update(buffer, 0, numRead);
		           }
		       } while (numRead != -1);
			
			return md.digest();
		} catch (Exception e) {
			throw Utils.runtime(e);
		}
	}
	/**
	 * Save an object to file, uses an XML serialised form of object generated
	 * by {@link XStreamUtils#serialiseToXml(Object)}.
	 *
	 * @param obj
	 * @param file
	 */
	public static void save(Object obj, File file) {
		if (file.getParentFile() != null) {
			file.getParentFile().mkdirs();
		}
		Writer w = null;
		try {
			w = getWriter(file);
			XStreamUtils.serialiseToXml(w, obj);
		} finally {
			close(w); // release the file handle
		}
	}

	private static File setDataDir() {
		try {
			String dd = System.getenv("WINTERWELL_DATA");
			if (!Utils.isBlank(dd))
				return new File(dd).getAbsoluteFile();
			// .winterwell
			String home = System.getProperty("user.home");
			File ddf = new File(home, ".winterwell/data");
			ddf.mkdirs();
			if (ddf.exists() && ddf.isDirectory())
				return ddf;
			// local/data
			dd = "data";
			File f = new File(dd).getAbsoluteFile();
			Log.report("Using fallback data directory " + f, Level.WARNING);
			return f;
		} catch (Exception e) {
			// Google App Engine doesn't allow any file system use
			return null;
		}
	}

	/**
	 * Write page to file (over-writes if the file already exists), closing
	 * streams afterwards.
	 *
	 * @param out
	 * @param page
	 */
	public static void write(File out, CharSequence page) {
		assert out != null : "no file!";
		assert page != null : out;
		try {
			BufferedWriter writer = getWriter(new FileOutputStream(out));
			writer.append(page);
			close(writer);
		} catch (IOException e) {
			throw new WrappedException(e);
		}
	}

	/**
	 * Convenience method for a human String describing the size of file
	 *
	 * @param file
	 * @return e.g. "10mb"
	 */
	public static String size(File file) {
		long len = file.length();
		return size2(len);
	}

	public static String size2(long len) {
		if (len > 1000000) {
			return StrUtils.toNSigFigs(len / 1000000.0, 2) + "mb";
		}
		if (len > 1000) {
			return StrUtils.toNSigFigs(len / 1000.0, 2) + "k";
		}
		return StrUtils.toNSigFigs(len, 3);
	}

	/**
	 * Convenience for a common case: Read a file, trim each line, skip lines that start with #
	 * and blanks.
	 * If the file cannot be read: returns an empty-list!
	 * @param f
	 */
	public static Collection<String> readList(File f) {
		try {
			return readList(new FileInputStream(f));
		} catch (Exception e) {
			Log.e("IO", e);
			return Collections.EMPTY_LIST;
		}
	}

	/**
	 * Convenience for a common case: Read a file, trim each line, skip lines that start with #
	 * and blanks.
	 * @param in
	 */
	public static List<String> readList(InputStream in) {
		String s = read(in);
		List<String> list = new ArrayList();
		for(String line : StrUtils.splitLines(s)) {
			if (Utils.isBlank(line)) continue;
			if (line.startsWith("#")) continue;
			line = line.trim();
			list.add(line);
		}
		return list;
	}

	/**
	 * Like new File(path) EXCEPT this will convert Windows/Max/Linux separators into the local format.
	 * Use-case: so getParentFile() will work reliably.
	 * @param path e.g. C:\foo\bar.txt
	 * @return e.g. C:/foo/bar.txt
	 * Note: this does not remove or change windows drive markers 
	 */
	public static File toFile(String path) {
		if (path==null) return null;
		if (File.separatorChar=='\\') {
			// From Windows to Linux/Mac
			return new File(path.replace('/', File.separatorChar));
		} else {
			// From Linux/Mac to Windows
			return new File(path.replace('\\', File.separatorChar));
		}
	}

	/**
	 * Note: .zip files can contain many sub-files! This method JUST reads the first entry.
	 * @param file a single-entry .zip file
	 * @return a reader for the first entry in the zip.
	 */
	public static BufferedReader getZIPReader(File file) {
		try {
			ZipFile zipf = new ZipFile(file);
			ZipEntry entry = zipf.entries().nextElement();
			InputStream in = zipf.getInputStream(entry);
			return FileUtils.getReader(in);
		} catch(Exception ex) {
			throw Utils.runtime(ex);
		}
	}

	/**
	 * The first non-null existing file, or null
	 * @param files Can be null, empty, or contain null.
	 * @return
	 */
	public static File or (File... files) {
		if (files==null) return null;
		for (File file : files) {
			if (file !=null && file.exists()) return file;
		}
		return null;
	}
	public static File or (List<File> files) {
		if (files==null) return null;
		for (File file : files) {
			if (file !=null && file.exists()) return file;
		}
		return null;
	}

	/**
	 * 
	 * @param dir
	 * @param f
	 * @return true if f is in dir -- includes sub-directories and dir = f
	 */
	public static boolean contains(File dir, File f) {
		String fp = f.getAbsolutePath();
		String bp = dir.getAbsolutePath();
		boolean yes = fp.startsWith(bp);
		return yes;
	}

	/**
	 * Glob matching, e.g. "*.txt". 
	 *
	 * NB: This makes Java NIO's methods more user-friendly - but you may at times need to use PathMatcher directly. 
	 * @param glob e.g. "*.txt" or "**\/foo\/*.txt"
	 * @param file Unless the glob starts "**", then only the filename is used (ie parent directories are ignored)
	 */
	public static boolean globMatch(String glob, File file) {
//		File justTheName = new File(file.getName()); // otherwise PathMatcher behaves wierdly!
		PathMatcher pathMatcher = FileSystems.getDefault().getPathMatcher("glob:"+glob);
		boolean m = pathMatcher.matches(file.toPath());
		if ( ! m && ! glob.startsWith("**/")) {
			glob = "**/"+glob; // try again, allowing parent dirs (NB: "**/*.txt" would not match eg "foo.txt")
			pathMatcher = FileSystems.getDefault().getPathMatcher("glob:"+glob);
			m = pathMatcher.matches(file.toPath());
		}
		return m;
	}


}

/**
 * Regex based filtr. Should this be in Utils? See
 * {@link FileUtils#getRegexFilter(String)}
 *
 * @author daniel
 *
 */
final class RegexFileFilter implements IFilter<File>, FileFilter {

	private final Pattern regex;

	/**
	 * See {@link FileUtils#getRegexFilter(String)}
	 *
	 * @param regex
	 *            matches against the file name
	 */
	RegexFileFilter(String regex) {
		this.regex = Pattern.compile(regex);
	}

	@Override
	public boolean accept(File x) {
		String name = x.getAbsolutePath();
		return regex.matcher(name).matches();
	}

}
