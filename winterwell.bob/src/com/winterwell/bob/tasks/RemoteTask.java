package com.winterwell.bob.tasks;

/**
 * Execute a command on a remote machine. Works via SSH
 * 
 * @author daniel
 * 
 */
public class RemoteTask extends ProcessTask {

	/**
	 * E.g. new RemoteTask("localhost", "echo \"Monkeys monkeys monkeys\"");
	 * 
	 * @param sshConnection Typically "user@host"
	 * @param command
	 */
	public RemoteTask(String sshConnection, String command) {
		super("ssh");
		// Disable host key checks
		addArg("-o StrictHostKeyChecking=no"); //TODO this crashes Eclipse! April 2012 DBW on Ubuntu
		addArg("-o UserKnownHostsFile=/dev/null");
		addArg(sshConnection);
		addArg(command);
	}


}
