package com.winterwell.maths.timeseries;


import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.logging.Level;

import com.winterwell.json.JSONArray;
import com.winterwell.json.JSONObject;
import com.winterwell.maths.matrix.MatrixUtils;
import com.winterwell.maths.stats.KScore;
import com.winterwell.maths.stats.StatsUtils;
import com.winterwell.maths.stats.algorithms.IPredictor;
import com.winterwell.maths.stats.algorithms.ITimeSeriesFilter;
import com.winterwell.maths.stats.distributions.IDistribution;
import com.winterwell.maths.stats.distributions.d1.IDistribution1D;
import com.winterwell.maths.stats.distributions.discrete.ObjectDistribution;
import com.winterwell.maths.timeseries.ExtraDimensionsDataStream.KMatchPolicy;
import com.winterwell.maths.vector.VectorUtilsTest;
import com.winterwell.maths.vector.X;
import com.winterwell.maths.vector.XY;
import com.winterwell.maths.vector.XYZ;
import com.winterwell.utils.FailureException;
import com.winterwell.utils.MathUtils;
import com.winterwell.utils.Printer;
import com.winterwell.utils.StrUtils;
import com.winterwell.utils.TodoException;
import com.winterwell.utils.Utils;
import com.winterwell.utils.WrappedException;
import com.winterwell.utils.containers.AbstractIterator;
import com.winterwell.utils.containers.Pair;
import com.winterwell.utils.containers.TopNList;
import com.winterwell.utils.io.FileUtils;
import com.winterwell.utils.io.LineReader;
import com.winterwell.utils.log.Log;
import com.winterwell.utils.time.Dt;
import com.winterwell.utils.time.Time;
import com.winterwell.utils.time.TimeUtils;
import com.winterwell.utils.web.SimpleJson;
import com.winterwell.utils.web.XStreamUtils;

import no.uib.cipr.matrix.DenseMatrix;
import no.uib.cipr.matrix.DenseVector;
import no.uib.cipr.matrix.Matrix;
import no.uib.cipr.matrix.MatrixEntry;
import no.uib.cipr.matrix.Vector;
import no.uib.cipr.matrix.Vector.Norm;
import no.uib.cipr.matrix.VectorEntry;
import no.uib.cipr.matrix.sparse.SparseVector;

class CSVDataStream extends ADataStream {
	private static final long serialVersionUID = 1L;
	private final File file;
	private final LineReader lreader;

	transient Time prev = TimeUtils.ANCIENT;

	public CSVDataStream(File file, LineReader lreader, int dim) {
		super(dim);
		this.file = file;
		this.lreader = lreader;
	}

	@Override
	public void close() {
		lreader.close();
	}

	@Override
	public AbstractIterator<Datum> iterator() {
		return new AbstractIterator<Datum>() {
			@Override
			protected Datum next2() {
				if (!lreader.hasNext())
					return null;
				String line = lreader.next();
				if (Utils.isBlank(line) || line.charAt(0) == '#')
					return next2(); // this is redundant - the header is already
									// gone
				String[] bits = line.split("\t");
				assert bits.length == (getDim() + 2) : "(exp:" + getDim()
						+ "found:" + (bits.length - 2) + ") " + file + ":"
						+ lreader.getLineNum() + " dimension error "
						+ Printer.toString(bits);
				// time
				Time t = new Time(Long.valueOf(bits[0]));
				// check correct ordering
				assert !prev.isAfter(t) : prev + " vs " + t + " in " + file
						+ " line " + lreader.getLineNum();
				prev = t;

				double[] vec = new double[bits.length - 2];
				for (int i = 1, n = bits.length - 1; i < n; i++) {
					double vi = Double.valueOf(bits[i]);
					assert MathUtils.isFinite(vi) : file + " line "
							+ lreader.getLineNum() + ":	" + vi;
					vec[i - 1] = vi;
				}
				// deseralise label
				Object label = null;
				String lastBit = bits[bits.length - 1];
				if (Utils.isBlank(lastBit)) {
					label = null;
				} else if (lastBit.charAt(0) == '<') { // XML?
					label = XStreamUtils.serialiseFromXml(lastBit);
				} else { // JSON?
					label = DataUtils.simpleJson.fromJson(lastBit);
				}
				return new Datum(t, vec, label);
			}
		};
	}

	@Override
	public String toString() {
		return "CSVDataStream:" + file;
	}

}

/**
 * Utility methods for working with time-series data.
 * 
 * merge with {@link VectorUtils}?? 
 * 
 * NB: Probably no merge with {@link MatrixUtils}, which is extra-methods for matrices like getColumn().
 * 
 * @testedby {@link DataUtilsTest}
 * 
 * @author daniel
 * 
 */
public class DataUtils extends StatsUtils {

	private DataUtils() {	
	}
	
	/**
	 * Renders Datums as {time:Java-time-stamp, label:"", data:[]}
	 * @param data
	 * @return
	 */
	public static String toJson(IDataStream data) {
		JSONArray highchartsData = new JSONArray();
		
		for (Datum datum : data) {
			JSONObject point = new JSONObject();
			
			if (datum.isTimeStamped()) {
				point.put("x", datum.time.getTime());
			}
			
			point.put("y", datum.x());
			
			highchartsData.put(point);
		}
		
		return highchartsData.toString();
	}
	
	static final SimpleJson simpleJson = new SimpleJson();

	/**
	 * Apply the matrix to elements of the base data stream
	 * 
	 * @param transform
	 * @param data
	 * @return a filtered data stream that transforms points as it reads them.
	 * It preservs any time and label info.
	 */
	public static IDataStream applyMatrix(final Matrix transform,
			Iterable<? extends Vector> baseData) 
	{
		assert transform != null;
		IDataStream ds;
		if (baseData instanceof IDataStream) {
			ds = (IDataStream) baseData;
		} else {
			ds = new ListDataStream(baseData);
		}
		return new FilteredDataStream(ds) {
			@Override
			protected Datum filter(Datum datum) {
				assert datum!=null;
				Datum y = new Datum(datum.time,
						new double[transform.numRows()], datum.getLabel());
				y.setModifiable(true);
				transform.mult(datum, y);
				return y;
			}
		};
	}

