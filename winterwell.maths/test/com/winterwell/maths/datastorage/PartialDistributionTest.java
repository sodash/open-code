package com.winterwell.maths.datastorage;

import static org.junit.Assert.*;

import java.util.Map;

import org.junit.Test;

public class PartialDistributionTest {

	@Test
	public void testSize() {
		{
			PartialDistribution pd = new PartialDistribution<>();
			pd.setProb("apple", 0.5);
			pd.setProb("pear", 0.25);
			int size = pd.size();
			assert size == -1; // unkown
			pd.setSize(10);
			assert pd.size() == 10;
		}
	}

	@Test
	public void testGetTotalWeight() {
		{
			PartialDistribution pd = new PartialDistribution<>();
			pd.setProb("apple", 0.5);
			pd.setProb("pear", 0.25);
			double wt = pd.getTotalWeight();
			assert wt == 0; // unkown
		}
		{
			PartialDistribution pd = new PartialDistribution<>("apple", 0.5, 10, 0.75);
			double wt = pd.getTotalWeight();
			assert wt == 0.75; // unkown
			assert pd.size() == 10;
			assert pd.prob("apple") == 0.5;
		}
	}

	@Test
	public void testLogProb() {
		{
			PartialDistribution pd = new PartialDistribution<>("apple", 0.5, 10, 0.75);
			double wt = pd.getTotalWeight();
			assert wt == 0.75; // unkown
			assert pd.size() == 10;
			assert pd.prob("apple") == 0.5;
			assert pd.logProb("apple") == Math.log(0.5);
		}
	}

	@Test
	public void testNormProb() {
		{
			PartialDistribution pd = new PartialDistribution<>("apple", 0.5, 10, 0.75);
			double wt = pd.getTotalWeight();
			assert wt == 0.75; // unkown
			assert pd.size() == 10;
			assert pd.prob("apple") == 0.5;
			assert pd.normProb("apple") == 0.5/0.75;
		}
	}

	@Test
	public void testProb() {
		{
			PartialDistribution pd = new PartialDistribution<>("apple", 0.5, 10, 0.75);
			double wt = pd.getTotalWeight();
			assert wt == 0.75; // unkown
			assert pd.size() == 10;
			assert pd.prob("apple") == 0.5;
		}
	}

	@Test
	public void testSetProb() {
		{
			PartialDistribution pd = new PartialDistribution<>();
			pd.setProb("apple", 0.6);
			assert pd.prob("apple") == 0.6;
		}
		// Note: the single-item constructor makes an immutable distro
	}

	@Test
	public void testAsMap() {
		{
			PartialDistribution<String> pd = new PartialDistribution<>("apple", 0.5, 10, 0.75);
			Map<String, Double> map = pd.asMap();
			assert map.containsKey("apple");
		}
	}

}
