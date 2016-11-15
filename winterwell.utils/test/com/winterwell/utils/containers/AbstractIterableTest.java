package winterwell.utils.containers;

import junit.framework.TestCase;
import winterwell.utils.Mutable;

public class AbstractIterableTest extends TestCase {

	public void testPeek() {
		final int[] array = new int[] { 1, 7, 3, -2 };
		final Mutable.Int i = new Mutable.Int(0);
		AbstractIterable<Integer> iter = new AbstractIterable<Integer>() {
			@Override
			protected Integer next2() {
				if (i.value >= array.length)
					return null;
				int v = array[i.value];
				i.value++;
				return v;
			}
		};
		// Test
		for (int c = 0; c < array.length; c++) {
			for (int j = 0; j < 10; j++) {
				Integer px = iter.peekNext();
				assert px == array[c];
			}
			Integer x = iter.next();
			assert x == array[c];
		}

	}

	public void testSimple() {
		final int[] array = new int[] { 1, 7, 3, -2 };
		final Mutable.Int i = new Mutable.Int(0);
		AbstractIterable<Integer> iter = new AbstractIterable<Integer>() {
			@Override
			protected Integer next2() {
				if (i.value >= array.length)
					return null;
				int v = array[i.value];
				i.value++;
				return v;
			}
		};
		// Test
		int c = 0;
		for (Integer x : iter) {
			assert x == array[c];
			c++;
		}
		assert c == array.length;
	}
}
