/**
 * 
 */
package com.winterwell.utils.web;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.winterwell.utils.ReflectionUtils;
import com.winterwell.utils.containers.ArraySet;
import com.winterwell.utils.containers.Slice;

/**
 * Utility methods for working with #tags and @you tags.
 * 
 * @author daniel
 * @testedby {@link HashTaggerTest}
 */
public class HashTagger {
	
	private String text;

	public HashTagger(String tagFrom) {
		text = tagFrom;
		assert tagFrom!=null;
	}

	public static final Pattern HASHTAG = 
			ReflectionUtils.getJavaVersion() >= 1.7?
			Pattern.compile("(?<!\\p{IsAlphabetic})#([\\w\\-\\.]*\\w+)")
			: Pattern.compile("(?<!\\w)#([\\w\\-\\.]*\\w+)");
	
	/**
	 * Use group 1 to get the name *without* the @.
	 * You may wish to post-filter.
	 *  This is more lenient than Twitter: it allows @ in a name, so it will recognise "@alice@sodash.com"<br>
	 *  The minimum name length is 2 letters. Names can start with a number, 'cos Twitter allow that.
	 */
	public static final Pattern atYouSir = 
			ReflectionUtils.getJavaVersion() >= 1.7?
			Pattern.compile("(?<!\\p{IsAlphabetic})@([\\w\\-@\\.]*\\w+)")
			: Pattern.compile("(?<!\\w)@(\\w[\\w\\-@\\.]*\\w+)");
	

	/**
	 * @return never null, can be empty. No duplicates. Lower-cased if {@link #setLowerCase(boolean)} is set -- as-is by default.
	 */
	public List<String> getTags() {
		Matcher m = HASHTAG.matcher(text);
		ArraySet<String> tags = new ArraySet();
		while(m.find()) {
			String tag = m.group(1);
			if (lowercase) tag = tag.toLowerCase();
			tags.add(tag);
		}
		return tags.asList();
	}

	boolean lowercase;
	
	/**
	 * If true, lowercase the @you and #tag items that are found.
	 * @param lowerCase false by default.
	 */
	public void setLowerCase(boolean lowerCase) {
		this.lowercase = lowerCase;
	}
	
	/**
	 * @return never null, can be empty
	 */
	public List<String> getMentions() {
		Matcher m = atYouSir.matcher(text);
		List<String> tags = new ArrayList();
		while(m.find()) {
			String tag = m.group(1);
			if (lowercase) tag = tag.toLowerCase();
			tags.add(tag);
		}
		return tags;
	}

	public List<Slice> getMentionSlices() {
		Matcher m = atYouSir.matcher(text);
		List<Slice> tags = new ArrayList();
		while(m.find()) {
			String tag = m.group(1);
			if (lowercase) tag = tag.toLowerCase();
			tags.add(new Slice(text, m.start(1), m.end(1)));
		}
		return tags;
	}
	
}
