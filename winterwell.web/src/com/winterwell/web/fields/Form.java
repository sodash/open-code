package com.winterwell.web.fields;

import java.util.Map;

import com.winterwell.utils.IBuildStrings;
import com.winterwell.utils.web.WebUtils;
import com.winterwell.utils.web.WebUtils2;
import com.winterwell.web.IWidget;

/**
 * Helper class for creating HTML forms.
 * 
 * @author Daniel
 */
public class Form implements IWidget, IBuildStrings {

	public static final SafeString ACTION = new SafeString("action");

	public static final String GET = "get";

	/**
	 * The default
	 */
	public static final String POST = "post";

	public static final String MULTIPART_MIME_ENCODING = "multipart/form-data";
	
	
	private String action;
	private String actionUrl;
	public String cssClass;
	public String id;
	private boolean inTable;
	/**
	 * post or get - post by default
	 */
	protected String method = "post";

	private CharSequence onSubmit = null;

	private final StringBuilder sb = new StringBuilder();

	private String submit = " <button class='btn btn-primary' type='submit'>Submit Form</button> ";

	private String encType;

	/**
	 * @param encType e.g. #MULTIPART_MIME_ENCODING
	 */
	public void setEncType(String encType) {
		this.encType = encType;
	}

	/**
	 * 
	 * @param actionUrl
	 *            If this contains GET args, e.g. "/page?foo=bar", then the get
	 *            args will be removed from the form-action and added instead as
	 *            form fields.
	 */
	public Form(CharSequence actionUrl) {
		// are there GET args in the url?
		// If so add them as form fields
		if (actionUrl == null) return;
		String aus = actionUrl.toString();
		Map<String, String> map = WebUtils2.getQueryParameters(aus);
		for (String k : map.keySet()) {
			String v = WebUtils.attributeEncode(map.get(k));
			sb.append("<input type='hidden' name='" + k + "' value='" + v
					+ "'>");
		}
		/*
		 * if ( ! map.isEmpty()) { aus = aus.substring(0, aus.indexOf('?')); }
		 */
		this.actionUrl = aus;
	}

	/**
	 * If in a table, add within a row which spans both table columns. If not,
	 * just append
	 * 
	 * @param cellText
	 */
	public void add(String cellText) {
		if (isInTable()) {
			sb.append("<tr><td colspan='2'>");
			sb.append(cellText);
			sb.append("</td></tr>\n");
		} else {
			sb.append(cellText);
		}
	}

	public <X> void addHidden(AField<X> field, X value) {
		// HACK: Ignore PasswordField's security defence
		if (field instanceof PasswordField) {
			field = new AField(field.getName());
		}
		String v = value == null ? "" : field.toString(value);
		v = WebUtils.attributeEncode(v);
		sb.append("<input type='hidden' name='" + field + "' value='" + v
				+ "'>");
	}

	/**
	 * Add a row which spans both table columns.
	 * 
	 * @param cellText
	 */
	public void addOneCellRow(IWidget cellField) {
		assert isInTable();
		sb.append("<tr><td colspan='2'>");
		cellField.appendHtmlTo(sb);
		sb.append("</td></tr>\n");
	}

	/**
	 * Add a row which spans both table columns.
	 * 
	 * @param cellText
	 */
	public void addOneCellRow(String contents) {
		assert isInTable();
		sb.append("<tr><td colspan='2'>");
		sb.append(contents);
		sb.append("</td></tr>\n");
	}

	/**
	 * Add a 2-column table row of the form Question: Input-Field, <i>specifying
	 * the field value to display</i> (otherwise it would be drawn from the
	 * request or be null). Must previously have called startTable(). The table
	 * is built as we go along, so this is equivalent to poking a row onto the
	 * form's StringBuilder.
	 * 
	 * @param question
	 * @param input
	 * @param value
	 */
	public <X> void addRow(String question, AField<X> input, X value,
			Object... attributes) {
		assert inTable : "Call startTable()";
		sb.append("<tr><td>");
		if (input.id != null) {
			sb.append("<label for='" + input.id + "'>");
			sb.append(question);
			sb.append("</label>");
		} else {
			sb.append(question);
		}
		sb.append("</td><td>");
		input.appendHtmlTo(sb, value, attributes);
		sb.append("</td></tr>\n");
	}

	/**
	 * Add a 2-column table row of the form Question: Input-Field. Must
	 * previously have called startTable(). The table is built as we go along,
	 * so this is equivalent to poking a row onto the form's StringBuilder.
	 * 
	 * @param question
	 * @param input
	 */
	public void addRow(String question, IWidget input) {		
		assert inTable : "Call startTable()";		
		sb.append("<tr><td>");
		sb.append(question);
		sb.append("</td><td>");
		input.appendHtmlTo(sb);
		sb.append("</td></tr>\n");
	}

