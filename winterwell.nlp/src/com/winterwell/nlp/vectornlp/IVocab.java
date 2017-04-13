package com.winterwell.nlp.vectornlp;

/**
 * Which words to we recognise? JH: Does this *interface* represent the concept
 * of having a vocabulary? e.g. implemented by WrdCoMat and DocVec?
 * 
 * @author Daniel
 */
public interface IVocab {

	boolean contains(String word);

	int dim(String word);

}
