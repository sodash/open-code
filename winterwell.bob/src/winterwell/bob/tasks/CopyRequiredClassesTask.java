package winterwell.bob.tasks;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.winterwell.utils.Printer;

import winterwell.bob.BuildTask;
import winterwell.utils.IFilter;
import winterwell.utils.StrUtils;
import winterwell.utils.Utils;
import winterwell.utils.containers.Tree;
import com.winterwell.utils.io.FileUtils;
import winterwell.utils.reporting.Log;

/**
 * Copy in the Winterwell java files needed by a project.
 * 
 * TODO refactor to have a "getRequiredClasses" method
 * 
 * @testedby {@link CopyRequiredClassesTaskTest}
 * @author daniel
 *
 */
public class CopyRequiredClassesTask extends BuildTask {
	
	/**
	 * 
	 * @param srcDir Recursively search this for .java files
	 * @param classDir Copy required .class files into here (creating package directories
	 * as needed)
	 */
	public CopyRequiredClassesTask(File srcDir, File classDir) {
		this.srcDir = srcDir;
		startFiles = null;
		this.outDir = classDir;
		if ( ! srcDir.isDirectory()) {
			throw new IllegalArgumentException(srcDir.getPath());
		}
	}
	
	/**
	 * @param startFiles These must be .java source files
	 * @param classDir Copy required .class files into here (creating package directories
	 * as needed)
	 */
	public CopyRequiredClassesTask(Collection<File> startFiles, File classDir) {
		this.startFiles = new ArrayList(startFiles);
		srcDir = null;
		this.outDir = classDir;
	}
	
	final File srcDir;
	final File outDir;
	List<File> startFiles;
	
	Tree<File> dependencies;
	
	@Override
	public void doTask() throws Exception {
		if (srcDir!=null) {
			assert startFiles==null;
			startFiles = FileUtils.find(srcDir, ".*\\.java");
		} else {
			assert startFiles != null;
			for(File f : startFiles) {
				assert f.isFile() : f;
			}
		}
		Utils.check4null(outDir); 
		outDir.mkdirs();
		// pull in the Java classes we need
		// TODO refactor this into a "collect needed files" step,
		// then copy		
		dependencies = new Tree<File>();
		for (File file : startFiles) {
			doTask2_copyInClasses(file, dependencies);
		}		
	}

	Set<File> klassesDone = new HashSet<File>();
	
	static final Pattern JAVA_COMMENT = Pattern.compile("\\/\\*.+?\\*\\/", Pattern.DOTALL);
	static final Pattern JAVA_COMMENT2 = Pattern.compile("\\/\\/.*?$", Pattern.MULTILINE);
	
	/**
	 * @param goFromHere A java source file
	 */
	void doTask2_copyInClasses(File goFromHere, Tree<File> depNode) {
		if ( ! goFromHere.isFile()) return;		
		Tree<File> hereNode = new Tree<File>(depNode, goFromHere);
				
		String src = FileUtils.read(goFromHere);
		
		// remove comments (which often talk about non-essential classes)
		src = JAVA_COMMENT.matcher(src).replaceAll("");
		src = JAVA_COMMENT2.matcher(src).replaceAll("");
		
		String[] lines = StrUtils.splitLines(src);		

		// imports
		List<String> imports = doTask3_copyInClasses2_imports(lines, hereNode);
		
		// get in the package
		String pckge = goFromHere.toString();
		// HACK this regex assumes a name for the base directory!
		// TODO get package from src
		Pattern p = Pattern.compile("(src|test|build)\\/(.+)\\/");
		Matcher m = p.matcher(pckge);
		m.find();
		pckge = m.group(2);
		
		// same package
		doTask3_copyInClasses2_samePackage(lines, pckge, hereNode, imports);
	}
	
	/**
	 * Look for import statements
	 * @param lines
	 * @param hereNode 
	 * @return 
	 */
	List<String> doTask3_copyInClasses2_imports(String[] lines, Tree<File> depNode) {
		List<String> imports = new ArrayList();
		// anything Winterwell
		Pattern klass = Pattern.compile("^import (.*?winterwell\\.[\\w\\.]+);");
		for (String line : lines) {
			Matcher m = klass.matcher(line);			
			if ( ! m.find()) continue;
			String klassName = m.group(1);
			File klassFile = getClassFile(klassName);
			if (klassFile==null) continue;
			
			// no loops
			if (klassesDone.contains(klassFile)) continue;
			klassesDone.add(klassFile);
			
			File klassSrc = getSrcFile(klassName);
			if ( ! klassFile.exists()) {
				Log.report("\tNo "+klassFile, Level.WARNING);
				continue;
			}
			if (filter!=null) {
				boolean ok = filter.accept(klassSrc);
				if ( ! ok) continue;
			}
			doTask4_copyClassFile(klassName, klassSrc);
			int i = klassName.lastIndexOf('.');
			imports.add(klassName.substring(i));
			// recurse!
			doTask2_copyInClasses(klassSrc, depNode);
		}		
		return imports;
	}
	
