package winterwell.bob.tasks;

import java.io.File;

/**
 * Create a zip file.
 */
public class ZipTask extends JarTask {

	/**
	 * Create a jar from the files in a directory (recursive, includes
	 * everything)
	 * 
	 * @param jar
	 * @param inputDir
	 */
	public ZipTask(File jar, File inputDir) {
		super(jar, inputDir);
	}

	/**
	 * 
	 * @param jar
	 *            Jar file to create
	 * @param inputFiles Can include directories, which will be recursively included 
	 * @param inputBase
	 *            Use this to chop file names down to size. E.g. "/mydir" will
	 *            lead to "/mydir/myfile.class" being stored as "myfile.class"
	 */
	public ZipTask(File jar, Iterable<File> inputFiles, File inputBase) {
		super(jar, inputFiles, inputBase);
	}
}
