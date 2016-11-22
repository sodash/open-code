package winterwell.web;

import winterwell.utils.IBuildStrings;

public interface IWidget {

	/**
	 * Add html (XHTML 1.0 please) to this string builder
	 * 
	 * @param sb
	 */
	void appendHtmlTo(IBuildStrings bs);

	void appendHtmlTo(StringBuilder sb);

}
