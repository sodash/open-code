package com.winterwell.bob.tasks;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import com.winterwell.bob.BuildTask;
import com.winterwell.utils.FailureException;
import com.winterwell.utils.TodoException;
import com.winterwell.utils.containers.Containers;
import com.winterwell.utils.log.Log;

import junit.framework.AssertionFailedError;
import junit.framework.Test;
import junit.framework.TestListener;
import junit.framework.TestResult;
import junit.framework.TestSuite;

/**
 * Class for creating a HTML JUnit test report.
 * 
 * @author Jacob Dreyer, released as public with permission to edit and use on
 *         http://www.velocityreviews.com/forums/t149403-junit-html-report.html
 *         Some modifications by Daniel Winterstein
 */
final class HtmlTestReport implements TestListener {
	private static final String LOGTAG = "JUnitTask";

	/**
	 * Method for creating a test suite from all tests found in the present
	 * directory and its sub directories.
	 * 
	 * @param directory
	 *            Root directory for test search.
	 * @return Test suite compound of all tests found.
	 */
	private static TestSuite build(File directory) throws IOException {
		assert directory != null : "Directory cannot be null";

		TestSuite suite = new TestSuite(directory.getName());

		// Get list of all classes in the path
		List<String> classNames = HtmlTestReport.getAllClasses(directory);

		List notFound = new ArrayList();
		
		for (String className : classNames) {
			try {
				Class clazz = Class.forName(className);

				// Filter out all non-test classes
				if (!junit.framework.TestCase.class.isAssignableFrom(clazz))
					continue;
				// Skip the BuildTasks! - DBW
				if (BuildTask.class.isAssignableFrom(clazz))
					continue;

				// Because a 'suite' method doesn't always exist in a
				// TestCase,
				// we need to use the try/catch so that tests can also be
				// automatically extracted
				try {
					Method suiteMethod = clazz.getMethod("suite", new Class[0]);
					Test test = (Test) suiteMethod.invoke(null);
					suite.addTest(test);
				} catch (NoSuchMethodException exception) {
					suite.addTest(new TestSuite(clazz));
				} catch (IllegalAccessException exception) {
					exception.printStackTrace();
					// Ignore
				} catch (InvocationTargetException exception) {
					exception.printStackTrace();
					// Ignore
				}

			} catch (ClassNotFoundException exception) {
				notFound.add(className);
				// Ignore (log a bit later)
			}
		}
		if ( ! notFound.isEmpty()) {
			Log.d("JUnitTask", "Some ClassNotFoundExceptions: "+notFound);
		}

		return suite;
	}

	/**
	 * Retrieve all classes from the specified path.
	 * 
	 * @param root
	 *            Root of directory of where to search for classes.
	 * @return List of classes on the form "com.company.ClassName".
	 */
	private static List<String> getAllClasses(File root) throws IOException {
		assert root != null : "Root cannot be null";

		// Prepare the return array
		List<String> classNames = new ArrayList<String>();

		// Get all classes recursively
		String path = root.getCanonicalPath();
		HtmlTestReport.getAllClasses(root, path.length() + 1, classNames);

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
				if (entry.canRead())
					getAllClasses(entry, prefixLength, result);
			}

