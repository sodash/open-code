package com.winterwell.utils;

import java.util.ArrayList;
import java.util.List;

public final class VersionString 
// if you want to: implements Comparable<VersionString> 
{

	private final List bits;
	private final String version;

	public VersionString(String version) {
		this.version = version;
		String[] sbits = version.split("\\.");
		bits = new ArrayList();
		for (int i = 0; i < sbits.length; i++) {
			if (StrUtils.isInteger(sbits[i])) {
				bits.add(Integer.valueOf(sbits[i]));
			} else {
				bits.add(sbits[i]);
			}
		}
	}

	/**
	 * true if this is greater-than-or-equals to b
	 * @param b
	 * @return
	 */
	public boolean geq(String b) {
		VersionString vsb = new VersionString(b);
		return geq(vsb);
	}

	
	/**
	 * true if this is greater-than-or-equals to b
	 * @param b
	 * @return
	 */
	public boolean geq(VersionString vsb) {
		if (equals(vsb)) {
			return true;
		}
		for(int i=0; i<bits.size(); i++) {
			Object abi = bits.get(i);
			if (vsb.bits.size() <= i) {
				return true;
			}
			Object bbi = vsb.bits.get(i);
			int ci = Utils.compare(abi, bbi);
			if (ci==0) continue;
			return ci > 0; // a after b means this is greater than b
		}
		// ??
		return true;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((version == null) ? 0 : version.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		VersionString other = (VersionString) obj;
		if (version == null) {
			if (other.version != null)
				return false;
		} else if (!version.equals(other.version))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "v"+version;
	}

}
