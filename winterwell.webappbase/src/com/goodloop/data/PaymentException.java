package com.goodloop.data;

import com.winterwell.web.WebEx;

/**
 * Payment failure -- which we consider a type of user-input error hence WebEx code 400
 * @author daniel
 *
 */
public class PaymentException extends WebEx.E40X {

	private static final long serialVersionUID = 1L;

	public PaymentException() {
		this(null);
	}
	
	public PaymentException(String string) {
		super(400, null, string);
	}
	
	public static class DuplicateSpendException extends PaymentException {
		public DuplicateSpendException(String string) {
			super(string);
		}

		private static final long serialVersionUID = 1L;
		
	}
	public static class DuplicatePaymentException extends PaymentException {
		private static final long serialVersionUID = 1L;
		public DuplicatePaymentException(String string) {
			super(string);
		}
	}
}
