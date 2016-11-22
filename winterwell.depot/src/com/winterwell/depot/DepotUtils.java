package com.winterwell.depot;


public final class DepotUtils {


	/**
	 * For easy appending of version info into log messages, etc.
	 * @param artifact
	 * @return " v2" (with a leading space), or ""
	 */
	public static String vStamp(Object artifact) {
		if (artifact==null) return "";
		if (artifact instanceof IHasVersion) {
			return " v"+ ((IHasVersion) artifact).getVrsn();
		}
		return "";
	}

}
