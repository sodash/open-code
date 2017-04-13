package com.winterwell.nlp.tablesense;

import com.winterwell.maths.stats.distributions.d1.IDistribution1D;
import com.winterwell.maths.stats.distributions.discrete.IFiniteDistribution;

@Aspect
public class PCell {

	IFiniteDistribution<String> text;
	IFiniteDistribution<PCategory> category;
	
	IDistribution1D row;
	IDistribution1D column;
	
	IDistribution1D top;
	IDistribution1D left;
	IDistribution1D width;
	IDistribution1D height;
	
}
