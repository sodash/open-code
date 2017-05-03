package com.winterwell.depot;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.File;
import java.io.Flushable;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentMap;
import java.util.logging.Level;

import com.thoughtworks.xstream.XStreamException;
import com.winterwell.datalog.DataLog;
import com.winterwell.depot.merge.ClassMap;
import com.winterwell.depot.merge.IMerger;
import com.winterwell.depot.merge.MapMerger;
import com.winterwell.utils.Printer;
import com.winterwell.utils.ReflectionUtils;
import com.winterwell.utils.Utils;
import com.winterwell.utils.WrappedException;
import com.winterwell.utils.gui.GuiUtils;
import com.winterwell.utils.io.ArgsParser;
import com.winterwell.utils.io.FileUtils;
import com.winterwell.utils.log.KErrorPolicy;
import com.winterwell.utils.log.Log;
import com.winterwell.utils.threads.EqualsLocker;
import com.winterwell.utils.time.StopWatch;

/**
 * Cache and supply data artifacts.
 * <p>
 * Config: via a file called DepotConfig.props
 * <p>
 * Note: the generic typing on this is not ideal, but attempts to try something
 * better were foiled by the compiler's annoying inability to properly handle
 * extends typing.
 * <p>
 * This class acts as the front-end and handles thread-safety with {@link DescCache}. 
 * Actual storage is delegated to a chain of IStores:
 * 
 * 1. {@link SlowStorage} introduces a delay in persistence to ease the I/O load.
 * 2. {@link RemoteStore} gets & puts between servers.
 * 3. {@link FileStore} handles the save/load to file.
 * <p>
 * <h3>Requirements:</h3>
 * 
 * 1. Reads & writes must be cheap-ish.
 * 2. The keys aren't known in advance.
 * 3. Cross-cluster with fairly low latency.
 * 4. Multi-threaded memory management guarantee: If key1 equals key2, then value1 == value2.
 *
 * <p>
 * This can be configured via {@link DepotConfig}, and you could add a different backend.
 * @author Daniel
 */
public class Depot implements Closeable, Flushable, IStore, INotSerializable
{
	
		
	@Override
	public String toString() {
		return "Depot[base="+base+"]";
	}

	/**
	 * Equivalent to {@link Desc#desc(Object)}.
	 * If this artifact has been bound to a description, then return that Desc.
	 * @param artifact
	 * @return desc for artifact, or null
	 */
	public static <X> Desc<X> getDesc(X artifact) {
		return Desc.desc(artifact);
	}

	/**
	 * What's in the cache? For debugging use only.
	 */
	public Map<Class,List> getCurrentUsage() {
		return ((DescCache)Desc.descCache).getCurrentUsage();
	}
		
	// Is it feasible to have a command-line depot?
	//  Probably not -- building Descs is hard work
//	public static void main(String[] args) {
//	}
	
	
	public static final String TAG = "depot";

	/**
	 * Probably a SlowStorage
	 */
	final IStore base;
	
	final static EqualsLocker<Desc> locker = new EqualsLocker();
	
	/**
	 * Cache a data artifact. This will overwrite any previously stored value
	 * for config.
	 * 
	 * @param desc
	 *            This will be bound to the artifact
	 * @param artifact
	 *            Caching should not alter the object, though it is likely to be
	 *            copied via some form of serialisation.
	 * @return the cached version of the artifact
	 */
	public <Y> void put(Desc<Y> desc, Y artifact) {
		put2(desc, artifact, KOverwrite.OVERWRITE);
	}
	
	/**
	 * What to do if a value already exists?
	 */
	static enum KOverwrite {
		OVERWRITE,
		USE_EXISTING,
		MERGE
	}
	
