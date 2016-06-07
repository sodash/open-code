package com.google.gson.internal.bind;

import java.io.IOException;
import java.util.EnumMap;
import java.util.Map;
import java.util.Set;

import com.winterwell.utils.containers.ArrayMap;

import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

/**
 * Handle EnumMaps -- need to store the enum class if we want to deserialise.
 * Testedby: {@link EnumMapTypeAdapterTest}
 * @author Daniel
 */
public class EnumMapTypeAdapter extends TypeAdapter<EnumMap> {
    private final Gson gson;
	private final String eProp;

    public EnumMapTypeAdapter(Gson gson) {
        super();
        this.gson = gson;
        // This adapter only makes sense if we can save the class details
        assert gson.getClassProperty()!=null;
        eProp = gson.getClassProperty()+".enum";
    }

    @Override
    public void write(JsonWriter out, EnumMap value) throws IOException {
        // Loop check
        boolean ok = out.beginLoopCheck(gson.getLoopPolicy(), value);
        if (!ok) {
            out.nullValue();
            return;
        }

        out.beginObject();
        // class and enum-class
        out.name(gson.getClassProperty());
        out.value(EnumMap.class.getCanonicalName());
        Set keys = value.keySet();
        if (keys.isEmpty()) {
            out.endObject();
            out.endLoopCheck(value);
            return;
        }
        Class eClass = keys.iterator().next().getClass();
        out.name(gson.getClassProperty() + ".enum");
        out.value(eClass.getCanonicalName());

        for(Object key : keys) {
            Object v = value.get(key);
            if (v==null) continue;
            TypeAdapter adapter = gson.getAdapter(v.getClass());
            out.name(key.toString());
            adapter.write(out, v);
        }

        // Done
        out.endObject();
        out.endLoopCheck(value);
    }

    @Override
    public EnumMap read(JsonReader in) throws IOException {
    	Map<String,Object> temp = new ArrayMap();
    	in.beginObject();    	
		Class e = null;
    	while(in.hasNext()) {
    		String name = in.nextName();
    		// Is it the enum prop?
    		if (eProp.equals(name)) {
    			try {
    				e = Class.forName(in.nextString());
    			} catch (ClassNotFoundException e1) {
    				throw new IOException(e1);
    			}
    			continue;
    		}
    		if (gson.getClassProperty().equals(name)) {
    			in.nextString(); // discard -- we know its EnumMap
    			continue;
    		}
    		// recursively read the value
    		Object value = gson.fromJson(in, Object.class);
    		temp.put(name, value);
    	}
    	in.endObject();
    	
    	if (e==null) throw new IOException("No enum class info in: "+temp);
		
		EnumMap map = new EnumMap(e);		
		for(String k : temp.keySet()) {
			Object v = temp.get(k);
            Enum eKey = Enum.valueOf(e, k);
			map.put(eKey, v);
		}
		return map;
    }
    
}
