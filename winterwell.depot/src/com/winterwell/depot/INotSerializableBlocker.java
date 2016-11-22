package com.winterwell.depot;

import winterwell.utils.web.XStreamUtils;

import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import com.winterwell.utils.ReflectionUtils;

/**
 * Alter XStream to guard {@link INotSerializable}.
 * TODO test this!
 * @see XStreamUtils which uses reflection to access this.
 * @author daniel
 */
public class INotSerializableBlocker implements Converter {


	public INotSerializableBlocker() {
	}
	
	@Override
	public boolean canConvert(Class type) {
		return ReflectionUtils.isa(type, INotSerializable.class);
	}

	@Override
	public void marshal(Object source, HierarchicalStreamWriter writer,
			MarshallingContext context) {
		throw new UnsupportedOperationException("Not for serialising: "+source.getClass()+": "+source);
	}

	
	@Override
	public Object unmarshal(HierarchicalStreamReader reader,
			UnmarshallingContext context) 
	{
		throw new IllegalStateException(reader.getNodeName());
	}

}
