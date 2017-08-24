package com.winterwell.nlp.corpus.brown;

import java.io.File;

import com.winterwell.nlp.corpus.IDocument;
import com.winterwell.nlp.io.ITokenStream;
import com.winterwell.nlp.io.ListTokenStream;
import com.winterwell.nlp.io.Tkn;
import com.winterwell.utils.IProperties;
import com.winterwell.utils.StrUtils;
import com.winterwell.utils.containers.Slice;
import com.winterwell.utils.io.FileUtils;

/**
 * Whitespace separated, word/tag format
 * 
 * @author daniel
 * 
 */
public class BrownDocument implements IDocument {

	private String rawTxt;
	/**
	 * shorten to compress the brown tags (since the least significant info is
	 * at the end)
	 */
	int simplify = 10;

	@Override
	public String getLang() {
		return "en";
	}
	public BrownDocument(File file) {
		rawTxt = FileUtils.read(file);
	}

	public BrownDocument(String rawText) {
		this.rawTxt = rawText;
	}

	@Override
	public String getAuthor() {
		return null;
	}
	
	@Override
	public String getTitleAndContents() {
		return StrUtils.joinWithSkip("\n\n", getName(), getContents());
	}

	/**
	 * The text, minus the brown POS tags
	 */
	@Override
	public String getContents() {
		// strip out brown tags
		String txt = rawTxt.replaceAll("/\\S+", "");
		return txt;
	}

	@Override
	public Slice getFocalRegion() {
		return null;
	}

	@Override
	public IProperties getMetaData() {
		return null;
	}

	@Override
	public String getName() {
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * For information on Brown POS tags, see
	 * 
	 * @return
	 */
	@Override
	public ITokenStream getTokenStream() {
		String[] rawTokens = rawTxt.split("\\s+");
		ListTokenStream list = new ListTokenStream(rawTokens.length);
		for (String rt : rawTokens) {
			if (rt.length() == 0) {
				continue;
			}
			int i = rt.lastIndexOf('/');
			assert i != -1 : rt;
			String w = rt.substring(0, i);
			String brownPos = StrUtils.substring(rt, i + 1, i + 1 + simplify);
			// skip punctuation?
			if (swallowPunctuation && BrownCorpusTags.isPunctuation(brownPos)) {
				if (BrownCorpusTags.SENTENCE.equals(brownPos)) {
					list.add(Tkn.START_TOKEN);
				}
				continue;
			}
			if (lowercase) w = w.toLowerCase();
			Tkn token = new Tkn(w);
			token.setPOS(brownPos);
			list.add(token);
		}
		return list;
	}
	
	public BrownDocument setSwallowPunctuation(boolean swallowPunctuation) {
		this.swallowPunctuation = swallowPunctuation;
		return this;
	}
	
	/**
	 * If true, punctuation tokens will not be emitted.
	 */
	boolean swallowPunctuation;
	private boolean lowercase;

	/**
	 * How many chars of brown POS tag to keep
	 * 
	 * @param simplify
	 */
	public void setSimplify(int simplify) {
		this.simplify = simplify;
	}

	@Override
	public String toString() {
		return StrUtils.ellipsize("# " + getName() + "\n" + getContents(), 500);
	}
	public void setLowerCase(boolean b) {
		lowercase = b;
	}

}
