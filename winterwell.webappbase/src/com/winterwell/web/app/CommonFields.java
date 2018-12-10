package com.winterwell.web.app;

import com.winterwell.data.KStatus;
import com.winterwell.web.data.XId;
import com.winterwell.web.fields.AField;
import com.winterwell.web.fields.EmailField;
import com.winterwell.web.fields.EnumField;
import com.winterwell.web.fields.IntField;
import com.winterwell.web.fields.SField;
import com.winterwell.web.fields.TimeField;
import com.winterwell.web.fields.XIdField;

public class CommonFields {

	/**
	 * Cookie set on login (and cleared on logout) that identifies the user
	 */
	public static final XIdField USER_XID = new XIdField("uxid");
	public static final AField<XId> XID = new XIdField("xid");
	/**
	 * target xid
	 */
	public static final XIdField TXID = new XIdField("txid"); 
	
	/**
	 * Auto lower-case on the returned value
	 */
	public static final EmailField EMAIL = new EmailField("email");
	public static final TimeField START = new TimeField("start").setPreferEnd(false);
	public static final TimeField END = new TimeField("end").setPreferEnd(true);

	public static final EnumField<KStatus> STATUS = new EnumField<>(KStatus.class, "status");
	public static final SField DESC = new SField("desc");
		

}
