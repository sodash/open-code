package com.winterwell.utils.io;

import org.junit.Test;

import com.winterwell.datalog.DataLogConfig;

public class ConfigFactoryTest {

	@Test
	public void testGetConfig() {
		ConfigFactory cf = ConfigFactory.get();
		assert cf != null;
		DataLogConfig dlc = cf.getConfig(DataLogConfig.class);
		System.out.println(dlc);
	}

	
	@Test
	public void testGetConfigPrivateClass() {
		ConfigFactory cf = ConfigFactory.get();
		assert cf != null;
		PrivateConfig dlc = cf.getConfig(PrivateConfig.class);
		System.out.println(dlc);
	}

}

class PrivateConfig {
	
	private PrivateConfig() {
		
	}
	
	@Option
	String foo = "bar";
	
}