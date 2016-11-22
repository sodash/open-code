package winterwell.web.fields;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringEscapeUtils;

import winterwell.utils.Utils;
import winterwell.utils.web.WebUtils;
import winterwell.web.WebInputException;

/**
 * A drop down selection field.
 * 
 * @author daniel
 * @author joe
 */
public class SelectField<X> extends AField<X> {

	static final String SELECT_TYPE = "select";

	private static final long serialVersionUID = 1L;
	protected List<String> optionHtml;
	/**
	 * Can include null
	 */
	protected List<X> options;

	protected String title;

	protected boolean useValuesDirectly = true;

	/**
	 * Does not setup options or anything. Override in subclasses
	 * 
	 * @param name
	 */
	protected SelectField(String name) {
		super(name, SELECT_TYPE);
	}

	public SelectField(String name, Collection<? extends X> options) {
		super(name, SELECT_TYPE);
		this.options = new ArrayList<X>(options);
		optionHtml = new ArrayList<String>(this.options.size());
		for (X x : this.options) {
			String html = getDefaultHtml(x);
			optionHtml.add(html);
		}
	}

	/**
	 * 
	 * @param name
	 * @param options
	 *            The null key is supported for "no selection". If null is not
	 *            present, something will always get selected. The keys of this
	 *            map are the values returned (ie. the options), the values are
	 *            the text displayed.
	 */
	public SelectField(String name, Map<X, String> options) {
		super(name, SELECT_TYPE);
		this.options = new ArrayList<X>(options.keySet());
		optionHtml = new ArrayList<String>(this.options.size());
		for (X x : this.options) {
			String os = options.get(x);
			String html = StringEscapeUtils.escapeHtml4(os);
			optionHtml.add(html);
		}
	}

	@Override
	public void appendHtmlTo(StringBuilder sb, X value, Object... attributes) {
		sb.append("<select name='" + getName() + "'");
		if (id != null) {
			sb.append("id='" + id + "' ");
		}
		if (onChange != null) {
			sb.append("onChange='" + WebUtils.attributeEncode(onChange) + "' ");
		}
		if (cssClass != null) {
			sb.append("class='" + cssClass + "' ");
		}
		if (title != null) {
			sb.append("title='" + title + "' ");
		}
		addAttributes(sb, attributes);
		sb.append(">");
		appendOptions(sb, value);
		sb.append("</select>");
	}

	/**
	 * TODO document this, and assumptions
	 * 
	 * @param sb
	 * @param value
	 * @param i
	 * @param option
	 * @param html
	 */
	protected void appendOption(StringBuilder sb, X value, int i, X option,
			String html) {
		String vi;
		if (useValuesDirectly) {
			// encode null as ""
			vi = option == null ? "" : WebUtils
					.attributeEncode(toString(option));
		} else {
			vi = Integer.toString(i);
		}
		if (Utils.equals(option, value)) {
			sb.append("<option value='" + vi + "' selected='true'>" + html
					+ "</option>");
		} else {
			sb.append("<option value='" + vi + "'>" + html + "</option>");
		}
	}

	/**
	 * Display all the options. Overridden in {@link GroupedSelectField}.
	 * 
	 * @param sb
	 * @param value
	 *            The chosen value
	 */
	protected void appendOptions(StringBuilder sb, X value) {
		if (title != null) {
			appendOption(sb, value, -1, null, title);
		}
		for (int i = 0; i < options.size(); i++) {
			X option = options.get(i);
			// Use index values or string serialisation of actual values?
			appendOption(sb, value, i, option, optionHtml.get(i));
		}
	}

	@Override
	public X fromString(String v) throws WebInputException {
		if (v == "")
			return null;
		if (useValuesDirectly) {
			// Search for the option which will produce this string
			for (X x : options) {
				if (x == null) {
					continue;
				}
				String sx = toString(x);
				if (v.equals(sx))
					return x;
			}
			// throw an exception
			throw new WebInputException("Unrecognised option: "+v+" is not in "+options);
		}
		// Use indices
		Integer i = Integer.valueOf(v);
		if (i == -1)
			// -1 codes for "some muppet selected the title"
			return null;
		return options.get(i);
	}

	/**
	 * Get default HTML to display for a given option, to be used if the caller
	 * doesn't specify anything better.
	 * 
	 * @param x
	 * @return
	 */
	protected String getDefaultHtml(X x) {
		String os = x == null ? "" : x.toString();
		String html = StringEscapeUtils.escapeHtml4(os);
		return html;
	}

	public List<X> getOptions() {
		return options;
	}

	public String getOptionString(X value) {
		int i = options.indexOf(value);
		if (i == -1)
			throw new IllegalArgumentException("Unknown value: " + value);
		return optionHtml.get(i);
	}

	/**
	 * Retrieve the index associated with a selectable value or -1 if not
	 * present. This index is only meaningful if the options were ordered i.e.
	 * if the Map or Collection used to construct the object was itself ordered.
	 * 
	 * @param value
	 * @return
	 */
	public final int indexOf(X value) {
		int index = 0;
		for (X key : options) {
			if (Utils.equals(key, value))
				return index;
			index++;
		}
		return -1;
	}

	/**
	 * If set, this is shown as the default option. It return null if selected.
	 * 
	 * @param title
	 */
	public void setTitle(String title) {
		this.title = title;
	}

	/**
	 * True by default. If true, the values sent by the select field will be the
	 * actual options, converted via {@link #toString(Object)}. If false, index
	 * values will be used instead - which requires that the field object be
	 * kept to decipher the return values.
	 * 
	 * @param useValuesDirectly
	 */
	public void setUseValuesDirectly(boolean useValuesDirectly) {
		this.useValuesDirectly = useValuesDirectly;
	}

}
