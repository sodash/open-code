package com.winterwell.utils.web;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;

import org.junit.Test;

import com.winterwell.utils.Printer;
import com.winterwell.utils.containers.Containers;
import com.winterwell.utils.containers.Slice;

public class HashTaggerTest {


	
	@Test
	public void testGetTags() {
		{
			HashTagger ht = new HashTagger("");
			assert ht.getTags().isEmpty();
		}
		{
			HashTagger ht = new HashTagger("#a #b");
			assert Containers.same(ht.getTags(), Arrays.asList("a", "b"));
		}
		{
			HashTagger ht = new HashTagger(" #a #b, foo");
			assert Containers.same(ht.getTags(), Arrays.asList("a", "b"));
		}
		{
			HashTagger ht = new HashTagger("Look: #a\r\n #b	 foo");
			assert Containers.same(ht.getTags(), Arrays.asList("a", "b"));
		}	
	}
	

	@Test
	public void testAtYou() {
		{
			HashTagger ht = new HashTagger("");
			Matcher m = ht.atYouSir.matcher("@alice @bob");
			assert m.find();
			assert m.group(1).equals("alice");
			assert m.find();
			assert m.group(1).equals("bob");
		}		
		{
			HashTagger ht = new HashTagger("");
			Matcher m = ht.atYouSir.matcher("@alice@sodash.com");
			assert m.find();
			assert m.group(1).equals("alice@sodash.com") : m.group(1);
		}		
		{	// test with spurious punctuation
			Matcher m = HashTagger.atYouSir.matcher("RT .@winterstein: stuff");
			assert m.find();
			String name = m.group(1);
			assertEquals("winterstein", name);
		}
		{	// test at start
			Matcher m = HashTagger.atYouSir.matcher("@winterstein: stuff");
			assert m.find();
			String name = m.group(1);
			assertEquals("winterstein", name);
		}
		{	// test inline
			Matcher m = HashTagger.atYouSir.matcher("hey @winterstein, stuff");
			assert m.find();
			String name = m.group(1);
			assertEquals("winterstein", name);
		}
		{	// No times
			HashTagger ht = new HashTagger("");
			Matcher m = ht.atYouSir.matcher("@9pm");
			assert m.find();
			assertEquals("9pm", m.group(1));
		}
	}
	
	
	@Test
	public void testGetMentionSlices() {
		{
			HashTagger ht = new HashTagger("Look: @dan or @daniel or @dan");
			List<Slice> slices = ht.getMentionSlices();
			assert slices.size() == 3;
			assert Printer.toString(slices, " ").equals("dan daniel dan");
		}
	}
	
	@Test
	public void testTagsAdvanced() {
		{
			HashTagger ht = new HashTagger("Look: #foo-bar");
			assert Containers.same(ht.getTags(), Arrays.asList("foo-bar"));
		}
		{
			HashTagger ht = new HashTagger("Look: #foo-");
			assert Containers.same(ht.getTags(), Arrays.asList("foo")) : ht.getTags();
		}
		{
			HashTagger ht = new HashTagger("What?#a,#b");
			assert Containers.same(ht.getTags(), Arrays.asList("a","b"));
		}
		{
			HashTagger ht = new HashTagger("#a.");
			assert Containers.same(ht.getTags(), Arrays.asList("a"));
		}
		{
			HashTagger ht = new HashTagger("What?#a.b");
			assert Containers.same(ht.getTags(), Arrays.asList("a.b"));
		}
		{
			HashTagger ht = new HashTagger("#a.b.");
			assert Containers.same(ht.getTags(), Arrays.asList("a.b"));
		}
	}

}
