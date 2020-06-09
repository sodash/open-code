package com.winterwell.maths.chart;

import java.util.List;

import no.uib.cipr.matrix.Matrix;
import no.uib.cipr.matrix.Vector;

public class MatrixChart extends Chart {

	List<String> colLabels;

	Scaling colorScaling = Scaling.EXP;

	Matrix matrix;
	List<String> rowLabels;

	public MatrixChart() {
	}

	public MatrixChart(String name) {
		super(name);
	}

	public void setColLabels(List<String> labels) {
		this.colLabels = labels;
	}

	public void setColorScaling(Scaling colorScaling) {
		this.colorScaling = colorScaling;
	}

	@Override
	public void setData(Iterable<? extends Vector> points)
			throws UnsupportedOperationException {
		throw new UnsupportedOperationException();
	}

	public void setData(Matrix matrix) {
		this.matrix = matrix;

	}

	public void setRowLabels(List<String> labels) {
		this.rowLabels = labels;
	}

}
