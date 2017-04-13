package com.winterwell.nlp;

import junit.framework.TestCase;

public class TrieTest extends TestCase {

	public void testExists() {
		Trie trie = new Trie();
		assert !trie.exists("abc");
		assert !trie.exists("ab");
		assert !trie.exists("");
		assert !trie.exists("Foo Bar");

		trie.add("abc");
		trie.add("ab");
		trie.add("abcd");
		trie.add("Foo Bar");

		assert trie.exists("abc");
		assert trie.exists("ab");
		assert trie.exists("abcd");
		assert trie.exists("Foo Bar");

		assert !trie.exists("");
		trie.add("");
		assert trie.exists("");
	}

}
