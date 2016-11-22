package winterwell.utils.web;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import winterwell.utils.containers.ArraySet;


/**
 * An immutable representation of a SoDash script block. Provides the name and scripts
 * contained by the block.
 * 
 * A script block is marked by start/end comments with "hidden" compile tags, like this:
 * <pre><code>
	&lt;!--[&lt;compile to="/static/code/base-all.min.js"&gt;]--&gt;
		&lt;script src="/static/code/leaflet.js"&gt;&lt;/script&gt;
		&lt;script src="/static/code/underscore/underscore.js?_=TIMESTAMP"&gt;&lt;/script&gt;
	&lt;!--[&lt;/compile&gt;]--&gt;
 * </code></pre>
 * 
 * TODO A better scheme might be to add an attribute into the script tags.
 * E.g. &lt;script src='myfile.js' data-compile-to='target-file'&gt; 
 * 
 * @author Steven King <steven@winterwell.com>
 * @testedby {@link ScriptBlockTest}
 */
public final class ScriptBlock {
	private static final Pattern BLOCK = Pattern.compile("\\Q<!--[<compile to=\"\\E(\\S+)\\Q\">]-->\\E.*?\\Q<!--[</compile>]-->\\E", Pattern.DOTALL);

//	static final Pattern SCRIPT = Pattern.compile("\\Q<script\\E.*?\\Qsrc=[\"']\\E(\\S+?\\.js)\\S*?\\Q[\"']\\E.*?\\Q></script>\\E");
	
	private final String block;
	private final String compiledFileName;
	private final List<String> scripts;
	
	/**
	 * 
	 * @param block The script block that is to be represented. Must be of the
	 * form:
	 * 
	 * 		<!--[<compile to="compiled-file">]-->
	 * 		<!-- Block content - the scripts that make-up this block. -->
	 * 		<!--[</compile>]-->
	 * 
	 * The name may contain word-characters [a-zA-Z_0-9], plus "-". The block
	 * content should be scripts, as standard html <script> elements.
	 */
	public ScriptBlock(String block) {
		if (block == null) {
			throw new NullPointerException("block");
		}
		
		this.block = block;
		
		Matcher nameMatcher = ScriptBlock.BLOCK.matcher(block);
		
		if (nameMatcher.find()) {
			this.compiledFileName = nameMatcher.group(1);
		} else {
			throw new IllegalArgumentException("block");
		}
		
		scripts = new ArrayList<String>();
		// wrap in set to avoid any dupes -- which our IE7-safe conditional AutoCompleteWidget include leads to
		ArraySet<String> scriptTags = new ArraySet(WebUtils2.extractXmlTags("script", block, true));
		for (String stag : scriptTags) {
			String src = WebUtils2.getAttribute("src", stag);
			assert src != null;
			// HACK: Remove parameters -- namely our timestamp cache-buster hack
			src = WebUtils2.removeQuery(src);
			scripts.add(src);
		}
	}
	
	static ScriptBlock[] createScriptBlocks(String text) {
		if (text == null) {
			throw new NullPointerException("text");
		}
		
		ArrayList<ScriptBlock> scriptBlockList = new ArrayList<ScriptBlock>();
		
		Matcher blockMatcher = ScriptBlock.BLOCK.matcher(text);
		
		while (blockMatcher.find()) {
			scriptBlockList.add(new ScriptBlock(blockMatcher.group()));
		}
		
		if (scriptBlockList.size() > 0) {
			return scriptBlockList.toArray(new ScriptBlock[scriptBlockList.size()]);
		} else {
			return new ScriptBlock[0]; 
		}
	}
	
	/**
	 * @return File-path to compile this block to. Can be relative to {something}!
	 */
	public String getCompiledFileName() {
		return this.compiledFileName;
	}
	
	public List<String> getScripts() {
		return this.scripts;
	}
	
	/**
	 * @return the raw block
	 */
	@Override
	public String toString() {
		return this.block;
	}
}
