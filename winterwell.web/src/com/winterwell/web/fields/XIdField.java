package com.winterwell.web.fields;

import com.winterwell.web.data.XId;

/**
 * An external id, e.g. winterstein@twitter
 * @author daniel
 *
 */
public final class XIdField extends AField<XId> {
	
	private static XId adminXId;
	
	public static void setAdminXId(XId adminXId) {
		XIdField.adminXId = adminXId;
	}

	public XIdField() {
		super("dummy");
	}
	
	public XIdField(String name) {
		super(name);
	}
	
	@Override
	public Class getValueClass() {
		return XId.class;
	}
	
	@Override
	public XId fromString(String v) throws Exception {
//		// HACK: special case! "me" or "su"
//		if ("me".equals(v)) {
//			return DBPerson.getUser().getXId();
//		}
//		if ("trueme".equals(v)) {
//			return DBPerson.getTrueUser().getXId();
//		}
		// HACK: Is it an email?
		int i = v.lastIndexOf('@');
		if (i == v.indexOf('@') && v.substring(i, v.length()).contains(".") 
				/* paranoia */ && ! v.endsWith("@soda.sh")) {
			return new XId(v, "email");
		}
		
		if ("su".equals(v)) {
			return adminXId; // this is safe -- its only the xid
		}
		return new XId(v);
	}	
	
	private static final long serialVersionUID = 1L;
}