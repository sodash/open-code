package winterwell.utils.io;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringReader;

import junit.framework.TestCase;

public class LineReaderTest extends TestCase {

	public void testLineReader() {
		LineReader r = new LineReader(new StringReader("line 1\n"
				+ "line two\r\n" + "line \"3\"|||\n"));
		assert r.hasNext();
		assert r.hasNext();
		String line = r.next();
		assert line.equals("line 1");
		line = r.next();
		assert line.equals("line two");
		assert r.hasNext();
		line = r.next();
		String test = "line \"3\"|||";
		assert line.equals(test) : line;
		assert !r.hasNext();
		r.close();
	}

	public void testLineReader2() throws IOException {
		BufferedReader br = new BufferedReader(new FileReader(new File(
				"test/test-easy.csv")));
		String line1 = br.readLine();
		String line2 = br.readLine();
		assert line2.length() < 500;
		String line3 = br.readLine();
		String end = br.readLine();
		br.close();

		LineReader r = new LineReader(new File("test/test-easy.csv"));
		assert r.hasNext();
		assert r.hasNext();
		String line = r.next();
		assert line.equals("# This is a comment line") : line;
		line = r.next();
		assert line.equals("Name|Notes|is OK?") : line;
		line = r.next();
		assert line.equals("\"Daniel Winterstein\"|Overrated|yes") : line;
		assert r.hasNext();
		line = r.next();
		String test = "\"Joe Halliwell\"|\"Should stop smoking\"|yes";
		assert line.equals(test) : line;
		assert !r.hasNext();
		r.close();
	}
}
