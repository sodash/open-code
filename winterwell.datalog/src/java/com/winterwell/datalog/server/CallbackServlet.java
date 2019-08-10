package com.winterwell.datalog.server;

import com.winterwell.datalog.Callback;
import com.winterwell.datalog.CallbackManager;
import com.winterwell.datalog.DataLogEvent;
import com.winterwell.es.FixedESRouter;
import com.winterwell.utils.Utils;
import com.winterwell.web.ajax.JThing;
import com.winterwell.web.app.CrudServlet;
import com.winterwell.web.app.IServlet;
import com.winterwell.web.app.WebRequest;
import com.winterwell.web.fields.UrlField;

/**
 * TODO allow management of Callbacks
 * For now we hard code adserver
 * @author daniel
 * @see CallbackManager
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
		String eventType = state.get(DataLogEvent.EVT);
		Callback cb = new Callback(dataspace, eventType, url); 
		// OK				
		return new JThing().setJava(cb);
	}

}
