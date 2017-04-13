package com.winterwell.maths.stats.distributions.cond;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Map;

import com.winterwell.utils.Printer;
import com.winterwell.utils.StrUtils;
import com.winterwell.utils.Utils;
import com.winterwell.utils.containers.Containers;

/**
 * Context for a conditional probability. This is a feature map.
 * <p>
 * What goes in a context?
 * The signature is the keys. For e.g. a text classifier, the sig might be
 *    [previous-word, author]
 * But not `word` - that would be the outcome part of the Sitn
 * <p>
 * Cntxt objects are immutable. Or to be precise, they should be treated as immutable.  
 * For performance reasons (you might make a *lot* of these), they use and expose arrays.
 * 
 * @author daniel
 */
public final class Cntxt implements Serializable {
	/**
	 * No context -- use this for "wrapping" unconditional outcomes.
	 */
	public static final Cntxt EMPTY = new Cntxt(new String[0]);

	private static final long serialVersionUID = 1L;

	/**
	 * Return a new Cntxt. 
	 * Must NOT be called on the blind uniform distribution.
	 * 
	 * @param context
	 * @param signature
	 * @param fixedFeatures Are some of the signature elements fixed here? This maps
	 * feature-name to feature-value, over-writing the value (if any) in this context.
	 * values will be put into the processed version of Cntxt. Can be null.
	 */
	public Cntxt pareDown(String[] signature, Map<String,Object> fixedFeatures) {
		final Cntxt context = this;
		// right length anyway?
		if (context.bits.length == signature.length && Utils.isEmpty(fixedFeatures)) {
			assert Arrays.equals(signature, context.sig)
						: context+" v "+Printer.toString(signature);
			return context;
		}
		// make a simpler context
		Object[] bits2 = new Object[signature.length];
		int csi=0;
		for(int msi=0; msi<signature.length; msi++) {
			String msis = signature[msi];
			if (fixedFeatures!=null) {
				Object v = fixedFeatures.get(msis);	
				if (v!=null) {
					bits2[msi] = v;
					continue;
				}
			}
			boolean ok = false;
			for(; csi<context.sig.length; csi++) {
				String csis = context.sig[csi];
				if (msis.equals(csis)) {
					bits2[msi] = context.bits[csi];
					ok = true;
					break;
				}
			}
			if ( ! ok) throw new IllegalArgumentException(
					"sig:"+Printer.toString(signature)+" Original:"+context+" Pared:"+bits2);
		}		 
		Cntxt sitn = new Cntxt(signature, bits2);
		return sitn;
	}
	
	
	/**
	 * Matches with {@link #sig}
	 */
	final Object[] bits;

	/**
	 * What object did this come from? E.g. for a text context (which might be
	 * the previous-word), this would be the original document.
	 * <p>
	 * Usage: this allows the raw object to be passed down through a couple of
	 * layers of intervening model classes, then have extra info extracted from
	 * it.
	 */
	public final transient Object raw;

	/**
	 * Describes what the bits are. This is needed for paring-down operations
	 */
	final String[] sig;

	/**
	 * @param provideRaw Ignored. Here to help avoid ambiguity over which
	 *            constructor to call.
	 * @param raw
	 * @param signature
	 * @param bits
	 */
	public Cntxt(boolean provideRaw, Object raw, String[] signature,
			Object... bits) {
		this.raw = raw;
		this.sig = signature;
		this.bits = bits;
		assert signature != null;
		assert bits.length == signature.length : signature.length + ":"
				+ Printer.toString(signature) + " vs " + bits.length + ":"
				+ Printer.toString(bits);
	}

	/**
	 * 
	 * @param signature
	 * @param bits
	 */
	public Cntxt(String[] signature, Object... bits) {
		this(false, null, signature, bits);
	}

	/**
	 * equals if same sig and bits
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;
		Cntxt other = (Cntxt) obj;
		if ( ! Arrays.equals(sig, other.sig)) {
			return false;
		}
		return Arrays.equals(bits, other.bits);
	}

	/**
	 * Fetch a bit of context by name
	 * 
	 * @param key must be in the signature
	 * @return bit for key
	 */
	public Object get(String key) {
		int i = Containers.indexOf(key, sig);
		assert i != -1 : key + " not in " + Printer.toString(sig) + " in "
				+ this;
		return bits[i];
	}

	/**
	 * Low-level test/debug access to internal bits
	 */
	@Deprecated
	public Object[] getBits() {
		return bits;
	}

	/**
	 * @return this context's signature (what features it contains).
	 * Do NOT edit signature arrays -- they are shared between objects.
	 * @warning: The order is important! Models can assume a canonical ordering, to allow for efficient access.
	 */
	public String[] getSignature() {
		return sig;
	}

	@Override
	public int hashCode() {
		return Arrays.hashCode(bits);
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder("Cntxt[");
		for (int i = 0; i < sig.length; i++) {
			sb.append(sig[i] + ":" + bits[i] + ", ");
		}
		if (sig.length != 0) StrUtils.pop(sb, 2);
		sb.append(']');
		return sb.toString();
	}
}
