package winterwell.optimization.genetic;

import java.util.Arrays;

import junit.framework.TestCase;

public class SetGAOpTest extends TestCase {

	public void testMutate() {
		SetGAOp<String> op = new SetGAOp<String>(Arrays.asList("ABCDEFGHIJKLMNOPQRSTUVWXYZ".split("")));
		String x = op.generate();
		String x2 = op.generate();
		
		String mx = op.mutate(x);
		String mx2 = op.mutate(x);
		// about 1 in 500 odds of failing
		assert ! x.equals(mx) || ! x.equals(mx2);
	}

}
