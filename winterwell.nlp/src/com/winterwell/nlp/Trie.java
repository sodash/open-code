/*
 * (c) Winterwell
 * December 2008
 */
package com.winterwell.nlp;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import com.winterwell.utils.TodoException;
import com.winterwell.utils.containers.AbstractMap2;

/**
 * A trie using a hashmap at each node. A fast set for Strings, also provides a
 * fast index for converting Strings to numbers (but the reverse lookup of
 * numbers to Strings is very inefficient). Note that comparison is case
 * sensitive.
 * 
 * TODO Should we have a lower-case latin version that uses arrays instead of
 * HashMaps? ?? Is having the tree carry arbitrary data (thus acting as a map) a
 * useful idea? Does it make the index property irrelevant? TODO speed trials
 * 
 * @param <V> Value-type which can be stored at nodes
 *  
 * @author Daniel Winterstein, Tiphaine Dalmas
 */
public final class Trie<V> extends AbstractMap2<String, V> {

	private final Trie2<V> guts = new Trie2();
//	private boolean indexed;

	public Trie() {
	}

	@Override
	public Set<String> keySet() {
		Set<String> keys = new HashSet();
		keySet2(keys, guts);
		return keys;
	}
	
	private void keySet2(Set<String> keys, Trie2<V> guts2) {
		if (guts2.endOfWord) {
			
		}
		throw new TodoException();
	}

	/**
	 * Add a string to the trie. Synchronized so that multiple threads can use
	 * this. TODO synchronize lower down in Trie2
	 * 
	 * @param str
	 */
	public synchronized void add(String str) {
//		indexed = false;
		guts.add(str);
	}

	public boolean exists(String str) {
		return guts.getNode2(str, 0) != null;
	}

	@Override
	public V get(Object key) {
		Trie2<V> node = guts.getNode2((String) key, 0);
		return node == null ? null : node.value;
	}
	
	/**
	 * @deprecated TODO
	 * @param key
	 * @return the keys which matched for the longest period
	 */
	public Collection<String> prefixMatch(String key) {
		Collection<String> ks = guts.prefixMatch(key, 0, 3);
		return ks==null? Collections.EMPTY_LIST : ks;
	}


//	/**
//	 * Warning: The index is arbitrary, and can change if words have been added.
//	 * 
//	 * @param str
//	 * @return the index of this string, or -1 if it does not exist in the Trie.
//	 * @see #isIndexed()
//	 */
//	public int getIndex(String str) {
//		if (!isIndexed()) {
//			updateIndex();
//		}
//		Trie2 elem = guts.getNode2(str, 0);
//		if (elem == null)
//			return -1;
//		return elem.index;
//	}

//	public String getString(int index) {
//		StringBuilder sb = new StringBuilder();
//		guts.getString2(sb, index);
//		return sb.toString();
//	}

//	public boolean isIndexed() {
//		return indexed;
//	}

	@Override
	public V put(String key, V value) {
		Trie2<V> node = guts.getNode2(key, 0);
		if (node == null) {
			add(key);
			node = guts.getNode2(key, 0);
		}
		V old = node.value;
		node.value = value;
		return old;
	}

//	public synchronized void updateIndex() {
//		guts.updateIndex2(0);
//		indexed = true;
//	}

}

final class Trie2<V> {

	boolean endOfWord;
//	int index;
	private final HashMap<Character, Trie2> node;
	V value;

	Trie2() {
		this.node = new HashMap<Character, Trie2>();
	}

	
	void add(String str) {
		if (str.length() == 0) {
			this.endOfWord = true;
			return;
		}
		char c = str.charAt(0);
		String left = str.substring(1, str.length());
		Trie2 child = this.node.get(c);
		if (child == null) {
			child = new Trie2();
			node.put(c, child);
		}
		child.add(left);
	}

	/**
	 * @param str
	 * @param posn
	 * @return node for end of word, or null
	 */
	Trie2<V> getNode2(String str, int posn) {
		if (str.length() == posn)
			return endOfWord ? this : null;
		char c = str.charAt(posn);
		Trie2 child = this.node.get(c);
		if (child == null)
			return null;
		else
			return child.getNode2(str, posn + 1);
	}
	
	Collection<String> prefixMatch(String str, int posn, int minMatch) {
		if (str.length() == posn)
			return posn<minMatch? null : prefixMatch2_flatten(str, posn);
		char c = str.charAt(posn);
		Trie2 child = this.node.get(c);
		if (child == null)
			return posn<minMatch? null : prefixMatch2_flatten(str, posn);
		else
			return child.prefixMatch(str, posn + 1, minMatch);
	}


	private Collection<String> prefixMatch2_flatten(String str, int posn) {
		ArrayList keys = new ArrayList();
		if (true) throw new TodoException();
		return keys;
	}


	void getString2(StringBuilder sb, int i) {
		throw new TodoException();
	}


}