	/**
	 * 
	 * @param desc
	 * @param artifact
	 * @param overwrite true=put, false=putIfAbsent
	 * @return previous (& still valid) value for desc if the put failed, or null if the put took place.
	 */
	<Y> Y put2(Desc<Y> desc, Y artifact, KOverwrite overwrite) {
		assert desc.symlink || ReflectionUtils.isa(artifact.getClass(), desc.getType()) : desc;
//		Log.v(TAG, "put2", desc, artifact); //verbose get log (normally ignored)
		if (desc.readOnly) throw new IllegalArgumentException(desc+" is read-only");
		locker.lock(desc);		
		
		// Safety check: what if the object's own Desc is wrong?
		if (artifact instanceof IHasDesc) {
			safetySyncDescs(desc, (IHasDesc) artifact);
		}
		
		try {
			if (desc.symlink && ! (artifact instanceof Desc)) {
				throw new IllegalArgumentException("Not a symlink: "+desc+" = "+artifact);
			}
			// put-if-absent?			
			if (overwrite != KOverwrite.OVERWRITE) {
				try {
					Y old = get(desc);
					if (old!=null) {
						if (old==artifact) {
							// too noisy
//							Log.d(TAG, "Going ahead with put() to save edits for "+desc+" = "+old);
						} else if (overwrite==KOverwrite.USE_EXISTING) {							
//							// TODO remove Bug hunting Apr 2014
//							// Still triggering Jan 2015 :(
							// But could this be legit due to sym-link Descs??
							// NB: This WILL get triggered if safetySyncDescs() modified the desc
							if (old instanceof IHasDesc) {
								Desc oldDesc = ((IHasDesc) old).getDesc();
								if ( ! oldDesc.equals(desc)) {
									// TODO investigate again
									Log.e(TAG, "(returning old) Put using existing with different desc?! "+oldDesc+" != "+desc+" old:"+old);
								}
							}							
							return old;				
						}
					}
				} catch(Throwable ex) {
					// oh well
					Log.e(TAG, ex);
				}
				// merge... done later when the underlying system does a put
			}
			
			// Paranoid Safety Check
			if (overwrite==KOverwrite.OVERWRITE) {
				put3_overwriteSafetyCheck(desc, artifact);
			}
			
			// Auto-bind objects/descriptions
			desc.bind(artifact);		
			
			// Support a merge on subsequent put? Not here: the user sets this up themselves.
			// Pass it on to the underlying store
			base.put(desc, artifact);
			
			// Any modules?
			// TODO what about race-conditions here? At least they're reduced.
			Collection<IHasDesc> modules = ModularConverter.getModules(artifact);
			for (IHasDesc m : modules) {
				try {
					Desc md = m.getDesc();
					// Skip the read-only parts
					if (md.readOnly) {
						Log.d(TAG, "skip put read-only "+md);
						continue;
					}
					// Use the same overwrite setting for modules
					IHasDesc oldPart = put2(m.getDesc(), m, overwrite);
					if (oldPart!=null) {
						// should we do something with this? Replace the module in artifact??
					}
				} catch(Throwable ex) {
					if (config.allowModuleExceptions) {
						Log.e(TAG, ex);
					} else {
						throw Utils.runtime(ex);
					}
				}
			}
			// done
			return null;
		} catch(Throwable ex) {
			switch(this.config.errorPolicy) {
			case IGNORE: 
				return null;
			case DELETE_CAUSE:
				if (ex instanceof INotOverwritable.OverWriteException) {
					// Don't remove! That would be bogus!
				} else {
					remove(desc);
				}
			}
			throw Utils.runtime(ex);
		} finally {
			locker.unlock(desc);
		}
	}
	
	private void put3_overwriteSafetyCheck(Desc desc, Object artifact) {
		if ( ! (artifact instanceof INotOverwritable)) {
			return;
		}
		// Is it a merge? So not an overwrite
		if (desc.getBefore()!=null) {
			return;
		}
		// Note: we've already done a get if ! overwrite. This method assumes you're trying to overwrite
		Object old;
		try {
			old = get(desc);
		} catch(Throwable ex) {
			// oh well
			Log.e(TAG, ex);
			return;
		}
		if (old==null) return;
		if (old == artifact) return;
		try {
			Desc.descCache.unbind(old, null);
		} catch(Throwable ex) {
			// paranoia!
			Log.escalate(ex);
		}
		if (old instanceof ILifeCycle) {	// get() should screen out any dead objects, but there is a race condition
			ILifeCycle old2 = (ILifeCycle) old;
			if ( ! old2.isLive()) {
				return;
			}
		}		
		Desc oldDesc = null;
		if (old instanceof IHasDesc) {
			oldDesc = ((IHasDesc) old).getDesc();
		}
		Log.escalate(new INotOverwritable.OverWriteException("Overwrite! "+desc+" = "+artifact+" overwrites " +oldDesc+" = "+ old));
		Log.d(TAG, "Carrying on with overwrite... "+desc);
	}

