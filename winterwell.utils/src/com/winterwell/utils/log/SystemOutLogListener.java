package com.winterwell.utils.log;

import com.winterwell.utils.Printer;

/**
 * TODO colours https://stackoverflow.com/questions/1448858/how-to-color-system-out-println-output
 * @author daniel
 *
 */
final class SystemOutLogListener implements ILogListener {
	@Override
	public void listen(Report report) {
		Printer.out(// Environment.get().get(Printer.INDENT)+
		'#' + report.tag + " " + report.getMessage()
		+(report.ex==null? "" : report.getDetails()
				));
	}
}