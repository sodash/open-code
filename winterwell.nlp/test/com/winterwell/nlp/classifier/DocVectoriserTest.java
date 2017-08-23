package com.winterwell.nlp.classifier;

import org.junit.Test;

import com.winterwell.maths.datastorage.Index;
import com.winterwell.maths.datastorage.Vectoriser;
import com.winterwell.maths.timeseries.DataUtils;
import com.winterwell.nlp.corpus.SimpleDocument;
import com.winterwell.nlp.io.WordAndPunctuationTokeniser;

import no.uib.cipr.matrix.Vector;
import no.uib.cipr.matrix.sparse.SparseVector;

public class DocVectoriserTest {

	@Test
	public void testSimple() {
		Index<String> index = new Index<String>();
		DocVectoriser dv = new DocVectoriser(new WordAndPunctuationTokeniser(),
				index, Vectoriser.KUnknownWordPolicy.Add);

		SimpleDocument cats = new SimpleDocument("cats are cute");
		SimpleDocument porn = new SimpleDocument("girls are cute");

		Vector catsVec = dv.toVector(cats);
		Vector pornVec = dv.toVector(porn);

		int catsI = index.indexOf("cats");
		assert catsI == 0 : index;

		SparseVector cv = new SparseVector(Integer.MAX_VALUE);
		cv.set(0, 1);
		cv.set(1, 1);
		cv.set(2, 1);
		assert DataUtils.equalish(catsVec, cv) : catsVec;

		SparseVector pv = new SparseVector(Integer.MAX_VALUE);
		int girlsI = index.indexOf("girls");
		pv.set(girlsI, 1);
		pv.set(1, 1);
		pv.set(2, 1);
		assert DataUtils.equalish(pornVec, pv) : pornVec;
	}
}
