package com.winterwell.maths.timeseries;

import java.util.ArrayList;

import com.winterwell.maths.stats.distributions.d1.MeanVar1D;
import com.winterwell.utils.MathUtils;
import com.winterwell.utils.Printer;
import com.winterwell.utils.TodoException;
import com.winterwell.utils.containers.AbstractIterator;
import com.winterwell.utils.time.Dt;

import no.uib.cipr.matrix.Vector;

/**
 * Status: BROKEN!
 * 
 * Normalise so that data has mean 0, variance 1. By default, this uses a
 * running mean/variance - i.e. the normalisation changes as the stream is read!
 * 
 * TODO or falls in [0,1] TODO or falls in [-1,1]
 * 
 * @testedby {@link NormalisedTest}
 * 
 * @author daniel
 * @deprecated 
 */
@Deprecated
public final class Normalised extends ADataStream {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private IDataStream base;

	MeanVar1D[] meanVar;

	private int numSamples;

	/**
	 * Create a normalised stream using a rolling mean/variance calculation.
	 * 
	 * @param base
	 */
	public Normalised(IDataStream base) {
		this(base, 0);
	}

	/**
	 * @param base
	 * @param bufferAll
	 *            Must be greater than 1, or -1 to indicate all samples
	 *            Indicates: read this many samples in first, then calculate the
	 *            mean and variance and normalise using that.
	 */
	public Normalised(IDataStream base, int samples) {
		super(base.getDim());
		assert base != null;
		// assert samples == -1 || samples > 1 : samples;
		this.numSamples = samples;
		this.meanVar = new MeanVar1D[base.getDim()];
		for (int i = 0; i < meanVar.length; i++) {
			meanVar[i] = new MeanVar1D();
		}
		this.base = init(base, samples);
	}

	@Override
	public void close() {
		base.close();
	}

	@Override
	public IDataStream factory(Object source) {
		// recurse
		if (base.isFactory()) {
			source = base.factory(source);
		}
		// clone
		Normalised clone = new Normalised((IDataStream) source, numSamples);
		return clone;
	}

	public Vector getMean() {
		throw new TodoException(); // TODO
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see winterwell.maths.timeseries.IDataStream#getSampleFrequency()
	 */
	@Override
	public Dt getSampleFrequency() {
		return base.getSampleFrequency();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see winterwell.maths.timeseries.IDataStream2#getSourceSpecifier()
	 */
	@Override
	public Object getSourceSpecifier() {
		return base.getSourceSpecifier();
	}

	public Vector getStdDev() {
		throw new TodoException(); // TODO
	}

	private IDataStream init(IDataStream bse, int numSamples) {
		// calc initial mean and var (using sneaky type conversion on the list)
		ArrayList<Datum> samples = DataUtils.toList(bse, numSamples);
		for (Datum datum : samples) {
			for (int i = 0; i < meanVar.length; i++) {
				meanVar[i].train1(datum.get(i));
			}
		}
		// // Put base data back together
		// ListDataStream start = new ListDataStream(samples);
		// if (bse.hasNext()) return new ChainedDataStream(Arrays.asList(start,
		// bse));
		return bse;
	}

	@Override
	public boolean isFactory() {
		return true;
	}

	@Override
	public AbstractIterator<Datum> iterator() {
		return new AbstractIterator<Datum>() {
			AbstractIterator<Datum> baseIt = base.iterator();

			@Override
			public double[] getProgress() {
				return baseIt.getProgress();
			}

			@Override
			protected Datum next2() {
				if (!baseIt.hasNext())
					return null;
				Datum datum = baseIt.next();
				assert DataUtils.isFinite(datum) : datum + "\n" + base;
				if (datum == null)
					return null;
				// update mean and var
				for (int i = 0; i < meanVar.length; i++) {
					meanVar[i].train1(datum.get(i));
				}
				// Subtract mean and scale by 1/std-dev to give mean=0, var=1
				Datum d = datum.copy();
				for (int i = 0; i < getDim(); i++) {
					double sdi = meanVar[i].getStdDev();
					assert MathUtils.isFinite(sdi) : Printer.toString(sdi)
							+ "\n" + base + "\n" + datum;
					double vi = d.get(i) - meanVar[i].getMean();
					if (sdi != 0) {
						vi = vi / sdi;
					}
					d.set(i, vi);
				}
				assert DataUtils.isFinite(d) : d + "\n" + base;
				return d;
			}
		};
	}

	public void setLossFactor(double lossFactor) {
		for (MeanVar1D mv : meanVar) {
			mv.setLossFactor(lossFactor);
		}
	}

	@Override
	public int size() {
		return base.size();
	}

	@Override
	public String toString() {
		return Normalised.class.getSimpleName() + " " + base.toString();
	}
}