			// Entry is a file. Filter out non-classes and inner classes
			else {
				String path = entry.getPath();
				boolean isClass = path.endsWith(".class")
						&& path.indexOf("$") < 0;
				if (isClass) {
					String name = entry.getCanonicalPath().substring(
							prefixLength);
					String className = name.replace(File.separatorChar, '.')
							.substring(0, name.length() - 6);
					result.add(className);
				}
			}
		}
	}

	/**
	 * Convert the specified number of milliseconds into a readable string in
	 * the form [nSeconds].[nMilliseconds]
	 * 
	 * @param nMillis
	 *            Number of milliseconds to convert.
	 * @return String representation of the input.
	 */
	private static String getTime(long nMillis) {
		assert nMillis >= 0 : "Illegal time specification";

		long nSeconds = nMillis / 1000;
		nMillis -= nSeconds * 1000;
		StringBuffer time = new StringBuffer();
		time.append(nSeconds);
		time.append('.');
		if (nMillis < 100)
			time.append('0');
		if (nMillis < 10)
			time.append('0');
		time.append(nMillis);

		return time.toString();
	}

	/** Background color of report */
	private final String BACKGROUND_COLOR = "\"#ffffff\"";

	/** Directory of classes root */
	private final File classDirectory_;

	/** Background color of failed tests */
	private final String FAILURE_COLOR = "\"#ff9999\"";

	/** Background color of header */
	private final String HEADER_COLOR = "\"#ccccff\"";

	/** Error message of current test */
	private StringBuffer message_;

	/** Total number of failed tests */
	private int nFailed_ = 0;

	/** Total number of succeeding tests */
	private int nSuccess_ = 0;

	private PrintStream out;

	private File outputFile;

	/** Directory of source root */
	private final File sourceDirectory_;

	/** Start time for current test */
	private long startTime_;

	/** Background color of successful tests */
	private final String SUCCESS_COLOR = "\"#99ff99\"";

	/** Overall start time for report generation */
	private final long time0_;

	/**
	 * Create a HTML report instance. Typical usage:
	 * 
	 * <pre>
	 * File sourceDir = new File(&quot;/home/joe/dev/src&quot;);
	 * File classDir = new File(&quot;/home/joe/dev/classes&quot;);
	 * HtmlTestReport report = new HtmlTestReport(sourceDir, classDir);
	 * report.print();
	 * </pre>
	 * 
	 * The HTML report is written to stdout and will typically be redirected to
	 * a .html file.
	 * 
	 * @param sourceDirectory
	 *            Root directory of source. If specified, it will be used to
	 *            create a link to the source file of failing classes. May be
	 *            null.
	 * @param classDirectory
	 *            Root directory of classes.
	 */
	public HtmlTestReport(File sourceDirectory, File classDirectory) {
		assert classDirectory != null : "Illegal class directory";

		sourceDirectory_ = sourceDirectory;
		classDirectory_ = classDirectory;

		time0_ = System.currentTimeMillis();
	}

	/**
	 * Add the appropriate report content for a failed test. This method is
	 * called by JUnit when a test fails.
	 * 
	 * @param test
	 *            Test that is failing.
	 * @param throwable
	 *            Throwable indicating the failure
	 */
	public void addError(Test test, Throwable throwable) {
		assert test != null : "Test cannot be null";
		assert throwable != null : "Exception must be specified for failing test";

		StackTraceElement stackTrace[] = throwable.getStackTrace();
		for (int i = 0; i < stackTrace.length; i++) {
			String className = stackTrace[i].getClassName();
			if (!className.startsWith("junit")) {
				message_.append(stackTrace[i].getClassName());
				message_.append(" ");
				if (sourceDirectory_ != null) {
					String fileName = stackTrace[i].getFileName();
					if (fileName != null) {
						message_.append(toLink(stackTrace[i].getClassName(),
							fileName, stackTrace[i].getLineNumber()));
					}
				}
				message_.append("<br>");
				break;
			}
		}
		String errorMessage = escapeHTMLEntities(throwable.getMessage());
		message_.append(" &nbsp; &nbsp; " + errorMessage);
	}

	private String escapeHTMLEntities(String message) {
		if (message == null) return null;
		message =  message.replace("&", "&amp;");
		message =  message.replace("<", "&lt;");
		message =  message.replace(">", "&gt;");
		return message;
	}

	/**
	 * Add the appropriate report content for a failed test. This method is
	 * called by JUnit when a test fails.
	 * 
	 * @param test
	 *            Test that is failing.
	 * @param throwable
	 *            Throwable indicating the failure
	 */
	public void addFailure(Test test, AssertionFailedError error) {
		assert test != null : "Test cannot be null";
		assert error != null : "Exception must be specified for failing test";

		// Treat failures and errors the same
		addError(test, error);
	}

	/**
	 * Add the appropriate report content for the end of a test. This method is
	 * called by JUnit when a test is done.
	 * 
	 * @param test
	 *            Test that is done.
	 */
	public void endTest(Test test) {
		assert test != null : "Test cannot be null";

		// Compute the test duration
		long time = System.currentTimeMillis() - startTime_;

		out.println(" <td valign=\"top\"><font face=\"courier\"> "
				+ HtmlTestReport.getTime(time) + "</font></td>");

		// Test was a success
		if (message_.length() == 0) {
			nSuccess_++;
			out.println(" <td bgcolor=" + SUCCESS_COLOR
					+ "<font face=\"helvetica\"><b>Success</b></font></td>");
		}

		// Test failed
		else {
			nFailed_++;
			out.println(" <td bgcolor=" + FAILURE_COLOR + ">"
					+ message_.toString() + "</td>");
		}

		out.println(" </tr>");
	}

	/**
	 * @return Number of failures and errors
	 */
	public int getFailureCount() {
		return nFailed_;
	}

	/**
	 * Print the HTML report to standard out.
	 * 
	 * @throws IOException
	 */
	public void print() throws IOException {
		if (outputFile == null) {
			out = System.out;
		} else {
			out = new PrintStream(new FileOutputStream(outputFile));
		}
		// Extract the test suite
		Log.d(LOGTAG, "Locating tests in " + classDirectory_);
		TestSuite suite = HtmlTestReport.build(classDirectory_);
		Log.d(LOGTAG, suite.countTestCases() + " test cases found.");

		// Print report header
		printHeader();

		// Loop through all the tests and make the report run the test
		// and capture the result
		for (int i = 0; i < suite.testCount(); i++) {
			Test test = suite.testAt(i);
			// Run
			run(test);
		}

		// Print report footer
		printFooter();
	}

	/**
	 * Print the HTML report footer.
	 */
	private void printFooter() {
		int nTotal = nSuccess_ + nFailed_;

		String color = nFailed_ == 0 ? SUCCESS_COLOR : FAILURE_COLOR;

		out.println(" <tr>");
		out.println(" <td colspan=3> &nbsp; </td>");
		out.println(" <td bgcolor=" + color + ">");

		out.println(" <table border=0 cellspacing=0 cellpadding=0>");

		out.println(" <tr>");
		out
				.println(" <td><font size=+0 face=\"helvetica\"><b>Failed</b></font></td>");
		out.println(" <td><font size=+0 face=\"courier\">" + nFailed_ + " ("
				+ (double) nFailed_ / nTotal * 100.0 + "%)" + "</font>"
				+ "</td>");
		out.println(" </tr>");

		out.println(" <tr>");
		out
				.println(" <td><font size=+0 face=\"helvetica\"><b>Success &nbsp; &nbsp; </b></font></td>");
		out.println(" <td><font size=+0 face=\"courier\">" + nSuccess_ + " ("
				+ (double) nSuccess_ / nTotal * 100.0 + "%)" + "</font>"
				+ "</td>");
		out.println(" </tr>");

		out.println(" <tr>");
		out
				.println(" <td><font size=+0 face=\"helvetica\"><b>Total</b></font></td>");
		out.println(" <td><font size=+0 face=\"courier\">" + nTotal + "</font>"
				+ "</td>");
		out.println(" </tr>");

		out.println(" <tr>");
		out
				.println(" <td><font size=+0 face=\"helvetica\"><b>Time</b></font></td>");
		out.println(" <td><font size=+0 face=\"courier\">"
				+ HtmlTestReport.getTime(System.currentTimeMillis() - time0_)
				+ "</td>");
		out.println(" </tr>");

		out.println(" </table>");
		out.println(" </td>");
		out.println(" </tr>");

		out.println("</table>");
		out.println("</html>");
	}

	/**
	 * Print the HTML report header.
	 */
	private void printHeader() {
		out.println("<html>");
		out.println("<table" + " cellpadding=\"5\"" + " cellspacing=\"0\""
				+ " bgcolor=" + BACKGROUND_COLOR + " border=\"1\""
				+ " width=\"100%\"" + ">");
		out.println(" <tr>");
		out.println(" <td bgcolor=" + HEADER_COLOR + " colspan=2>"
				+ "<font size=+2>" + "<b>Test</b>" + "</font>" + "</td>");
		out.println(" <td bgcolor=" + HEADER_COLOR + ">" + "<font size=+2>"
				+ "<b>Time</b>" + "</font>" + "</td>");
		out.println(" <td align=\"right\" bgcolor=" + HEADER_COLOR + ">"
				+ "<font size=+2>" + "<b>" + new Date() + "</b>" + "</font>"
				+ "</td>");
		out.println(" </tr>");
	}

	/**
	 * Run the specified test and capture the result in the report.
	 * 
	 * @param test
	 *            Test to run.
	 */
	private void run(Test test) {
		out.println(" <tr>");
		out.println(" <td colspan=4>" + "<font face=\"courier\">" + "<b>"
				+ test.toString() + "</b>" + "</font>" + "</td>");
		out.println(" </tr>");

		TestResult result = new TestResult();
		result.addListener(this);

		test.run(result);
	}

	public void setOutputFile(File outputFile) {
		this.outputFile = outputFile;

	}

	/**
	 * Add the appropriate report content for a starting test. This method is
	 * called by JUnit when a test is about to start.
	 * 
	 * @param test
	 *            Test that is about to start.
	 */
	public void startTest(Test test) {
		assert test != null : "Test cannot be null";

		startTime_ = System.currentTimeMillis();
		message_ = new StringBuffer();

		String name = test.toString();
		String testName = name.substring(0, name.indexOf('('));

		out.println(" <tr>");
		out.println(" <td> &nbsp; &nbsp; </td>");
		out.println(" <td valign=\"top\"><font face=\"courier\"><b>" + testName
				+ "</b></font></td>");
	}

	/**
	 * Create an anchor tag from the specified class and file name.
	 * 
	 * @param className
	 *            Fully specified class name of class to link to.
	 * @param fileName
	 *            File where the class resides.
	 * @param lineNo
	 *            Line number to scroll to (TODO: not currently used).
	 * @return Anchor string of the form "<a href='link'>file</a>".
	 */
	private String toLink(String className, String fileName, int lineNo) {
		assert sourceDirectory_ != null : "Source not available";
		assert className != null : "Missing class name";
		assert fileName != null : "Missing file name";
		// assert lineNo >= 0 : "Illegal line number spcifier"; This does happen

		String base = sourceDirectory_.toString() + "/";
		String packageName = className.substring(0, className.lastIndexOf('.'));

		String link = "<a href=\"" + base + packageName.replace(".", "/") + "/"
				+ fileName + "\">" + fileName + ":" + lineNo + "</a>";

		return link;
	}

	public int getSuccessCount() {
		return nSuccess_;
	}

}

