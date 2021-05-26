package com.winterwell.web.app;

import com.winterwell.utils.Utils;
import com.winterwell.utils.log.Log;

final class MainLoopThread extends Thread {
	
	private AMain app;
	private String LOGTAG;

	public MainLoopThread(AMain app) {
		super(app.getAppNameLocal()+".doMainLoop");
		this.app = app;		
		setDaemon(false);
		LOGTAG = app.getAppNameLocal();
	}
	
	@Override
	public void run() {
		Log.d(LOGTAG, "Starting MainLoopThread...");
		while( ! app.pleaseStop) {					
			try {						
				app.doMainLoop();
			} catch(Throwable ex) {
				Log.w(LOGTAG, "caught MainLoopThread exception");
				Log.e(LOGTAG, ex);
				if (app.pleaseStop) return;
				// pause a moment
				Utils.sleep(100);
				// loop again... 
				// NB: use stop() to stop
			}
		}
		Log.w(LOGTAG, "...Ending MainLoopThread");
	}
			
	
}
