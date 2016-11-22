/**
 * 
 */
package winterwell.web.fields;

import winterwell.utils.time.Dt;
import winterwell.utils.time.TimeUtils;

/**
 * @author daniel
 *
 */
public class DtField extends AField<Dt>{

	public DtField(String name) {
		super(name);
	}
	
	@Override
	public Dt fromString(String v) throws Exception {
		return TimeUtils.parseDt(v);
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
