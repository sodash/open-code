package com.winterwell.nlp.simpleparser;

public class ParserExampleGenerator {

	private Parser parser;

	public ParserExampleGenerator(Parser p) {
		this.parser = p;
	}
	
	Object sample() {
		return parser.sample();
	}
}

