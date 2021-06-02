/**
 * 
 */
package com.winterwell.web.fields;

import com.winterwell.utils.MathUtils;
import com.winterwell.utils.StrUtils;
import com.winterwell.utils.time.Dt;
import com.winterwell.utils.time.TUnit;
import com.winterwell.utils.time.TimeUtils;
import com.winterwell.web.WebEx;

/**
 * @author daniel
 * @testedby {@link DtFieldTest}
 */
public class DtField extends AField<Dt>{

	private static final long serialVersionUID = 1L;

	TUnit defaultUnit;
	
	public DtField setDefaultUnit(TUnit defaultUnit) {
		this.defaultUnit = defaultUnit;
		return this;
	}
	
	public DtField(String name) {
		super(name);
	}
	
	@Override
	public Dt fromString(String v) throws Exception {
		if (StrUtils.isNumber(v)) {
			if (defaultUnit==null) throw new WebEx.BadParameterException(getName(), v, new IllegalArgumentException("No unit set e.g. seconds"));
			double n = MathUtils.toNum(v);
			return new Dt(n, defaultUnit);			
		}
		Dt dt = TimeUtils.parseDt(v);
		// convert?
		if (defaultUnit != null) {
			dt = dt.convertTo(defaultUnit);
		}
		return dt;
	}

	@Override
	public String toString(Dt value) {
		return value.toString();
	}

	@Override
	public Class<Dt> getValueClass() {
		return Dt.class;
	}
}
