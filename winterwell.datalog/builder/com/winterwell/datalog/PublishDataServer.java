package com.winterwell.datalog;



import com.winterwell.web.app.build.KPubType;
import com.winterwell.web.app.build.PublishProjectTask;


public class PublishDataServer extends PublishProjectTask {
			
	public PublishDataServer() throws Exception {
		super("lg", "/home/winterwell/lg.good-loop.com/");
		typeOfPublish = KPubType.test;
		// non standard publish vs build names
		// -- cos the library and the server are sort of different projects
		BuildDataLog bd = new BuildDataLog();
		bd.setScpToWW(false);
		setBuildProjectTask(bd);
//		codePart = "backend";
//		jarFile = null;
	}

	
	@Override
	protected void doTask() throws Exception {
		super.doTask();
	}

}
