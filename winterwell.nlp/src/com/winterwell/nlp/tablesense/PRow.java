package com.winterwell.nlp.tablesense;

import com.winterwell.maths.stats.distributions.d1.IDistribution1D;
import com.winterwell.maths.stats.distributions.discrete.IFiniteDistribution;

public class PRow {

	IDistribution1D top;
	IDistribution1D height;

	IFiniteDistribution<PCategory> category;
	
}
