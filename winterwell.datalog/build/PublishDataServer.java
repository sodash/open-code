


import com.winterwell.web.app.KPubType;
import com.winterwell.web.app.PublishProjectTask;


public class PublishDataServer extends PublishProjectTask {
			
	public PublishDataServer() throws Exception {
		super("lg", "/home/winterwell/lg.good-loop.com/");
		typeOfPublish = KPubType.production;
		codePart = "backend";
		jarFile = null;
	}

	
	@Override
	protected void doTask() throws Exception {
		super.doTask();
	}

}
