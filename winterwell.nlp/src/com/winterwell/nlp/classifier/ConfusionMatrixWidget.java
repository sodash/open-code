package com.winterwell.nlp.classifier;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Set;

import com.winterwell.maths.classifiers.ConfusionMatrix;
import com.winterwell.utils.IBuildStrings;
import com.winterwell.utils.MathUtils;
import com.winterwell.utils.Printer;
import com.winterwell.utils.Utils;
import com.winterwell.utils.containers.ArraySet;
import com.winterwell.utils.containers.Containers;
import com.winterwell.utils.web.WebUtils;
import com.winterwell.web.HtmlTable;
import com.winterwell.web.IWidget;

/**
 * Create html for a {@link ConfusionMatrix}
 * 
 * Note: depends on winterwell.web & winterwell.maths -- which is why it's in
 * winterwell.nlp
 * 
 * @author daniel
 * 
 */
public class ConfusionMatrixWidget implements IWidget {

	private ConfusionMatrix<String> conf;

	public ConfusionMatrixWidget(ConfusionMatrix<String> conf) {
		this.conf = conf;
		assert conf != null;
	}

	@Override
	public void appendHtmlTo(IBuildStrings bs) {
		appendHtmlTo(bs.sb());
	}

	@Override
	public void appendHtmlTo(StringBuilder page) {
		page.append("<div class='ConfusionMatrixWidget'>");

		// use the row list for columns too (ensures square matrix & same
		// ordering)
		ArraySet<String> rows = new ArraySet<String>(conf.getRowValues());
		// I think rows & cols can be different, so let's use the union
		Set<String> cols = conf.getColumnValues();
		rows.addAll(cols); // NB: ArraySet will make sure there are no dupes
		// alphabetical
		Collections.sort(rows.asList(), new Comparator<String>() {
			public int compare(String o1, String o2) {
				if (o1==null) return 1; // handle null
				if (o2==null) return -1;
				return o1.compareTo(o2);
			}
		});

		// add in an extra column for the row-name
		ArrayList<String> cols2 = new ArrayList<String>(rows);
		Containers.replace(cols2, null, "null");
		cols2.add(0, "true↴ \\ AI→");
		cols2.add("Total Man");
		cols2.add("sens");

		// make the table
		HtmlTable table = new HtmlTable(cols2);
		table.setCSSClass("DataTable");

		// build table
		double total1 = 0;
		for (String actual : rows) {
			if (actual == null) {
				actual = "null";
			}
			table.addCell(actual);
			
			for (String pred : rows) {
				double score = conf.get(actual, pred);			
				if (Utils.equals(pred, actual)) {
					table.addCell("<td style='border:solid gray 1px; color:blue; padding:3px;' title='correct "
							+WebUtils.attributeEncode(pred)+"'>"
							+ (int)score + "</td>");
				} else {
					table.addCell("<td style='border:solid gray 1px; padding:3px;' title='is:"
							+WebUtils.attributeEncode(actual)+"; ai:"+WebUtils.attributeEncode(pred)+"'>"
							+ (int)score + "</td>");
				}
			}
			// total column
			total1 += conf.getRowTotal(actual);
			table.addCell("<td style='border:solid gray 1px; padding:3px;'>"
					+ (int)conf.getRowTotal(actual)
					+ "</td>");
			// sensitivity column
			table.addCell("<td style='border:solid gray 1px; padding:3px;'>"
					+ Printer.toStringNumber(conf.getSensitivity(actual) * 100)
					+ "%</td>");
		}

		// Total row
		table.addCell("Total AI");
		double total2 = 0;
		for (String pred : rows) {
			double score = conf.getTotalPredicted(pred);
			total2 += score;
			table.addCell("<td style='border:solid gray 1px; padding:3px;'>"
					+ (int)score + "</td>");
		}
		// total total
		String ttl = MathUtils.equalish(total1, total2)? ""+(int)total1 : "<span title='different totals?!'><b>"+((int)total2)+" \\ "+((int)total1)+"</b></span>";
		table.addCell(ttl);
		table.addCell("");
		// PPV row
		table.addCell("PPV");
		double meanPPV = 0;
		for (String pred : rows) {
			double score = conf.getPPV(pred);
			meanPPV += score*conf.getTotalPredicted(pred);
			table.addCell("<td style='border:solid gray 1px; padding:3px;'>"
					+ Printer.toStringNumber(score * 100) + "%</td>");
		}
		meanPPV = meanPPV / total2;
		table.addCell(Printer.toStringNumber(meanPPV * 100));
		table.addCell("");

		// done
		table.appendHtmlTo(page);
		page.append("</div>");
	}

	/**
	 * What is a sensible representation here??
	 * @param confusionMatrix
	 * @return
	 */
	public static Object toJson(ConfusionMatrix<String> confusionMatrix) {
		return "TODO"; // TODO
	}
}
