package com.winterwell.nlp.corpus.brown;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.util.Collection;
import java.util.Iterator;

import com.winterwell.nlp.corpus.ICorpus;
import com.winterwell.nlp.corpus.IDocument;
import com.winterwell.utils.TodoException;
import com.winterwell.utils.Utils;
import com.winterwell.utils.io.FileUtils;

/**
 * 
 * @testedby BrownCorpusTest
 * @author Daniel
 * 
 */
public class BrownCorpus implements ICorpus {

	File dir = new File(FileUtils.getWinterwellDir(),
			"code/winterwell.nlp/data/corpora/brown");

	// NLPWorkshop.get().getFilePointer("brown");

	public BrownCorpus() {
		if (dir.isDirectory()) return;
		File zip = new File(dir.getParentFile(), "brown.zip");
		if (zip.isFile()) {
			// TODO unzip
		}
		throw Utils.runtime(new FileNotFoundException("Cant find Brown corpus in: "+dir));		
	}

	@Override
	public Iterable<? extends IDocument> getDocumentsByTitle(String title) {
		// TODO Auto-generated method stub
		throw new TodoException();
	}

	@Override
	public Iterable<? extends IDocument> getDocumentsUsing(String term) {
		// TODO Auto-generated method stub
		throw new TodoException();
	}

	@Override
	public Collection<IDocument> getSample(int num) {
		// TODO Auto-generated method stub
		throw new TodoException();
	}

	@Override
	public Iterator<IDocument> iterator() {
		assert dir.isDirectory() : dir.getAbsolutePath();
		final File[] files = dir.listFiles(new FilenameFilter() {
			@Override
			public boolean accept(File dir, String name) {
				return name.length() == 4;
			}
		});
		assert files != null;
		Iterator<IDocument> it = new Iterator<IDocument>() {
			int i = 0;

			@Override
			public boolean hasNext() {
				return i < files.length;
			}

			@Override
			public BrownDocument next() {
				i++;
				return new BrownDocument(files[i - 1]);
			}

			@Override
			public void remove() {
				throw new UnsupportedOperationException();
			}
		};
		return it;
	}

	@Override
	public int size() {
		// ??
		return dir.list().length - 4;
	}

}
