package com.winterwell.nlp.tablesense;

import java.util.List;

public interface IModelTables {

	void setPrior(PTable model);
	
	/**
	 * 
	 * @param cells E.g. blindly accepting the bounding-boxes
	 * and text from an OCR pass.
	 * @return
	 */
	PTable process(List<PCell> cells);
}