	/**
	 * See {@link ConcurrentMap#putIfAbsent(Object, Object)}
	 * @param desc
	 * @param artifact
	 * @return previous (& still valid) value for desc if the put failed, or null if the put took place.
	 */
	public <X> X putIfAbsent(Desc<X> desc, X artifact) {
		X old = put2(desc, artifact, KOverwrite.USE_EXISTING);
		
		// descs could differ due to sym links
		// // NB: This WILL get triggered if safetySyncDescs() modified the desc
//		// TODO remove Bug hunting Apr 2014
		if (old instanceof IHasDesc) {
			Desc oldDesc = ((IHasDesc) old).getDesc();
			if ( ! desc.equals(oldDesc)) {
				Log.w(TAG, "Desc mismatch in putIfAbsent()?! old:"+oldDesc+" new: "+desc);
			}
//			assert ((IHasDesc) old).getDesc().equals(desc) : old+" != "+desc;
		}
		
		return old; 
	}



	private Depot(DepotConfig config) {
		// initialise DescCache?
		synchronized (DescCache.class) {
			if (Desc.descCache==null) {
				new DescCache();
			}					
		}
		assert Desc.descCache!=null;
		
		this.config = config;
		base = config.getStore(this);
		// a default merger for maps
		setMerger(Map.class, new MapMerger());
		Log.i(TAG+".init", "Depot "+base);
	}

	/**
	 * @warning NOT thread safe, and must not be used concurrently with #getInstance
	 * @param name
	 * @param base
	 * @return 
	 */
	public static Depot newInstance(String name, DepotConfig config) {
		Depot depot = new Depot(config);
		Depot old = instances.put(name, depot);
		if (old!=null) {
			FileUtils.close(old);
		}
		return depot;
	}
	
	
	
	public static Depot getInstance(String name) {
		Depot depot = instances.get(name);
		return depot;
	}
	
	static final Map<String,Depot> instances = new HashMap();

	/**
	 * Flush then close.
	 */
	@Override
	public void close() {
		flush();
		if (base instanceof Closeable) {
			FileUtils.close((Closeable) base);
		}
	}

	public boolean contains(Desc itemDesc) {
		return base.contains(itemDesc);
	}

	/**
	 * Do not rely on this! Call close() yourself.
	 */
	@Override
	protected void finalize() throws Throwable {
		close();
	}

	/**
	 * Get a data artifact if possible. May be fetched from cache or possibly
	 * freshly created. Freshly created artifacts may be cached.
	 * <p>
	 * <b>Thread Safety</b>: if desc1 equals desc2, then get(desc1) == get(desc2).
	 * To ensure this, get() locks on the desc during object-loading (which can involve
	 * a remote call!).
	 * 
	 * @param desc If the get is successful, this will be bound to the returned
	 *            artifact.
	 * @return the artifact or null if unknown
	 * 
	 *         WARNING: may swallow exceptions when serialisation goes wrong and
	 *         return null!! This is to make non backwards compatible code
	 *         changes more graceful.
	 */
	public <X2> X2 get(Desc<X2> desc) throws WrappedException {		
		StopWatch sw = new StopWatch();
		// get it
		X2 x;
		locker.lock(desc);
		try {			
			x = get2(desc);		
			// binding
			if (x!=null) {
				if ( ! Depot.locker.isHeldByCurrentThread(desc)) {
					Log.escalate(new IllegalStateException("Depot.get bind without lock for "+desc+" -> "+x));
				}						
				// Bind desc<->artifact
				boolean bindMod = desc.bind(x);
				if (x instanceof IHasDesc) {
					// and it's own version of Desc (which needn't be the same object at all).
					// If desc is a sym-link, then myDesc will likely be different -- but should have been bound to x already.
					// So this bind will be a no-op.
					Desc myDesc = ((IHasDesc) x).getDesc();
					boolean bmod = myDesc.bind(x);
					
//					// TODO delete debugging overwriteexceptions
//					if (bmod && "CreoleDocModel".equals(x.getClass().getSimpleName())) {
//						Log.d(TAG, "get mydesc "+desc+System.identityHashCode(myDesc)+" bind "+x+" "+ReflectionUtils.getSomeStack(20));	
//					}				
				}
				
//				// TODO delete debugging overwriteexceptions
//				if (bindMod && "CreoleDocModel".equals(x.getClass().getSimpleName())) {
//					Log.d(TAG, "get "+desc+System.identityHashCode(desc)+" bind "+x+" "+ReflectionUtils.getSomeStack(20));	
//				}				
			}
		} finally { 
			locker.unlock(desc);
		}
		DataLog.mean(sw.getTime(), "Depot.get");
		return x;
	}
	
