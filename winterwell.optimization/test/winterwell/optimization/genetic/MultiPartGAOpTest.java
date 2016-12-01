package winterwell.optimization.genetic;

import java.util.List;

import junit.framework.TestCase;

public class MultiPartGAOpTest extends TestCase {

	public void testMutate() {
		VectorGAOp vop1 = new VectorGAOp(20);
		VectorGAOp vop2 = new VectorGAOp(10);
		MultiPartGAOp op = new MultiPartGAOp(vop1, vop2);
		
		List x = op.generate();
		
		List mx = op.mutate(x);
		List mx2 = op.mutate(x);
		
		assert ! mx.equals(x) : mx;
	}

}
