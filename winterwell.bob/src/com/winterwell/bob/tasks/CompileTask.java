package com.winterwell.bob.tasks;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import javax.tools.Diagnostic;
import javax.tools.Diagnostic.Kind;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaCompiler.CompilationTask;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

import com.winterwell.bob.Bob;
import com.winterwell.bob.BuildTask;
import com.winterwell.utils.FailureException;
import com.winterwell.utils.Printer;
import com.winterwell.utils.Proc;
import com.winterwell.utils.StrUtils;
import com.winterwell.utils.Utils;
import com.winterwell.utils.WrappedException;
import com.winterwell.utils.containers.Containers;
import com.winterwell.utils.io.FileUtils;
import com.winterwell.utils.log.Log;

/**
 * Compile Java code. ??Ignores non-Java files! You may wish to use a
 * {@link CopyTask} for these. Uses Java 6's {@link JavaCompiler} class.
 * 
 * @author daniel
 * 
 */
public class CompileTask extends BuildTask {

	private transient Collection<File> javaFiles;
	List<String> options = new ArrayList<String>();

	private final File outputDir;

	private final File srcDir;
	
	private Classpath classpath;
	private List<File> srcFiles;
	private String srcJavaVersion;
	private String outputJavaVersion;

	/**
	 * Compile Java code.
	 * 
	 * @param srcDir
	 *            The base directory for files to compile
	 * @param outputDir
	 *            This will be created if needed
	 */
	public CompileTask(File srcDir, File outputDir) {
		this.srcDir = srcDir;
		this.outputDir = outputDir;
		/*
		 * Default to the version of Java that's running this code :)
		 */
		// NB: falls back to Java 9
		String javaVersion = Utils.or(System.getProperty("java.specification.version"), "1.9");
		setSrcJavaVersion(javaVersion);
		setOutputJavaVersion(javaVersion);
	}
	
	public void setOutputJavaVersion(String outputJavaVersion) {
		this.outputJavaVersion = outputJavaVersion;
	}
	public void setSrcJavaVersion(String srcJavaVersion) {
		this.srcJavaVersion = srcJavaVersion;
	}
	
	private void doJava6compile() throws IOException {
		JavaCompiler jc = getJavaCompiler();
		Log.d(LOGTAG, "compiler: "+jc.getClass());
		// TODO There is a bug in Java on Windows Vista - this call throws a
		// NullPointerException
		StandardJavaFileManager sjfm = jc.getStandardFileManager(null, null,
				null);
		DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<JavaFileObject>();
		// quiet
		options.add("-nowarn");
		// Java version: 8
		options.add("-source"); options.add(srcJavaVersion);
		options.add("-target"); options.add(outputJavaVersion);
		
		// ??Does lombok need anything??
		
		// What a lousy way to set the output dir
		options.add("-d");
		options.add(outputDir.getAbsolutePath());
		// classpath
		addClasspathToOptions();
		// Run it!
		Log.d(LOGTAG, "javac "+StrUtils.join(options, " ")+" "
				+Containers.first(javaFiles)+"   ("+javaFiles.size()+" java files)"
//				+StrUtils.join(javaFiles, " ") This can be a big list!
				);		
		Iterable fileObjects = sjfm.getJavaFileObjectsFromFiles(javaFiles);
		CompilationTask ctask = jc.getTask(null, sjfm, diagnostics, options, null, fileObjects);
		Boolean ok = ctask.call();
		sjfm.close();
		// Diagnostic output
		StringBuilder diags = new StringBuilder();
		for (Diagnostic diagnostic : diagnostics.getDiagnostics()) {
			if (!Bob.getSingleton().getSettings().verbose
					&& diagnostic.getKind() != Kind.ERROR)
				continue;
			Printer.appendFormat(diags, "{0}: {1} in {2}\n", diagnostic
					.getKind(), diagnostic.getMessage(null), diagnostic
					.getSource());
		}
		Log.d(LOGTAG, diags);
		// OK?
		if ( ! ok) {
			throw new FailureException("Compile task failed :( " + diags+" from javac "+options);
		}
	}

	/**
	 * Prefer the Eclipse compiler, if ecj.jar is on the classpath
	 * @return
	 */
	private JavaCompiler getJavaCompiler() {
		try {
//			new EclipseCompiler();
			return (JavaCompiler) Class.forName("org.eclipse.jdt.internal.compiler.tool.EclipseCompiler").newInstance();
		} catch(Exception ex) {
			return ToolProvider.getSystemJavaCompiler();
		}
	}

