package com.winterwell.bob.tasks;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.winterwell.bob.BuildTask;
import com.winterwell.utils.FailureException;
import com.winterwell.utils.Proc;
import com.winterwell.utils.log.Log;
import com.winterwell.utils.time.Dt;
import com.winterwell.utils.time.TUnit;

/**
 * Run external processes. Warning: This is not the same as a shell command!
 * <p>
 * Throws a {@link FailureException} if the process returns a code other than 0.
 * Throws an {@link InterruptedException} if a timeout is set and exceeded.
 * </p>
 *
 * @see {@link Proc}.
 * @author daniel
 */
public class ProcessTask extends BuildTask {
	protected List<String> args = new ArrayList<String>();

	String command;

	private transient String error;

	private transient String output;

	private File workingDir;

	protected void setWorkingDir(File workingDir) {
		this.workingDir = workingDir;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName()+"["+getCommand()+"]"; 
	}
	
	private boolean echoStdOut;

	public void setEchoStdOut(boolean b) {
		echoStdOut = b;
	}
	
	/**
	 * Added after the args
	 */
	private String endOfCommand;

	private transient Proc p;

	/**
	 * false by default. If true, also send all output to system out
	 */
	private boolean echo;

	private Integer code;

	/**
	 * false by default. If true, also send all output to system out
	 */
	public void setEcho(boolean echo) {
		this.echo = echo;
	}

	public void setEndOfCommand(String endOfCommand) {
		this.endOfCommand = endOfCommand;
	}

	/**
	 * Run an external command. Warning: This is not the same as a shell
	 * command!<br>
	 * When run, this task will block until the command finishes.
	 *
	 * @param command
	 *            The command to run. This can include options and arguments, or
	 *            these can be added via {@link #addArg(String)}, or both.
	 * @see {@link Proc}.
	 */
	public ProcessTask(String command) {
		this.command = command;
	}

	/**
	 * Run an external command. Warning: This is not the same as a shell
	 * command!<br>
	 * When run, this task will block until the command finishes, or the timeout
	 * expires.
	 *
	 * @param command
	 * @param timeout
	 *            in milliseconds
	 * @see {@link Proc}.
	 */
	public ProcessTask(String command, long timeout) {
		this.command = command;
		setMaxTime(new Dt(timeout, TUnit.MILLISECOND));
	}

	/**
	 * Add a command argument (which will be appended onto the command)
	 *
	 * @param option
	 *            E.g. "-m \"whatever\""
	 */
	public void addArg(String option) {
		args.add(option);
	}

	@Override
	public void doTask() throws Exception {
		String cmd = getCommand();
		// Make the process
		// FIXME: Use the List<String> constructor?
		p = new Proc(cmd);
		if (workingDir != null)
			p.setDirectory(workingDir);
		
		if (echoStdOut) {
			p.setEcho(true);
		}
		
		// Do it!
		Log.d(getClass().getSimpleName(), cmd+"...");
		p.setEcho(echo);
		p.start();
		
		code = p.waitFor(); //(timeout);
		// Done?
		output = p.getOutput();
		error = p.getError();
		
//		if ( ! echoStdOut) {
//			System.out.println(output);
//			System.out.println(error);
//		}
		
		// Error?
		if (processFailed(code)) {
			throw new FailureException(cmd.toString(), 
					output + " " + error);
		}
	}
	
	/**
	 * @return null if not run/completed, 0 for OK, other values prob indicate an error
	 */
	public Integer getOutputCode() {
		return code;
	}

	/**
	 * @return the command that will be / was run
	 */
	public String getCommand() {
		StringBuilder cmd = new StringBuilder();
		cmd.append(command);
		cmd.append(' ');
		for (String option : args) {
			cmd.append(option);
			cmd.append(' ');
		}
		if (endOfCommand!=null) cmd.append(endOfCommand);
		return cmd.toString();
	}

	/**
	 * Did the process fail?
	 * Generally, this means "return code != 0", but you
	 * may wish to ignore non-fatal-but-non-ideal error codes.
	 * @param code
	 * @return
	 */
	protected boolean processFailed(int code) {
		return code != 0;
	}

	/**
	 * @return captured error output from the process after it has finished
	 */
	public String getError() {
		return error;
	}

	/**
	 * @return captured output from the process after it has finished
	 */
	public String getOutput() {
		return output;
	}

	public void setDirectory(File workingDir) {
		this.workingDir = workingDir;
	}

	/**
	 * Ensure the process is destroyed.
	 */
	@Override
	public void close() {
		if (p==null) return;
		p.destroy();
		if (output==null) output = p.getOutput();
		if (error==null) error = p.getError();
		p=null;
	}
	
	@Override
	protected void finalize() throws Throwable {
		if (p!=null) p.destroy();
		super.finalize();
	}

}
