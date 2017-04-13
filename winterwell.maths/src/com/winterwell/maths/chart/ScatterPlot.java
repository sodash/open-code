package com.winterwell.maths.chart;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.winterwell.maths.datastorage.DataSet;
import com.winterwell.maths.datastorage.IDataSet;
import com.winterwell.maths.matrix.EVDAdapter;
import com.winterwell.maths.matrix.Eigenpair;
import com.winterwell.maths.matrix.RowPackedMatrix;
import com.winterwell.maths.stats.StatsUtils;
import com.winterwell.maths.timeseries.DataUtils;
import com.winterwell.maths.timeseries.ExtraDimensionsDataStream;
import com.winterwell.maths.timeseries.ExtraDimensionsDataStream.KMatchPolicy;
import com.winterwell.maths.timeseries.IDataStream;
import com.winterwell.maths.timeseries.ListDataStream;
import com.winterwell.utils.Printer;
import com.winterwell.utils.containers.Containers;

import no.uib.cipr.matrix.Matrix;
import no.uib.cipr.matrix.Vector;

/**
 * A scatter plot. And factory methods for generating them
 * 
 * @author daniel
 * @testedby ScatterPlotTest
 */
public class ScatterPlot extends XYChart {

	
	/**
	 * @param data
	 * @return rainbow-colored, one chart per label (including null-labelled
	 *         data)
	 */
	public static CombinationChart multiColor(IDataStream data) {
		return multiColor(data, null);
	}
	
	public static CombinationChart multiColor(IDataStream data, Rainbow rainbow) {
		assert data.getDim() == 2;
		Map<Object, ListDataStream> labelled = DataUtils.sortByLabel(data);
		assert labelled.size() != 0 : data;
		CombinationChart cc = new CombinationChart();
		List<Object> labels = new ArrayList(labelled.keySet());
		// Make the colouring predictable
		Collections.sort(labels, Containers.comparator());
		if (rainbow==null) {
			rainbow = new Rainbow(labels);
		}
		int i = 0;		
		for (Object label : labels) {
			ScatterPlot plot = new ScatterPlot();
			plot.setTitle(Printer.toString(label));
			plot.setData(labelled.get(label));
			Color col = rainbow.getKeys()==null? rainbow.getColor(i) : rainbow.getColor(label);
			plot.setColor(col);
			plot.setJitter(0.01);
			i++;
			cc.add(plot);
			cc.getLabelToChartMap().put(label, plot);
		}
		cc.setShowLegend(true);
		return cc;
	}

	/**
	 * Generate a multicolor scatter plot for each pair of columns in the
	 * dataset.
	 * 
	 * @param dataset
	 */
	public static CombinationChart multiPlot(IDataSet dataset, int col1,
			int col2) {
		List<String> cols = dataset.getColumnNames();
		String name1 = cols.get(col1);
		String name2 = cols.get(col2);
		IDataStream d1 = dataset.getDataStream1D(name1);
		IDataStream d2 = dataset.getDataStream1D(name2);
		ExtraDimensionsDataStream d12 = new ExtraDimensionsDataStream(
				KMatchPolicy.DISCARD_ON_MISMATCH, d1, d2);
		CombinationChart chart = multiColor(d12);
		// TODO chart.getAxis(0).setName(name1);
		// TODO chart.getAxis(1).setName(name2);
		chart.setTitle("x:" + name1 + " v y:" + name2);
		return chart;
	}

	/**
	 * Pick the top 2 principal components and create a scatter plot using them.
	 * 
	 * @param data
	 * @return
	 */
	public static CombinationChart withPCA(IDataStream data) {
		ListDataStream ldata = data.list();
		DataSet dataset = new DataSet(ldata.clone());

		Matrix covar = StatsUtils.covar(dataset,
				KMatchPolicy.DISCARD_ON_MISMATCH, null);

		EVDAdapter ef = new EVDAdapter();
		ef.setMaxEigenvectors(2);
		List<Eigenpair> eigenPairs = ef.getEigenpairs(covar);
		Matrix eigenTransform = new RowPackedMatrix(eigenPairs);
		IDataStream eigenData = DataUtils.applyMatrix(eigenTransform,
				ldata.clone());
		eigenData = eigenData.list();

		CombinationChart cc = multiColor(eigenData);
		return cc;
	}

	public ScatterPlot() {
		setType(ChartType.SCATTER);
	}

	public ScatterPlot(Iterable<? extends Vector> data) {
		this();
		setData(data);
	}


}
