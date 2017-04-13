package com.winterwell.maths.datastorage;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;

import com.winterwell.maths.timeseries.DataUtils;
import com.winterwell.maths.timeseries.Datum;
import com.winterwell.maths.timeseries.IDataStream;
import com.winterwell.maths.timeseries.ListDataStream;
import com.winterwell.maths.vector.Cuboid;
import com.winterwell.utils.TodoException;
import com.winterwell.utils.containers.Pair;
import com.winterwell.utils.log.Log;
import com.winterwell.utils.log.WeirdException;

import gnu.trove.TIntArrayList;
import no.uib.cipr.matrix.Vector;
import no.uib.cipr.matrix.VectorEntry;
import no.uib.cipr.matrix.sparse.SparseVector;

/**
 * Convert sequences - e.g. lists of words - into vectors.
 * 
 * Zero policy mode isn't very extensively tested. Be careful if working with
 * indexes directly.
 * 
 * @author daniel
 * @author Joe Halliwell <joe@winterwell.com>
 * 
 * @param <Document>
 *            the block in which input comes
 * @param <Word>
 *            the input gets broken into a sequence of Words
 */
public class Vectoriser<Word, Document> {

	/**
	 * How should words that are not in the intial index be handled?
	 */
	public static enum KUnknownWordPolicy {
		/**
		 * Unknown words are added to the index. NB This means the vector model
		 * is very high dimensional...
		 */
		Add,
		/**
		 * Unknown words cause an exception
		 */
		Forbid,
		/**
		 * Unknown words are ignored
		 */
		Ignore,
		/**
		 * Unknown words go into index slot zero
		 */
		Zero
	};

	public static KUnknownWordPolicy DEFAULT_POLICY = KUnknownWordPolicy.Forbid;
	final IIndex<Word> index;

	KUnknownWordPolicy policy;

	// JH: template retained, because it seems like that
	// might be a useful thing in future.
	Vector template = new SparseVector(Integer.MAX_VALUE);

	public Vectoriser(IIndex<Word> index) {
		this(index, DEFAULT_POLICY);
	}

	public Vectoriser(IIndex<Word> index, KUnknownWordPolicy policy) {
		this.index = index;
		this.policy = policy;

		// Figure out the dimension of the template
		int dimension = index.size();
		if (policy == KUnknownWordPolicy.Add) {
			dimension = Integer.MAX_VALUE;
		}
		if (policy == KUnknownWordPolicy.Zero) {
			dimension++;
		}

		this.template = new SparseVector(dimension);
	}

	/**
	 * WARNING! In "Zero" policy mode, the indexes provided by e.g. #toIndexList
	 * are shifted (by one) relative to this index
	 * 
	 * @return
	 */
	public IIndex<Word> getIndex() {
		return index;
	}

	public KUnknownWordPolicy getPolicy() {
		return policy;
	}

	/**
	 * Vectorisation is usually a one-way operation. This is really only for
	 * debugging / analysis.
	 * 
	 * @param x
	 * @return
	 */
	public List<Word> inverse(Vector x) {
		Iterator<VectorEntry> it = x.iterator();
		List<Word> list = new ArrayList<Word>();
		while (it.hasNext()) {
			VectorEntry ve = it.next();
			Word xi = index.get(ve.index());
			list.add(xi);
		}
		return list;
	}

	public List<Word> inverseIndexList(TIntArrayList vs) {
		List<Word> list = new ArrayList<Word>();
		int[] avs = vs.toNativeArray();
		for (int i : avs) {
			Word v = index.get(i);
			list.add(v);
		}
		return list;
	}

	/**
	 * Inverse to {@link #toIndexStream(Iterable)}
	 * 
	 * @param indexValues
	 * @return
	 */
	public Iterable<Word> inverseIndexStream(IDataStream indexValues) {
		assert indexValues.is1D() : indexValues;
		List<Word> list = new ArrayList<Word>();
		for (Datum datum : indexValues) {
			int i = (int) Math.round(datum.x());
			Word v = index.get(i);
			list.add(v);
		}
		return list;
	}

