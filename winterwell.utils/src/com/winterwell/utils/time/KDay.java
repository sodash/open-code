package com.winterwell.utils.time;

/**
 * 0 = Sunday, 1=Monday ... 6=Saturday
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