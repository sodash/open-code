package com.winterwell.maths.classifiers;

import static org.junit.Assert.*;

import java.util.Map;

import org.junit.Test;

import com.winterwell.utils.MathUtils;
import com.winterwell.utils.containers.Containers;

public class AccuracyScoreTest {

	@Test
	public void testCountTagTagDouble() {
		AccuracyScore ac = new AccuracyScore<>();
		ac.count("a", "a", 0.5);
		ac.count("a", "b", 0.5);
		Map jobj = ac.toJson2();
		System.out.println(jobj);
		assert ((Number)jobj.get("count")).doubleValue() == 1.0;
	}
	
	
	@Test
	public void testInflation() {
		AccuracyScore ac = new AccuracyScore<>();
		// a really high inflation rate for easy testing -- the unit doubles every 2 counts 
		ac.setInflationRate(2);
		ac.count("a", "a", 0.5);
		ac.count("a", "b", 0.5);
		Map jobj = ac.toJson2();
//		System.out.println(jobj);
		double unit = ac.getUnit();
		assert unit > 1;
		
		ac.count("a", "a", 0.75);
		ac.count("a", "b", 0.25);
		Map jobj2 = ac.toJson2();
		System.out.println(jobj2);
		double unit2 = ac.getUnit();
		assert MathUtils.equalish(unit2, 2) : unit2;
	}

}
