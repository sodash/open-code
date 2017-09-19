package com.winterwell.utils.threads;

import java.util.ArrayList;

/**
 * An actor that very efficiently ignores everything
 * @author daniel
 *
 */
public final class DeadLetterActor extends Actor {

	@Override
	public void send(Object msg, Actor sender) {
		// no-op
	}
	
	@Override
	protected void accept(Object msg, Actor from) {		
	}
	@Override
	protected void acceptBatch(ArrayList batch) throws Exception {	
	}
	
}
