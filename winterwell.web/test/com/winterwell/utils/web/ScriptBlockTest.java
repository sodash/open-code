package com.winterwell.utils.web;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import com.winterwell.utils.web.ScriptBlock;

public class ScriptBlockTest {
	private String createBlock(String name, String contents) {
		return "<!--[<compile to=\"" + name + "\">]-->" + contents + "<!--[</compile>]-->";
	}
	
	private String createScript(String src) {
		return "<script type=\"text/javascript\" src=\"" + src + "?_=TIMESTAMP\" charset=\"UTF-8\"></script>";
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void blockWithoutToAttribute() {
		String block = "";
		
		ScriptBlock sut = new ScriptBlock(block);
	}
	
	@Test(expected=NullPointerException.class)
	public void createBlockWithNullText() {
		ScriptBlock[] sut = ScriptBlock.createScriptBlocks(null);
	}
	
	@Test
	public void createBlockWithNoBlocks() {
		ScriptBlock[] sut = ScriptBlock.createScriptBlocks("");
		
		assertEquals(0, sut.length);
	}
	
	@Test
	public void createBlockWithBlocks() {
		String name1 = "f-o-o";
		String script1 = "script-1.js";
		String block1 = createBlock(name1, createScript(script1));
		String name2 = "b-a-r";
		String script2 = "script-2.js";
		String block2 = createBlock(name2, createScript(script2));
		String text = block1 + '\n' + block2;
		
		ScriptBlock[] sut = ScriptBlock.createScriptBlocks(text);
		
		assertEquals(2, sut.length);
	}
	
	@Test
	public void blockReturnsCompiledFileName() {
		String name = "dir/foo-name.js";
		String contents = "";
		String block = createBlock(name, contents);
		
		ScriptBlock sut = new ScriptBlock(block);
		
		assertEquals(name, sut.getCompiledFileName());
	}
	
	@Test
	public void blockReturnsScripts() {
		String name = "f-o-o";
		String script1 = "dir1/script-1.js";
		String script2 = "dir2/script-2.js";
		String contents = '\n' + createScript(script1) + '\n' + createScript(script2) + '\n';
		String block = createBlock(name, contents);
		
		ScriptBlock sut = new ScriptBlock(block);
		
		List<String> expected = Arrays.asList(
				script1,
				script2
		);
		assert expected.equals(sut.getScripts()) : sut.getScripts();
	}
	
	@Test
	public void blockToStringReturnsBlock() {
		String name = "f-o-o";
		String script1 = "script-1.js";
		String script2 = "script-2.js";
		String contents = '\n' + createScript(script1) + '\n' + createScript(script2) + '\n';
		String block = createBlock(name, contents);
		
		ScriptBlock sut = new ScriptBlock(block);
		
		assertEquals(block, sut.toString());
	}
}
