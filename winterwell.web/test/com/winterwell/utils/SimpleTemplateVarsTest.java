package com.winterwell.utils;

import static org.junit.Assert.assertEquals;

import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import com.winterwell.utils.containers.ArrayMap;
import com.winterwell.utils.web.WebUtils;

public class SimpleTemplateVarsTest {


	@Test
	public void testEncodeURIFn() {
		{
			Map src = new ArrayMap("name", "Daniel Winterstein");
			SimpleTemplateVars props = new SimpleTemplateVars(src);
			props.addFn("encodeURI", WebUtils::urlEncode);
			String s = props.process("Hello link://${encodeURI($name)}");
			assertEquals("Hello link://Daniel%20Winterstein", s);
		}
	}
	
	@Test
	public void testProcessNoJS() {
		{
			Map src = new ArrayMap("name", "Daniel");
			SimpleTemplateVars props = new SimpleTemplateVars(src);
			String s = props.process("Hello $name! Leave $namething alone.");
			assertEquals("Hello Daniel! Leave $namething alone.", s);
		}
	}
	
	@Test
	public void testProcessNumberFormat() {
		{
			Map src = new ArrayMap("name", "Daniel", "age", 1207);
			SimpleTemplateVars props = new SimpleTemplateVars(src);
			props.setNumberFormat(new DecimalFormat("#,###"));
			String s = props.process("Hello $name aged $age");
			assert "Hello Daniel aged 1,207".equals(s) : s;
		}
	}
	
	@Test
	public void testProcessScript() {
		{
			Map src = new ArrayMap("name", "Daniel");
			SimpleTemplateVars props = new SimpleTemplateVars(src).setUseJS(true);
			String s = props.process("Hello ${name}!");
			assertEquals("Hello Daniel!", s);
		}
		{	// ?
			Map src = new ArrayMap("name", "Daniel");
			SimpleTemplateVars props = new SimpleTemplateVars(src).setUseJS(true);
			String s = props.process("Hello ${window.name? name : ''}!");
			assertEquals("Hello Daniel!", s);
		}
		{	// unset!
			Map src = new ArrayMap("name", "Daniel");
			SimpleTemplateVars props = new SimpleTemplateVars(src).setUseJS(true);
			String s = props.process("Hello ${window.foo? foo : 'dev'}!");
			assertEquals("Hello dev!", s);
		}
	}
		

}
