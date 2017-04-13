package com.winterwell.maths.stats;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.winterwell.utils.containers.Containers;

/**
 * Support for using K-Fold cross validation on an ADataStream.
 * 
 * @param <X> the type of a data record
 * @author Daniel
 */
public class KFold<X> {

	private final List<List<X>> bits;

	/**
	 * This slices randomly - which it does by loading and shuffling ALL the
	 * data.
	 * 
	 * @param base
	 *            If base is a list, it will get shuffled directly!
	 */
	public KFold(int k, Iterable<X> base) {
		// Load all into memory - kind of unavoidable.
		// -- I suppose we could have a stochastic streaming version, if really
		// needed.
		List<X> data = Containers.getList(base);
		Collections.shuffle(data);
		assert data.size() >= k : data.size() + " < " + k;
		bits = Containers.chop(data, k);
	}

	public List<X> getTestData(int i) {
		return bits.get(i);
	}

	public List<X> getTrainingData(int i) {
		List<X> streams = new ArrayList<X>();
		for (int j = 0; j < bits.size(); j++) {
			if (j == i) {
				continue;
			}
			streams.addAll(bits.get(j));
		}
		return streams;
	}

	public final int k() {
		return bits.size();
	}

}
