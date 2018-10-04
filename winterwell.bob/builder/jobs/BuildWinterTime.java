package jobs;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import com.winterwell.bob.BuildTask;
import com.winterwell.bob.tasks.CopyRequiredClassesTask;
import com.winterwell.bob.tasks.JarTask;
import com.winterwell.utils.Printer;
import com.winterwell.utils.io.FileUtils;

/**
 * An experiment in exporting selected slices of Winterwell.
 * @author daniel
 *
 */
public class BuildWinterTime extends BuildTask {

	@Override
	public void doTask() throws Exception {
		File f1 = new File(FileUtils.getWinterwellDir(), "code/winterwell.utils/src/"
							+"com/winterwell/utils/time/Time.java");
		
		List<File> target = Arrays.asList(f1);
		
		File jardir = new File("temp-time-jar");
		jardir.mkdirs();
		FileUtils.deleteDir(jardir);
		jardir.mkdirs();
		
		CopyRequiredClassesTask req = new CopyRequiredClassesTask(target, jardir);
		req.setIncludeSource(true);
		req.run();
		
		File jar = new File(FileUtils.getWinterwellDir(), "code/winterwell.utils/wintertime.jar");
		
		FileUtils.delete(jar);
		JarTask jarTask = new JarTask(jar, jardir);
		jarTask.run();
		
		Printer.out(req.getDependencyTree().toString2(0, 30));
	}

}