	/**
	 * Pull one dimension out of a vector data stream.
	 * 
	 * @param base
	 * @param dim
	 *            The dimension to keep
	 * @see #getColumns(IDataStream, int...)
	 */
	public static IDataStream get1D(final IDataStream baseData,
			final int dimIndex) {
		assert baseData != null;
		assert dimIndex < baseData.getDim()
			: String.format("Request for dimension %d of %dD stream", dimIndex,baseData.getDim());
		return new ADataStream(1) {
			private static final long serialVersionUID = 1L;

			@Override
			public Dt getSampleFrequency() {
				return baseData.getSampleFrequency();
			}

			@Override
			public AbstractIterator<Datum> iterator() {
				return new AbstractIterator<Datum>() {
					AbstractIterator<Datum> base = baseData.iterator();

					@Override
					public final double[] getProgress() {
						return base.getProgress();
					}

					@Override
					protected Datum next2() {
						if (!base.hasNext())
							return null;
						Datum d = base.next();
						assert d != null : "Base: " + base;
						assert d.getData() != null : d + " from base: " + base;
						assert d.time != null : d + " from: " + base;
						return new Datum(d.time, d.getData()[dimIndex],
								d.getLabel());
					}
				};
			}

			@Override
			public String toString() {
				return "ADataStream1D:" + dimIndex + baseData;
			}
		};
	}

	/**
	 * Multi-dim variant of {@link #get1D(IDataStream, int)}
	 * @param base
	 * @param outputColumns
	 * @return
	 */
	public static IDataStream getColumns(IDataStream base, int... outputColumns) {
		DenseMatrix dm = new DenseMatrix(outputColumns.length, base.getDim());
		for (int i = 0; i < outputColumns.length; i++) {
			dm.set(i, outputColumns[i], 1);
		}
		return new MatrixStream(dm, base);
	}
	
	
	// /**
	// * Switches on some extra (not always appropriate) sanity checks
	// */
	// private static final boolean PARANOID_DEBUG = false;

	/**
	 * Extract the nth dimension
	 * 
	 * @param sample
	 * @param nth
	 * @return an array of the values for dimension n from the samples
	 */
	public static double[] get1DArr(Iterable<? extends Vector> sample, int n) {
		if (sample instanceof Collection) {
			// fastest copy
			double[] xs = new double[((Collection) sample).size()];
			int i = 0;
			for (Vector v : sample) {
				xs[i] = v.get(n);
				i++;
			}
			return xs;
		}
		// copy out
		ArrayList<Double> list = new ArrayList();
		for (Vector v : sample) {
			double vn = v.get(n);
			list.add(vn);
		}
		return MathUtils.toArray(list);
	}

	/**
	 * Convert into a data stream by taking the Euclidean length of vectors.
	 * 
	 * @param rawDataStream
	 * @return
	 */
	public static ADataStream getEuclidean(final ADataStream base) {
		return new ADataStream(1) {
			private static final long serialVersionUID = 1L;

			@Override
			public AbstractIterator<Datum> iterator() {
				return new AbstractIterator<Datum>() {
					final AbstractIterator<Datum> baseIt = base.iterator();

					@Override
					protected Datum next2() throws Exception {
						if (!baseIt.hasNext())
							return null;
						Datum d = baseIt.next();
						double x = MathUtils.euclideanLength(d.getData());
						return new Datum(d.time, x, d.getLabel());
					}
				};
			}

			@Override
			public String toString() {
				return "||" + base + "||";
			}
		};
	}

	/**
	 * Create a mixture sampling from two distributions. COnvenient for some
	 * tests.
	 * 
	 * @param labelA
	 * @param distA
	 * @param labelB
	 * @param distB
	 * @param dt
	 * @return
	 */
	public static IDataStream mix(Object labelA, IDistribution distA,
			Object labelB, IDistribution distB, Dt dt) {
		RandomDataStream a = new RandomDataStream(distA, null, dt);
		a.setLabel(labelA);
		RandomDataStream b = new RandomDataStream(distB, null, dt);
		b.setLabel(labelB);
		return new MixedDataStream(a, b);
	}

	/**
	 * Lazily read contents of a file created by save. Closes the file when
	 * done.
	 * 
	 * @testedby {@link DataUtilsTest#testSaveRead1D()}
	 */
	public static IDataStream read(final File streamData) {
		// Find out the dimensions
		int dim = 0;
		LineReader lreader = new LineReader(streamData);
		while (lreader.hasNext()) {
			String line = lreader.peekNext();
			if (Utils.isBlank(line) || line.charAt(0) == '#') {
				lreader.next(); // throw away header
				continue;
			}
			String[] bits = line.split("\t");
			dim = bits.length - 2;
			break;
		}
		return new CSVDataStream(streamData, lreader, dim);
	}

	/**
	 * Load a data stream, adding in a factory method
	 * 
	 * @param f
	 * @param factory
	 * @return
	 */
	public static IDataStream read(File f, final IDataStream factory) {
		final IDataStream s = read(f);
		return new ADataStream(s.getDim()) {
			private static final long serialVersionUID = 1L;

			@Override
			public IDataStream factory(Object source) {
				return factory.factory(source);
			}

			@Override
			public AbstractIterator<Datum> iterator() {
				return s.iterator();
			}
		};
	}

	/**
	 * Attempt to equalise the labels by creating more data from the rare
	 * labels.
	 * 
	 * @param data
	 * @return
	 */
	public static ListDataStream resampleEqualLabels(IDataStream data) {
		// count 'em
		ListDataStream ldata = data.list();
		ObjectDistribution dist = new ObjectDistribution();
		for (Datum object : ldata) {
			if (object.getLabel() == null) {
				continue;
			}
			dist.count(object.getLabel());
		}
		dist.normalise();
		// try to avoid dropping data points (we prefer duplicating to maake up
		// numbers)
		double maxP = dist.prob(dist.getMostLikely());
		HashMap<Object, Double> boost = new HashMap<Object, Double>();
		for (Object label : dist) {
			double p = dist.prob(label);
			assert p != 0;
			double b = maxP / p;
			// limit how much resampling we will do
			b = Math.max(b, 1000);
			boost.put(label, b);
		}
		ResampledDataStream rds = new ResampledDataStream(ldata.clone(), boost);
		return rds.list();
	}

	/**
	 * Synonym for {@link #toList(ADataStream, int)}.
	 */
	public static ArrayList<Datum> sample(IDataStream stream, int maxValues) {
		return toList(stream, maxValues);
	}

