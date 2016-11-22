package winterwell.optimization.genetic;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Random;

import winterwell.utils.Utils;

/**
 * Generate, mutate and crossover for selection-from-a-fixed-set.
 * It just picks members at random. Mutate picks from scratch.
 * Crossover just picks one of its inputs.
 * 
 * TODO: optionally have a distribution and sample from that?
 * @author Daniel
 *
 */
public class SetGAOp<X> implements IBreeder<X>
{

	private final ArrayList<X> set;

	public SetGAOp(Collection<? extends X> set) {
		this.set = new ArrayList<X>(set);
	}
	
	Random r = Utils.getRandom();
	
	@Override
	public X generate() {
		int i = r.nextInt(set.size());
		return set.get(i);
	}
	
	@Override
	public X mutate(X candidate) {	
		return generate();
	}

	@Override
	public X crossover(X a, X b) {		
		return r.nextBoolean()? a : b;
	}

	@Override
	public void setRandomSource(Random r) {
		this.r = r;
	}

}
