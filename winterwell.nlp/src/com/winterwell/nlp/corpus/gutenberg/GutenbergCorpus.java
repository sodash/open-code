package com.winterwell.nlp.corpus.gutenberg;

import java.io.File;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import com.winterwell.nlp.corpus.ICorpus;
import com.winterwell.nlp.corpus.IDocument;
import com.winterwell.utils.TodoException;
import com.winterwell.utils.io.FileUtils;

/**
 * 
 * @author daniel
 * @testedby {@link GutenbergCorpusTest}
 */
public class GutenbergCorpus implements ICorpus {

	final File dir;

	public GutenbergCorpus() {
		this(new File(FileUtils.getWinterwellDir(),
				"code/winterwell.nlp/data/corpora/gutenberg"));
	}

	public GutenbergCorpus(File dir) {
		this.dir = dir;
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
		List<File> files = FileUtils.find(dir, ".*\\.txt");
		final Iterator<File> fit = files.iterator();
		Iterator<IDocument> it = new Iterator<IDocument>() {
			@Override
			public boolean hasNext() {
				return fit.hasNext();
			}

			@Override
			public GutenbergDocument next() {
				return new GutenbergDocument(fit.next());
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
		List<File> files = FileUtils.find(dir, ".*\\.txt");
		return files.size();
	}

}
