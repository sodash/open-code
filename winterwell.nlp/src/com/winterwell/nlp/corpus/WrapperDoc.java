//package com.winterwell.nlp.corpus;
//
//import com.winterwell.maths.stats.distributions.cond.ISitnStream;
//import com.winterwell.nlp.io.ITokenStream;
//import com.winterwell.utils.IProperties;
//import com.winterwell.utils.containers.Slice;
//import com.winterwell.web.data.IDoCanonical;
//
///**
// * TBH this class is a hack for putting not-quite-documents through an IDocument pipeline.
// * Use it with a special case tokeniser that handles the
// * 
// * @author daniel
// *
// */
//public class WrapperDoc implements IDocument {
//
//	public String lang;
//	public String author;
//
//	@Override
//	public String getLang() {
//		return lang;
//	}
//
//	@Override
//	public String getAuthor() {
//		return author;
//	}
//
//	@Override
//	public String getContents() {
//		// TODO Auto-generated method stub
//		return null;
//	}
//
//	@Override
//	public Slice getFocalRegion() {
//		// TODO Auto-generated method stub
//		return null;
//	}
//
//	@Override
//	public IProperties getMetaData() {
//		// TODO Auto-generated method stub
//		return null;
//	}
//
//	@Override
//	public String getName() {
//		// TODO Auto-generated method stub
//		return null;
//	}
//
//	@Override
//	public ITokenStream getTokenStream() throws UnsupportedOperationException {
//		// TODO Auto-generated method stub
//		return null;
//	}
//}
