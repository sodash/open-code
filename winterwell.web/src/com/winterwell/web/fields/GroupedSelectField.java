/**
 * 
 */
package com.winterwell.web.fields;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import com.winterwell.utils.web.WebUtils2;

/**
 * A SelectField whose options are grouped into &lt;optgroups&gt;.
 * 
 * @author miles
 * 
 */
public class GroupedSelectField<X> extends SelectField<X> {

	private static final long serialVersionUID = 1292387595046708949L;
	/**
	 * Magic string for the unset option (allows Javascript to re-select it)
	 */
	public static final String UNSET_VALUE = "!title";
	private Map<String, Collection<X>> optgroups;
	private String optionIfUnset;
	private Map<X, String> specialLabels = Collections.emptyMap();

	/**
	 * 
	 * @param name
	 *            Name of the field
	 * @param optionIfUnset
	 *            An optional label to display for when the user hasn't made a
	 *            selection. This will be given the magic value "!title".
	 * @param optgroups
	 */
	public GroupedSelectField(String name, String optionIfUnset,
			Map<String, Collection<X>> optgroups) {
		super(name);
		this.optionIfUnset = optionIfUnset;
		this.optgroups = optgroups;
	}

	@Override
	protected void appendOptions(StringBuilder sb, X value) {
		assert useValuesDirectly; // I think this would fail - DBW
		int i = 0;
		if (optionIfUnset != null) {
			sb.append("<option value='" + UNSET_VALUE + "' selected='"
					+ (value == null) + "'>" + optionIfUnset + "</option>");
		}
		for (String optgroup : optgroups.keySet()) {
			sb.append("<optgroup label='" + WebUtils2.htmlEncode(optgroup)
					+ "'>");
			Collection<X> opts = optgroups.get(optgroup);
			for (X option : opts) {
				String html = specialLabels.get(option);
				if (html == null) {
					html = getDefaultHtml(option);
				}
				appendOption(sb, value, i++, escapeSpaces(option), html);
			}
			sb.append("</optgroup>\n");
		}
	}

	/**
	 * ??Why is this needed?
	 * 
	 * @param value
	 * @return
	 */
	private X escapeSpaces(X value) {
		if (!(value instanceof String))
			return value;
		String strVal = (String) value;
		return (X) ((strVal.contains(" ")) ? "\"" + strVal + "\"" : strVal);
	}

	/**
	 * An empty map by default
	 * 
	 * @param specials
	 */
	public void setSpecialLabels(Map<X, String> specials) {
		assert specials != null;
		this.specialLabels = specials;
	}

}
