/**
 * 
 */
package winterwell.web.fields;

import java.util.Map;

import winterwell.utils.StrUtils;
import com.winterwell.utils.containers.ArrayMap;
import winterwell.utils.time.TUnit;

/**
 * A TUnit field. I don't know whether this is really useful or not.
 * 
 * @author Joe Halliwell <joe@winterwell.com>
 * 
 */
public class TUnitField extends SelectField<TUnit> {

	private static final Map<TUnit, String> defaultMap = getUnitMap();

	private static final long serialVersionUID = 1L;

	/**
	 * Make a map of units and display names
	 * 
	 * @return
	 */
	private static ArrayMap<TUnit, String> getUnitMap() {
		ArrayMap<TUnit, String> unitMap = new ArrayMap<TUnit, String>();
		for (TUnit unit : TUnit.values()) {
			unitMap.put(unit,
					StrUtils.toTitleCase(unit.toString().toLowerCase()));
		}
		return unitMap;
	}

	/**
	 * Construct a unit field with the specified name
	 * 
	 * @param name
	 */
	public TUnitField(String name) {
		super(name, defaultMap);
	}

}
