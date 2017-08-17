package com.winterwell.datalog.server;

import java.util.List;
import java.util.Map;

import com.winterwell.data.JThing;
import com.winterwell.datalog.Callback;
import com.winterwell.datalog.DataLogEvent;
import com.winterwell.datalog.ESStorage;
import com.winterwell.es.FixedESRouter;
import com.winterwell.es.IESRouter;
import com.winterwell.es.client.ESHttpClient;
import com.winterwell.es.client.SearchRequestBuilder;
import com.winterwell.utils.Dep;
import com.winterwell.utils.Utils;
import com.winterwell.utils.web.WebUtils2;
import com.winterwell.web.ajax.JsonResponse;
import com.winterwell.web.app.CrudServlet;
import com.winterwell.web.app.IServlet;
import com.winterwell.web.app.WebRequest;
import com.winterwell.web.fields.UrlField;

/**
 * TODO allow management of Callbacks
 * For now we hard code adserver
 * @author daniel
 *
 */
public class CallbackServlet extends CrudServlet<Callback> implements IServlet {


	public CallbackServlet() {
		super(Callback.class, new FixedESRouter("datalog.callbacks", "Callback"));
	}

	@Override
	protected JThing<Callback> doNew(WebRequest state, String id) {
		Utils.check4null(id, state);
		String dataspace = state.getRequired(DataServlet.DATASPACE);
		String url = state.getRequired(new UrlField("callback"));
		String eventType = state.get(DataLogEvent.EVENTTYPE);
		Callback cb = new Callback(dataspace, eventType, url); 
		// OK				
		return new JThing().setJava(cb);
	}

}
