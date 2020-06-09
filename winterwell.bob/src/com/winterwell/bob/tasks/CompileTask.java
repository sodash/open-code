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
import com.winterwell.bob.BobConfig;
import com.winterwell.bob.BuildTask;
import com.winterwell.utils.Dep;
import com.winterwell.utils.FailureException;
import com.winterwell.utils.Printer;
import com.winterwell.utils.Proc;
import com.winterwell.utils.StrUtils;
import com.winterwell.utils.Utils;
import com.winterwell.utils.WrappedException;
import com.winterwell.utils.containers.Containers;
import com.winterwell.utils.io.FileUtils;
import com.winterwell.utils.log.Log;
//import com.winterwell.utils.web.XStreamUtils;

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
	
	/**
	 * False by default.
	 * If true, then all files in the outputDir are deleted before the compile.
	 * This prevents any old .class files sneaking into a jar.
	 */
	boolean cleanOutputDir;
	
	public void setCleanOutputDir(boolean cleanOutputDir) {
		this.cleanOutputDir = cleanOutputDir;
	}

	/**
	 * HACK: Can also contain single files!
	 */
	private final List<File> srcDirs = new ArrayList();
	
	private Classpath classpath;
	private List<File> srcFiles;
	private String srcJavaVersion;
	private String outputJavaVersion;
	private boolean debug;
	
	public CompileTask addSrcDir(File srcDir) {
		if ( ! srcDirs.contains(srcDir)) {
			srcDirs.add(srcDir);
		}		
		return this;
	}
	
	/**
	 * @param debug If true, switch on warnings output.
	 * @return this
	 */
	public CompileTask setDebug(boolean debug) {
		this.debug = debug;
		return this;
	}

	/**
	 * Compile Java code.
	 * 
	 * @param srcDir
	 *            The base directory for files to compile
	 * @param outputDir
	 *            This will be created if needed
	 */
	public CompileTask(File srcDir, File outputDir) {
		if (srcDir!=null) addSrcDir(srcDir);
		this.outputDir = outputDir;
		/*
		 * Default to the version of Java that's running this code :)
		 */
		// NB: falls back to Java 11 (LTS)
		String javaVersion = Utils.or(System.getProperty("java.specification.version"), "11");
		setSrcJavaVersion(javaVersion);
		setOutputJavaVersion(javaVersion);
	}
	
	/**
	 * @param outputJavaVersion e.g. "1.9" or "11"
	 */
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
		if ( ! debug) options.add("-nowarn");
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
		Log.d(LOGTAG, "javac " //+StrUtils.join(options, " ")+" "
				+Containers.first(javaFiles)+"   ("+javaFiles.size()+" java files)"
//				+StrUtils.join(javaFiles, " ") // This can be a big list! But its the only way to make a valid javac command
				);
		// save a linux command
		try {			
			BobConfig bs = Dep.has(BobConfig.class)? Dep.get(BobConfig.class) : new BobConfig();
			// HACK - to pick a nice name for the debug file
			File projectDir = srcDirs.isEmpty()? outputDir : srcDirs.get(0);			
			List<String> notThese = Arrays.asList("src", "test", "source", "java", "build", "builder", "main");
			while (projectDir!=null && notThese.contains(projectDir.getName())) projectDir = projectDir.getParentFile();			
			String sname = FileUtils.safeFilename(Utils.or(
					projectDir!=null? projectDir.getName() : null, srcFiles, "weird").toString(), 
					false);
			File cmdfile = new File(bs.logDir, "CompileTask."+sname+".sh");
			FileUtils.write(cmdfile, 
					"# "+StrUtils.compactWhitespace(getDesc().getId())+"\n"+
					"javac "+StrUtils.join(options, " ")+" "+StrUtils.join(javaFiles, " ")
//					+"\n\n\n"
//					+XStreamUtils.serialiseToXml(this) // for debug - what are the diffs??
					);
			Log.d(LOGTAG, "javac compile command saved to: "+cmdfile.getAbsolutePath());
		} catch(Throwable ex) {
			// oh well
			Log.i(LOGTAG, ex);
		}
		
		Iterable fileObjects = sjfm.getJavaFileObjectsFromFiles(javaFiles);
		CompilationTask ctask = jc.getTask(null, sjfm, diagnostics, options, null, fileObjects);
		Boolean ok = ctask.call();
		sjfm.close();
		// Diagnostic output
		StringBuilder diags = new StringBuilder();
		for (Diagnostic diagnostic : diagnostics.getDiagnostics()) {
			if (!Bob.getSingleton().getConfig().verbose
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
			throw new FailureException(StrUtils.ellipsize(p.getCommand(), 100)+" -> "+p.getError());
	}

	@Override
	public void doTask() throws Exception {
		javaFiles = getFiles();
		outputDir.mkdirs();
		if (javaFiles.size() == 0) {
			Printer.out("Nothing to compile");
			return;
		}
		if (getConfig().verbose) {
			Printer.out("Compiling to " + outputDir + ":");
			Printer.out(javaFiles);
		}
		if (cleanOutputDir) {
			if (outputDir.exists()) {
				Log.d(LOGTAG, "Cleaning old outputs "+outputDir);
				FileUtils.deleteDir(outputDir);
				outputDir.mkdir();
			}			
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

	/**
	 * Collect all java files from srcDirs
	 * @return
	 */
	private ArrayList<File> getFiles() {
		if (srcFiles != null) {
			return new ArrayList(srcFiles);
		}
		List<File> files = new ArrayList();
		for(File srcDir : srcDirs) {
			if (srcDir.isDirectory()) {
				List<File> sdFiles = FileUtils.find(srcDir, ".*\\.java");
				files.addAll(sdFiles);
			} else {
				// HACK - we allow single files in too
				files.add(srcDir);
			}
		}
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

	public void addSrcFile(File f) {
		srcDirs.add(f);
	}

}
