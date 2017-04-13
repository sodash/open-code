package com.winterwell.nlp.tablesense;

import com.winterwell.maths.stats.distributions.d1.IDistribution1D;
import com.winterwell.maths.stats.distributions.discrete.ObjectDistribution;

@Aspect
public class PTable  {

	int maxColumns = 20;
	int maxRows = 20;
	
	IDistribution1D numColumns;
	IDistribution1D numRows;
	
	ObjectDistribution<PRow> rows;
	ObjectDistribution<PColumn> columns;
	
	public SemanticTable getMostLikely() {
		SemanticTable st = new SemanticTable();		
		st.setRows(rows.getSortedObjects());
		return st;
	}

}
