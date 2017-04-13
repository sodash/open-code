package com.winterwell.maths.classifiers;

import java.util.ArrayList;
import java.util.List;

import com.winterwell.maths.stats.distributions.ATrainableBase;

import no.uib.cipr.matrix.Vector;

public abstract class AClassifier<Tag> extends ATrainableBase<Vector, Tag>
		implements IClassifier<Tag> {

	@Override
	public List<Tag> classifySeqn(List<? extends Vector> seqn) {
		List<Tag> tags = new ArrayList<Tag>(seqn.size());
		for (Vector xi : seqn) {
			Tag ti = classify(xi);
			tags.add(ti);
		}
		return tags;
	}

}
