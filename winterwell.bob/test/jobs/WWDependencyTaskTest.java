package jobs;

import static org.junit.Assert.*;

import java.io.File;

import org.junit.Test;

public class WWDependencyTaskTest {

	@Test
	public void testDoTask() {
		WWDependencyTask buildJTwitter = new WWDependencyTask("jtwitter", "winterwell.jtwitter.BuildJTwitter");
		buildJTwitter.run();
		File jar = buildJTwitter.getJar();
		assert jar.isFile();
	}

}
