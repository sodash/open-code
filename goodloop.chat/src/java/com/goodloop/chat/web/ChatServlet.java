package com.goodloop.chat.web;

import com.goodloop.chat.data.Chat;
import com.winterwell.web.app.CrudServlet;

public class ChatServlet extends CrudServlet<Chat> {

	public ChatServlet(Class<Chat> type) {
		super(type);
	}

}
