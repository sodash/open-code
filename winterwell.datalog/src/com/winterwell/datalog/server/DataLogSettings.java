package com.winterwell.datalog.server;

import java.io.File;

import com.winterwell.datalog.ESStorage;
import com.winterwell.datalog.IDataLogStorage;
import com.winterwell.datalog.StatConfig;
import com.winterwell.utils.io.FileUtils;
import com.winterwell.utils.io.Option;

/**
 * @deprecated TODO Merge with {@link StatConfig}
 * @author daniel
 *
 */
public class DataLogSettings {
	
	@Option
	String COOKIE_DOMAIN = ".good-loop.com";

	@Option
	int port = 8585;

	@Option
	public File logFile = new File(FileUtils.getWorkingDirectory(), "lg.txt"); 

	@Option
	Class<? extends IDataLogStorage> storageClass = ESStorage.class;
}
