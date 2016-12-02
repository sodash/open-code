package com.winterwell.utils.web;

import static org.junit.Assert.assertEquals;

import java.io.File;

import org.junit.Ignore;
import org.junit.Test;

import com.winterwell.utils.io.FileUtils;

public class ScriptExtractorTest {
	private String getOpeningTag(String name) {
		return "<!--[<compile to=\"" + name + "\">]-->";
	}
	
	private String getClosingTag() {
		return "<!--[</compile>]-->";
	}
	
	private String getScriptBlock(String name, String content) {
		return getOpeningTag(name) + '\n' + content + '\n' + getClosingTag();
	}
	
	private String getScriptElement(String name) {
		return "<script src=\"/static/code/" + name + "\"></script>";
	}
	
	@Test(expected=NullPointerException.class)
	public void createWithNullFileThrowsNullPointerException() {
		ScriptExtractor sut = new ScriptExtractor(null);
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void createWithInvalidFileThrowsIllegalArgumentException() {
		ScriptExtractor sut = new ScriptExtractor(new File("thisfiledoesnotexist"));
	}
	
	@Test
	public void getScriptBlocksReturnsCorrectArray() {
		String script1 = "dir/foo.js";
		String block1 = getScriptBlock("dir/out1.js", getScriptElement(script1));
		String script2 = "dir2/bar.js";
		String block2 = getScriptBlock("dir/out2.js", getScriptElement(script2));
		File file = FileUtils.createTempFile("scripts", "html");
		FileUtils.write(file, block1 + '\n' + block2);
		
		ScriptExtractor sut = new ScriptExtractor(file);
		
		ScriptBlock[] expecteds = new ScriptBlock[] {
			new ScriptBlock(block1),
			new ScriptBlock(block2)
		};
		ScriptBlock[] actuals = sut.getScriptBlocks();
		assertEquals(expecteds[0].toString(), actuals[0].toString());
		assertEquals(expecteds[1].toString(), actuals[1].toString());
	}
	
	@Test
	public void toCompiledFileCreatesCorrectFile() {
		File outputFile = FileUtils.createTempFile("out", ".min.js");
		String script = "dir/foo.js";
		String cacheBustCode = "foobar";
		String block = getScriptBlock(outputFile.getName(), getScriptElement(script));
		File file = FileUtils.createTempFile("scripts", "html");
		FileUtils.write(file, block);
		
		ScriptExtractor sut = new ScriptExtractor(file);
		sut.toCompiledFile(outputFile, cacheBustCode);
		
		String expected = "<script src=\"" + outputFile.getName() + "?_=" + cacheBustCode + "\"></script>";
		String actual = FileUtils.read(outputFile);
		assertEquals(expected, actual);
	}
	
	@Test
	@Ignore
	public void toCompiledFileRemovesComments() {
		File outputFile = FileUtils.createTempFile("out", ".min.js");
		String comment = "<!-- A comment. -->Not a comment.<!-- Another\ncomment. -->";
		File file = FileUtils.createTempFile("scripts", "html");
		FileUtils.write(file, comment);
		
		ScriptExtractor sut = new ScriptExtractor(file);
		sut.toCompiledFile(outputFile, null);
		
		String expected = "Not a comment.";
		String actual = FileUtils.read(outputFile);
		assertEquals(expected, actual);
	}
	
	@Test
	public void toCompiledFileDoesNotRemoveBlocks() {
		File outputFile = FileUtils.createTempFile("out", ".min.js");
		String block = "<!--[ A block. ]-->";
		File file = FileUtils.createTempFile("scripts", "html");
		FileUtils.write(file, block);
		
		ScriptExtractor sut = new ScriptExtractor(file);
		sut.toCompiledFile(outputFile, null);
		
		String expected = block;
		String actual = FileUtils.read(outputFile);
		assertEquals(expected, actual);
	}
}