	/**
	 * 
	 * @param x Can be null
	 * @return x-as-Desc, or null if x is not a sym-link (the normal case)
	 */
	Desc isSymLink(Object x) {
		if (x instanceof Desc) return (Desc) x;
		if ( ! (x instanceof File)) return null;
		File file = (File) x;
		if ( ! file.isFile()) return null;
		try {
			// HACK: It could be a Desc xml -- let's sniffing it
			BufferedReader r = FileUtils.getReader(file);
			String line = r.readLine();
			r.close();
			String marker = "<"+Desc.class.getName();
			if (line.startsWith(marker)) {
				Desc d = FileUtils.load(file);
				return d;
			}
			return null;
		} catch(Exception ex) {
			return null;
		}
	}
	
	private <X2> X2 get3_bound(Desc<X2> desc) {
		X2 bv = desc.getBoundValue();
		if (bv==null) return null;
		// old?
		if (bv instanceof ILifeCycle) {
			if ( ! ((ILifeCycle) bv).isLive()) {
				// this can happen, as kill() does not unbind
				Log.w(TAG, "Bound to the dead! "+desc+" "+bv);
				return null;
			}
		}
		// sym link?
		if (bv instanceof Desc) {
			assert desc.symlink : desc+" -> "+bv;
			assert ! bv.equals(desc) : desc;					
			return (X2) get((Desc)bv);
		}
		assert ReflectionUtils.isa(bv.getClass(), desc.getType()) : bv.getClass()+" not a "+desc.getType()+" for "+desc;
		DataLog.count(1,"Depot","cache_hit","bound");
//		Log.v("depot.got.bound", desc); //verbose get log (normally ignored)
		return bv;
	}
	
	<X2> X2 get3_cached(Desc<X2> desc) {
		DescCache dc = (DescCache)Desc.getDescCache();
		X2 bv = (X2) dc.getArtifact(desc);
		if (bv==null) return null;
		// old?
		if (bv instanceof ILifeCycle) {
			if ( ! ((ILifeCycle) bv).isLive()) {
				// this can happen, as kill() does not unbind
				Log.w(TAG, "Bound to the dead cached! "+desc+" "+bv);
				return null;
			}
		}
		assert ReflectionUtils.isa(bv.getClass(), desc.getType()) : bv.getClass()+" not a "+desc.getType()+" for "+desc;
		DataLog.count(1,"Depot","cache_hit","DescCache");
//		Log.v("depot.got.cache", desc); //verbose get log (normally ignored)				
		return bv;		
	}
	
	
	private void safetySyncDescs(Desc realDesc, IHasDesc artifact) {
		if (realDesc.symlink) return;
		Desc myDesc = artifact.getDesc();
		// no problem?
		if (myDesc==null || myDesc.equals(realDesc)) return;
		Log.escalate(new IllegalStateException("Desc mismatch: artifact-desc: "+myDesc+" != depot-desc: "+realDesc));
		// HACK try to correct this		
		try {
			// Does this happen?? Are we maybe alternating setting a summary desc then a real dec??
			if (realDesc.getId().length() < myDesc.getId().length()) {
				Log.escalate(new IllegalStateException("Desc too long?! artifact-desc: "+myDesc+" != depot-desc: "+realDesc));
			}
			ReflectionUtils.setPrivateField(artifact, "desc", realDesc);
		} catch(Throwable ex) {
			Log.escalate(ex);
			try {
				ReflectionUtils.setPrivateField(myDesc, "id", realDesc.getId());
			} catch(Throwable ex2) {
				Log.e(TAG, ex2);
			}
		}
		assert myDesc.equals(realDesc);		
	}
	
