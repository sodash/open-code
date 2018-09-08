package com.winterwell.bob.tasks;

import java.io.File;
import java.util.List;

import com.winterwell.bob.BuildTask;
import com.winterwell.utils.Proc;
import com.winterwell.utils.ReflectionUtils;
import com.winterwell.utils.io.FileUtils;

/**
 * TODO This task runs a separate Java process
 * 
 * FIXME It does not preserve the file settings
 * Maybe send an xstream aml blob via a temp file??
 * 
 * @author daniel
 * @testedby ForkJVMTaskTest
 */
public class ForkJVMTask extends BuildTask {

	BuildTask target;
	
	public ForkJVMTask(BuildTask target) {
		this.target = target;
	}
	
	/**
	 * HACK
	 */
	String classpath = FileUtils.getWinterwellDir()+"/open-code/winterwell.bob/bob-all.jar";
	/**
	 * working dir for task
	 */
	private File dir;
	
	public void setClasspath(String classpath) {
		this.classpath = classpath;
	}
	
	@Override
	protected void doTask() throws Exception {
		String command = "java -cp "+classpath+" com.winterwell.bob.Bob "+target.getClass().getName();
		System.out.println("");
		System.out.println(command);
		System.out.println("");
		Proc proc = new Proc(command);
		if (dir !=null) proc.setDirectory(dir);
		
		proc.start();
		int ok = proc.waitFor();
		
		System.out.println(ok);
		System.err.println(proc.getError());
		System.out.println(proc.getOutput());
	}

}