	/**
	 * Convenience for generating labelled samples from a distribution
	 * 
	 * @param dist
	 * @param numSamples
	 * @param label
	 * @return
	 */
	public static ListDataStream sample(IDistribution dist, int numSamples,
			Object label) {
		ListDataStream samples = new ListDataStream(dist.getDim());
		for (int i = 0; i < numSamples; i++) {
			Vector sample = dist.sample();
			Datum d = new Datum(sample, label);
			samples.add(d);
		}
		return samples;
	}

	/**
	 * Sort a data stream by the labels.
	 * 
	 * @param data
	 * @return one data stream per label. null-labelled data is included (but
	 *         you can remove it afterwards)
	 */
	public static Map<Object, ListDataStream> sortByLabel(IDataStream data) {
		Map<Object, ListDataStream> sorted = new HashMap<Object, ListDataStream>();
		for (Datum datum : data) {
			ListDataStream stream = sorted.get(datum.getLabel());
			if (stream == null) {
				stream = new ListDataStream(data.getDim());
				sorted.put(datum.getLabel(), stream);
			}
			stream.add(datum);
		}
		return sorted;
	}

	/**
	 * Get a filtered view of the datastream from start (inclusive) to end
	 * (exclusive).
	 * 
	 * @param stream
	 *            This must be properly ordered.
	 * @param start
	 *            Earliest data point (inclusive). Can be null for no start
	 *            filtering
	 * @param end
	 *            Latest data point (exclusive). Can be null for no end
	 *            filtering
	 * @return slice-of-stream. Uses lazy-filtering
	 * @testedyb {@link DataUtilsTest#testSubStream}
	 */
	// name is for similarity with List.subList
	public static IDataStream subStream(IDataStream stream, final Time start,
			final Time end) {
		assert stream != null;
		if (start == null && end == null)
			return stream;
		assert !Utils.equals(start, end) : start;
		return new SubStream(stream, start, end);
	}

	/**
	 * Sum all the entries in data.
	 * 
	 * @param data
	 *            Must be a finite stream. Cannot have zero dimension.
	 */
	public static Vector sum(IDataStream data) {
		assert data.getDim() != 0 : data;
		Vector sum = DataUtils.newVector(data.getDim());
		for (Datum datum : data) {
//			if ( ! datum.isZero()) { // HACK debugging
//				assert true;
//			}
			sum.add(datum);
		}
		return sum;
	}

	/**
	 * Extract one dimension from a stream, and copy into an array.
	 * 
	 * @param stream
	 * @param dim
	 * @return the dim dimension values from stream
	 * @see #get1D(IDataStream, int)
	 * @see #toList(IDataStream, int)
	 */
	public static double[] toArray(Iterable<Datum> stream, int dim) {		
		ArrayList<Double> list = new ArrayList();
		for (Datum datum : stream) {
			list.add(datum.get(dim));
		}
		return MathUtils.toArray(list);
	}

	/**
	 * Convert a stream into an array. Can only be called once on a stream!
	 * 
	 * @param stream
	 *            Must not be empty
	 * @param maxValues
	 *            Will pull up to this many values. Needed as s(stream)ome data streams
	 *            are unending. -1 for unlimited
	 * @return contents of the stream as an array
	 */
	public static ArrayList<Datum> toList(IDataStream stream, int maxValues) {
		int capacity = maxValues == -1 ? 2000 : maxValues;
		ArrayList<Datum> list = new ArrayList<Datum>(capacity);
		for (Datum d : stream) {
			assert DataUtils.isFinite(d) : d;
			list.add(d);
			if (list.size() == maxValues) {
				break;
			}
		}
		return list;
	}

	/**
	 * Save contents of the stream as a list of
	 * <code>time-code	value label</code> lines in a a file. Label objects are
	 * saved using toString() for primitives and String, or converted using
	 * XStream for other objects. Tabs and line-endings will be discarded
	 * 
	 * @param stream
	 * @param out
	 * @param maxValues
	 *            Will save up to this many values. Needed as some data streams
	 *            are unending. -1 for unlimited
	 * @param headerComment
	 *            Can be null. If set, a description is added to the start of
	 *            the file. Each line must begin with a #
	 * 
	 *            <p>
	 *            The output file is only written to on success. Should
	 *            something go wrong this method shouldn't leave a partially
	 *            saved file. C.f. append which will leave partial files.
	 */
	public static void write(IDataStream stream, File out, int maxValues,
			String headerComment) {
		File temp = FileUtils.createTempFile("data", ".csv");
		BufferedWriter writer = FileUtils.getWriter(temp);
		try {
			write2(stream, writer, maxValues, headerComment);
			assert temp.exists() : temp;
			FileUtils.move(temp, out);
			assert out.exists() : out;
		} catch (Exception e) {
			// Something went wrong - don't leave a partially saved file
			// c.f. append which will leave partial files
			FileUtils.delete(temp);
			throw Utils.runtime(e);
		}
	}

	static void write2(IDataStream stream, BufferedWriter writer,
			int maxValues, String headerComment) {
		try {
			Time prev = TimeUtils.ANCIENT;
			int cnt = 0;
			// Header?
			if (!Utils.isBlank(headerComment)) {
				for (String h : StrUtils.splitLines(headerComment)) {
					// assert h.startsWith("#") : headerComment; This isn't
					// really essential or part of csv standard
					writer.write(h + StrUtils.LINEEND);
				}
			}
			// Data
			int dim = stream.getDim();
			for (Datum d : stream) {
				assert dim == d.size() : dim + " vs " + d.size() + ": " + d
						+ "\t" + stream;
				if (cnt == maxValues) {
					break;
				}
				if (cnt == Integer.MAX_VALUE) {
					Log.report("Data stream exceeded max-int values!",
							Level.SEVERE);
				}
				cnt++;

				// time stamp
				writer.append(Long.toString(d.time.longValue()));
				// check correct ordering
				assert !prev.isAfter(d.time) : prev + " vs " + d.time + " in "
						+ stream + " count " + cnt;
				prev = d.time;

				// vector
				double[] array = d.getData();
				for (int i = 0; i < array.length; i++) {
					writer.append('\t');
					double vi = array[i];
					assert MathUtils.isFinite(vi) : stream + ":	" + d;
					writer.append(Double.toString(vi));
				}
				writer.append('\t');

				// label
				Object lbl = d.getLabel();
				String xml;
				try {
					xml = simpleJson.toJson(lbl);
				} catch (Exception e) {
					xml = XStreamUtils.serialiseToXml(lbl);
				}
				xml = xml.replace('\t', ' ');
				xml = xml.replace('\r', ' ');
				xml = xml.replace('\n', ' ');
				writer.append(xml);
				writer.append('\n');
			}
			writer.close();
		} catch (IOException e) {
			throw new WrappedException(e);
		}
	}

