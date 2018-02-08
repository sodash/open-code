package com.winterwell.depot;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import com.winterwell.datalog.DataLog;
import com.winterwell.utils.IFn;
import com.winterwell.utils.StrUtils;
import com.winterwell.utils.Utils;
import com.winterwell.utils.io.FileUtils;
import com.winterwell.utils.log.Log;
import com.winterwell.utils.time.Period;
import com.winterwell.utils.time.Time;
import com.winterwell.utils.web.XStreamUtils;

/**
 * A simple file-based depot.
 * 
 * @author daniel
 * @testedby {@link FileStoreTest}
 */
public class FileStore implements IStore {

	private static final String LOGTAG = "depot.file";

	@Override
	public MetaData getMetaData(Desc desc) {
		MetaData md = getMetaData2(desc);
		if (md!=null) {
			desc.metadata = md;
			return md;
		}
		// new
		md = new MetaData(desc);
		desc.metadata = md;
		md.file = getLocalPath(desc);
		return md;
	}

	@Override
	public void flush() {
		// no-op
	}

	/**
	 * type directory/id based file name
	 * 
	 * @param config
	 * @return the File to use for this config (need not exist)
	 */
	@Override
	public File getLocalPath(Desc desc) {
		try {
			File f = getFilingFn().apply(desc);
			if (f.isAbsolute()) {
				assert f.getPath().startsWith(dir.getPath()) : f+" not in "+dir;
				return f;
			}
			return new File(dir, f.getPath());
		} catch (Exception ex) {
			throw Utils.runtime(ex);
		}
	}
	
	/**
	 * The base directory for the depot
	 */
	final File dir;
	
	/**
	 * What file should we use for Desc?
	 * Uses the structure
	 * dir/tag/type/id
	 * (leaving out /tag/ if null)
	 */
	private IFn<Desc, File> filingFn = new DefaultFilingFn();

	private DepotConfig depotConfig;

	public FileStore(DepotConfig config) {
		this(config.dir);
		this.depotConfig = config;
	}
	
	public FileStore(File dir) {
		this.dir = dir.getAbsoluteFile();
		boolean action = dir.mkdirs();
		assert dir.isDirectory();
		if (action) {
			Log.i(LOGTAG, "New file depot created at: " + dir);
		}
	}

	@Override
	public <X> void put(Desc<X> desc, X artifact) {
		// Probably a bug
		assert ! (artifact instanceof InputStream)
				&& ! (artifact instanceof OutputStream) : artifact;
		File storeHere = getLocalPath(desc);

		// make directory
		File fDir = storeHere.getParentFile();
		boolean ok = fDir.mkdirs();
		assert fDir.isDirectory() : storeHere.getAbsolutePath();

		Log.d(LOGTAG, "Storing " + StrUtils.ellipsize(desc.toString(), 140)
				+ " at " + storeHere+DepotUtils.vStamp(artifact));
		
		// special handling for Files: cache their contents by a file copy
		if (desc.getType() == File.class && artifact instanceof File) {
			// NB: if the desc is a sym-link, then artifact could be a Desc
			// -- hence check artifact instanceof File
			File f = (File) artifact;
//			// Safety check: No re-storing depot files! -- actually you might, to move them
//			assert ! f.getAbsolutePath().startsWith(dir.getAbsolutePath()) 	: "Already in the depot! "+f;
			if ( ! f.equals(storeHere)) {
				FileUtils.copy(f, storeHere, true);
			}
		} else {		
			assert ! desc.symlink || artifact instanceof Desc : desc+" "+artifact;
			assert ! (artifact instanceof File) : desc+" "+artifact;
			// Save
			OutputStream strm = null;
			// TODO How to defend against modifications during the write??
			// We can synch on artifact, but that relies on editors doing the same.  
			// We could test loading the object, though that's inefficient (esp if it's modular).
			try {
				File tmpFile = File.createTempFile("depot", ".xml");
				strm = new FileOutputStream(tmpFile);
				if (desc.gzip) strm = new GZIPOutputStream(strm); 
				if (desc.ser==Desc.KSerialiser.JAVA) {
					// use java serialisation (good for some forms of dense data)
					ObjectOutputStream objStrm = new ObjectOutputStream(strm);
					objStrm.writeObject(artifact);			
					objStrm.close();	
				} else {
					// xstream by default
					Writer w = FileUtils.getWriter(strm);
					XStreamUtils.serialiseToXml(w, artifact);
					w.close();
				}
				FileUtils.close(strm);
				// move it into place
				FileUtils.move(tmpFile, storeHere);
			} catch(Exception ex) {
				throw Utils.runtime(ex);
			} finally {
				// really make sure we release the file handle
				FileUtils.close(strm);
			}			
		}
		
		// Desc too
		File index = getMetaFile(storeHere);
		MetaData md = desc.getMetaData();
		if (md==null) {
			md = new MetaData(desc);
		} 
		md.file = storeHere;
		FileUtils.save(md, index);		
	}
	

