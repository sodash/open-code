/**
 *
 */
package winterwell.utils.io;

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

import com.winterwell.utils.ShellScript;
import com.winterwell.utils.io.FileUtilsTest;

import winterwell.utils.FailureException;
import winterwell.utils.IFilter;

import com.winterwell.utils.Printer;
import com.winterwell.utils.Proc;
import winterwell.utils.StrUtils;
import winterwell.utils.TodoException;
import winterwell.utils.Utils;
import winterwell.utils.WrappedException;
import com.winterwell.utils.containers.Pair2;
import winterwell.utils.reporting.Log;
import winterwell.utils.web.WebUtils;
import winterwell.utils.web.XStreamUtils;

/**
 * @deprecated Use the com. version
 * Static file-related utility functions.
 *
 * @author Daniel Winterstein
 * @testedby {@link FileUtilsTest}
 */
public final class FileUtils extends com.winterwell.utils.io.FileUtils {

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
