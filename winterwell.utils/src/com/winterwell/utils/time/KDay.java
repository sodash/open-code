package com.winterwell.utils.time;

/**
 * ISO definitions, compatible with Date and Time classes
 * 0 = Sunday, 1=Monday ... 6=Saturday, 7=Sunday
 */
public enum KDay {
	Sunday, Monday, Tuesday, Wednesday, Thursday, Friday, Saturday;
	
	/**
	 * @return 0 or 7 = Sunday, 1 = Monday, etc.
	 */
	public static KDay valueOf(int weekday) {
		weekday = weekday % 7;
		return values()[weekday];
	}
}