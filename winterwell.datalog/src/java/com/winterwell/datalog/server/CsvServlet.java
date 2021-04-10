package com.winterwell.datalog.server;

import java.io.File;

import com.winterwell.datalog.CSV;
import com.winterwell.web.app.CrudServlet;
import com.winterwell.web.app.IServlet;
import com.winterwell.web.app.WebRequest;

public class CsvServlet extends CrudServlet<CSV>  {

	public CsvServlet() {
		super(CSV.class);
	}	

}
