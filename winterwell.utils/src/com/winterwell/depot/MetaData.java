package com.winterwell.depot;

import java.io.File;
import java.io.StringWriter;

import winterwell.utils.time.Dt;
import winterwell.utils.time.Time;
import winterwell.utils.web.XStreamUtils;

/**
 * This is always about the local copy. It's essentially file metadata
 *
 * ??perhaps it should also have fields for describing the remote?
 *
 * TODO Tidy up. Remove unused fields.
 * TODO Use stat to provide last-accessed-time
 * TODO see what Java 7's Path offers
 * @author daniel
 */
public class MetaData {

	@Override
	public String toString() {
		return "MetaData[desc=" + desc + ", file=" + file + "]";
	}

	private Desc desc;

	public Desc getDesc() {
		return desc;
	}

	/**
	 * 
	 * @param desc
	 */
	<X> MetaData(Desc<X> desc) {
		this.desc = desc;
		desc.metadata = this;
	}
	
	public Time getModifiedTime() {
		return new Time(getFile().lastModified());
	}

	File file;

	/**
	 * @return the local file. Can be null
	 */
	public File getFile() {
		return file;
	}

	/**
	 * TODO is this data stale?
	 * @return
	 */
	public boolean isValid() {		
		Dt age = desc.maxAge;
		// dunno!
		if (age==null) return true; 
		Time modTime = getModifiedTime();
		long dt = System.currentTimeMillis() - modTime.getTime();		
		return dt < age.getMillisecs();
	}

	long size = -1;
	
	public long getSize() {
		if (size>0) return size;
		if (file!=null && file.exists()) {
			size = file.length();
			return size;
		}
		if (getDesc().getBoundValue()!=null) {
			Object bv = getDesc().getBoundValue();
			StringWriter sw = new StringWriter();
			XStreamUtils.serialiseToXml(sw, bv);
			size = sw.toString().length();
			return size;
		}		
		return 0;
	}
	
}
