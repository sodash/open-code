package com.goodloop.data;

/**
 * // ISO 4217 £
 * https://www.iso.org/iso-4217-currency-codes.html
 * 
 * @author daniel
 *
 */
public enum KCurrency {

	GBP("£"), USD("$"),
	
	/**
	 * HACK: allow Money objects to also represent %s and other multipliers. 
	 * @deprecated Not actually but use with caution 
	 */
	MULTIPLY("x");
	
	public final String symbol;

	KCurrency(String symbol) {
		this.symbol = symbol;
	}
	
}
