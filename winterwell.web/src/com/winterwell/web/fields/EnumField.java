package com.winterwell.web.fields;

import com.winterwell.utils.web.WebUtils;

/**
 * Option select field for enum values. A value of "false" will be returned as a
 * null.
 * 
 * @author Daniel
 */
public class EnumField<T extends Enum> extends AField<T> {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private final Class enumClass;

	public EnumField(Class<? extends Enum> enumClass, String name) {
		this(enumClass, name, SelectField.SELECT_TYPE);
	}

	public EnumField(Class<? extends Enum> enumClass, String name, String type) {
		super(name, type);
		this.enumClass = enumClass;
	}

	@Override
	public void appendHtmlTo(StringBuilder sb, T value, Object... attributes) {
		// hidden or other?
		if (!getType().equals(SelectField.SELECT_TYPE)) {
			super.appendHtmlTo(sb, value, attributes);
			return;
		}
		// Make a selecter
		sb.append("<select name='" + getName() + "' ");
		if (id != null) {
			sb.append("id='" + id + "' ");
		}
		if (onChange != null) {
			sb.append("onChange='" + WebUtils.attributeEncode(onChange) + "' ");
		}
		if (tooltip != null) {
			sb.append("title='" + WebUtils.attributeEncode(tooltip) + "' ");
		}
		addAttributes(sb, attributes);
		sb.append(">");
		for (Object o : enumClass.getEnumConstants()) {
			String os = o.toString();
			if (o == value) {
				sb.append("<option value='" + os + "' selected='true'>" + o
						+ "</option>");
			} else {
				sb.append("<option value='" + os + "'>" + o + "</option>");
			}
		}
		sb.append("</select>");
	}

	/**
	 * Use "false" to convey null
	 */
	@Override
	public T fromString(String v) {
		if ("false".equals(v))
			return null;
		return (T) Enum.valueOf(enumClass, v);
	}

}