	public static void writeAppend(IDataStream stream, File out, int maxValues) {
		try {
			BufferedWriter w = FileUtils.getWriter(new FileOutputStream(out,
					true));
			write2(stream, w, maxValues, null);
		} catch (FileNotFoundException e) {
			throw new WrappedException(e);
		}
	}

	/**
	 * @param data
	 * @return all the labels used, converted by toString 
	 * (including "null" if there is unlabelled data)
	 */
	public static List<String> getLabels(IDataStream data) {
		HashSet labels = new HashSet();
		for (Datum datum : data) {
			Object l = datum.getLabel();
			labels.add(l==null? "null" : l.toString());
		}
		return new ArrayList(labels);
	}

	/**
	 * @param timeSeries
	 * @return Remove leading and trailing 0s
	 */
	public static ListDataStream trim(IDataStream timeSeries) {
		int dim = timeSeries.getDim();
		ArrayList trimmed = new ArrayList();
		AbstractIterator<Datum> it = timeSeries.iterator();
		// burn-in
		while(it.hasNext()) {
			Datum d = it.next();
			if (d.norm(Norm.Infinity) != 0) {
				trimmed.add(d);
				break;
			}
		}
		if (trimmed.isEmpty()) {
			return new ListDataStream(dim);
		}
		// burn through
		int last = trimmed.size();
		while(it.hasNext()) {
			Datum d = it.next();
			trimmed.add(d);
			if (d.norm(Norm.Infinity) != 0) {
				last = trimmed.size();
			}
		}
		// burn-out
		if (last == trimmed.size()) {
			return new ListDataStream(trimmed);
		}
		List trimmed2 = trimmed.subList(0, last);
		return new ListDataStream(trimmed2);
	}

	/**
	 * Just a wrapper for {@link DeltaDataStream}
	 * @param iDataStream
	 * @return the datastream measuring the difference between points. I.e. the discrete/stepping 1st derivative.
	 * The first bucket is zero. The timestamps are for the later buckets. 
	 */
	public static IDataStream diff(IDataStream iDataStream) {
		return new DeltaDataStream(iDataStream);
	}

	public static ListDataStream max(IDataStream a, IDataStream b) {
		// TODO refactor to be a 2-arg fun-datat-stream
		int dim = Math.min(a.getDim(), b.getDim());
		ListDataStream max = new ListDataStream(dim);
		AbstractIterator<Datum> bit = b.iterator();
		AbstractIterator<Datum> ait = a.iterator();
		while(true) {
			Datum ad = ait.hasNext()? ait.next() : null;
			Datum bd = bit.hasNext()? bit.next() : null;
			if (ad==null && bd==null) break;
			if (ad==null) {
				max.add(bd); continue;
			}
			if (bd==null) {
				max.add(ad); continue;
			}
			if (dim==1) {
				if (ad.x() > bd.x()) {
					max.add(ad);				
				} else {
					max.add(bd);
				}
			} else {
				Vector vector = DataUtils.elementWiseMax(ad, bd);
				Datum md = new Datum(ad.time, vector, Utils.or(ad.getLabel(),bd.getLabel()));
				max.add(md);				
			}
		}
		
		return max;
	}

	public static double[] getResiduals(double[] targets, IPredictor regression,
			List<? extends Vector> expRows) 
	{
		assert expRows.size()==targets.length;
		double[] predictions = getPredictions(regression, expRows);
		double[] residuals = new double[expRows.size()];
		for(int i=0; i<targets.length; i++) {
			double ti = targets[i];
			double pti = predictions[i];
			residuals[i] = ti - pti;
		}
		return residuals;
	}
	
	public static double[] getPredictions(IPredictor regression, List<? extends Vector> expRows) 
	{
		double[] predictions = new double[expRows.size()];
		for(int i=0; i<predictions.length; i++) {
			double pti = regression.predict(expRows.get(i));
			predictions[i] = pti;
		}
		return predictions;
	}
	