	// NB: ?? bind is done in get not here??
	private <X2> X2 get2(final Desc<X2> desc) {
		assert locker.isHeldByCurrentThread(desc); 
		// Already bound?
		X2 bv = get3_bound(desc);
		if (bv!=null) return bv;
		// DescCache?
		bv = get3_cached(desc);
		if (bv!=null) return bv;
		// From the store...
		KErrorPolicy popPolicy = ModularConverter.setOnNotFound(KErrorPolicy.THROW_EXCEPTION);
		try { // Do we need 2 levels of try?? But the catches here could be nasty if mis-triggered
			X2 x = base.get(desc);			
			
			// Fail?
			if (x==null) {
				// ask user?
				if (config.errorPolicy==KErrorPolicy.ASK && desc.getType()==File.class) {
					File f = GuiUtils.selectFile(desc.toString(), FileUtils.getWinterwellDir());
					if (f != null && f.exists()) {
						put(desc, (X2)f);
						// fast-flush
						flush();
						return base.get(desc);
					}
				}
//				Log.w(TAG, "Could not locate " + desc);
				DataLog.count(1,"Depot","fail");
				return null;
			}
			
			// Is it a sym-link? (or an error)			
			Desc symLink = isSymLink(x); 
			if (symLink!=null) {
				if (desc.equals(symLink)) {
					throw new StackOverflowError(symLink+" points to itself. This will end badly.");
				}
				Object x2 = get(symLink);
//				if (x2!=null) Log.v("depot.got.link", symLink); //verbose get log (normally ignored)
				return (X2) x2;
			}

			// Safety check: class cast
			if ( ! ReflectionUtils.isa(x.getClass(), desc.getType())) {
				throw new ClassCastException("Class mismatch: "+x.getClass()+" not a "+desc.getType()+" for "+desc);
			}
			// Safety check: don't raise the dead
			if (x instanceof ILifeCycle) {
				if ( ! ((ILifeCycle) x).isLive()) {
					Log.w("depot.lifecycle", "Not resurrecting dead "+x);
					return null;
				}
			}
			// Safety check: what if the object's own Desc is wrong?
			if (x instanceof IHasDesc) {
				safetySyncDescs(desc, (IHasDesc) x);
			}
								
			desc.history = ReflectionUtils.stacktrace();
			// TODO metadata						
			
			// Do re-setup on re-inflation?
			if (x instanceof IInit) {
				((IInit) x).init();
			}
			
//			if (x!=null) Log.v("depot.got", desc); //verbose get log (normally ignored)
			return x;
			
		} catch (Throwable e) {						
			switch (config.errorPolicy) {
			case DELETE_CAUSE:
				// Can we move the bogus raw xml?
				String xml = base.getRaw(desc);
				if (xml!=null) {
					Desc moved = new Desc(desc);
					moved.setType(String.class);
					moved.setTag("error");
					moved.put("ex", e.getClass().getSimpleName());
					base.put(moved, xml);						
				}
				// Actually ONLY delete on certain known bugs, such as XStream errors.
				// Do not delete files due to some bad code (or a broken publish, as happened on egan May 2012)
				if (e instanceof XStreamException || e instanceof AssertionError) { // any other types??
					Log.e(TAG, "Deleting "+desc+": " + e);
					base.remove(desc);
				} else {
					Log.e(TAG, "NOT deleting "+desc+" for " + Printer.toString(e, true));
				}
			case RETURN_NULL: case IGNORE:
				Log.report(TAG, "Returning null; Error reading " +desc
						+ ": " + e, Level.SEVERE);
				return null;
			case ASK:
				// For convenient uploading of missing files
				if (desc.getType()==File.class) {
					File f = GuiUtils.selectFile(desc.toString(), FileUtils.getWinterwellDir());
					if (f != null && f.exists()) {
						put(desc, (X2)f);
						return base.get(desc);
					}
				}
			case DIE:
				Log.report(TAG, "Suicide!", Level.SEVERE);
				e.printStackTrace();
				System.exit(1);
			case THROW_EXCEPTION: case ACCEPT:
				throw Utils.runtime(e);
			case REPORT:
				Log.e(TAG, e);
				return null;
			}
			throw Utils.runtime(e);
		} finally {
			// Not really important, but we may as well be tidy
			ModularConverter.setOnNotFound(popPolicy);
		}
	}

