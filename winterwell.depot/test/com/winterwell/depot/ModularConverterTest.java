/**
 * 
 */
package com.winterwell.depot;

import java.util.Collection;

import org.junit.Test;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.converters.Converter;
import com.winterwell.depot.ModularXML;
import com.winterwell.utils.web.XStreamUtils;

/**
 * @tested {@link ModularConverter}
 * @author daniel
 *
 */
public class ModularConverterTest {

	@Test
	public void testGetModules() {
		ModularConverter mc = new ModularConverter();
		Inner i = new Inner("bar");
		Outer o = new Outer("foo", i);
		Collection<IHasDesc> ms = mc.getModules(o);
		assert ms.contains(i) : ms;
		assert ! ms.contains(o);
		assert ms.size() == 1;
	}
	

	@Test
	public void testCanConvert() {
		ModularConverter mc = new ModularConverter();
		assert mc.canConvert(MyTopLevel.class);
	}
	
	/**
	 * Test method for {@link com.winterwell.depot.ModularConverter#marshal(java.lang.Object, com.thoughtworks.xstream.io.HierarchicalStreamWriter, com.thoughtworks.xstream.converters.MarshallingContext)}.
	 */
	@Test
	public void testMarshal() {		
		XStream xs = XStreamUtils.xstream();
		Converter conv = xs.getConverterLookup().lookupConverterForType(MyTopLevel.class);
		System.out.println(conv);
		
		MyTopLevel object = new MyTopLevel("monkeys", "apes");
		
		// store them
		Depot.getDefault().put(object.getDesc(), object);
		Depot.getDefault().put(object.sub.getDesc(), object.sub);
		
		String xml = XStreamUtils.serialiseToXml(object);
		System.out.println(xml);
		assert xml.contains("monkeys");
		assert ! xml.contains("apes");
		
		MyTopLevel obj2 = XStreamUtils.serialiseFromXml(xml);
		assert obj2.val.equals("monkeys");
		assert obj2.sub.subVal.equals("apes") : obj2.sub.subVal;
	}
	
	
	/**
	 * Test method for {@link com.winterwell.depot.ModularConverter#marshal(java.lang.Object, com.thoughtworks.xstream.io.HierarchicalStreamWriter, com.thoughtworks.xstream.converters.MarshallingContext)}.
	 */
	@Test
	public void testMarshalSimple() {		
		XStream xs = XStreamUtils.xstream();
		Converter conv = xs.getConverterLookup().lookupConverterForType(MyTopLevel.class);
		System.out.println(conv);
		
		MyTopLevel object = new MyTopLevel("monkeys", null);
		String xml = XStreamUtils.serialiseToXml(object);
		System.out.println(xml);
		assert xml.contains("monkeys");
		assert ! xml.contains("apes");
		
		MyTopLevel obj2 = XStreamUtils.serialiseFromXml(xml);
		assert obj2.val.equals("monkeys");
		assert obj2.sub == null;
	}

//	@ModularXML
	class MyTopLevel implements IHasDesc, com.winterwell.depot.ModularXML {

		public IHasDesc[] getModules() { return new IHasDesc[]{sub}; }

		String val;
		
		public MyTopLevel(String a, String b) {
			this.val = a;
			this.sub = b==null? null : new MySubLevel(b);
		}

		@Override
		public Desc getDesc() {
			Desc d = new Desc("foo", getClass());
			d.setTag("test");
			return d;
		}
		
		MySubLevel sub;
	}

//	@ModularXML
	class MySubLevel implements IHasDesc, com.winterwell.depot.ModularXML {
		
		String subVal;
		
		public MySubLevel(String b) {
			subVal = b;
		}

		@Override
		public Desc getDesc() {
			Desc d = new Desc("bar", getClass());
			d.setTag("test");
			return d;
		}

		@Override
		public IHasDesc[] getModules() { return null; }
		
	}
}