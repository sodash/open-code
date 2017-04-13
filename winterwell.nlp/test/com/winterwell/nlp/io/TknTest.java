package com.winterwell.nlp.io;

import org.junit.Test;

import com.winterwell.utils.Key;

public class TknTest {

	@Test
	public void test() {
		Tkn test1 = new Tkn("a test string", 0, 6);
		Tkn test2 = new Tkn("a test string", 1, 5);
		Tkn test3 = new Tkn("a test string", 0, 5);
		assert test1.equals(test2) : "These should be equal";
		assert test1.hashCode() == test2.hashCode() : "Hashcodes are unequal";
		test1.put(new Key<String>("Prop1"), "Val1");
		test2.put(new Key<String>("Prop1"), "Val1");
		assert test1.equals(test2) : "Identical props - should be equal";
		assert test1.hashCode() == test2.hashCode() : "Identical strings & props but hashcodes are unequal";
		test3.put(new Key<String>("Prop2"), "Val2");
		assert !test1.equals(test3) : "Different props - should be unequal";
		assert test1.hashCode() != test3.hashCode() : "Different props but hashcodes are equal";
	}

}
