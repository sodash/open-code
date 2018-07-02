package com.winterwell.depot.merge;

import org.junit.Test;

import com.winterwell.utils.Mutable;
import com.winterwell.utils.Mutable.Bool;
import com.winterwell.utils.Printer;
import com.winterwell.web.WebPage;

public class POJOMergerTest {

	@Test
	public void testDiff() {
		// what shall we test on? how about a WebPage?
		WebPage before = new WebPage();
		
		WebPage after = new WebPage();
		after.setTitle("After Title");
		after.append("Hello After-World :)");
		
		Merger merger = new Merger();
		Diff diff = merger.diff(before, after);
		Printer.out(diff);
	}

	@Test
	public void testBooleanDiff() {
		Merger merger = new Merger();
		merger.addMerge(Mutable.Bool.class, new POJOMerger(new Merger()));
		{	// null -> true
			Mutable.Bool before = new Mutable.Bool();
			Mutable.Bool after = new Mutable.Bool(true);
			
			
			Diff diff = merger.diff(before, after);
			Printer.out(diff);
			

			Mutable.Bool before2 = new Mutable.Bool();
			Mutable.Bool after2 = (Bool) merger.applyDiff(before2, diff);
			assert after2.equals(after) : after2+" != "+after;
		}
		
		{	// true -> false
			Mutable.Bool before = new Mutable.Bool(true);
			Mutable.Bool after = new Mutable.Bool(false);
			
			Diff diff = merger.diff(before, after);
			Printer.out(diff);
			
			Mutable.Bool before2 = new Mutable.Bool(false);
			Mutable.Bool after2 = (Bool) merger.applyDiff(before2, diff);
			assert after2.equals(after);
		}
	}

	
	@Test
	public void testDoMergeNumPOJO() {
		// what shall we test on? how about a WebPage?
		NumThing before = new NumThing();
		
		NumThing after = new NumThing();
		after.x = 7;
		after.sub = new NumThing();
		
		NumThing latest = new NumThing();
		latest.y = 4;
		
		POJOMerger merger = new POJOMerger(new Merger());
		NumThing m = (NumThing) merger.doMerge(before, after, latest);
		
		assert m != null;
		assert m.x == 7;
		assert m.y == 4;
		assert m.sub == after.sub;		
	}
	
	@Test
	public void testDoMergeNumPOJO2_Recursive() {
		// what shall we test on? how about a WebPage?
		NumThing before = new NumThing();
		before.x = 1;
		before.y = 1;
		before.sub = new NumThing();
		
		NumThing after = new NumThing();
		after.x = 7;
		after.y = 2;
		after.sub = new NumThing();
		after.sub.x = 8;
		
		NumThing latest = new NumThing();
		latest.x = 1;
		latest.y = 4;
		latest.sub = new NumThing();
		latest.sub.x = 1;
		latest.sub.y = 1;
		
		POJOMerger merger = new POJOMerger(new Merger());
		NumThing m = (NumThing) merger.doMerge(before, after, latest);
		
		assert m != null;
		assert m.x == 7 : m;
		assert m.y == 5;
		assert m.sub.x == 9 : m.sub;
		assert m.sub.y == 1 : m.sub;
		System.out.println(m);
	}
	
	
	@Test
	public void testDoMergeNumPOJO_NewSubObject() {
		Merger merger = new Merger();
		POJOMerger pmerger = new POJOMerger(merger);
		merger.addMerge(NumThing.class, pmerger);

		NumThing before = new NumThing();
		
		NumThing after = new NumThing();
		after.sub = new NumThing();
		after.sub.x = 8;
		
		NumThing latest = new NumThing();
		latest.sub = new NumThing();
		latest.sub.x = 1;
		latest.sub.y = 1;
		
		NumThing m = (NumThing) pmerger.doMerge(before, after, latest);
		
		assert m != null;
		assert m.sub != null;
		assert m.sub.y == 1 : m.sub;
		assert m.sub.x == 9 : m.sub;
	}

	

	@Test
	public void testDiffNumPOJO_NewSubObject() {
		// what shall we test on? how about a WebPage?
		NumThing before = new NumThing();
		
		NumThing after = new NumThing();
		after.sub = new NumThing();
		after.sub.x = 8;
		
		POJOMerger merger = new POJOMerger(new Merger());
		Diff diff = merger.diff(before, after);
		
		System.out.println(diff);
	}


}

class NumThing {
	
	
	@Override
	public String toString() {
		return "NumThing [x=" + x + ", y=" + y + ", sub=" + sub + "]";
	}

	double x;
	
	Integer y;
	
	NumThing sub;
}