	/**
	 * Just a straight append. Be aware whether we are in a table or not!
	 * 
	 * @param string
	 */
	@Override
	public Appendable append(CharSequence string) {
		sb.append(string);
		return sb;
	}

	@Override
	public void appendHtmlTo(IBuildStrings ibs) {
		appendHtmlTo(ibs.sb());
	}

	@Override
	public void appendHtmlTo(StringBuilder page) {
		// opening form tag
		page.append("\n<form");
		if (id != null) {
			page.append(" id='" + id + "'");
		}
		page.append(" action='" + actionUrl + "' method='" + method + "'");
		if (cssClass != null) {
			page.append(" class='" + cssClass + "'");
		}
		if (onSubmit != null) {
			page.append(" onSubmit='");
			WebUtils.attributeEncode(page, onSubmit);
			page.append('\'');
		}
		if (encType!=null) {
			page.append(" enctype='"+encType+"'");
		}
		page.append('>');

		// that special action field
		if (action != null) {
			page.append("<input type='hidden' name='action' value='" + action
					+ "'>\n");
		}

		// Manipulate backing StringBuilder *before* writing to page
		if (inTable) {
			if (submit != null) {
				sb.append("<tr><td colspan='2'>" + submit + "</td></tr>");
			}
			closeTable();
		} else if (submit != null) {
			sb.append(submit);
		}
		submit = null; // prevent any duplicate appends if the form is somehow
						// re-strung
		// splat out the form
		page.append(sb);
		// done
		page.append("</form>\n");
	}

	/**
	 * Close the table (which must be open)
	 */
	public void closeTable() {
		assert inTable;
		sb.append("</table>\n");
		inTable = false;
	}

	/**
	 * Warning: This is not part of a standard form! It is a convenience method
	 * for adding a hidden field with the name "action", and the specified
	 * value.
	 * 
	 * @param action
	 *            Can be null
	 */
	public String getAction() {
		return action;
	}

	public CharSequence getOnSubmit() {
		return onSubmit;
	}

	/**
	 * The submit buttons, or null if none
	 * 
	 * @return
	 */
	public String getSubmit() {
		return submit;
	}

	public boolean isInTable() {
		return inTable;
	}

	@Override
	public StringBuilder sb() {
		return sb;
	}

	/**
	 * Warning: This is not part of a standard form! It is a convenience method
	 * for adding a hidden field with the name "action", and the specified
	 * value.
	 * 
	 * @param action
	 *            Can be null
	 */
	public void setAction(String action) {
		// Did someone mistake this - reasonably enough - for setting the <form
		// action="url">?
		assert ! action.startsWith("http://") : action;
		this.action = action;
	}

	/**
	 * post or get - post by default
	 */
	public void setMethod(String method) {
		this.method = method;
	}

	/**
	 * If not null (which is the default), this will set an onSubmit event
	 * handler for the form.
	 * 
	 * @param onSubmit
	 */
	public void setOnSubmit(String onSubmit) {
		// assert this.onSubmit == null : this.onSubmit+" vs "+onSubmit;
		this.onSubmit = onSubmit;
	}

	/**
	 * Set to over-ride the default submit button. Set to null to ignore.
	 * @param submit html, e.g. "&lt;input type='submit'&gt;GO!&lt;/input&gt;", or null
	 */
	public void setSubmit(String submit) {
		this.submit = submit;
	}

	/**
	 * Start a two column table. Equivalent to {@link #startTable(String)} with
	 * "" Must be called before {@link #addRow(String, IWidget)} can be used. It
	 * is not necessary to call {@link #closeTable()} however.
	 */
	public void startTable() {
		startTable("");
	}

	/**
	 * Start a two column table
	 * 
	 * @param gumpf
	 *            extra table attributes (but NOT css class which is hard coded
	 *            to "form-table"). Must not be null.
	 */
	public void startTable(String gumpf) {
		assert !inTable;
		assert gumpf != null;
		sb.append("\n<table class='form-table' " + gumpf + ">\n");
		inTable = true;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + ":" + sb.toString();
	}

	public void addBootstrapField(String label, AField field) {
		sb.append("<div class='form-group'><label>");
		sb.append(label);
		sb.append("</label>");
		// HACK
		if (field.cssClass==null || field.cssClass.length()==0) field.setCssClass("form-control");
		field.appendHtmlTo(sb);
		sb.append("</div>");
	}

	public void addHidden(String name, String value) {
		value = WebUtils.attributeEncode(value);
		sb.append("<input type='hidden' name='"+name+"' value='"+value+"'>");
	}

}