	/**
	 * Do the actual copy. Also copy inner classes.
	 * @param klassName
	 */
	private void doTask4_copyClassFile(String klassName, File srcFile) {		
		File out = new File(outDir, klassName.replace('.', '/')+".class");
		out.getParentFile().mkdirs();
		File original = getClassFile(klassName);
		FileUtils.copy(original, out);
		Printer.out(klassName);//+"\tto\t"+out);
		
		List<File> innerClasses = FileUtils.find(original.getParentFile(), 
				".*/"+original.getName().replace(".class", "\\$\\w+.class"));
		for (File file : innerClasses) {
			String fName = file.getName();
			File out2 = new File(out.getParentFile(), fName);
			FileUtils.copy(file, out2);
			Printer.out("	..."+fName);//+"\tto\t"+out);
		}
		
		// Source code too?
		if (includeSource && srcFile!=null) {
			FileUtils.copy(srcFile, out.getParentFile());	
		}
	}

	Set<String> klassesSkipped = new HashSet<String>();
	
	IFilter<File> filter;
	private boolean includeSource;
	
	/**
	 * @param filter Can be null (default). If set, only consider files
	 * which pass the filter.
	 */
	public void setFilter(IFilter<File> filter) {
		this.filter = filter;
	}
	
	/**
	 * Pull in stuff from the same package (which doesn't need
	 * an import)
	 * @param lines
	 * @param pckge
	 * @param hereNode 
	 * @param temp
	 */
	private void doTask3_copyInClasses2_samePackage(String[] lines, String pckge, 
			Tree<File> depNode, List<String> imports) 
	{
		Pattern klass = Pattern.compile("\\b([A-Z][A-Z_a-z0-9]+)\\b");
		for (String line : lines) {
			Matcher m = klass.matcher(line);
			while(m.find()) {
				String klassName = m.group(1);					
				// Did we import it?
				if (imports.contains(klassName)) {
					continue;
				}
				// add the package
				klassName = pckge+"/"+klassName;
				
				// save time & avoid redundant error messages
				if (klassesSkipped.contains(klassName)) {
					continue;
				}
				try {
					File klassFile = getClassFile(klassName);
					if (klassFile==null) continue;
					// ignore?
					if (filter!=null && ! filter.accept(klassFile)) {
						continue;
					}
					
					// no loops
					if (klassesDone.contains(klassFile)) continue;
					klassesDone.add(klassFile);
					
					// Find it!
					File klassSrc = getSrcFile(klassName);
					
					if ( ! klassFile.exists()) {
						Log.report("\tNo "+klassFile);
						continue;
					}
					// copy
					doTask4_copyClassFile(klassName, klassSrc);
					// recurse!
					doTask2_copyInClasses(klassSrc, depNode);
				} catch (IllegalArgumentException e) {
					// could not find the class		
//					Log.report("	skipping "+klassName);
					klassesSkipped.add(klassName);
				}
			}
		}		
	}
	
	
//	protected boolean doTask4_ignore(File klassFile) {
//		if (klassFile.getName().contains("Test.")) return true;
//		return false;
//	}

	/**
	 * Where does this class live?
	 * @param klassName
	 * @return
	 * @throws IllegalArgumentException
	 */
	private File getSrcFile(String klassName) throws IllegalArgumentException {
		String klassPath = klassName.replace('.', '/')+".java";
		// local?
		File lf = new File(klassPath);
		if (lf.exists()) {
			return lf;
		}
		// HACK: Try e.g. code/winterwell.utils/src/winterwell/utils
		String[] bits = StrUtils.find("winterwell.\\w+", klassName);
		if (bits!=null) {
			String project = bits[0];			
			File f = new File(FileUtils.getWinterwellDir(), "code/"+
					project.replace('/', '.')+"/src/"+klassPath);
			
			if (f.exists()) return f;
		}
		
		// Try in creole?
		File f2 = new File(FileUtils.getWinterwellDir(), "code/creole/src/"+klassPath);
		if (f2.exists()) return f2;
		// jgeoplanet
		f2 = new File(FileUtils.getWinterwellDir(), "forest-of-gits/jgeoplanet/src/main/java/"+klassPath);
		if (f2.exists()) return f2;	
		
//		// a search!
//		try {
//			com.winterwell.utils.Proc;ess find = new com.winterwell.utils.Proc;ess(
//					"find "+FileUtils.getWinterwellDir()+" -wholename \"*/"+klassPath+".java\"");
//			find.run();
//			find.waitFor(20000);
//			String out = find.getOutput();
//			String[] lines = StrUtils.splitLines(out);
//			File f3 = new File(lines[0]);
//			if (f3.exists()) {
//				return f3;
//			}
//		} catch(Exception ex) {
//			System.out.println(ex);
//		}
//		List<File> fs = FileUtils.find(new File(FileUtils.getWinterwellDir(),"code"), ".+/"+klassPath+"\\.java");
//		if ( ! fs.isEmpty()) {
//			return fs.get(0);
//		}
//		assert f.exists() : klassName+" -> "+f.getAbsolutePath();
//		System.out.println("\t\tFail: "+klassName);
		return null;
	}

	private File getClassFile(String klassName) throws IllegalArgumentException {
		File sf = getSrcFile(klassName);
		if (sf==null) return null;
		File cf = new File(sf.getPath().replace("/src/", "/bin/"));
		cf = FileUtils.changeType(cf, "class");
//		assert cf.exists() : klassName+" -> "+cf.getAbsolutePath();
		return cf;
	}

	public Tree<File> getDependencyTree() {
		return dependencies;
	}

	public void setIncludeSource(boolean incSrc) {
		this.includeSource = incSrc;		
	}

}
