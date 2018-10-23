package com.winterwell.bob.wwjobs;

import static org.junit.Assert.*;

import java.io.File;

import org.junit.Test;

import com.winterwell.bob.wwjobs.WWDependencyTask;

public class WWDependencyTaskTest {

	@Test
	public void testDoTask() {
		WWDependencyTask buildJTwitter = new WWDependencyTask("jtwitter", "winterwell.jtwitter.BuildJTwitter");
		buildJTwitter.run();
		File jar = buildJTwitter.getJar();
		assert jar.isFile();
	}

}
