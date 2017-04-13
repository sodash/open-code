package com.winterwell.nlp.dict;

import static org.junit.Assert.assertEquals;

import java.io.File;

import org.junit.Test;

import com.winterwell.utils.containers.ArrayMap;
import com.winterwell.utils.io.FileUtils;

public class DictionaryTest {

	@Test
	public void testBogusFile() {
		try {
			Dictionary dict = new Dictionary(new File("bleurgh"), '\t');
			assert false;
		} catch (Exception e) {
			// ok
		}
	}
	
	

	@Test
	public void testContains() {
		ArrayMap<String, String> map = new ArrayMap<String, String>("hello",
				"", "world", "");
		{
			Dictionary dict = new Dictionary(map) {
				@Override
				protected String toCanonical(String word) {
					return word.trim();
				}
			};
			assert dict.contains("hello");
			assert !dict.contains("Hello");
		}
		{
			Dictionary dict = new Dictionary(map);
			assert dict.contains("hello");
			assert dict.contains("Hello");
		}
	}

	@Test
	public void testEmptyFile() {
		File ef = new File("bin/empty.txt");
		FileUtils.write(ef, "");
		Dictionary dict = new Dictionary(ef, '\t');
		assert dict.size() == 0;
	}

	@Test
	public void testMatch() {
		ArrayMap<String, String> map = new ArrayMap<String, String>("hello",
				"", "world", "", "foo", "", "foobar", "", "foob", "", "f", "");
		{
			Dictionary dict = new Dictionary(map) {
				@Override
				protected String toCanonical(String word) {
					return word.trim();
				}
			};

			String w = dict.match("hello world", 0);
			assertEquals("hello", w);

			w = dict.match("Hello world", 0);
			assertEquals("Hello", w);

			w = dict.match("blah hello world", 5);
			assertEquals("hello", w);

			w = dict.match("blah hello world", 0);
			assert w == null;
		}
		{ // case insensitive
			Dictionary dict = new Dictionary(map);

			String w = dict.match("hello world", 0);
			assertEquals("hello", w);

			w = dict.match("Hello world", 0);
			assertEquals("hello", w);
		}
		{ // test length
			Dictionary dict = new Dictionary(map);

			String w = dict.match("foobar", 0);
			assertEquals("foobar", w);

			w = dict.match("fooby", 0);
			assertEquals("foob", w);
		}
	}

}
