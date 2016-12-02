package com.winterwell.utils;

import java.util.concurrent.atomic.AtomicReference;

/**
 * Mutable wrappers for the primitive types. These can be used as arguments to a
 * method, allowing multiple optional return values.
 * 
 * @author daniel
 * 
 */
public final class Mutable {

	/**
	 * Mis-spelled Double to avoid name-clashes
	 */
	public static final class Dble {
		public double value;

		public Dble() {
			this(0);
		}

		public Dble(double v) {
			value = v;
		}

		@Override
		public String toString() {
			return "" + value;
		}
	}


	public static final class Bool {
		public boolean value;

		public Bool() {
			this(false);
		}

		public Bool(boolean v) {
			value = v;
		}

		@Override
		public String toString() {
			return "" + value;
		}
	}

	public static final class Int {
		public int value;

		public Int() {
			this(0);
		}

		public Int(int v) {
			value = v;
		}

		@Override
		public String toString() {
			return Integer.toString(value);
		}
	}

	/**
	 * Mutable wrapper for an object reference.
	 * 
	 * @see AtomicReference (this is lighter for single thread use)
	 */
	public static final class Ref<T> {
		public T value;

		/**
		 * A mutable object reference with null value.
		 */
		public Ref() {
		}

		public Ref(T value) {
			this.value = value;
		}

		@Override
		public String toString() {
			return Printer.toString(value);
		}
	}

	/**
	 * Mis-spelled String to avoid name-clashes
	 */
	public static final class Strng {
		public String value;

		public Strng() {
			this("");
		}

		public Strng(String v) {
			value = v;
		}

		@Override
		public String toString() {
			return value;
		}
	}
}