	/**
	 * Just a convenience for {@link #getScore(KScore, double[], double[], int)}
	 * @param targets
	 * @param residuals
	 * @return
	 */
	public static double getR2(double[] targets, double[] residuals) {
		return getScore(KScore.R2, targets, residuals, 1);
	}
	
	
	public static double getScore(KScore score, double[] targets, double[] residuals, int numExpVars) {
		switch(score) {
		case R2:
			double mean = mean(filterFinite(targets));
			double sumressq=0,sumsq=0;
			for(int i=0; i<residuals.length; i++) {
				double ti = targets[i];
				if (Double.isNaN(ti)) {
					// target was missing -- skip from calculation
					continue;
				}
				sumsq += MathUtils.sq(ti - mean); // yes this is n*variance
				sumressq += MathUtils.sq(residuals[i]);
			}
			// no variance in the targets??
			if (MathUtils.isTooSmall(sumsq)) {
				if (MathUtils.isTooSmall(sumressq)) {
					return 1;
				}
				return 0;
			}
			double r2 = 1 - sumressq/sumsq;
			assert MathUtils.isFinite(r2) && r2<=1 : r2;
			return r2;
		case ADJUSTED_R2:
			int n = targets.length, k = numExpVars;
			double r2score = getScore(KScore.R2, targets, residuals, numExpVars);
			double ar2 = 1 - ((1- r2score)*(n-1)*1.0/(n-k-1));
			return ar2;
		case CHI2:
//			ChiSquaredDistribution d;
//			ChiSquaredTest t;
			throw new TodoException();
		case RMSE:
			double se = 0;
			for (double d : residuals) {
				se +=d*d;
			}
			double mse = se / residuals.length;
			return Math.sqrt(mse);
		case NRMSE:
			double rmse = getScore(KScore.RMSE, targets, residuals, numExpVars);
			mean = StatsUtils.mean(targets);
			return rmse / Math.abs(mean);
		case MAPE:
			double sae = 0;
			for (int j = 0; j < residuals.length; j++) {
				double tj = targets[j];
				// skip 0 to avoid nan??
//				if (tj==0) continue;
				sae += Math.abs(residuals[j] / tj);
			}			
			return sae / residuals.length;
		}
		throw new TodoException(score);
	}
	
	
	/**
	 * The vector-valued version of getScore()
	 * @param score
	 * @param testtargets
	 * @param residuals
	 * @param numExpVars
	 * @return
	 */
	public static double getScore(KScore score, List<? extends Vector> testtargets, final List<? extends Vector> residuals, int numExpVars) {
		switch(score) {
		case R2:
			// the r2 for each dimension
			Vector targetVar = StatsUtils.var(testtargets);
			Vector residualVar = StatsUtils.var(residuals);
			Vector r2 = newVector(targetVar.size());
			for(int d=0; d<targetVar.size(); d++) {
				double rvard = residualVar.get(d);
				double tvard = targetVar.get(d);
				// Robustness: no variance in the targets??
				if (MathUtils.isTooSmall(tvard)) {
					if (MathUtils.isTooSmall(rvard)) {
						r2.set(d, 1);
					} else {
						r2.set(d, 0);	
					}
				} else {
					r2.set(d, 1 - rvard / tvard);
				}
			}						
			// average them
			return r2.norm(Norm.One) / r2.size();
		case ADJUSTED_R2:
			double r2score = getScore(KScore.R2, testtargets, residuals, numExpVars);
			int n = residuals.size();
			int k = numExpVars; // how many explanatory variables does this filter use?
			// This isn't really valid, cos it potentially has the full history trace.
			double ar2 = 1 - ((1-r2score)*(n-1)*1.0/(n-k-1));
			return ar2;
		case RMSE:			
			double se = 0;
			for (Vector r : residuals) {
				se += r.dot(r);
			}
			double mse = se / residuals.size();
			return Math.sqrt(mse);
		case NRMSE:
			double rmse = getScore(KScore.RMSE, testtargets, residuals, numExpVars);
			Vector mean = StatsUtils.mean(testtargets);
			return rmse / mean.norm(Norm.Two);
		case MAPE:
			// HACK -- MAPE for vectors? How should we define that??
			double sae = 0;
			int dims = residuals.get(0).size();
			for (int j = 0; j < residuals.size(); j++) {
				Vector tj = testtargets.get(j);
				// skip 0 to avoid nan??
//				if (tj==0) continue;
				Vector rj = residuals.get(j);
				for(int d=0; d<dims; d++) {
					sae += Math.abs(rj.get(d) / tj.get(d));	
				}				
			}			
			return sae / (residuals.size()*dims);
		}
		throw new TodoException(score);
	}

	/**
	 * 
	 * @param targets Possibly the same as inputs!
	 * @param filter
	 * @param inputs
	 * @return
	 */
	public static List<Vector> getResiduals(List<? extends Vector> targets, ITimeSeriesFilter filter, List<? extends Vector> inputs) 
	{
		List<IDistribution> smoothed = filter.smooth(null, new ListDataStream(inputs));
		List<Vector> residuals = new ArrayList();
		for(int i=0; i<targets.size(); i++) {
			Vector ti = targets.get(i);
			Vector ri = ti.copy();
			Vector si = smoothed.get(i).getMean();
			ri.add(-1, si);
			residuals.add(ri);
		}
		return residuals;
	}
	
	/**
	 * 
	 * @param filter
	 * @param inputs
	 * @return
	 */
	public static List<Vector> getPredictions(ITimeSeriesFilter filter, List<? extends Vector> inputs) 
	{
		List<IDistribution> smoothed = filter.smooth(null, new ListDataStream(inputs));
		List<Vector> residuals = new ArrayList();
		for(int i=0; i<inputs.size(); i++) {
			Vector si = smoothed.get(i).getMean();
			residuals.add(si);
		}
		return residuals;
	}


	/**
	 * Convert all values to absolute values
	 * 
	 * @param v This will be modified!
	 * @return 
	 * @return v 
	 */
	public static Vector abs(Vector v) {
		for (VectorEntry ve : v) {
			double x = ve.get();
			double ax = Math.abs(x);
			ve.set(ax);
		}
		return v;
	}

	/**
	 * The angle between a and b.
	 * <p>
	 * For sort order, you could use a.b and skip the inverse cos step.
	 * 
	 * @param a
	 *            Must have non-zero length
	 * @param b
	 *            Must have non-zero length
	 * @return 0 to pi
	 * @throws FailureException
	 *             if a or b have zero length
	 */
	public static double angle(Vector a, Vector b) throws FailureException {
		double ab = a.norm(Norm.Two) * b.norm(Norm.Two);
		if (ab == 0)
			throw new FailureException("zero length vector");
		double aDotB = a.dot(b);
		assert MathUtils.isFinite(aDotB);
		double angle = Math.acos(aDotB / ab);
		assert angle >= 0 && angle <= Math.PI;
		return angle;
	}

	/**
	 * Create a new vector consisting of x followed by y
	 * 
	 * @param x
	 * @param y
	 * @return x append y, which has dimennsions dim(x)+dimm(y)
	 * @testedby {@link VectorUtilsTest#testAppend()}
	 */
	public static Vector append(Vector x, Vector y) {
		// if (x instanceof DenseVector && y instanceof DenseVector) {
		// double[] arr = Arrays.copyOf(((DenseVector)x).getData(),
		// x.size()+y.size());
		// double[] yd = ((DenseVector)y).getData();
		// for(int i=0; i<yd.length; i++) {
		// arr[i+x.size()]=yd[i];
		// }
		// DenseVector xy = new DenseVector(arr, false);
		// return xy;
		// }
		Vector xy = DataUtils.newVector(x.size() + y.size());
		for (VectorEntry ve : x) {
			xy.set(ve.index(), ve.get());
		}
		for (VectorEntry ve : y) {
			xy.set(x.size() + ve.index(), ve.get());
		}
		return xy;
	}

	/**
	 * Is x approximately equal to y? x and y must have the same dimension
	 * 
	 * @see MathUtils.approx()
	 */
	public static boolean approx(Vector x, Vector y) {
		assert x.size() == y.size(); // Or return false?
		for (int i = 0; i < x.size(); i++) {
			if (!MathUtils.approx(x.get(i), y.get(i)))
				return false;
		}
		return true;
	}

