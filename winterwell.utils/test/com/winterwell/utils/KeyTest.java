package winterwell.utils;

import junit.framework.TestCase;

public class KeyTest extends TestCase {

	public void testEqualsObject1() {
		Key<String> key1 = new Key<String>("name");
		Key<String> key2 = new Key<String>("name");
		assert key1.equals(key2);
	}

	public void testEqualsObject2() {
		Key<String> key1 = new Key<String>("name");
		Key<String> key2 = new Key<String>("name ");
		assert key1.equals(key2) == false;
	}

	public void testEqualsObject3() {
		Key<String> key1 = new Key<String>("name");
		int number = 2;
		assert key1.equals(number) == false;
	}

	public void testEqualsObject4() {
		Key<String> key1 = new Key<String>("name");
		assert key1.equals(null) == false;
	}

	public void testGetName1() {
		Key<String> key = new Key<String>("name");
		assert key.getName().equals("name");
	}

	public void testHashCode1() {
		Key<String> key1 = new Key<String>("name");
		Key<String> key2 = new Key<String>("name");
		assert key1.hashCode() == key2.hashCode();
	}

	public void testHashCode2() {
		Key<String> key1 = new Key<String>("name");
		Key<String> key2 = new Key<String>("245gh");
		assert key1.hashCode() != key2.hashCode();
	}

	public void testToString() {
		Key<String> key = new Key<String>("name");
		assert key.toString().equals("name");
	}

}
