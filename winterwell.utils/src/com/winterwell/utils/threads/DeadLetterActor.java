package com.winterwell.utils.threads;

import java.util.ArrayList;

/**
 * An actor that very efficiently ignores everything
 * @author daniel
 *
 */
public final class DeadLetterActor extends Actor {

	public static final Actor dflt = new DeadLetterActor();
	
	@Override
	public void send(Object msg, Actor sender) {
		// no-op
	}
	
	@Override
	protected void consume(Object msg, Actor from) {		
	}
	@Override
	protected void consumeBatch(ArrayList batch) throws Exception {	
	}
	
}
