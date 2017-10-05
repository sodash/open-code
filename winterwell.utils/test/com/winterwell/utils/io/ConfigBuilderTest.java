package com.winterwell.utils.io;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Properties;

import org.junit.Test;

import com.winterwell.utils.containers.ArrayMap;

import junit.framework.TestCase;

public class ConfigBuilderTest extends TestCase {


	@Test
	public void testBoolean() throws IOException {
		TestSettings settings = new TestSettings();
		ConfigBuilder parser = new ConfigBuilder(settings);
		Properties props = new Properties();
		props.put("maybe", "true");
		parser.set(props);
		assert settings.maybe;
	}

	
	@Test
	public void testArgsSet() throws IOException {
		TestSettings settings = new TestSettings();
		ConfigBuilder parser = new ConfigBuilder(settings);
		Properties props = new Properties();
		props.put("nolog", "true");
		props.put("logdir", "/home/daniel/temp");
		props.put("n", "7"); 
		props.put("map.a", "Alice");
		parser.set(props);
		assert settings.noLogging == true;
		assert settings.loggingDir.equals(new File("/home/daniel/temp"));
		assert settings.n == 7;				
	}
	
	@Test
	public void testMap() throws IOException {
		TestSettings settings = new TestSettings();
		ConfigBuilder parser = new ConfigBuilder(settings);
		Properties props = new Properties();
		props.put("map.a", "Alice");
		parser.set(props);
		assert settings.map.get("a").equals("Alice") : settings.map;
	}

	@Test
	public void testNest() throws IOException {
		NestedTestSettings settings = new NestedTestSettings();
		ConfigBuilder parser = new ConfigBuilder(settings);
		Properties props = new Properties();
		props.put("n", "2");
		props.put("ts.n", "3");
		parser.set(props);
		assert settings.n == 2;
		assert settings.ts.n == 3;
	}
	
	@Test
	public void testDebug() throws IOException {
		NestedTestSettings settings = new NestedTestSettings();
		ConfigBuilder parser = new ConfigBuilder(settings).setDebug(true);
		Properties props = new Properties();
		props.put("n", "2");
		props.put("ts.n", "3");
		parser.set(props);
		parser.setFromMain(new String[] {"-ts.meh", "foo"});
		parser.get();
		assert settings.n == 2;
		assert settings.ts.n == 3;
	}
	
	@Test
	public void testOptionsMessage() throws IOException {
		TestSettings settings = new TestSettings();
		ConfigBuilder parser = new ConfigBuilder(settings);
		String om = parser.getOptionsMessage();
		System.out.println(om);
	}
	
	@Test
	public void testCommandLineOptions() throws IOException {
		{
			TestSettings settings = new TestSettings();		
			ConfigBuilder parser = new ConfigBuilder(settings);
			parser.setFromMain(new String[] {"-meh", "whatever", "-logdir", "home"});
			assert settings.meh.equals("whatever") : settings.meh;
		}
		{
			TestSettings settings = new TestSettings();		
			ConfigBuilder parser = new ConfigBuilder(settings);
			parser.setFromMain(new String[] {"-n", "10", "-logdir", "home"});
			assert settings.n == 10 : settings.n;
		}
	}

}


class TestSettings {

	@Option
	public Boolean maybe;

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
	
	@Option
	public String meh;

}

class NestedTestSettings {
	
	@Option
	TestSettings ts = new TestSettings();
	
	@Option
	int n = 5;
	
}