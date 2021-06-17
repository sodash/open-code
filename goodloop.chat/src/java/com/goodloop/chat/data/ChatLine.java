package com.goodloop.chat.data;

import com.winterwell.es.ESKeyword;
import com.winterwell.utils.time.Time;
import com.winterwell.web.data.XId;
/**
 * Status: Not used
 * @author daniel
 *
 */
public class ChatLine {

	@ESKeyword
	XId from;
	
	String text;
	
	Time created;
	
}
