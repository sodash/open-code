package com.goodloop.chat.data;

import com.winterwell.es.ESKeyword;
import com.winterwell.utils.time.Time;
import com.winterwell.web.data.XId;

public class ChatLine {

	@ESKeyword
	XId from;
	
	String text;
	
	Time created;
	
}
