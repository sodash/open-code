package com.winterwell.bob.tasks;

import java.io.File;
import java.util.List;

import com.winterwell.bob.BuildTask;
import com.winterwell.utils.FailureException;
import com.winterwell.utils.Proc;
import com.winterwell.utils.ReflectionUtils;
import com.winterwell.utils.io.FileUtils;
import com.winterwell.utils.log.Log;

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

	/**
	 * NB: String 'cos the class might not be on the classpath for this JVM
	 */
	final String target;
	
	public ForkJVMTask(Class<? extends BuildTask> target) {
		this(target.getName());
	}
	
	public ForkJVMTask(String target) {
		this.target = target;
	}
	
	Classpath classpath = Classpath.getSystemClasspath();
	
	public ForkJVMTask setDir(File dir) {
		this.dir = dir;
		return this;
	}
	
	/**
	 * working dir for task
	 */
	private File dir;
	
	public Classpath getClasspath() {
		return classpath;
	}
	
	public void setClasspath(Classpath classpath) {
		this.classpath = classpath;
	}
	
	@Override
	protected void doTask() throws Exception {
		String command = "java -cp "+classpath+" com.winterwell.bob.Bob "+target;
		Log.d(LOGTAG, "fork "+target+" Full command: "+command);
		Proc proc = null;
		try {
			proc = new Proc(command);
			if (dir !=null) proc.setDirectory(dir);
			
			proc.start();
			int ok = proc.waitFor();
			
			if (ok != 0) {
				throw new FailureException(command+" -> "+proc.getError());
			}
			Log.d(LOGTAG, "fork error: "+proc.getError());
			Log.d(LOGTAG, "fork output: "+proc.getOutput());
		} finally {
			FileUtils.close(proc);
		}
	}

}
