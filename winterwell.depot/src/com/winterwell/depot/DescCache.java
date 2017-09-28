package com.winterwell.depot;

import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import com.winterwell.utils.containers.ListMap;
import com.winterwell.utils.log.Log;

/**
 * Use weak-references to manage the desc-artifact linking.
 * There is a 1-to-1 link between equals() Descs and their artifacts.
 * <p>
 * DescCache Guarantee: two equals (not necc. identical) Descs will always return 
 * the == identical object. 
 * <p>
 * NB: this has an interface in base to allow
 * for better project modularity, separating 
 * winterwell.base & winterwell.depot.
 * 
 * @testedby {@link DescCacheTest}
 * @author daniel
 */
class DescCache implements IDescCache {

	@Override
	public Object getArtifact(Desc desc) {
		WeakReference ref = desc2bound.get(desc);
		return ref==null? null : ref.get();
	}
	
	DescCache() {
		// wire up
		assert Desc.descCache == null; 
		Desc.descCache = this;
	}
	
	
	public <T> Desc<T> getDescription(T x) {
		assert x != null;
		assert ! (x instanceof Desc) : x;
		Desc desc = sharedObject2Desc.get(x);
		if (desc != null)
			return desc;
		
		// make/get one from the artifact?
		if (x instanceof IHasDesc) {
			desc = ((IHasDesc) x).getDesc();
			return desc;
		}
		
		// careless call with a list or similar?
		if (x.getClass().getPackage().getName().startsWith("java.util")
			|| x.getClass().isArray()) {
			throw new IllegalArgumentException("Probably bogus type for Depot: "+x);
		}
		return null;
	}

	/**
	 * This does not keep artifacts alive, but it does keep Descs alive as long
	 * as the object they are bound to.
	 * <p>
	 * Note: For most classes of interest, a Desc can never hold a strong reference to 
	 * the object it describes.
	 */
	static Map<Object, Desc> sharedObject2Desc =
	// new MapMaker().weakKeys().concurrencyLevel(4).makeMap();
	Collections.synchronizedMap(new WeakHashMap<Object, Desc>());

	/**
	 * This does not keep Descs (weak keys) or artifacts (WeakRef wrappers) alive.
	 * @see WeakHashMap
	 */
	static Map<Desc,WeakReference> desc2bound =
			Collections.synchronizedMap(new WeakHashMap());
	
	/**
	 * What's in the cache? For debugging use only.
	 */
	public Map<Class,List> getCurrentUsage() {
		Desc[] keys = desc2bound.keySet().toArray(new Desc[0]);
		ListMap usage = new ListMap();
		for (Desc desc : keys) {
			WeakReference bnd = desc2bound.get(desc);
			if (bnd==null) continue;
			Object obj = bnd.get();
			Class klass = desc.getType();
			usage.add(klass, obj);
		}
		return usage;
	}

	/**
	 * Monitor the size of the shared descriptions weak cache
	 * 
	 * @return
	 */
	public int getSharedDescriptionsSize() {
		return sharedObject2Desc.size();
	}

	@Override
	public <X> void unbind(X artifact, Desc<X> desc) {
		// TODO remove debug code
//		if (artifact!=null && "CreoleDocModel".equals(artifact.getClass().getSimpleName())) {
//			Log.d("debug", "DescCache.unbind "+desc+" from "+artifact+" "+ReflectionUtils.getSomeStack(20));	
//		}
		desc2bound.remove(desc);
		if (desc2bound.get(desc) != null) {
			// This can trigger on a harmless (?) race condition
		}
		if (artifact==null) return;
		assert ! (artifact instanceof WeakReference);
		assert ! (artifact instanceof Desc);
		sharedObject2Desc.remove(artifact);				
		if (desc==null || desc.boundValue==null) return;
		// sanity check
		Object v = desc.boundValue.get();
		assert v == null || v==artifact : desc+" "+artifact+" "+v;
	}


	@Override
	public <X> boolean bind(X artifact, Desc<X> desc) {
		if (artifact==desc.getBoundValue()) return false; // no-op
		// Don't bind symlinks (it can cause bugs -- they dont get unbound)
		if (desc.symlink) {
			return false;
		}
		// sanity checks / logging
		if ( ! Depot.locker.isHeldByCurrentThread(desc)) {
			Log.escalate(new IllegalStateException("bind without lock for "+desc+" -> "+artifact
					+" lock-info:"+Depot.locker.getLockInfo(desc)));
		}						
//		if (artifact!=null && "CreoleDocModel".equals(artifact.getClass().getSimpleName())) {
//			Log.d("debug", "DescCache.bind "+desc+System.identityHashCode(desc)+" to "+artifact+" "+ReflectionUtils.getSomeStack(20));	
//		}
		
		// Paranoid Safety Checks
		X oldArtifact = desc.getBoundValue();
		if (oldArtifact==null) { // quite likely -- Descs in get are usually fresh minted
			WeakReference bvr = desc2bound.get(desc);
			if (bvr!=null) oldArtifact = (X) bvr.get();
		}
		if (artifact instanceof INotOverwritable) {			
			if (oldArtifact!=null && oldArtifact != artifact) {
				if (oldArtifact instanceof ILifeCycle && ! ((ILifeCycle) oldArtifact).isLive()) {
					// OK, it was dead
					Log.d("DescCache", "Overwrite of dead artifact "+desc+", was "+oldArtifact);
				} else {
					Log.escalate(new INotOverwritable.OverWriteException(desc+" = "+artifact+" is going to overwrite "+oldArtifact));
					Log.d("DescCache", "Carrying on with overwrite of "+desc+", was "+oldArtifact+", to "+artifact);
				}
				// NB: unbind will happen below
			}
		}
		if (artifact instanceof ILifeCycle) {
			if ( ! ((ILifeCycle) artifact).isLive()) {
				throw new LifeCycleException(desc+" "+artifact);
			}
		}
		
		// clear out any old binding
		desc.unbind();
		if (oldArtifact!=null && oldArtifact!=artifact) {
			sharedObject2Desc.remove(oldArtifact);
		}
		
		// & in the darkness bind them
		desc.boundValue = new WeakReference(artifact);
		sharedObject2Desc.put(artifact, desc);			
		desc2bound.put(desc, new WeakReference(artifact));
		return true;
	}

}