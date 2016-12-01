package com.winterwell.bob.tasks;

import java.io.File;

/**
 * This is just a wrapper for calling SVN via the command line. You must have
 * SVN installed.
 * 
 * @author daniel
 */
public class SVNTask extends ProcessTask {

	public static final String COMMIT = "ci";
	public static final String UPDATE = "up";
	private String action;	

	public SVNTask(String action, File target) {
		super("svn ");		
		this.action = action;
		addArg("--non-interactive");
		addArg(action);		
		setEndOfCommand(target.getAbsolutePath());
	}

	/**
	 * Set the commit message
	 * 
	 * @param message
	 *            E.g. "Hello World"
	 */
	public void setMessage(String message) {
		assert action.equals(SVNTask.COMMIT) : action;		
		// Switch quotes
		message = message.replace('"', '\'');
		addArg("-m \"" + message + '"');
	}

}
