package com.winterwell.datalog.server;

import com.winterwell.datalog.Dataspace;
import com.winterwell.web.app.CommonFields;
import com.winterwell.web.fields.AField;
import com.winterwell.web.fields.SField;
import com.winterwell.web.fields.TimeField;

public class DataLogFields extends CommonFields {

	public static final AField<Dataspace> d = new DataspaceField("d");
	/**
	 * tag
	 */
	public static SField t = new SField("t");
	public static TimeField time = new TimeField("time");

}

class DataspaceField extends AField<Dataspace> {
	private static final long serialVersionUID = 1L;
	
	public DataspaceField(String name) {
		super(name);
	}
	
	@Override
	public Dataspace fromString(String v) throws Exception {
		return new Dataspace(v);
	}
}
