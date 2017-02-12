package com.winterwell.datalog.server;

import java.io.File;

import com.winterwell.datalog.ESStorage;
import com.winterwell.datalog.IStatStorage;
import com.winterwell.utils.io.FileUtils;
import com.winterwell.utils.io.Option;

public class DataLogSettings {
	
	@Option
	String COOKIE_DOMAIN = ".good-loop.com";

	@Option
	int port = 8585;

	@Option
	public File logFile = new File(FileUtils.getWorkingDirectory(), "lg.txt"); 

	@Option
	Class<? extends IStatStorage> storageClass = ESStorage.class;
}
