package winterwell.web.fields;

import java.util.Arrays;

import javax.servlet.http.HttpServletRequest;

import winterwell.utils.Utils;
import winterwell.web.WebInputException;

/**
 * Uses the values "true" and "false". Missing is interpreted as false. This
 * means Checkboxes never return null in normal usage!
 * <p>
 * They also accept "unknown", which does return as null.
 * <p>
 * {@link #isSet(HttpServletRequest)} can be used to check for non-nullness -
 * but this is only valid if handling input where you expect "false" to be
 * explicitly specified.
 * 
 * @see BoolField which provides true/false/null
 * @author daniel
 * @testedyb {@link CheckboxTest}
 */
public class Checkbox extends AField<Boolean> {

	/**
	 *
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * @deprecated Use {@link Utils#yes(Object)} instead
	 * A generous true/false interpreter -- can handle on/off, true/false, 1/0. 
	 * <p>
	 * Note: This method does null=>null. But the Checkbox field itself, does null=>false.
	 * 
	 * @param s
	 *            can be null or empty
	 * @return true/false/null (only returns null for s=null or s=unknown)
	 * @see TrueFalseUnknown
	 */
	public static Boolean convert(String s) {
		if (s == null)
			return null;
		s = s.toLowerCase();
		// "on" will be sent by a value-less checkbox
		// - eg if the sending form is hand-written html
		if (s.equals("true") || s.equals("on") || s.equals("1"))
			return Boolean.TRUE;
		// "off" shouldn't happen, but just in case
		if (s.equals("false") || s.equals("off") || s.equals("")
				|| s.equals("0"))
			return Boolean.FALSE;
		if (s.equals("unknown"))
			return null;
		throw new WebInputException(s + " is not a valid true/false specifier");
	}

	// ?? promote this to AField as a data store for the question relating to a
	// field?
	private String label;

	public Checkbox(String name) {
		this(name, null);
	}

	/**
	 * @param name
	 * @param label
	 *            Optional. See {@link #setLabel(String)}
	 */
	public Checkbox(String name, String label) {
		super(name, "checkbox");
		setLabel(label);
	}

	@Override
	public void appendHtmlTo(StringBuilder sb, Boolean value,
			Object... attributes) {
		boolean v = value != null && value;
		// We always set value=true (this is what gets reported back if the box is checked)
		// And we set checked=true if the current value is true.
		// Add in the checked setting:
		if (v) {
			if (attributes == null || attributes.length == 0) {
				attributes = new Object[] { "checked", "true" };
			} else {
				attributes = Arrays.copyOf(attributes, attributes.length + 2);
				attributes[attributes.length - 2] = "checked";
				attributes[attributes.length - 1] = "true";
			}
		}
		super.appendHtmlTo(sb, Boolean.TRUE, attributes);
		// StringBuilder ab = new StringBuilder();
		// addAttributes(ab, attributes);
		// if (onChange!=null) {
		//
		// }
		// Printer.appendFormat(sb,
		// "<input type='checkbox' name='{0}' {3} value='true' {1} class='{2}' {4}/>",
		// getName(), (v? "checked='true'" : ""), cssClass,
		// (id==null? "" : "id='"+id+"'"), ab.toString());
		if (label != null) {
			sb.append("<label for='" + id + "'>" + label + "</label>");
		}
	}

	@Override
	public Boolean fromString(String s) {
		return convert(s);
	}

	public String getLabel() {
		return label;
	}

	/**
	 * Interpret null as false.
	 */
	@Override
	public String getStringValue(HttpServletRequest request) {
		String value = super.getStringValue(request);
		return value == null ? "false" : value;
	}

	/**
	 * Was a value specified by the request? WARNING: checkboxes can return
	 * blank or false for unticked. So this is not an ideal way to distinguish
	 * false/unset.
	 * 
	 * @param req
	 * @return true if a value was sent
	 */
	public boolean isSet(HttpServletRequest req) {
		String v = req.getParameter(getName());
		return v != null;
	}

	/**
	 * If set, this text will be displayed to the right of the checkbox inside a
	 * &lt;label&gt;.
	 * 
	 * @param text
	 * @return this
	 */
	public Checkbox setLabel(String text) {
		this.label = text;
		// set an id for the label to connect with
		if (id == null) {
			setId(Utils.getRandomString(6));
		}
		return this;
	}

	@Override
	public String toString(Boolean value) {
		return value ? "true" : "false";
	}

	@Override
	public Class<Boolean> getValueClass() {
		return Boolean.class;
	}

}
