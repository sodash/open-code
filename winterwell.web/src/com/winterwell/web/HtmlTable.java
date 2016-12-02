package com.winterwell.web;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.winterwell.utils.IBuildStrings;
import com.winterwell.utils.Printer;
import com.winterwell.utils.StrUtils;
import com.winterwell.utils.containers.Containers;
import com.winterwell.utils.containers.Pair;
import com.winterwell.utils.web.WebUtils;

/**
 * Helper class for producing tables in HTML.
 * <p>
 * Cells can use &lt;td> or &lt;th> to take control of their html.
 * 
 * @author Daniel
 * @author Joe Halliwell <joe@winterwell.com>
 */
public class HtmlTable implements IWidget {

	private int cellIndex;
	String cssClass;
	// TODO this is a bit ungainly
	Map<Pair<Integer>, String> cell2css = new HashMap<Pair<Integer>, String>();

	CharSequence headerBumpf;

	String[] headers;
	public String id;
	private final int numColumns;

	List<String[]> rows = new ArrayList<String[]>();

	private String style;

	/**
	 * Create a table with the specified number of columns
	 * 
	 * @param numColumns
	 */
	public HtmlTable(int numColumns) {
		this.numColumns = numColumns;
	}

	public HtmlTable(List<String> headers) {
		this(headers.toArray(StrUtils.ARRAY));
	}

	public HtmlTable(String... headers) {
		this.headers = headers;
		assert headers.length != 0;
		numColumns = this.headers.length;
	}

	/**
	 * Builds the table one cell at a time. Will start new rows automatically.
	 * 
	 * @param cell
	 */
	public void addCell(String cell) {
		assert numColumns > 0;
		int i = cellIndex % numColumns;
		if (i == 0) {
			addRow(new Object[numColumns]);
		}
		String[] row = Containers.last(rows);
		row[i] = cell;
		cellIndex++;
	}

	public void addRow(List row) {
		addRow(row.toArray());
	}

	/**
	 * @return Header row if set. Can be null
	 */
	public String[] getHeaders() {
		return headers;
	}
	
	/**
	 * Add one cell which will span the table width.
	 * @param value
	 */
	public void addSingleColumnRow(String value) {
		rows.add(new String[]{value});
	}
	
	/**
	 * @param row
	 *            one object per column, converted to strings using
	 *            {@link Printer}.
	 * 
	 * @throws an
	 *             assertion error when 'row' != 'width'
	 * @return row number (zero indexed)
	 */
	public int addRow(Object... row) {
		// check size of row is ok
		assert row != null;
		assert headers == null || row.length == headers.length : "Problem with row! "
				+ row.length
				+ " vs "
				+ Printer.toString(headers)
				+ " "
				+ WebUtils.stripTags(Printer.toString(row));
		assert headers != null || row.length == numColumns : "Problem with headerless row! "
				+ row.length
				+ " vs "
				+ numColumns
				+ " "
				+ WebUtils.stripTags(Printer.toString(row));
		String[] tmp = new String[row.length];
		for (int i = 0; i < row.length; i++) {
			tmp[i] = Printer.toString(row[i]);
		}
		rows.add(tmp);
		return rows.size() - 1;
	}

	@Override
	public void appendHtmlTo(IBuildStrings ibs) {
		appendHtmlTo(ibs.sb());
	}

	/**
	 * Append this table's html to a {@link StringBuilder}
	 * 
	 * @param stringBuilder
	 * @throws IOException
	 */
	@Override
	public void appendHtmlTo(StringBuilder appendable) {
		appendable.append(toHTML());
	}

	private void appendRow(StringBuilder result, int r, String[] row) {
		// odd or even row? depends on whether we had a header row or not
		boolean oddRow = r % 2 == 1;
		if (headers != null) {
			oddRow = !oddRow;
		}

		// Mark the row as odd or even, and give it a number too
		String css = StrUtils.joinWithSkip(" ", 
						oddRow ? "odd" : "even", "row" + r,
						row.length == 1 ? "spanning" : "");
		result.append("<tr class='" + css +"'");
		appendCustomCss(result, new Pair(r, -1), null);
		result.append(">");

		// add a row form?
		appendRow2Form(result);

		// FIXME: This seems a bit dodgy -- where is it used?
		if (row.length == 1) {
			// Just one object - make it span
			result.append("<td colspan='");
			result.append(getWidth());
			result.append("' class='spanning'");
			Pair<Integer> kvp = new Pair<Integer>(r, 0);
			appendCustomCss(result, kvp, null);
			result.append(">");
			result.append(row[0]);
			result.append("</td></tr>");
			return;
		}

		for (int col = 0; col < row.length; col++) {
			// Write in this row
			String value = row[col];

			// allow individual cells to take control of their html
			if (value != null && value.startsWith("<td")
					|| value.startsWith("<th")) {
				result.append(value);
				continue;
			}		
			result.append("<td class='"+(col % 2 == 0 ? "even" : "odd")+" col"+col+"'");
			Pair<Integer> kvp = new Pair<Integer>(r, col);
			appendCustomCss(result, kvp, new Pair(-1, col));
			result.append(">");
			result.append(value);
			result.append("</td>");
		}
		oddRow = !oddRow;
		result.append("</tr>\n");
	}

