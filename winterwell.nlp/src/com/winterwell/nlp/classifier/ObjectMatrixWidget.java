package com.winterwell.nlp.classifier;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import com.winterwell.maths.matrix.ObjectMatrix;
import com.winterwell.utils.IBuildStrings;
import com.winterwell.utils.MathUtils;
import com.winterwell.utils.Printer;
import com.winterwell.utils.containers.ArrayMap;
import com.winterwell.utils.containers.ArraySet;
import com.winterwell.utils.containers.Containers;
import com.winterwell.utils.web.SimpleJson;
import com.winterwell.web.HtmlTable;
import com.winterwell.web.IWidget;

/**
 * Create html for an {@link ObjectMatrix}
 * 
 * Note: depends on winterwell.web & winterwell.maths -- which is why it's in
 * winterwell.nlp
 * 
 * @author daniel
 * 
 */
public class ObjectMatrixWidget implements IWidget {

	private ObjectMatrix conf;

	public ObjectMatrixWidget(ObjectMatrix conf) {
		this.conf = conf;
		assert conf != null;
	}

	@Override
	public void appendHtmlTo(IBuildStrings bs) {
		appendHtmlTo(bs.sb());
	}

	String css = getClass().getSimpleName();
	
	String rowsName;
	String colsName;
	
	@Override
	public void appendHtmlTo(StringBuilder page) {
		page.append("<div class='"+css+"'>");

		// use the row list for columns too (ensures square matrix & same
		// ordering)
		List<String> rows = rows();
		// alphabetical
		Collections.sort(rows, new Comparator<String>() {
			public int compare(String o1, String o2) {
				if (o1==null) return 1; // handle null
				if (o2==null) return -1;
				return o1.compareTo(o2);
			}
		});

		// add in an extra column for the row-name, and another for the totals
		ArrayList<String> cols2 = columns();
		List<String> meatCols = cols2.subList(1, cols2.size()-1);
		
		// make the table
		HtmlTable table = new HtmlTable(cols2); // with headers
		table.setCSSClass("DataTable");

		// 2 decimal places, if not zero
		DecimalFormat format = new DecimalFormat("#,###.##");
		
		// build table
		double total1 = 0;
		for (String actual : rows) {
			if (actual == null) {
				actual = "null";
			}
			table.addCell(actual);
			
			for (String pred : meatCols) {
				double score = conf.get(actual, pred);			
//				if (Utils.equals(pred, actual)) {	// No special colouring for "diagonal" cells
//					table.addCell("<td style='border:solid gray 1px; color:blue; padding:3px;' title='correct "
//							+WebUtils.attributeEncode(pred)+"'>"
//							+ (int)score + "</td>");
//				} else {
				table.addCell(
//						"<td style='border:solid gray 1px; padding:3px;' title='"
//						+WebUtils.attributeEncode(actual)+"; "+WebUtils.attributeEncode(pred)+"'>"
						format.format(score)
//						+"</td>"
						);
//				}
			}
			
			// total column
			total1 += conf.getRowTotal(actual);
			table.addCell("<td style='border:solid gray 1px; padding:3px;'>"
					+ Printer.toStringNumber(conf.getRowTotal(actual))
					+ "</td>");
		}

		// Total row
		table.addCell("Total");
		double total2 = 0;
		for (String pred : meatCols) {
			double score = conf.getColumnTotal(pred);
			total2 += score;
			table.addCell("<td style='border:solid gray 1px; padding:3px;'>"
					+ (int)score + "</td>");
		}
		// total total
		String ttl = MathUtils.equalish(total1, total2)? ""+(int)total1 : "<span title='different totals?!'><b>"+((int)total2)+" \\ "+((int)total1)+"</b></span>";
		table.addCell(ttl);
		table.addCell("");

		// done
		table.appendHtmlTo(page);
		page.append("</div>");
	}

	List<String> rows() {
		return new ArraySet<String>(conf.getRowValues()).asList();	
	}

	ArrayList<String> columns() {
		ArrayList<String> cols2 = new ArrayList<String>(conf.getColumnValues());
		Containers.replace(cols2, null, "null");

		// alphabetical
		Collections.sort(cols2, new Comparator<String>() {
			public int compare(String o1, String o2) {
				if (o1==null) return 1; // handle null
				if (o2==null) return -1;
				return o1.compareTo(o2);
			}
		});

		// Add row-name column
		String corner = "";
		if (rowsName!=null) {
			corner = rowsName+"↴";
		}
		if (colsName!=null) {
			if (corner.length()!=0) corner += " \\ ";
			corner += colsName+"→";
		}
		cols2.add(0, corner);
		// Add totals column
		cols2.add(colsName==null? "Total" : "Total "+colsName);
		return cols2;
	}

	/**
	 * What is a sensible representation here??
	 * @param confusionMatrix
	 * @return
	 */
	public String toJson() {
		Object rows = toJson2();
		return new SimpleJson().toJson(rows);
	}

	public Map<Object, Map> toJson2() {
		Map<Object, Map> rows = new ArrayMap();
		for(Object rowName : conf.getRowValues()) {
			Map row = conf.getRow(rowName);
			rows.put(rowName, row);
		}
		return rows;
	}
}