	/**
	 * Java 6 compilation has failed (which happens in Windows Vista 2008). So
	 * try a javac process call. TODO test
	 * 
	 * @throws InterruptedException
	 */
	private void doJavacProcessCompile() throws InterruptedException {
		try {
			// Try javac via a shell process
			// Setup options
//			options.add("-d");
//			options.add(outputDir.getAbsolutePath());
//			addClasspathToOptions();
			for (File f : javaFiles) {
				options.add(f.getAbsolutePath());
			}
			// Run javac
			doJavacProcessCompile2("javac");
		} catch (WrappedException e) {
			if (!e.getMessage().contains("Cannot run program"))
				throw e;
			// javac is not on the path. Try to find it!
			String path = System.getenv("JAVA_HOME");
			String cpath = System.getenv("CLASSPATH");
			if (path == null)
				throw new FailureException(
						"Could not run javac. Try setting the JAVA_HOME environment variable to point to the JDK directory.");
			String binJavac = "bin/javac";
			String os = Utils.getOperatingSystem();
			if (os.contains("windows"))
				binJavac += ".exe";
			File javacFile = new File(path, binJavac);
			doJavacProcessCompile2(javacFile.getAbsolutePath());
		}
	}

	private void addClasspathToOptions() {
		if (classpath != null && ! classpath.isEmpty()) {
			options.add("-classpath");
			options.add(classpath.toString());
		}
	}

	private void doJavacProcessCompile2(String javacCmd)
			throws InterruptedException {
		ArrayList<String> cmd = new ArrayList<String>();
		cmd.add(javacCmd);
		cmd.addAll(options);
		Proc p = new Proc(cmd);
		p.run();
		int ok = p.waitFor();
		System.out.println(p.getOutput());
		System.err.println(p.getError());
		if (ok != 0)
			throw new FailureException(p.getError());
	}

	@Override
	public void doTask() throws Exception {
		javaFiles = getFiles();
		outputDir.mkdirs();
		if (javaFiles.size() == 0) {
			Printer.out("Nothing to compile");
			return;
		}
		if (Bob.getSingleton().getSettings().verbose) {
			Printer.out("Compiling to " + outputDir + ":");
			Printer.out(javaFiles);
		}
		// Try Java 6
		try {
			doJava6compile();
		} catch (IOException e) {
			throw e;
		} catch (FailureException e) {
			throw e;
		} catch (Exception e) {
			// Try something else!
			doJavacProcessCompile();
		}
		// ??Copy the non-Java files
		// copyNonJavaFiles();
	}

	private ArrayList<File> getFiles() {
		if (srcFiles != null) {
			return new ArrayList(srcFiles);
		}
		List<File> files = FileUtils.find(srcDir, ".*\\.java");
		ArrayList<File> jFiles = new ArrayList<File>(files.size());
		// Remove dirs from files 'cos the compiler gets upset
		for (File f : files.toArray(new File[0])) {
			if (f.isDirectory())
				continue;
			// ignore non-java files!
			if (f.getName().endsWith(".java")) {
				jFiles.add(f);
			}
		}
		return jFiles;
	}

	public void setClasspath(Collection<File> classpath) {
		this.classpath = new Classpath();
		for (File file : classpath) {
			String path = file.getPath(); 
			if (isJarDir(file)) {
				// Include directories of jars with /*
				this.classpath.add(path	+	(path.endsWith("/")?"":"/")	+ "*");
			} else {
				// jars and /bin code directories
				this.classpath.add(path);
			}
		}		
	}

	/**
	 * HACK try to decide whether this is a directory full of jars,
	 * or a class directory
	 * @param file
	 * @return
	 */
	private boolean isJarDir(File file) {
		if ( ! file.isDirectory()) return false;
		File[] files = file.listFiles();
		int jars=0;		
		int dirs=0;
		for (File f : files) {
			if (f.getName().endsWith(".jar")) jars++;
			if (f.isDirectory()) dirs++;
		}
		if (jars==0) return false; // definitely not a jar directory
		if (dirs==0) return true;  // definitely not a class directory
		// Guess!
		// ?? maybe look for .class files?
		return jars > (files.length/4) && jars > dirs;
	}

	public Classpath getClasspath() {
		return classpath;
	}

	public void setSrcFiles(File... files) {
		this.srcFiles = Arrays.asList(files);
	}

	public void setClasspath(Classpath cpfiles) {
		this.classpath = cpfiles;
	}

}
