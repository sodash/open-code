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
	 * Don't make these
	 */
	private Mutable() {}
	
	/**
	 * Mis-spelled Double to avoid name-clashes
	 */
	public static final class Dble {
		public volatile double value;

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			long temp;
			temp = Double.doubleToLongBits(value);
			result = prime * result + (int) (temp ^ (temp >>> 32));
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
			Dble other = (Dble) obj;
			if (Double.doubleToLongBits(value) != Double.doubleToLongBits(other.value))
				return false;
			return true;
		}

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
		public volatile boolean value;

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

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + (value ? 1231 : 1237);
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
			Bool other = (Bool) obj;
			if (value != other.value)
				return false;
			return true;
		}
		
	}

	public static final class Int {
		public volatile int value;

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

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + value;
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
			Int other = (Int) obj;
			if (value != other.value)
				return false;
			return true;
		}
		
	}

	/**
	 * Mutable wrapper for an object reference.
	 * 
	 * @see AtomicReference (this is lighter for single thread use)
	 */
	public static final class Ref<T> {
		public volatile T value;

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

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((value == null) ? 0 : value.hashCode());
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
			Ref other = (Ref) obj;
			if (value == null) {
				if (other.value != null)
					return false;
			} else if (!value.equals(other.value))
				return false;
			return true;
		}
		
	}

	/**
	 * Mis-spelled String to avoid name-clashes
	 */
	public static final class Strng {
		public volatile String value;

		public Strng() {
			this("");
		}

		public Strng(String v) {
			value = v;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((value == null) ? 0 : value.hashCode());
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
			Strng other = (Strng) obj;
			if (value == null) {
				if (other.value != null)
					return false;
			} else if (!value.equals(other.value))
				return false;
			return true;
		}

		@Override
		public String toString() {
			return value;
		}
	}
}
