package com.winterwell.web.data;

import java.util.regex.Matcher;

import org.junit.Test;

import com.winterwell.web.data.XId;

public class XIdTest {

	


	@Test
	public void testXIdPattern() {
		{
			Matcher m = XId.XID_PATTERN.matcher("hello alice@twitter");
			assert m.find();
			assert m.group().equals("alice@twitter");
		}
		{
			Matcher m = XId.XID_PATTERN.matcher("hello alice@sodash.com@soda.sh yes");
			assert m.find();
			assert m.group().equals("alice@sodash.com@soda.sh") : m.group();
			assert m.group(1).equals("alice@sodash.com") : m.group(1);
			assert m.group(2).equals("soda.sh") : m.group(2);
		}
		{
			Matcher m = XId.XID_PATTERN.matcher("123@facebook");
			assert m.find();
			assert m.group().equals("123@facebook");
		}
		{
			Matcher m = XId.XID_PATTERN.matcher("123@facebook, b");
			assert m.find();
			assert m.group().equals("123@facebook");
		}
	}
	
	@Test
	public void testXIdString() {
		XId id = new XId("alice@foo", "bar.com", false);
		String s = id.toString();
		XId id2 = new XId(s, false);
		assert id.equals(id2);
		assert id.hashCode() == id2.hashCode();
	}

}
