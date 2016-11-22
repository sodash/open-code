package com.winterwell.utils.web;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.winterwell.utils.containers.ArraySet;
import com.winterwell.utils.io.FileUtils;

/**
 * Find blocks of scripts -- see {@link ScriptBlock}
 * 
 * TODO rewrite to use HtmlParser or WebUtils2.parseXmlToTree(xml) or WebUtils2.xpath()
 * Regexes are fragile!
 * 
 * @author Steven
 * @testedby {@link ScriptExtractorTest}
 */
public class ScriptExtractor {
	
	private final String contents;
	private final ScriptBlock[] scriptBlocks;
	
	/**
	 * @param src
	 * @param cacheBustCode
	 * @return script tag for src, with cacheBustCode added if not null 
	 */
	private String createScriptElement(String src, String cacheBustCode) {
		assert src != null;
		if (cacheBustCode != null) {
			src += "?_=" + cacheBustCode;
		}		
		return "<script src=\"" + src + "\"></script>";
	}
	
	public ScriptExtractor(File file) {
		if (file == null) {
			throw new NullPointerException("file");
		}		
		try {
			contents = FileUtils.read(file);			
			
			scriptBlocks = ScriptBlock.createScriptBlocks(contents);			
		} catch (Exception e) {
			throw new IllegalArgumentException("file", e);
		}
		
		// safety check: no overlapping blocks allowed
		Set<String> cfns = new HashSet();
		for (ScriptBlock sb : scriptBlocks) {
			String cfn = sb.getCompiledFileName();
			if (cfns.contains(cfn)) {
				throw new IllegalArgumentException(file+" contains overlapping blocks "+cfn);
			}
			cfns.add(cfn);
		}
	}
	
	public ScriptBlock[] getScriptBlocks() {
		return this.scriptBlocks;
	}
	
	
	/**
	 * Alter a template file to use compiled resources.
	 * 
	 * @param file Save to this file
	 * @param cacheBustCode
	 * @return scripts outside of script-blocks which should be minified
	 */
	public ArrayList<String> toCompiledFile(File file, String cacheBustCode) {
		String compiledContents = this.contents;				
		// Replace the script blocks
		for (ScriptBlock block : this.scriptBlocks) {
			String compiledBlock = createScriptElement(block.getCompiledFileName(), cacheBustCode);
			compiledContents = compiledContents.replace(block.toString(), 
					compiledBlock);
		}
		
		// Replace the remaining un-blocked scripts
		ArrayList<String> scriptsToCompile = new ArrayList<String>();
		ArraySet<String> scriptTags = new ArraySet(WebUtils2.extractXmlTags("script", compiledContents, true));
		for (String stag : scriptTags) {
			assert compiledContents.contains(stag);
		}
		for(int i=0; i<scriptTags.size(); i++) {
			String stag = scriptTags.get(i);
			String src = WebUtils.getAttribute("src", stag);
			// Is it a script-in-the-page tag?
			if (src==null) continue;
			// HACK: Remove parameters -- namely our timestamp cache-buster hack
			src = WebUtils2.removeQuery(src);
			
			// Don't try to compile external JS!
			if (src.startsWith("//") || src.startsWith("http://") || src.startsWith("https://")) {
				continue;
			}		
			if (src.contains(".min.js")) {
				continue;
			}			
			if (src.contains("socket.io.js")) {
				continue;
			}
			
			scriptsToCompile.add(src);
			assert ! src.contains(";");
			
			String compiledScriptElement = createScriptElement(src.replace(".js", ".min.js"), null);
			// Replace it in the html			
			assert compiledContents.contains(stag);
			String _compiledContents = compiledContents.replace(stag, compiledScriptElement);
			assert ! _compiledContents.equals(compiledContents);
			compiledContents = _compiledContents;			
			for(int j=i+1; j<scriptTags.size(); j++) {
				String stagj = scriptTags.get(j);
				assert compiledContents.contains(stagj);
			}
		}
		// save to file
		FileUtils.write(file, compiledContents);
		
		return scriptsToCompile;
	}
}
