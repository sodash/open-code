package com.winterwell.depot;

import java.io.File;
import java.util.Set;

/**
 * Simplify building an IStore which is part of a chain.
 * @author daniel
 *
 */
public abstract class AStore implements IStore {

	@Override
	public String getRaw(Desc desc) {
		return base.getRaw(desc);
	}
	
	protected DepotConfig config;

	protected AStore(DepotConfig config, IStore base) {
		this.base = base;
		this.config = config;
	}

	protected IStore base;
	
	@Override
	public File getLocalPath(Desc desc) throws UnsupportedOperationException {	
		return base.getLocalPath(desc);
	}
	
	@Override
	public void remove(Desc desc) {
		base.remove(desc);
	}

	@Override
	public boolean contains(Desc desc) {		
		return get(desc) != null;
	}

	@Override
	public Set<Desc> loadKeys(Desc partialDesc) {
		Set<Desc> matches = base.loadKeys(partialDesc);
		return matches;
	}

	@Override
	public MetaData getMetaData(Desc desc) {
		return base.getMetaData(desc);
	}

	public IStore getBase() {
		return base;
	}

}