	/**
	 * @return true if unknown words will not cause problems for vector indices.
	 *         I.e. unknowns are either added, ignored or code to >= 0.
	 */
	public boolean isUnknownOK() {
		return policy != KUnknownWordPolicy.Forbid;
	}

	protected Iterable<Word> toBitStream(Document doc) {
		throw new UnsupportedOperationException();
	}

	/**
	 * Convert an object into a region of space.
	 * 
	 * @param object
	 * @return
	 */
	public Cuboid toCuboid(Document object) {
		throw new TodoException();
	}

	public Pair<Vector> toCuboid(Iterable<Word> seqn) {
		Vector topLeft = toVector(seqn);
		Pair<Vector> box = DataUtils.getBoxAround(topLeft, 0.5);
		return box;
	}

	private int toIndex(Word bit) {
		int i = index.indexOf(bit);

		// If policy Zero shift everything up a slot
		if (policy == KUnknownWordPolicy.Zero) {
			i += 1;
		}

		if (i != IIndex.UNKNOWN)
			return i;

		switch (policy) {
		case Add:
			return index.add(bit);
		case Forbid:
			throw new IllegalArgumentException("Unknown word: " + bit);
		case Zero:
			return 0;
		case Ignore:
			Log.report("Ignoring unknown word: " + bit, Level.FINEST);
			return -1;
		default:
			throw new WeirdException("Can't happen");
		}
	}

	/**
	 * Break the object into bits, then convert them to index-numbers. E.g. turn
	 * a sentence into a list of numbers.
	 * 
	 * @param doc
	 * @return
	 */
	public TIntArrayList toIndexList(Document doc) {
		return toIndexList(toBitStream(doc));
	}

	/**
	 * Creates a list index-values. I.e. it uses the index to convert the
	 * sequence Bits into integers. Like {@link #toIndexStream(Iterable)} but
	 * with less object wrapper overhead.
	 * 
	 * @param seqn
	 * @return list of index values
	 */
	public TIntArrayList toIndexList(Iterable<Word> seqn) {
		TIntArrayList indexValues = new TIntArrayList();
		for (Word bit : seqn) {
			int i = toIndex(bit);
			if (i == -1) {
				assert policy == KUnknownWordPolicy.Ignore;
				continue;
			}
			indexValues.add(i);
		}
		return indexValues;
	}

	/**
	 * Creates a 1D data stream of index-values. I.e. it uses the index to
	 * convert the sequence Bits into integers. The Datums carry ANCIENT time
	 * stamps.
	 * 
	 * @param seqn
	 * @return stream of index values
	 */
	public IDataStream toIndexStream(Iterable<Word> seqn) {
		List<Datum> indexValues = new ArrayList<Datum>();
		for (Word bit : seqn) {
			int i = toIndex(bit);
			// ignore non-indexed??
			if (i == -1) {
				assert policy == KUnknownWordPolicy.Ignore;
				continue;
			}
			indexValues.add(new Datum(i));
		}
		return new ListDataStream(indexValues);
	}

	/**
	 * Override this to define how Documents become Words
	 * 
	 * @param object
	 * @return
	 */
	public Vector toVector(Document object) {
		return toVector(toBitStream(object));
	}

	/**
	 * Count the occurrences of each indexed bit.
	 * 
	 * @param seqn
	 * @return
	 */
	public Vector toVector(Iterable<Word> seqn) {
		Vector vector = template.copy();
		for (Word x : seqn) {
			int i = toIndex(x);
			// ignore non-indexed??
			if (i == -1) {
				assert policy == KUnknownWordPolicy.Ignore;
				continue;
			}
			vector.add(i, 1);
		}
		return vector;
	}

}
