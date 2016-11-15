package winterwell.utils;

import com.winterwell.utils.Environment;

import junit.framework.TestCase;

//NOTE: Environment is a static class, so the changes in the class made by a test might 
//affect other tests (there is no method to clear the class).
public class EnvironmentTest extends TestCase {

	public void testContainsKey1() {
		Environment
				.putDefault(new Key<String>("globalProperty"), "globalValue");
		Environment env = new Environment();
		assert env.containsKey(new Key<String>("globalProperty"));
	}

	public void testContainsKey2() {
		Environment
				.putDefault(new Key<String>("globalProperty"), "globalValue");
		Environment env = new Environment();
		assert env.containsKey(new Key<String>("inexistentProperty")) == false;
	}

	public void testContainsKey3() {
		Environment
				.putDefault(new Key<String>("globalProperty"), "globalValue");
		Environment env = new Environment();
		env.put(new Key<String>("localProperty"), "localValue");
		assert env.containsKey(new Key<String>("localProperty"));
	}

	public void testPushPop() {
		Key<String> key = new Key<String>("foo");
		Environment e = new Environment();
		e.push(key, "bar");
		assert e.get(key).equals("bar");
		e.push(key, "baz");
		assert e.get(key).equals("baz");
		e.pop(key);
		assert e.get(key).equals("bar");
	}

	public void testPut1() {
		Environment env = new Environment();
		env.put(new Key<String>("localProperty"), "localValue");
		assert env.get(new Key<String>("localProperty")).equals("localValue");
	}

	public void testPut2() {
		Environment.putDefault(new Key<String>("property"), "globalValue");
		Environment env = new Environment();
		Environment env2 = new Environment();
		env.put(new Key<String>("property"), "localValue");
		assert env.get(new Key<String>("property")).equals("localValue");
		assert env2.get(new Key<String>("property")).equals("globalValue");
	}

	public void testPut3() {
		Environment.putDefault(new Key<String>("property"), "globalValue");
		Environment env = new Environment();
		env.put(new Key<String>("property"), null);
		assert env.get(new Key<String>("property")).equals("globalValue");
	}

	public void testPutDefault1() {
		Environment.putDefault(new Key<String>("str"), "value1");
		Environment env = new Environment();
		assert env.get(new Key<String>("str")).equals("value1");
	}

	public void testPutDefault2() {
		Environment.putDefault(new Key<String>("str"), "value1");
		Environment.putDefault(new Key<String>("str"), null);
		Environment env = new Environment();
		assert env.get(new Key<String>("str")) == null;
	}
}
