package winterwell.utils.containers;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import junit.framework.TestCase;

public class PairTest extends TestCase {

	public void testEqualsObect3() {
		Pair<String> pair1 = new Pair<String>("a", null);
		Pair<String> pair2 = new Pair<String>("a", null);
		assert pair1.equals(pair2);
	}

	public void testEqualsObect4() {
		Pair<String> pair1 = new Pair<String>(null, null);
		Pair<String> pair2 = new Pair<String>(null, null);
		assert pair1.equals(pair2);
	}

	public void testEqualsObject1() {
		Pair<String> a = new Pair<String>("ref1", "ref8");
		Pair<String> b = new Pair<String>("ref1", "ref8");
		assert a.equals(b);
	}

	public void testEqualsObject2() {
		Pair<String> pairString = new Pair<String>("a", "b");
		Pair<Character> pairChar = new Pair<Character>('a', 'b');
		assert pairString.equals(pairChar) == false;
		assert pairChar.equals(pairString) == false;
		assert pairString.equals(null) == false;
		assert pairChar.equals(null) == false;
	}

	public void testHashCode() {
		Pair<String> a = new Pair<String>("ref1", "ref8");
		Pair<String> b = new Pair<String>("ref1", "ref8");
		Pair<String> c = new Pair<String>("x", "ref8");
		Pair<String> d = new Pair<String>("ref1", "x");
		assert a.hashCode() == b.hashCode();
		assert a.hashCode() != c.hashCode();
		assert a.hashCode() != d.hashCode();
	}

	public void testSort() {
		{
			Pair<String> aa = new Pair<String>("a", "a");
			Pair<String> ba = new Pair<String>("b", "a");
			Pair<String> ab = new Pair<String>("a", "b");
			Pair<String> bb = new Pair<String>("b", "b");

			List<Pair<String>> list = Arrays.asList(bb, ba, ab, aa);
			Collections.sort(list);

			assert list.get(0) == aa;
			assert list.get(1) == ab;
			assert list.get(2) == ba;
			assert list.get(3) == bb;
		}
		{ // null goes last
			Pair<String> aa = new Pair<String>("a", "a");
			Pair<String> ba = new Pair<String>(null, "a");
			Pair<String> ab = new Pair<String>("a", null);
			Pair<String> bb = new Pair<String>(null, null);

			List<Pair<String>> list = Arrays.asList(bb, ba, ab, aa);
			Collections.sort(list);

			assert list.get(0) == aa : list;
			assert list.get(1) == ab : list;
			assert list.get(2) == ba : list;
			assert list.get(3) == bb : list;
		}
	}

	public void testToString1() {
		Pair<String> pair = new Pair<String>("first", "second");
		assert pair.toString().equals("(first, second)");
	}

	public void testToString2() {
		Pair<String> pair = new Pair<String>("first", null);
		assert pair.toString().equals("(first, null)");
	}
}