/**
 * Class for creating a HTML JUnit test report.
 * 
 * @author Jacob Dreyer, released as public with permission to edit and use on
 *         http://www.velocityreviews.com/forums/t149403-junit-html-report.html
 * @author Daniel Winterstein - modifications to Jacob's code
 * @testedby {@link JUnitTaskTest}
 */
public class JUnitTask extends BuildTask {

	private final Collection<File> classpath;
	private boolean exceptionOnTestFailure;

	private final File outputFile;
	private final File sourceDirectory;
	private transient HtmlTestReport report;
//	private transient File classDirectory;

	/**
	 * Create a HTML report instance. Typical usage:
	 * 
	 * <pre>
	 * File classDir = new File("/home/joe/dev/classes");
	 * JUnitTask unitTest = new JUnitTask(null, classDir, new File("tests.html"));
	 * unitTest.run();
	 * </pre>
	 * 
	 * @param sourceDirectory
	 *            Root directory of source. If specified, it will be used to
	 *            create a link to the source file of failing classes. May be
	 *            null.
	 * @param classDirectory
	 *            Root directory of classes.
	 */
	public JUnitTask(File srcDir, File classDir, File outputFile) {
		this(srcDir, Arrays.asList(classDir), outputFile);
	}
	
	public JUnitTask(File srcDir, Collection<File> classPath, File outputFile) {
		sourceDirectory = srcDir;
		classpath = classPath;
		this.outputFile = outputFile;
	}

