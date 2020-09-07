package com.goodloop.data;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import com.winterwell.data.AThing;
import com.winterwell.depot.IInit;
import com.winterwell.es.ESType;
import com.winterwell.gson.StandardAdapters.LenientLongAdapter;
import com.winterwell.utils.MathUtils;
import com.winterwell.utils.Utils;
import com.winterwell.utils.containers.ArrayMap;
import com.winterwell.utils.time.Time;
import com.winterwell.utils.web.IHasJson;

/**
 * Support values down to 0.01p (a hundredth of a pence)
 * (i.e. £0.10 CPM is the lowest value)
 */
public class Money 
//extends AThing // dubious -- no id, no url, and we dont store these as top-level data objects TODO dont extend this
implements Comparable<Money>, IHasJson, IInit {
	
	public static final ESType ESTYPE = new ESType().object()
			// core
			.property("currency", new ESType().keyword())
			.property("value", new ESType().keyword())
			.property("value100p", new ESType().LONG())
			// old data!
			.property("value100", new ESType().DOUBLE()) 
			// less core
			.property("@type", new ESType().keyword().noAnalyzer())			
			.property("start", new ESType().date())
			.property("end", new ESType().date())
			.property("year", new ESType().INTEGER())
			.lock();
	
	private static final long serialVersionUID = 1L;
	
	// FIXME are these used??
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

	protected Money() {	
	}
	
	public KCurrency currency = KCurrency.GBP;
	/**
	 * Support values down to 0.01p (a hundredth of a pence).
	 * This is the canonical value of the Money object.
	 * 
	 * NB: You are advised to use {@link LenientLongAdapter} in Gson to avoid floating point errors.
	 */
	private long value100p;
	
	@Deprecated // HACK to upgrade old objects
	private Object value100;
	
	private transient BigDecimal _value;

	/**
	 * best store as a string too, as otherwise json conversion would likely be a source of bugs
	 */
	private String value;
	
	// The client uses raw to hold an interim value whilst the user is typing. I don't think we need to store it. ^DW June 2019
//	@Deprecated
//	private String raw;
		
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
	 * Copy currency, value, name
	 * @param copyMe
	 */
	public Money(Money copyMe) {
		this(copyMe.getCurrency(), copyMe.getValue());
		this.name = copyMe.getName();
	}
	
	String name;
	
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * 
	 * @param x
	 * @return a new MA for this + x, or this/x if x/this = 0
	 */
	public Money plus(Money x) {
		if (x.isZero()) return this;
		if (isZero()) return x;
		if (x.currency==KCurrency.MULTIPLY) {
			// HACK
			return multiply(x.getValue());
		}
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
	public Money multiply(Number x) {
		if (x.doubleValue()==1) return this;
		// is this the best way to make a BigDecimal??
		BigDecimal bdx = x instanceof BigDecimal? (BigDecimal) x : new BigDecimal(x.toString());
		BigDecimal v2 = getValue().multiply(bdx);
		return new Money(currency, v2);
	}
	
	
	/**
	 * 
	 * @param x
	 * @return a new Mony for this * x, or this if x=1
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

	

	/**
	 * e.g. £10
	 */
	@Override
	public String toString() {
		return (currency==null? "" : currency.symbol) 
					+ value 
//					+(name==null? "" : ", name=" + name)					
					;
	}

	/**
	 * Correct for value vs value100p glitches
	 */
	@Override
	public void init() {
//		super.init();
		try {
			// value
			if (value100p==0 && value!=null && ! "0".equals(value)) {
				value100p = new BigDecimal(value).multiply(P100).longValue();
			}
			// HACK old format (this code added Apr 2018)
			if (value100p==0 && value==null && value100!=null) {
				value100p = new BigDecimal(value100.toString()).multiply(new BigDecimal(100)).longValue();
			}
			value100 = null;
			// end hack
			if (value==null) {
				value = new BigDecimal(value100p).divide(P100).toPlainString();
			}
		} catch(NumberFormatException ex) {
			// add info
			throw new NumberFormatException(ex+" from "+toJson2());
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
			"value", value,
			"value100p", value100p
				);
	}

	/**
	 * @return value in hundredth of a pence, e.g. £1 = 10000 !
	 * This is a "high precision" int, stored to support database arithmetic.
	 */
	public long getValue100p() {
		init();
		return value100p;
	}

	public KCurrency getCurrency() {
		return currency;
	}

	/**
	 * 
	 * @param c
	 * @param v100p value in hundredth of a pence, e.g. £1 = 10000 !
	 * @return
	 */
	public static Money from100p(KCurrency c, Number v100p) {	
		Utils.check4null(v100p);
		BigDecimal bd = MathUtils.cast(BigDecimal.class, v100p);
		return new Money(c, bd.divide(P100));
	}

	public static Money total(List<Money> costs) {
		Money total = new Money();
		for (Money money : costs) {
			total = total.plus(money);
		}
		return total;
	}

	
}