	public static Vector asVector(double x) {
		return new X(x);
	}

	/**
	 * @param a
	 * @param b
	 * @return Euclidean distance between a and b TODO how to handle the case
	 *         where a is sparse & basically infinite (but empty), and b is
	 *         dense?
	 */
	public static double dist(Vector a, Vector b) {
		Vector a2 = a.copy();
		a2.add(-1, b);
		return a2.norm(Norm.Two);
	}

	/**
	 * @param a
	 * @param b
	 * @return Each element of a, divided by the corresponding element of b, in
	 *         order
	 */
	public static Vector elementWiseDivide(Vector a, Vector b) {
		if (a.size() != b.size())
			throw new IllegalArgumentException("Vectors must be of same size");
		Vector result = DataUtils.newVector(a.size());
		for (Integer idx = 0; idx < a.size(); idx++) {
			result.set(idx, a.get(idx) / b.get(idx));
		}
		return result;
	}

	/**
	 * For each *element* (i.e. entry), get the max of the two vectors (so
	 * result is (max(a0,b0),max(a1,b1) ....)
	 * 
	 * @param a
	 * @param b
	 * @return max for each entry
	 */
	public static Vector elementWiseMax(Vector a, Vector b) {
		if (a.size() != b.size())
			throw new IllegalArgumentException("Vectors must be of same size");
		Vector result = DataUtils.newVector(a.size());
		for (Integer idx = 0; idx < a.size(); idx++) {
			result.set(idx, MathUtils.max(a.get(idx), b.get(idx)));
		}
		return result;
	}

	/**
	 * For each *element* (i.e. entry), get the min of the two vectors (so
	 * result is (min(a0,b0),min(a1,b1) ....)
	 * 
	 * @param a
	 * @param b
	 * @return min for each entry
	 */
	public static Vector elementWiseMin(Vector a, Vector b) {
		if (a.size() != b.size())
			throw new IllegalArgumentException("Vectors must be of same size");
		Vector result = DataUtils.newVector(a.size());
		for (Integer idx = 0; idx < a.size(); idx++) {
			result.set(idx, MathUtils.min(a.get(idx), b.get(idx)));
		}
		return result;
	}

	/**
	 * @param a
	 * @param b
	 * @return Each element of a, multiplied by the corresponding element of b,
	 *         in order
	 */
	public static Vector elementWiseMultiply(Vector a, Vector b) {
		if (a.size() != b.size())
			throw new IllegalArgumentException("Vectors must be of same size");
		Vector result = DataUtils.newVector(a.size());
		for (Integer idx = 0; idx < a.size(); idx++) {
			result.set(idx, a.get(idx) * b.get(idx));
		}
		return result;
	}

	/**
	 * Like {@link #equals(Object)} but allows some leeway for rounding errors.
	 * uses {@link MathUtils#equalish(double, double)} on the distance between
	 * the two vectors added to the average length.
	 * 
	 * @param a
	 * @param b
	 * @return
	 */
	public static boolean equalish(Vector a, Vector b) {
		double d = dist(a, b);
		double len = DataUtils.len(a) + DataUtils.len(b);
		return MathUtils.equalish(len, len + d);
	}

	/**
	 * 
	 * @param a
	 * @param b
	 * @param tolerance
	 * @return false if the difference in any dimension exceeds tolerance. true
	 *         otherwise
	 */
	public static boolean equalish(Vector a, Vector b, double tolerance) {
		// dense cases
		if (a instanceof DenseVector || a instanceof XY || a instanceof XYZ) {
			for (int i = 0, n = a.size(); i < n; i++) {
				if (Math.abs(a.get(i) - b.get(i)) > tolerance)
					return false;
			}
			return true;
		}
		// Check both ways - inefficient for dense, efficient for sparse
		for (VectorEntry e : a) {
			if (Math.abs(e.get() - b.get(e.index())) > tolerance)
				return false;
		}
		for (VectorEntry eb : b) {
			if (Math.abs(eb.get() - a.get(eb.index())) > tolerance)
				return false;
		}
		return true;
	}

	/**
	 * Convenience for testing!
	 * 
	 * @param x
	 * @param xi
	 * @return true if x has the values given by xi
	 */
	public static boolean equals(Vector x, double... xi) {
		return DataUtils.equals(x, DataUtils.newVector(xi));
	}

	/**
	 * Ignores class
	 * 
	 * @see equalish which allows for rounding errors
	 */
	public static boolean equals(Vector a, Vector b) {
		// lenient size comparison (doesn't check for sparse vectors)
		if (a.size() != -1 && b.size() != -1
				&& a.size() < Integer.MAX_VALUE / 2
				&& b.size() < Integer.MAX_VALUE / 2) {
			if (a.size() != b.size())
				return false;
		}
		// dense cases
		if (a instanceof DenseVector || a instanceof XY || a instanceof XYZ) {
			for (int i = 0, n = a.size(); i < n; i++) {
				if (a.get(i) != b.get(i))
					return false;
			}
			return true;
		}
		// Check both ways - inefficient for dense, efficient for sparse
		for (VectorEntry e : a) {
			if (e.get() != b.get(e.index()))
				return false;
		}
		for (VectorEntry eb : b) {
			if (eb.get() != a.get(eb.index()))
				return false;
		}
		return true;
	}

	public static Vector filledVector(int dim, double value) {
		Vector v = new DenseVector(dim);
		for (int i = 0; i < dim; i++) {
			v.set(i, value);
		}
		return v;
	}

	/**
	 * The "top-left"(min) and "bottom-right"(max) corners of a bounding
	 * hyper-box for these data points.
	 * 
	 * @param data
	 * @return min, max
	 */
	public static Pair<Vector> getBounds(Iterable<? extends Vector> data) {
		assert data != null;
		assert data.iterator().hasNext() : "no data";
		Vector first = data.iterator().next();
		Vector min = first.copy();
		Vector max = first.copy();
		for (Vector vector : data) {
			for (VectorEntry ve : vector) {
				double v = ve.get();
				assert MathUtils.isFinite(v) : vector;
				if (ve.get() > max.get(ve.index())) {
					max.set(ve.index(), v);
				} else if (ve.get() < min.get(ve.index())) {
					min.set(ve.index(), v);
				}
			}
		}
		return new Pair<Vector>(min, max);
	}