	@Override
	public boolean contains(Desc config) {
		return getLocalPath(config).exists();
	}
	
	/**
	 * @param dataDir The directory holding the bits, dir = getLocalPath(config).getParentFile();
	 * @param config
	 * @return the files which this data is split across 
	 * (does a file-level filter; there is a 2nd filter later)
	 */
	<X2> List<X2> getRangedData2_bits(File dataDir, Desc<X2> config) {
		// does this key-type have a naming scheme?
//		TUnit slice = depotConfig==null? TUnit.MONTH : depotConfig.timeSlice(config);
		dataDir.mkdir();
		String[] listing = dataDir.list();
		List<File> bits = getRangedData3_bitsFilter(dataDir, listing, config.range);
		
		// Load them (does nothing if they're Files)
		List<X2> bits2 = new ArrayList(bits.size());
		for (File bit : bits) {
			X2 b = get2_deserialise(bit, config);
			bits2.add(b);
		}
		return bits2;
	}

	/**
	 * @param dataDir
	 * @param listing = dir.list() usually
	 * @param config
	 * @return sorted bits
	 */
	<X2> List<File> getRangedData3_bitsFilter(File dataDir, String[] listing, Period range) {
		Time s = range.first;
		Time e = range.second;
		Pattern s_e = //slice.millisecs < TUnit.DAY.millisecs? 
				Pattern.compile("(\\d+)__(\\d+)"); // : null;
		List<File> bits = new ArrayList();
		for(String f : listing) {
			if (f.endsWith(".meta")) continue;
			if (s_e!=null) {
				Matcher m = s_e.matcher(f);
				boolean ok = m.find();
				if ( ! ok) continue;
				// NB: Desc.getId() uses seconds (not milliseconds)
				Time fs = new Time(Long.valueOf(m.group(1))*1000);
				if (fs.isAfter(e)) continue;
				Time fe = new Time(Long.valueOf(m.group(2))*1000);
				if (fe.isBefore(s)) continue;
	
				File ff = new File(dataDir, f);
//				assert ff.isFile() : ff; may not be for remote files
				bits.add(ff);
			}
		}
		
		Collections.sort(bits); // ?? is this the right order??
		return bits;
	}
	
	<X2> X2 getRangedData(Desc<X2> config) {
		// look for the possible pieces
		File dataDir = getLocalPath(config).getParentFile();
		List<X2> bits = getRangedData2_bits(dataDir, config);		
		// filter, then join (via Desc.join)
		return getRangedData2_filter_join(config, bits);
	}

	/**
	 * @param config
	 * @param bits
	 * @return the object! or null
	 */
	<X2> X2 getRangedData2_filter_join(Desc<X2> config, List<X2> bits) {
		// treat no bits as a fail
		if (bits.isEmpty()) {
			return null;
		}
		// filter the pieces
		ArrayList filtered = new ArrayList();
		for (int i=0; i<bits.size(); i++) {
			Log.d(LOGTAG, "filter "+bits.get(i)+" for "+config);
			X2 bit = config.filter(bits.get(i));
			if (bit==null) continue;
			filtered.add(bit);
		}			
		if (filtered.isEmpty()) return null;
		// join
		Log.d(LOGTAG, "join "+filtered.size()+" for "+config);
		X2 combo = (X2) config.join(filtered);
		
//		// Hack: clean up temp files?
//		for(Object x : filtered) {
//			if (x instanceof File) {
//				File f = (File) x;		
//				if (f.equals(combo)) continue;
//				if (f.toString().startsWith("/tmp")) {
//					FileUtils.delete(f);
//				}
//			}
//		}
		
		return combo;
	}
	
