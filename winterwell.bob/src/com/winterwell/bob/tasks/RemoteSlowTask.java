package com.winterwell.bob.tasks;


/**
 * Use nohup to start a remote task which may take some time.
 * @author daniel
 * @testedby {@link RemoteSlowTaskTest}
 */
public class RemoteSlowTask extends ProcessTask {

	private String pipeOutTo;

	/**
	 * 
	 * @param sshConnection e.g. alice@winterwell.com
	 * @param command	e.g. "java SlowThing"
	 * @param pipeOutTo e.g. output.txt Can be null (outputs to /dev/null)
	 */
	public RemoteSlowTask(String sshConnection, String command, String pipeOutTo) {
		super("ssh ");
//		addArg("-o StrictHostKeyChecking=no"); TODO this crashes Eclipse! April 2012 DBW on Ubuntu
		addArg(sshConnection);
		addArg("nohup "+command);		
		this.pipeOutTo = pipeOutTo;
	}
	
	public String getCommand() {
		String cmd = super.getCommand();
		// Is this needed??
		if (pipeOutTo==null) pipeOutTo ="/dev/null"; 
		return cmd+" > "+pipeOutTo+" 2> "+pipeOutTo+" < /dev/null";
	}
}
