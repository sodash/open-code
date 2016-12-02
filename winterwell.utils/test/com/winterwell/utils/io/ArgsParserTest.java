package com.winterwell.utils.io;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import com.winterwell.utils.containers.ArrayMap;

import junit.framework.TestCase;

public class ArgsParserTest extends TestCase {

	public void testArgsSet() throws IOException {
		TestSettings settings = new TestSettings();
		ArgsParser parser = new ArgsParser(settings);
		Properties props = new Properties();
		props.put("nolog", "true");
		props.put("logdir", "/home/daniel/temp");
		props.put("n", "7"); 
		props.put("map.a", "Alice");
		List<Field> set = parser.set(props);
		assert settings.noLogging == true;
		assert settings.loggingDir.equals(new File("/home/daniel/temp"));
		assert settings.n == 7;				
	}
	
	public void testMap() throws IOException {
		TestSettings settings = new TestSettings();
		ArgsParser parser = new ArgsParser(settings);
		Properties props = new Properties();
		props.put("map.a", "Alice");
		List<Field> set = parser.set(props);
		assert settings.map.get("a").equals("Alice") : settings.map;
	}

	
	class TestSettings {

		@Option
		public Map map = new ArrayMap("a","b");
		
		@Option(tokens = "-logdir", description = "Directory to write log files to", required = true)
		public File loggingDir = new File("boblog");

		@Option(tokens = "-n", description = "Number of things")
		public int n = 5;

		@Option(tokens = "-nolog", description = "Switch off logging")
		public boolean noLogging;

		@Option(tokens = "-v")
		public boolean verbose;

	}
}