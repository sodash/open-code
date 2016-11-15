package winterwell.utils.containers;

import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;

public class ReverseIteratorTest extends TestCase {

	public void testBasic1() {
		List<String> list = new ArrayList<String>();
		ReverseIterator<String> reverseIt = new ReverseIterator<String>(list);
		assert reverseIt.hasNext() == false;
	}

	public void testBasic2() {
		List<String> list = new ArrayList<String>();
		list.add("first");
		ReverseIterator<String> reverseIt = new ReverseIterator<String>(list);
		assert reverseIt.hasNext();
		assert reverseIt.next().equals("first");
		assert reverseIt.hasNext() == false;
	}

	public void testBasic3() {
		List<String> list = new ArrayList<String>();
		list.add("first");
		list.add("second");
		ReverseIterator<String> reverseIt = new ReverseIterator<String>(list);
		assert reverseIt.hasNext();
		assert reverseIt.next().equals("second");
		assert reverseIt.hasNext();
		assert reverseIt.next().equals("first");
		assert reverseIt.hasNext() == false;
	}

	/*
	 * public void testBasic4() { List<String> list=new ArrayList<String>();
	 * list.add("first"); ReverseIterator<String> reverseIt=new
	 * ReverseIterator<String>(list); assert reverseIt.hasNext();
	 * list.remove(0); //the iterator has an obsolete copy of the list //this
	 * behaviour is different to the class Iterator assert
	 * reverseIt.hasNext()==false; }
	 */
}
