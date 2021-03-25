package com.winterwell.youagain.client;

import com.winterwell.utils.AString;

/**
 * A reference to an item to be shared.
 * NB: These get further namespaced by app
 * @author daniel
 *
 */
public class ShareTarget extends AString {

	public ShareTarget(Class klass, CharSequence id) {
		super(klass.getSimpleName()+":"+id);
		assert ! (id instanceof ShareTarget);
	}
	
	public ShareTarget(CharSequence name) {
		super(name);
	}

}
