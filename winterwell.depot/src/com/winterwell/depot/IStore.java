package com.winterwell.depot;

import java.io.File;
import java.io.Flushable;
import java.util.List;
import java.util.Set;

import com.winterwell.utils.containers.Pair2;

/**
 * This is similar to Map (but type-safe) and to IProperties (but with Desc instead of Key).
 * 
 * Implementations should have a constructor that takes in a {@link DepotConfig} object.
 * A no-argument constructor will also work.
 * @author daniel
 *
 */
public interface IStore extends Flushable {

	/**
	 * Use-case: Damage limitation only. For handling damaged artifacts, which can't be de-seraliased.
	 * @param desc
	 * @return The raw serialised gunk, or null.
	 */
	String getRaw(Desc desc);
	
	void remove(Desc arg0);
	
	/**
	 * force an immediate save
	 */
	void flush();
	
	boolean contains(Desc desc);
	
	/**
	 * 
	 * @param desc Must not be null
	 * @param artifact Must not be null -- use remove() instead for that.
	 */
	<X> void put(Desc<X> desc, X artifact);
	
	/**
	 * @param desc
	 * @return artifact, or null
	 */
	<X> X get(Desc<X> desc);
	
	/**
	 * TODO not supported yet by the implementations!
	 * @param partialDesc Can be null
	 * @return keys for stored artifacts that match the partialDesc.
	 */
	@Deprecated
	Set<Desc> loadKeys(Desc partialDesc);

	/**
	 * @param desc
	 * @return never null -- may just have been made up though
	 */
	MetaData getMetaData(Desc desc);

	/**
	 * This does not involve any remote calls.
	 * @return path where the local File would be stored. May not exist.
	 * @throws UnsupportedOperationException if the store just don't swing that way.
	 */
	File getLocalPath(Desc desc) throws UnsupportedOperationException;

	/**
	 * Initialise database setup, etc.
	 */
	default void init() {
		// do nothing by default
	}

	/**
	 * Allows for efficient processing by e.g. ElasticSearch
	 * @param add
	 * @param remove
	 */
	default void storeBatch(List<Pair2<Desc, Object>> add, List<Desc> remove) {
		for (Desc desc : remove) {
			remove(desc);
		}
		for (Pair2<Desc, Object> desc_obj : add) {
			put(desc_obj.first, desc_obj.second);
		}
	}
	
}
