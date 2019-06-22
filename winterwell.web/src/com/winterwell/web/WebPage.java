/**
 *
 */
package com.winterwell.web;

import java.io.IOException;

import com.winterwell.utils.IBuildStrings;
import com.winterwell.utils.containers.ArraySet;

/**
 * Helper class for putting together html pages.
 * 
 * @author Daniel
 * 
 */
public class WebPage implements Appendable, IBuildStrings {

	// web pages tend to be fairly big
	protected final StringBuilder contents = new StringBuilder(2024);

	protected final StringBuilder headContents = new StringBuilder();

	/**
	 * JavaScripts that will be loaded. Note that ArraySet is insertion ordered
	 * which means scripts will get loaded in the order you add them. This is a
	 * good thing when there are dependencies.
	 */
	private final ArraySet<String> scripts = new ArraySet<String>(10);
	
	
	private final ArraySet<String> stylesheets = new ArraySet<String>(5);
	
	protected String title = "";

	/**
	 * Add a script to the ordered set used by this page. Scripts are included
	 * in the body. No scripts
	 * are installed by default (though some may be included in the template)
	 * <p>
	 * Since scripts are included only once and in order, this provides a limited
	 * form of dependency management e.g. if myWidget's javascript requires
	 * jQuery, do: addScript("jquery"); addScript("/static/code/myWidget.js");
	 * 
	 * @param script
	 *            URL of script to add. As a special case hack, "jquery" will
	 *            convert to the latest stable version of JQuery (as hosted by
	 *            Google).
	 */
	public final void addScript(String script) {
		assert script != null;
		// Special case: jquery
		if ("jquery".equals(script)) {		
			// We could alternatively use http://code.jquery.com/jquery-latest.min.js
			script = "http://ajax.googleapis.com/ajax/libs/jquery/1.8/jquery.min.js";
		}
		scripts.add(script);
	}
	
	/**
	 * Add a stylesheet to the ordered set used by this page. Stylesheets are
	 * appended to the page header.
	 * 
	 * @param stylesheet Url for the css file.
	 */
	public final void addStylesheet(String stylesheet) {
		assert ! stylesheet.isEmpty();		
		stylesheets.add(stylesheet);
	}
	
	@Override
	public final Appendable append(char c) throws IOException {
		contents.append(c);
		return this;
	}

	@Override
	public final Appendable append(CharSequence csq) {
		contents.append(csq);
		return this;
	}

	@Override
	public final Appendable append(CharSequence csq, int start, int end) {
		contents.append(csq, start, end);
		return this;
	}

	public final void appendToHeader(CharSequence text) {
		headContents.append(text);
	}

	public final String getBody() {
		return contents.toString();
	}

	/**
	 * Retrieve the ordered set of scripts for this page. There is no good
	 * reason to use this outside of
	 * {@link APageServlet#postProcess(PageBuilder, RequestState)}
	 */
	public final ArraySet<String> getScripts() {
		return scripts;
	}
	
	/**
	 * Gets the sylesheets added to a page by the servlet.
	 * @return An ArraySet<String> of the stylesheets, encoded as html link
	 * elements.
	 */
	public final ArraySet<String> getStylesheets() {
		return stylesheets;
	}
	
	public final String getTitle() {
		return title;
	}

	/**
	 * @return the contents StringBuilder underlying this object.
	 */
	@Override
	public final StringBuilder sb() {
		return contents;
	}
	
/**
 * 
 * @param title The title for the page. Any double-quote characters in the title
 * will be replaced with single-quotes, to prevent issues with translating while
 * rendering.
 */
	public final void setTitle(String title) {
		this.title = title==null? null : title.replace("\"", "'"); // To ensure string is correctly passed to front-end translation method.
	}

	/**
	 * The HTML for this web page
	 */
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("<!DOCTYPE html PUBLIC \"-//W3C//DTD HTML 4.01 Transitional//EN\" \"http://www.w3.org/TR/html4/loose.dtd\">\n");
		sb.append("<html>\n<head>\n");
		sb.append("<meta http-equiv='Content-Type' content='text/html; charset=utf-8'>\n");
		sb.append("<title>");
		sb.append(title);
		sb.append("</title>\n");
		// style sheets, styles
		for (String cssUrl : stylesheets) {
			sb.append("<link type=\"text/css\" rel=\"stylesheet\" href=\"" + cssUrl + "\"/>\n");
		}
		// javascript
		for (String script : scripts) {
			sb.append("<script type='text/javascript' src='" + script
					+ "'></script>\n");
		}
		sb.append(headContents);
		sb.append("</head>\n<body>\n");
		// Body
		sb.append(contents);
		sb.append("</body>\n</html>\n");
		return sb.toString();
	}

}
