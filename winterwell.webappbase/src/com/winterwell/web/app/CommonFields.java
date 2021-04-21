package com.winterwell.web.app;

import com.winterwell.data.KStatus;
import com.winterwell.utils.threads.ICallable;
import com.winterwell.utils.time.Period;
import com.winterwell.utils.time.Time;
import com.winterwell.web.data.XId;
import com.winterwell.web.fields.AField;
import com.winterwell.web.fields.EmailField;
import com.winterwell.web.fields.EnumField;
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
	public static final SField Q = new SField("q");
	/**
	 * convenience for using start/end
	 * @param state Can be null
	 * @return period if start or end are set, or null.
	 * NB: Period will use WELL_OLD / WELL_FUTURE to fill in a single null start/end
	 */
	public static Period getPeriod(WebRequest state) {
		if (state==null) {
			return null;
		}
		ICallable<Time> _start = state.get(CommonFields.START);
		ICallable<Time> _end = state.get(CommonFields.END);
		if (_start==null && _end==null) return null;
		Time start = _start==null? null : _start.call();
		Time end = _end==null? null : _end.call();
		return new Period(start, end);
	}
		

}
