package winterwell.web.fields;

import org.apache.commons.lang3.StringEscapeUtils;

import winterwell.utils.StrUtils;

public class TextAreaField extends AField<String> {

	static final int defaultCols = 80;
	static final int defaultRows = 15;

	private static final long serialVersionUID = 1L;
	private int cols;

	private int rows;

	public TextAreaField(String name) {
		this(name, 0, 0);
	}

	public TextAreaField(String name, int columns, int rows) {
		super(name, "textarea");
		setSize(columns, rows);
	}

	@Override
	public void appendHtmlTo(StringBuilder sb, String value,
			Object... attributes) {
		String v = value == null ? "" : value;
		sb.append("<textarea name='" + getName() + "' class='" + cssClass
				+ "' ");
		// set rows/cols?
		if (rows > 0) {
			sb.append("rows='" + rows + "' cols='"
					+ (cols == 0 ? defaultCols : cols) + "'");
		} else {
			// Adjust to the number of lines in the text - to some extent
			int rs = defaultRows;
			if (value != null) {
				rs = StrUtils.splitLines(value).length;
			}
			rs = Math.max(defaultRows, rs);
			rs = Math.min(3 * defaultRows, rs);
			// If its a big text, lets boost the width too
			int cs = defaultCols;
			if (rs > 2 * defaultRows) {
				cs = (int) (1.5 * defaultCols);
			}
			sb.append("rows='" + rs + "' cols='" + cs + "'");
		}
		if (id != null) {
			sb.append(" id='" + id + "'");
		}
		addAttributes(sb, attributes);
		sb.append(">");
		// Escape Html chars
		v = StringEscapeUtils.escapeHtml4(v);
		sb.append(v);
		sb.append("</textarea>");
	}

	public int getColumns() {
		return cols;
	}

	public void setSize(int columns, int rows) {
		this.rows = rows;
		this.cols = columns;
	}

}