	/**
	 * 
	 * @param result Assumes inside a tag
	 * @param pair
	 * @param object
	 */
	private void appendCustomCss(StringBuilder result, Pair rc1, Pair rc2) {
		String customCss = cell2css.get(rc1);
		String customCss2 = cell2css.get(rc2);
		String css = StrUtils.joinWithSkip(" ", customCss, customCss2);
		if (css.isEmpty()) return;
		result.append(" style='"+css+"'"); 
	}

	/**
	 * A
	 * <tr>
	 * has just been added. Do we want to stick a <form> in?
	 * 
	 * @param result
	 */
	protected void appendRow2Form(StringBuilder result) {
		// do nothing
	}

	/**
	 * Retrieve the number of columns
	 * 
	 * @return
	 */
	public int getWidth() {
		return numColumns;
	}

	/**
	 * Set the CSS for a particular column
	 * 
	 * @param row
	 * @param style raw CSS
	 */
	public void setColumnCSS(int column, String style) {
		cell2css.put(new Pair<Integer>(-1, column), style);
	}

	/**
	 * Set the CSS for a particular cell
	 * 
	 * @param row
	 * @param col
	 * @param style raw CSS
	 */
	public void setCSSForCell(int row, int col, String style) {
		cell2css.put(new Pair<Integer>(row, col), style);
	}

	/**
	 * Set the class of the entire table
	 * 
	 * @param cssClass
	 */
	public void setCSSClass(String cssClass) {
		this.cssClass = cssClass;
	}

	/**
	 * misc gunk squirted into the &lt;table&gt; tag.
	 * 
	 * @param headerBumpf
	 */
	public void setHeaderBumpf(CharSequence headerBumpf) {
		this.headerBumpf = headerBumpf;
	}

	/**
	 * Set the CSS for a particular row
	 * 
	 * @param row zero-indexed
	 * @param style e.g. "color:blue;"
	 */
	public void setRowCSS(int row, String style) {
		cell2css.put(new Pair<Integer>(row, -1), style);
	}

	public void setStyle(String css) {
		style = css;
	}

	public String toHTML() {
		StringBuilder result = new StringBuilder();
		result.append("<table ");
		if (id != null) {
			result.append("id='" + id + "' ");
		}
		if (style != null) {
			result.append("style='" + style + "' ");
		}
		if (cssClass != null) {
			result.append("class='" + cssClass + "' ");
		}
		if (headerBumpf != null) {
			result.append(headerBumpf + " ");
		}
		result.append(">");

		// Write headers
		if (headers != null) {
			result.append("<thead><tr class='header'>");
			for (int i = 0; i < headers.length; i++) {
				String css = StrUtils.join(
						new String[] { i % 2 == 0 ? "even " : "odd ", "col" + i },
						" ");
				result.append("<th class='" + css + "' col='" + i + "'");
				String customCss = cell2css.get(new Pair(-1, i));
				if (customCss!=null) result.append(" style='"+customCss+"'");
				result.append(">");
				result.append(headers[i] + "</th>");
			}
			result.append("</tr></thead>\n");
		}
		result.append("<tbody>");

		// Write rows
		for (int r = 0; r < rows.size(); r++) {
			String[] row = rows.get(r);
			appendRow(result, r, row);
		}

		result.append("</tbody>");
		result.append("</table>");
		return result.toString();
	}

	@Override
	public String toString() {
		return Printer.toString(headers) + "\n" + Printer.toString(rows, "\n");
	}

	public HtmlTable(Map map) {
		this(2);
		addMap(map);
	}
	
	public void addMap(Map map) {
		assert getWidth() == 2 : this;
		for(Object k : map.keySet()) {
			addRow(k, map.get(k));
		}
	}

}
