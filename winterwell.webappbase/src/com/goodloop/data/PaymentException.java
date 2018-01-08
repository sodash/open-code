package com.goodloop.data;

public class PaymentException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public PaymentException() {		
	}
	
	public PaymentException(String string) {
		super(string);
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