	@Override
	public void doTask() throws Exception {
//		// Build temp dir of classes ??why??
//		classDirectory = FileUtils.createTempDir();
//		for(File f : classpath) {
//			FileUtils.copy(f, classDirectory, true);
//		}
		// Create a HTML report instance
		if (classpath.size() != 1) throw new TodoException();
		File classDirectory = Containers.first(classpath);
		report = new HtmlTestReport(sourceDirectory, classDirectory);
		report.setOutputFile(outputFile);
		outputFile.getParentFile().mkdirs();
		// Run the tests!
		report.print();
		// report
		Log.d(LOGTAG, "Tested " + classDirectory + ". " + getSuccessCount()
				+ " tests passed, " + getFailureCount() + " tests failed.");
		// Exception?
		if (exceptionOnTestFailure && report.getFailureCount() > 0) {
			throw new FailureException("junit",
					"Test failed (and JUnitTask is set to throw exceptions). See "
							+ outputFile + " for details.");
		}		
	}
	
	public void close() {
//		FileUtils.deleteDir(classDirectory);
	}

	/**
	 * @param exceptionOnTestFailure
	 *            If true, this task will throw an exception should any test
	 *            fail. Default: false. Useful for when the tests are crucial to
	 *            the next task (e.g. deploying an updated version).
	 */
	public void setExceptionOnTestFailure(boolean exceptionOnTestFailure) {
		this.exceptionOnTestFailure = exceptionOnTestFailure;
	}

	public int getSuccessCount() {
		return report.getSuccessCount();
	}

	public int getFailureCount() {	
		return report.getFailureCount();
	}

}