	@Override
	public String getRaw(Desc desc) {
		return base.getRaw(desc);
	}

	/**
	 * Delete the cached version if there is one. If this is a symlink, only the symlink is deleted.
	 * 
	 * ??Does NOT remove sub-modules.
	 * 
	 * @param deadConfig
	 * @return true if the artifact was in cache (and has now been deleted)
	 */
	public void remove(Desc deadConfig) {
		locker.lock(deadConfig);
		try {
//			Log.v("depot.remove", deadConfig); //verbose get log (normally ignored)
			Object old = deadConfig.getBoundValue();
			// unbind
			deadConfig.unbind();
			assert deadConfig.boundValue==null : deadConfig;
			// Remove from depot's storage base
			base.remove(deadConfig);
			
			// sanity check TODO remove
			if (old!=null) {
				assert deadConfig.getBoundValue() != old : old;		
				IDescCache dc = Desc.getDescCache();
				assert dc.getArtifact(deadConfig) != old : old;
	//			get(config) // this test could trigger a fresh fetch-from-remote
			}
		} finally {
			locker.unlock(deadConfig);
		}
	}
	

	/**
	 * Delete the cached version if there is one, plus any sub-modules.
	 * Note that sub-modules can be shared with other artifacts -- so this can have
	 * side-effects beyond the given artifact.
	 * 
	 * @param deadConfig
	 * @return true if the artifact was in cache (and has now been deleted)
	 */
	public void removeAll(IHasDesc dead) {
		Log.d(TAG, "remove all of "+dead+"...");
		Desc deadConfig = dead.getDesc();
		locker.lock(deadConfig);		
		try {			
			Object old = deadConfig.getBoundValue();
			// unbind
			deadConfig.unbind();
			assert deadConfig.boundValue==null : deadConfig;
			// Remove from depot's storage base
			Log.v("depot.remove", deadConfig); //verbose get log (normally ignored)
			base.remove(deadConfig);
			
			// Recursive purge of sub-modules
			IHasDesc[] ms = ((IHasDesc) dead).getModules();
			for (IHasDesc mod : ms) {
				try {
					removeAll(mod);
				} catch(Throwable ex) {
					if (config.allowModuleExceptions) {
						Log.e(TAG, ex);
					} else {
						throw Utils.runtime(ex);
					}
				}
			}
			
			// sanity check TODO remove
			if (old!=null) {
				assert deadConfig.getBoundValue() != old : old;		
				IDescCache dc = Desc.getDescCache();
				assert dc.getArtifact(deadConfig) != old : old;
	//			get(config) // this test could trigger a fresh fetch-from-remote
			}
		} finally {
			locker.unlock(deadConfig);
			Log.d(TAG, "removed all of "+dead);
		}
	}
	
	
	public synchronized void flush() {
		if (base instanceof Flushable) {
			try {
				((Flushable) base).flush();
			} catch (IOException e) {
				throw Utils.runtime(e);
			}
		}
	}
	
	/**
	 * 
	 * @param errPolicy
	 * @return this
	 */
	public Depot setErrorPolicy(KErrorPolicy errPolicy) {
		this.config.errorPolicy = errPolicy;
		return this;
	}

	/**
	 * never null
	 */
	final DepotConfig config;
	