	@Override
	public String getRaw(Desc desc) {
		File f = getLocalPath(desc);
		if ( ! f.exists()) {
			return null;
		}
		return FileUtils.read(f);
	}
	
	@Override
	public <X2> X2 get(Desc<X2> config) {		
		// Is it a ranged object?
		if (config.range!=null) {
			return getRangedData(config);
		}
		
		File f = getLocalPath(config);
		if ( ! f.exists()) {
			Log.report(LOGTAG, "No artifact: " + config
					+ " at " + f.getAbsolutePath(), Level.FINE);		
			return null;
		}
				
		// de-serialise
		return get2_deserialise(f, config);
	}

	<X2> X2 get2_deserialise(File f, Desc<X2> config) {
		// special case for Files -- don't try & use xstream to load them
		if (config.getType()==File.class) {
			return (X2) f;
		}
		assert f.isFile() : f;
		
		DataLog.count(1,"Depot","load", config.getType().getSimpleName());
		InputStream in = null;
		try {
			in = new FileInputStream(f);
			if (config.gzip) in = new GZIPInputStream(in); 
			if (config.ser==Desc.KSerialiser.JAVA) {
				// use java serialisation (good for some forms of dense data)
				ObjectInputStream objIn = new ObjectInputStream(in);
				Object data = objIn.readObject();			
				objIn.close();
				return (X2) data;	
			}
			// xstream by default
			Object data = XStreamUtils.serialiseFromXml(in);
			return (X2) data;
		} catch(Exception ex) {
			throw Utils.runtime(ex);
		} finally {
			FileUtils.close(in);
		}
	}

	public File getDepotDir() {
		return dir;
	}

	IFn<Desc,File> getFilingFn() {
		return filingFn;
	}
	
	/**
	 * @param f
	 * @return File used to store the MetaData, which is f.meta
	 */
	protected File getMetaFile(File artifactFile) {
		File index = new File(artifactFile.getPath()+".meta");
		return index;
	}

	@Override
	public void remove(Desc config) {
		Log.report(LOGTAG, "Removing file for " + config, Level.FINE);
		File f = getLocalPath(config);

		FileUtils.delete(f);
		
		File mf = getMetaFile(f);
		FileUtils.delete(mf);

	}
	
	@Override
	public String toString() {
		return "FileStore["+dir+"]";
	}

	@Override
	public Set<Desc> loadKeys(Desc partialDesc) {
		String tag = partialDesc.getTag();
		Class type = partialDesc.getType();
		// Have a reliable starting form, suitable for directory structuring.
		File highDir = new File(dir, tag+'/'+type.getSimpleName());
		
		// safety check
		File lp = getLocalPath(partialDesc);
		String rp = FileUtils.getRelativePath(lp, highDir);
		assert rp != null;
		
		// Sadly, we can't go from an artifact to it's Desc
		// Find meta-data files
		List<File> files = FileUtils.find(highDir, ".+\\.meta");
		Set<Desc> descs = new HashSet();
		for (File file : files) {
			try {
				MetaData md = FileUtils.load(file);
				Desc d = md.getDesc();
				if (partialDesc.partialMatch(d)) {
					descs.add(d);
				}
			} catch(Exception ex) {
				Log.w(LOGTAG, ex);
			}
		}
		return descs;
	}

	/**
	 * @param desc
	 * @return null if it doesn't exist
	 */
	MetaData getMetaData2(Desc desc) {
		File storeHere = getLocalPath(desc);
		File index = getMetaFile(storeHere);
		if ( ! index.exists()) {
			return null;
		}
		try {
			MetaData md = FileUtils.load(index);
			
			// HACK: Poke maxAge if set! This allows the user making the request to specify a max-age
			if (desc.maxAge!=null) md.getDesc().setMaxAge(desc.maxAge);
			
			return md;
		} catch (Throwable e) {
			// Metadata can be corrupted deleted etc.
			// Try to handle any error by regenerating
			return null;
		}
	}

	class DefaultFilingFn implements IFn<Desc, File> {		
		@Override
		public File apply(Desc config) {
			String id = config.getId();
			// encode into filename safe format
			String encId = FileUtils.filenameEncode(id);
			// Note: the tag/type sub-dir structure is done in Desc.getId()
			return new File(dir, encId);
		}
	}
}


