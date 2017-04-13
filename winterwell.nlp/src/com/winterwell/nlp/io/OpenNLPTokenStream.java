///**
// * 
// */
//package com.winterwell.nlp.io;
//
//import opennlp.tools.tokenize.Tokenizer;
//import opennlp.tools.util.Span;
//import com.winterwell.utils.containers.AbstractIterator;
//
///**
// * Wrap an OpenNLP tokenizer as a TokenStream
// * 
// * @author Joe Halliwell <joe@winterwell.com>
// * 
// */
//public class OpenNLPTokenStream extends ATokenStream {
//
//	String input;
//	Span[] spans;
//	Tokenizer tokenizer;
//
//	public OpenNLPTokenStream(Tokenizer tokenizer, String input) {
//		this.tokenizer = tokenizer;
//		this.spans = tokenizer.tokenizePos(input);
//		this.input = input;
//	}
//
//	/*
//	 * (non-Javadoc)
//	 * 
//	 * @see winterwell.nlp.io.ATokenStream#instantiate(java.lang.String)
//	 */
//	@Override
//	public ITokenStream factory(String input) {
//		return new OpenNLPTokenStream(this.tokenizer, input);
//	}
//
//	
//	@Override
//	public AbstractIterator<Tkn> iterator() {		
//		return new AbstractIterator<Tkn>() {
//			int position = -1;
//			@Override
//			protected Tkn next2() {
//				position++;
//				if (position == spans.length)
//					return null;
//				CharSequence original = spans[position].getCoveredText(input);
//				return new Tkn(original, spans[position].getStart(),
//						spans[position].getEnd());
//			}
//		};
//	}
//
//}