	public static Depot getDefault() {
		Depot dflt = getInstance("default");
		if (dflt==null) {
			dflt = getDefault2();
		}
		return dflt;
	}

	private synchronized static Depot getDefault2() {
		Depot dflt = getInstance("default");
		if (dflt!=null) {
			return dflt;
		}
		// setup default
		DepotConfig dconfig = new DepotConfig();
		// Load any params we can find
		ArgsParser ap = new ArgsParser(dconfig);
		File propsFile = new File("config/Depot.properties");
		if (propsFile.exists()) {			
			ap.set(propsFile);
		}
		ap.setFromSystemProperties("Depot");
		dflt = newInstance("default", dconfig);
		return dflt;
	}

	@Override
	public Set<Desc> loadKeys(Desc partialDesc) {
		return base.loadKeys(partialDesc);
	}

	public MetaData getMetaData(Desc desc) {
		MetaData md = base.getMetaData(desc);
		assert md != null : desc;
		return md;
	}

	/**
	 * Low level access
	 * @return
	 */
	@Deprecated
	public IStore getBase() {
		return base;
	}

	/**
	 * Convenience for using Depot to cache expensive-to-calculate stuff
	 * @param klass
	 * @param tag
	 * @param name
	 * @param callable
	 * @return
	 */
	public static <X> X calc(boolean forceRecalc, Class<X> klass, String tag, String name, Callable<X> callable) {
		Desc<X> desc = new Desc(name, klass);
		desc.setTag(tag);
		Depot depot = Depot.getDefault();
		// cached?
		if ( ! forceRecalc) {
			X v = depot.get(desc);
			if (v!=null) return v;
		}
		try {
			X v = callable.call();
			depot.put(desc, v);
			return v;
		} catch (Exception e) {
			throw Utils.runtime(e);
		}
	}

	/**
	 * Set one Desc to point / redirect at another.<p>
	 * 
	 * Use case: you have a bland version-free desc, which you wish to point
	 * at the latest & greatest detailed & versioned object.
	 * You could save the same object twice, but then they could get out of sync.
	 * Instead, save a sym-link.
	 * 
	 * FIXME this doesn't work with remote files!
	 * 
	 * @param from
	 * @param src This is the "true" desc.
	 */
	public void putSymLink(Desc from, Desc src) {
		Log.v(TAG, "putSymLink", from, src);
		if (from.readOnly) throw new IllegalArgumentException(from+" is read-only");
		assert from != null && src != null;
		assert ! src.equals(from) : "loopy: "+src;
		// ?? maybe use actual symlinks in file-based IStores for a bit of extra speed??
		base.put(from, src);		
	}

	@Override
	public File getLocalPath(Desc desc) {
//		Log.v("depot.get.path", desc); //verbose get log (normally ignored)
		
		File path = base.getLocalPath(desc);
		
//		Log.v("depot.got.path", desc); //verbose get log (normally ignored)		
		return path;
	}

	public DepotConfig getConfig() {
		return config;
	}

	/**
	 * 
	 * @param desc
	 * @param before
	 * @param after
	 * @param latest
	 * @return The merged version of the artifact: the latest from the depot + the diff of (after - before).
	 */
	Object doMerge(Desc desc, Object before, Object after, Object latest) {
		assert after != null;
		IMerger m = mergers.get(desc.getType());
		if (m==null) {
			Log.e(TAG, "No merger! "+desc.getType()+" "+desc);
			return after;
		}
		return m.doMerge(before, after, latest);
	}
	
	ClassMap<IMerger> mergers = new ClassMap();

	public void setMerger(Class klass, IMerger m) {
		mergers.put(klass, m);
	}
	
	public ClassMap<IMerger> getMergers() {
		return mergers;
	}

	/**
	 * Like put(), but this will perform a merge if it can.
	 * You must call {@link Desc#markForMerge()} before editing the object in order to use this.
	 * @param desc
	 * @param copy
	 */
	public <Y> void update(Desc<Y> desc, Y artifact) {
		assert desc.getBefore() != null : desc;
		put2(desc, artifact, KOverwrite.MERGE);
	}
	
}