	/**
	 * Create a box around a point. Useful for working with
	 * {@link IDistribution}s which need a box to give a probability.
	 * 
	 * @param x
	 * @param dx
	 * @return a top-left, bottom-right box, with x in the middle and width 2*dx
	 *         (i.e. we take a +/- dx step in each axes to reach a corner).
	 *         <p>
	 *         Note: It is probably a mistake to use this with high-dimensional
	 *         vectors - or at least, it suggests something strange.
	 */
	public static Pair<Vector> getBoxAround(Vector x, double dx) {
		int dim = x.size();
		double[] a = new double[dim];
		double[] b = new double[dim];
		for (int i = 0; i < x.size(); i++) {
			a[i] = x.get(i) - dx;
			b[i] = x.get(i) + dx;
		}
		Vector av = DataUtils.newVector(a);
		Vector bv = DataUtils.newVector(b);
		return new Pair<Vector>(av, bv);
	}

	/**
	 * Get the top num dimensions with the highest values. Note: you quite
	 * probably want to call {@link abs} first! Otherwise large
	 * negative numbers will be bottom of the list.
	 * 
	 * @param vector
	 * @param num
	 * @return Can return less than n if the vector is small.
	 * @testedby {@link VectorUtilsTest#testGetSortedIndices()}
	 */
	public static List<Integer> getSortedIndices(final Vector vector, int num) {
		TopNList<Integer> topn = new TopNList<Integer>(num,
				new Comparator<Integer>() {
					@Override
					public int compare(Integer o1, Integer o2) {
						if (o1 == o2)
							return 0;
						double v1 = vector.get(o1);
						double v2 = vector.get(o2);
						if (v1 == v2)
							// arbitrary: order lowest index first
							return o1 < o2 ? -1 : 1;
						return -Double.compare(v1, v2);
					}
				});
		for (VectorEntry ve : vector) {
			int i = ve.index();
			topn.maybeAdd(i);
		}
		return topn;
	}

	public static double getVolume(Vector minCorner, Vector maxCorner) {
		assert minCorner.size() == maxCorner.size();
		double volume = 1;
		for (int i = 0; i < minCorner.size(); i++) {
			double w = maxCorner.get(i) - minCorner.get(i);
			assert w >= 0 : minCorner + " " + maxCorner;
			volume *= w;
		}
		return volume;
	}

	/**
	 * Check that each element is a finite number.
	 * 
	 * @param x
	 * @return true if finite, false if any element is infinite or NaN
	 */
	public static boolean isFinite(Vector x) {
		for (VectorEntry e : x) {
			if (!MathUtils.isFinite(e.get()))
				return false;
		}
		return true;
	}
	
	public static boolean isSafe(Vector x) {
		for (VectorEntry e : x) {
			if (!MathUtils.isSafe(e.get()))
				return false;
		}
		return true;
	}
	/**
	 * Check that each element is a finite number.
	 * 
	 * @param x
	 * @return true if finite, false if any element is infinite or NaN
	 */
	public static boolean isFinite(Matrix x) {
		for (MatrixEntry e : x) {
			if ( ! MathUtils.isFinite(e.get()))
				return false;
		}
		return true;
	}
	public static boolean isSafe(Matrix x) {
		for (MatrixEntry e : x) {
			if ( ! MathUtils.isSafe(e.get()))
				return false;
		}
		return true;
	}
	public static boolean isNormalised(Vector normal) {
		double len = normal.norm(Norm.Two);
		return MathUtils.equalish(len, 1.0);
	}

	/**
	 * @param x
	 * @return true if x is zero
	 */
	public static boolean isZero(Vector x) {
		for (VectorEntry ve : x) {
			if (ve.get() != 0)
				return false;
		}
		return true;
	}

	/**
	 * @param vectorValues
	 * @return a new vector based on this array. This should copy the input
	 *         array.
	 */
	public static Vector newVector(double[] vectorValues) {
		int dim = vectorValues.length;
		if (dim == 1)
			return new X(vectorValues[0]);
		if (dim == 2)
			return new XY(vectorValues[0], vectorValues[1]);
		if (dim == 3)
			return new XYZ(vectorValues[0], vectorValues[1], vectorValues[2]);
		if (dim < 2000)
			return new DenseVector(vectorValues);
		return new SparseVector(new DenseVector(vectorValues, false));
	}

	/**
	 * Create a new vector of the given dimension. Selects an appropriate class,
	 * switching from specialised-low-dim, to dense, to sparse.
	 * 
	 * @param dim
	 * @return zero in dim dimensions
	 */
	public static Vector newVector(int dim) {
		if (dim == 1)
			return new X(0);
		if (dim == 2)
			return new XY(0, 0);
		if (dim == 3)
			return new XYZ(0, 0, 0);
		if (dim < 2000)
			return new DenseVector(dim);
		return new SparseVector(dim);
	}
	
	/**
	 * Create a zero vector of the same dimension as x
	 * @param x
	 * @return
	 */
	public static Vector newVector(Vector x) {
		if (x instanceof DenseVector) {
			return new DenseVector(x.size());
		}
		return newVector(x.size());
	}

	/**
	 * Note: includes an assert check for NaN
	 * 
	 * @param v
	 * @return minimum value in v (note: negative numbers win)
	 */
	public static double min(Vector v) {
		double min = Double.POSITIVE_INFINITY;
		for (VectorEntry ve : v) {
			assert !Double.isNaN(ve.get());
			if (ve.get() < min) {
				min = ve.get();
			}
		}
		return min;
	}

	/**
	 * @param x
	 * @return mean of values in x (including zeroes)
	 */
	public static double mean(Vector x) {
		return StatsUtils.mean(DataUtils.toArray(x));
	}

	/**
	 * @param v
	 * @return maximum value in v
	 */
	public static double max(Vector v) {
		double max = Double.NEGATIVE_INFINITY;
		for (VectorEntry ve : v) {
			if (ve.get() > max) {
				max = ve.get();
			}
		}
		return max;
	}

	/**
	 * Convenience method for the Euclidean norm
	 */
	public static double len(Vector x) {
		return x.norm(Norm.Two);
	}

