package com.winterwell.utils.threads;

import java.util.List;

import com.winterwell.utils.Utils;

/**
 * This is NOT normally needed. It adds a "to" to Actor.Packet.
 * Use-case: messages can be collected up, sifted, and then sent. 
 * @author daniel
 *
 */
public final class MsgToActor<M> {
	
	Actor<M> to;

	Actor.Packet<M> packet;

	private boolean posted;	
	
	public MsgToActor(Actor to, M msg, Actor sender) {
		Utils.check4null(to, msg);
		this.to= to;
		this.packet = new Actor.Packet(msg, sender);
	}
	
	public void post() {
		assert ! posted;
		to.send(packet);
		posted = true;
	}

	public static void postAll(List<MsgToActor> msgs) {
		if (msgs==null) return;
		for (MsgToActor msgToActor : msgs) {
			msgToActor.post();
		}
	}
}
