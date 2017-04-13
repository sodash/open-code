package com.winterwell.maths.stats.distributions.cond;

/**
 * The situation is an outcome + a context, e.g. "frog" | previous-word="green",
 * tag=animals
 * 
 * 
 * @param <X> The outcome type (the focus of this Sitn). Usually String.
 * @author daniel
 */
public final class Sitn<X> {

	/**
	 * E.g. for a sentence, this might be the previous word.
	 */
	public final Cntxt context;

	/**
	 * E.g. for a sentence, this would be the word under consideration.
	 */
	public final X outcome;

	/**
	 * 
	 * @param outcome
	 *            Can be null
	 * @param bits
	 */
	public Sitn(X outcome, Cntxt context) {
		assert context != null;
		assert outcome != null;
		this.context = context;
		this.outcome = outcome;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Sitn other = (Sitn) obj;
		return outcome.equals(other.outcome) && context.equals(other.context);
	}

	public Cntxt getContext() {
		return context;
	}

	@Override
	public int hashCode() {
		return 31 * outcome.hashCode() + context.hashCode();
	}

	@Override
	public String toString() {
		if (context.sig.length == 0)
			return "Sitn[" + outcome + "]";
		return "Sitn[" + outcome + " | " + context + "]";
	}

}