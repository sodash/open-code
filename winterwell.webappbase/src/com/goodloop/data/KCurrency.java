package com.goodloop.data;

/**
 * // ISO 4217 £
 * @author daniel
 *
 */
public enum KCurrency {

	GBP("£"), USD("$");
	
	public final String symbol;

	KCurrency(String symbol) {
		this.symbol = symbol;
	}
	
}
