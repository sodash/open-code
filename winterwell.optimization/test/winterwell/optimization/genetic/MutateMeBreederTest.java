package winterwell.optimization.genetic;

import org.junit.Test;

import winterwell.optimization.AEvaluate;

public class MutateMeBreederTest {

	@Test
	public void testGA() {
		MyThing a = new MyThing();
		MutateMeBreeder mmb = new MutateMeBreeder(a);
		GA<MyThing> ga = new GA(10, mmb);
		MyThing best = ga.optimize(new AEvaluate<MyThing>() {
			@Override
			public double evaluate(MyThing candidate) {
				double score = candidate.b? 10 : -10;
				score += candidate.x;
				score += candidate.x2;
				score += candidate.i;
				if ("a".equals(candidate.s)) {
					score = score * 2;
				}				
				return score;
			}
		});
		System.out.println(best);
		assert best.i > 50;
	}
	
	@Test
	public void testMutate10() {
		MyThing a = new MyThing();
		MutateMeBreeder<MyThing> mmb = new MutateMeBreeder(a);
		int cnt = 0;
		for(int i=0; i<1000; i++) {
			MyThing mod = mmb.mutate(a);
			if (mod.i10==0) continue;
			System.out.println(mod.i10);
			cnt++;
		}
		System.out.println(cnt);
		assert cnt > 0;
	}
	
	
	@Test
	public void testMutateT() {
		MyThing a = new MyThing();
		MutateMeBreeder<MyThing> mmb = new MutateMeBreeder(a);
		// mutate a few times to make sure something changes
		MyThing mod = mmb.mutate(a);
		for (int i=0; i<100; i++) {
			mod = mmb.mutate(mod);
		}
		assert ! mod.equals(a);
	}

	class MyThing {
		
		@MutateMe(choices="a,b,c")
		String s = "b";
		
		@MutateMe(high=10,low=-10)
		double x = 5;
		
		@MutateMe(high=0,low=-10)
		Double x2 = -5.0;
		
		@MutateMe(high=0,low=-10)
		float f = -5.0f;
		
		@MutateMe(high=0,low=-10)
		Float f2 = -5.0f;
		
		@MutateMe(high=100,low=0)
		int i = 10;
		
		@MutateMe(high=10,low=0)
		int i10 = 0;
		
		@MutateMe
		boolean b;

		@Override
		public String toString() {
			return "MyThing [s=" + s + ", x=" + x + ", i=" + i + ", b=" + b + "]";
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + (b ? 1231 : 1237);
			result = prime * result + i;
			result = prime * result + ((s == null) ? 0 : s.hashCode());
			long temp;
			temp = Double.doubleToLongBits(x);
			result = prime * result + (int) (temp ^ (temp >>> 32));
			result = prime * result + ((x2 == null) ? 0 : x2.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			MyThing other = (MyThing) obj;
			if (b != other.b)
				return false;
			if (i != other.i)
				return false;
			if (s == null) {
				if (other.s != null)
					return false;
			} else if (!s.equals(other.s))
				return false;
			if (Double.doubleToLongBits(x) != Double.doubleToLongBits(other.x))
				return false;
			if (x2 == null) {
				if (other.x2 != null)
					return false;
			} else if (!x2.equals(other.x2))
				return false;
			return true;
		}
		
		
	}
}
