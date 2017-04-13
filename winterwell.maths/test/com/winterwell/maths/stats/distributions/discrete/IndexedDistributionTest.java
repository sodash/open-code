package com.winterwell.maths.stats.distributions.discrete;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.List;

import org.junit.Test;

import com.winterwell.maths.datastorage.Index;
import com.winterwell.utils.Printer;
import com.winterwell.utils.containers.Containers;

import junit.framework.TestCase;
import no.uib.cipr.matrix.Vector;
import no.uib.cipr.matrix.sparse.SparseVector;

public class IndexedDistributionTest extends TestCase {

	@Test
	public void testCorners() {
		Index<String> index = new Index<String>();
		IndexedDistribution<String> d = new IndexedDistribution<String>(index);
		d.normalise();
		d.setProb("A", 0.00001);
		assert d.getMostLikely().equals("A");

		d.setProb("B", 0);
		d.normalise();
		assert d.getMostLikely().equals("A");

	}
	
	@Test
	public void testEffectiveParticleCount() {
		{
			Index<String> index = new Index<String>();
			IndexedDistribution<String> oh = new IndexedDistribution<String>(
					index);
			oh.count("b");
			oh.count("c");
			for (int i = 0; i < 1000; i++) {
				oh.count("a");
			}

			assert oh.prob("a") == 1000;

			double cnt = oh.getEffectiveParticleCount();
			assert cnt > 1 && cnt < 2 : cnt;
			Printer.out(oh.toString() + " " + cnt);
		}
		{
			Index<String> index = new Index<String>();
			IndexedDistribution<String> oh = new IndexedDistribution<String>(
					index);
			for (int i = 0; i < 1000; i++) {
				oh.count("a" + i);
			}

			assert oh.prob("a567") == 1;

			double cnt = oh.getEffectiveParticleCount();
			assert cnt > 900 && cnt < 1100 : cnt;
			Printer.out(oh.toString() + " " + cnt);
		}
	}

	@Test
	public void testIndexedDistribution() {
		Index<String> index = new Index<String>();
		IndexedDistribution<String> oh = new IndexedDistribution<String>(index);
		for (String s : new String[] { "a", "b", "c", "d", "a", "b", "c", "a",
				"b" }) {
			oh.count(s);
		}
		Printer.out(oh.toString());
		assert oh.prob("a") == 3;

		double cnt = oh.getEffectiveParticleCount();
		assert cnt > 1 && cnt < 5 : cnt;
	}

	@Test
	public void testIterator() {
		Index<String> index = new Index<String>();
		IndexedDistribution<String> d = new IndexedDistribution<String>(index);
		d.setProb("A", 90);
		d.setProb("B", 10);
		List<String> xs = Containers.getList(d);
		assert xs.size() == 2;
		assert xs.contains("A");
		assert xs.contains("B");
	}

	@Test
	public void testNormalise() {
		Index<String> index = new Index<String>();
		IndexedDistribution<String> d = new IndexedDistribution<String>(index);
		d.setProb("A", 90);
		d.setProb("B", 10);
		assert !d.isNormalised();
		d.normalise();
		assert d.isNormalised();
		assert d.prob("A") == 0.9;
		assert d.prob("B") == 0.1;
		d.setProb("C", 1);
		assert !d.isNormalised();
		d.normalise();
		assert d.isNormalised();
		assert d.prob("A") == 0.45;
		assert d.prob("B") == 0.05;
		assert d.prob("C") == 0.5;
	}

	@Test
	public void testProb() {
		Index<String> index = new Index<String>();
		IndexedDistribution<String> d = new IndexedDistribution<String>(index);
		d.setProb("A", 90);
		d.setProb("B", 10);
		assert d.prob("A") == 90 : d;
		assert d.prob("B") == 10 : d;
		assert d.prob("C") == 0;
		d.normalise();
		assert d.prob("A") == 0.9;
		assert d.prob("B") == 0.1;
		assert d.prob("C") == 0;
	}

	@Test
	public void testSample() {
		Index<String> index = new Index<String>();
		IndexedDistribution<String> d = new IndexedDistribution<String>(index);
		d.setProb("A", 90);
		d.setProb("B", 10);
		d.setProb("C", 0);
		IndexedDistribution<String> sample = new IndexedDistribution<String>(
				index);
		for (int i = 0; i < 1000; i++) {
			sample.count(d.sample());
		}
		assert sample.prob("A") > 800;
		assert sample.prob("B") > 50;
		assert sample.prob("C") == 0;
	}

	@Test
	public void testSerialization() throws IOException {
		Index<String> index = new Index<String>();
		IndexedDistribution<String> oh = new IndexedDistribution<String>(index);
		for (String s : new String[] { "a", "b", "c", "d", "a", "b", "c", "a",
				"b" }) {
			oh.count(s);
		}
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		ObjectOutputStream oos = new ObjectOutputStream(os);
		oos.writeObject(oh);
	}

	@Test
	public void testSparseVectorSerializable() {
		SparseVector v = new SparseVector(10);
		assert (v instanceof Vector);
		assert (v instanceof Serializable);
	}

	@Test
	public void testSparseVectorSerialization() throws IOException {
		SparseVector v = new SparseVector(100);
		v.set(10, 9.5);
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		ObjectOutputStream oos = new ObjectOutputStream(os);
		oos.writeObject(v);
	}

	@Test
	public void testTrivial() {
		Index<String> index = new Index<String>();
		IndexedDistribution<String> oh = new IndexedDistribution<String>(index);
		for (String s : new String[] { "a", "a", "b" }) {
			oh.count(s);
		}
		Printer.out(oh.toString());
		assert oh.prob("a") == 2;
	}

}
