


import com.winterwell.web.app.build.KPubType;
import com.winterwell.web.app.build.PublishProjectTask;


public class PublishDataServer extends PublishProjectTask {
			
	public PublishDataServer() throws Exception {
		super("lg", "/home/winterwell/lg.good-loop.com/");
		typeOfPublish = KPubType.production;
//		codePart = "backend";
//		jarFile = null;
	}

	
	@Override
	protected void doTask() throws Exception {
		super.doTask();
	}

}
