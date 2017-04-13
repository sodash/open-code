package com.winterwell.nlp.classifier;

import org.junit.Test;

import com.winterwell.maths.matrix.ObjectMatrix;
import com.winterwell.utils.web.WebUtils;
import com.winterwell.web.WebPage;

public class ObjectMatrixWidgetTest {

	@Test
	public void testAppendHtmlTo() {
		WebPage page = new WebPage();
		ObjectMatrix matrix = new ObjectMatrix();
		matrix.plus("R1", "C1", 2);
		matrix.plus("R2", "C2", 4);
		matrix.plus("R1", "C2", 3);
		matrix.plus("R1", "C3", 4);
		
		ObjectMatrixWidget widget = new ObjectMatrixWidget(matrix);
		widget.appendHtmlTo(page);
		
		WebUtils.display(page.toString());
	}

}
