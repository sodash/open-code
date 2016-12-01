package jobs;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import com.winterwell.utils.Printer;

import com.winterwell.bob.BuildTask;
import com.winterwell.bob.tasks.CopyRequiredClassesTask;
import com.winterwell.bob.tasks.JarTask;
import com.winterwell.utils.io.FileUtils;

/**
 * An experiment in exporting selected slices of Winterwell.
 * @author daniel
 *
 */
public class BuildExportedSlices extends BuildTask {

	@Override
	public void doTask() throws Exception {
		File f1 = new File(FileUtils.getWinterwellDir(), "code/winterwell.maths/src/"
							+"winterwell/maths/chart/RenderWithFlot.java");
		File f2 = new File(FileUtils.getWinterwellDir(), "code/winterwell.maths/src/"
				+"winterwell/maths/chart/TimeSeriesChart.java");
		
//		File f1 = new File(FileUtils.getWinterwellDir(), "code/winterwell.utils/src/"
//				+"winterwell/utils/time/Time.java");
		List<File> target = Arrays.asList(f1
				,f2
				);
		
		File jardir = new File("temp-charting");
		jardir.mkdirs();
		FileUtils.deleteDir(jardir);
		jardir.mkdirs();
		
		CopyRequiredClassesTask req = new CopyRequiredClassesTask(target, jardir);
		req.run();
		
//		File jar = new File("time.jar");
		File jar = new File("charting.jar");
		
		FileUtils.delete(jar);
		JarTask jarTask = new JarTask(jar, jardir);
		jarTask.run();
		
		Printer.out(req.getDependencyTree().toString2(0, 30));
	}

}
