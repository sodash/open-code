package com.goodloop.data;

import java.math.BigDecimal;
import java.util.Map;

import com.winterwell.data.AThing;
import com.winterwell.utils.MathUtils;
import com.winterwell.utils.Printer;
import com.winterwell.utils.Utils;
import com.winterwell.utils.containers.ArrayMap;
import com.winterwell.utils.time.Time;
import com.winterwell.utils.web.IHasJson;

/**
 * Support values down to 0.01p (a hundredth of a pence)
 * (i.e. £0.10 CPM is the lowest value)
 */
public final class Money 
extends AThing // dubious -- no id, no url
implements Comparable<Money>, IHasJson {
	
	private static final long serialVersionUID = 1L;
	private Time start;
	private Time end;
	private int year;
	
	
	public void setPeriod(Time start, Time end) {
		this.start = start; //if (start!=null) put("start", start.toISOString());
		this.end = end;
		this.year = Utils.or(end, start).getYear();		
	}
	
	/**
	 * Conversion factor for turning £s into 100th-of-a-pennies 
	 */
	private static final BigDecimal P100 = new BigDecimal(10000);

	Money() {	
	}
	
	public KCurrency currency = KCurrency.GBP;
	/**
	 * Support values down to 0.01p (a hundredth of a pence)
	 */
	private long value100p;
	private transient BigDecimal _value;

	/**
	 * best store as a string too, as otherwise json conversion would likely be a source of bugs
	 */
	private String value;
		
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((currency == null) ? 0 : currency.hashCode());
		long v = getValue100p();
		result = prime * result + (int) (v ^ (v >>> 32));
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
		Money other = (Money) obj;
		if (currency != other.currency)
			return false;
		if (getValue100p() != other.getValue100p())
			return false;
		return true;
	}

	/**
	 * 
	 * @param gbp 
	 * @param value
	 */
	public Money(KCurrency currency, Number value) {
		this.currency = currency;
		this._value = MathUtils.cast(BigDecimal.class, value);
		this.value = _value.toPlainString();
		this.value100p = _value.multiply(P100).longValue();
	}

	public Money(KCurrency currency, String value) {
		this(currency, new BigDecimal(value));
	}

	/**
	 * 
	 * @param x
	 * @return a new MA for this + x, or this if x=0
	 */
	public Money plus(Money x) {
		if (x.isZero()) return this;
		if (currency!=null && x.currency!=null && currency != x.currency) {
			throw new IllegalArgumentException("Cannot plus across currency "+this+ " "+x);
		}
		return new Money(currency, getValue().add(x.getValue()));
	}

	
	/**
	 * 
	 * @param x
	 * @return a new MA for this * x, or this if x=1
	 */
	public Money multiply(BigDecimal x) {
		if (x.doubleValue()==1) return this;
		BigDecimal v2 = getValue().multiply(x);
		return new Money(currency, v2);
	}
	
	
	/**
	 * 
	 * @param x
	 * @return a new MA for this - x, or this if x=0
	 */
	public Money minus(Money x) {
		if (x.isZero()) return this;
		if (currency!=null && x.currency!=null && currency != x.currency) {
			throw new IllegalArgumentException("Cannot minus across currency "+this+ " "+x);
		}
		return new Money(currency, getValue().subtract(x.getValue()));
	}


	public boolean isZero() {
		return value100p==0;
	}

	public BigDecimal getValue() {
		if (_value==null) {
			init();
			_value = new BigDecimal(value100p).divide(P100);
		}
		return _value;
	}


	public static Money pound(double number) {
		return new Money(KCurrency.GBP, new BigDecimal(number));
	}

	

	@Override
	public String toString() {
		return "MonetaryAmount["
					+(currency==null? "" : currency.symbol) 
					+ value 
					+(name==null? "" : ", name=" + name)
					+ "]";
	}

	/**
	 * Correct for value vs value100p glitches
	 */
	@Override
	public void init() {
		super.init();
		// value
		if (value100p==0 && value!=null && ! "0".equals(value)) {
			value100p = new BigDecimal(value).multiply(P100).longValue();
		}
		if (value==null) {
			value = new BigDecimal(value100p).divide(P100).toPlainString();
		}
	}


	@Override
	public int compareTo(Money o) {
		return Long.compare(value100p, o.value100p);
	}


	@Override
	public Map<String,Object> toJson2() {
		return new ArrayMap(
			"currency", currency,
			"value", value
				);
	}

	public long getValue100p() {
		init();
		return value100p;
	}

	public KCurrency getCurrency() {
		return currency;
	}

	public static Money from100p(KCurrency c, Number v100p) {
		return new Money(c, MathUtils.cast(BigDecimal.class, v100p).divide(P100));
	}
	
}
