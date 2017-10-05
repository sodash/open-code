package com.winterwell.utils.threads;

import java.util.List;
import java.util.function.BiConsumer;

import com.winterwell.utils.threads.Actor.Packet;

/**
 * Allows Actor to be separated from the actual processing.
 * 
 * Note: some actors need access to the Actor, in order to communicate with each other.
 * In which case, they should override {@link Actor#accept(Packet)} instead.
 * 
 * @author daniel
 *
 * @param <Msg>
 */
public interface IActorMsgConsumer<Msg> extends BiConsumer<Msg, Actor> {

	/**
	 * Receive a message! This is where the actor does it's thing.
	 * 
	 * @param msg
	 * @param sender
	 *            Can be null (e.g. if the message was sent by a non-Actor
	 *            class)
	 */
	@Override
	void accept(Msg msg, Actor sender);	
	
	default void acceptBatch(List<Packet<Msg>> batch) throws Exception {
		for (Packet<Msg> packet : batch) {
			accept(packet.msg, packet.from);
		}
	}
	
}