	/**
	 * Normalise a vector in place, v = v / |v|
	 * 
	 * @param v
	 *            the vector to be normalised. Can be null (returns null).
	 * @return v for convenience
	 * @see StatsUtils#normaliseProbVector(double[])
	 */
	public static Vector normalise(Vector v) throws FailureException {
		if (v==null) return null;
		double size = v.norm(Norm.Two);
		if (size == 0)
			throw new FailureException("Vector has no non-zero values");
		v.scale(1.0 / size);
		return v;
	}

	/**
	 * @param a
	 *            a vector
	 * @param b
	 *            a scalar
	 * @return Each element of a, to the power of scalar b
	 */
	public static Vector power(Vector a, Double b) {
		Vector result = newVector(a.size());
		for (Integer idx = 0; idx < a.size(); idx++) {
			result.set(idx, Math.pow(a.get(idx), b));
		}
		return result;
	}

	/**
	 * Project a vector down into a subspace (as defined by a set of basis
	 * vectors).
	 * 
	 * @param vector
	 *            This is not edited directly.
	 * @param basis
	 *            These should have length 1
	 * @return A copy of vector as projected onto the basis vectors. Dimensions
	 *         come from the basis vectors (e.g. 2 basis vectors = 2
	 *         dimensions).
	 */
	public static Vector projectInto(Vector vector, Vector[] basis) {
		Vector projected = newVector(basis.length);
		for (int i = 0; i < basis.length; i++) {
			Vector e = basis[i];
			assert isNormalised(e) : e;
			double ve = vector.dot(e);
			projected.set(i, ve);
		}
		return projected;
	}

	/**
	 * @param var
	 * @return copy of var with each entry square rooted. Convenient in
	 *         statistics to convert from variance to standard deviation.
	 */
	public static Vector sqrt(Vector var) {
		var = var.copy();
		for (VectorEntry ve : var) {
			ve.set(Math.sqrt(ve.get()));
		}
		return var;
	}

	/**
	 * @param dim
	 * @return random vector, uniform over the [-0.5, 0.5]^n cuboid
	 */
	public static Vector randomVector(int dim) {
		Random r = Utils.getRandom();
		Vector v = newVector(dim);
		for (int i = 0; i < dim; i++) {
			v.set(i, r.nextDouble() - 0.5);
		}
		return v;
	}

	/**
	 * Subtract one vector from another, a = a - b;
	 * 
	 * @param a
	 *            This will be edited!
	 * @param b
	 */
	public static Vector subtract(Vector a, Vector b) {
		a.add(-1, b);
		return a;
	}

	/**
	 * TODO should this be a copy? or can it be "possibly shared structure"? If
	 * you want an array its probably for fast work
	 * 
	 * @param v
	 * @return array copy of v
	 */
	public static double[] toArray(Vector v) {
		assert v.size() < Integer.MAX_VALUE;
//		if (v instanceof DenseVector) { avoid the copy??
//			return ((DenseVector) v).getData();
//		}
		return new DenseVector(v).getData();
	}

	/**
	 * Project a vector down onto a sub-space which is one dimension less than
	 * the space, but still in the same space. I.e. get the component of vector
	 * which is orthogonal to the normal.
	 * 
	 * @param vector
	 *            This is not edited directly.
	 * @param normal
	 *            This should have length 1
	 * @return A copy of vector projected onto the (hyper)plane defined by the
	 *         given normal vector. Has the same dimensions as the input vector.
	 */
	public static Vector projectOrthogonal(Vector vector, Vector normal) {
		assert isNormalised(normal);
		vector = vector.copy();
		// v - (v.n)n
		double vn = vector.dot(normal);
		vector.add(-vn, normal);
		return vector;
	}

	/**
	 * Project a vector down onto a target line. I.e. get the component of
	 * vector which is in the same direction to the normal.
	 * 
	 * @param vector
	 *            This is not edited directly.
	 * @param normal
	 *            This should have length 1
	 * @return A copy of vector projected onto the given normal vector. Has the
	 *         same dimensions as the input vector.
	 */
	public static Vector projectOnto(Vector vector, Vector normal) {
		assert isNormalised(normal);
		Vector proj = normal.copy();
		// v - (v.n)n
		double vn = vector.dot(normal);
		proj.scale(vn);
		return proj;
	}

	/**
	 * 
	 * @param m
	 * @return [row][column]
	 */
	public static double[][] toArray(Matrix m) {
		double[][] vs = new double[m.numRows()][m.numColumns()];
		for (MatrixEntry me : m) {
			vs[me.row()][me.column()] = me.get();
		}
		return vs;
	}

	/**
	 * 
	 * @param columns
	 * @return
	 */
	public static ListDataStream combineColumns(List<double[]> columns) {
		KMatchPolicy policy = KMatchPolicy.IGNORE_TIMESTAMPS;		
		List<IDataStream> dataStreams = new ArrayList();
		for (double[] col : columns) {
			ListDataStream ds = new ListDataStream(col);
			dataStreams.add(ds);
		}
		ExtraDimensionsDataStream ds = new ExtraDimensionsDataStream(policy, dataStreams);
		return ds.list();
	}

	public static Vector slice(Vector x, int start, int end) {
		if (start<0) start = x.size()+start;
		if (end<0) end = x.size()+end;
		double[] xs = Arrays.copyOfRange(toArray(x), start, end);
		return new DenseVector(xs);
	}

	/**
	 * Sum the log of the likelihood.
	 * Use logs because a product would likely run to zero
	 * @param dist
	 * @param data
	 * @return
	 */
	public static double logLikelihood(IDistribution1D dist, double[] data) {
		double sum = 0;
		for (double d : data) {
			double ld = Math.log(dist.density(d));
			sum += ld;
		}
		return sum;
	}
	

}


final class SubStream extends FilteredDataStream {
	private static final long serialVersionUID = 1L;
	private Time start;
	private Time end;

	public String toString() {
		return "SubStream["+base+"]["+start+" to "+end+"]";
	}
	
	SubStream(IDataStream base, Time start, Time end) {
		super(base);
		this.start = start;
		this.end = end;
	}

	@Override
	protected Datum filter(Datum datum) {
		Time t = datum.getTime();
		if (start != null && t.isBefore(start))
			return null;
		if (end == null || t.isBefore(end))
			return datum;
		// we've hit the end
		throw new EnoughAlreadyException();
	}
}
