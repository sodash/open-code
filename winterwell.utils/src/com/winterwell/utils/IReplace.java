package winterwell.utils;

import java.util.regex.Matcher;

/**
 * Replace regex matches - for when more processing is required than can be done
 * using a regex replacement pattern.
 * 
 * @author Daniel
 * 
 */
public interface IReplace {

	/**
	 * Perform a replacement
	 * 
	 * @param sb
	 *            Append the replacement to this.
	 * @param match
	 *            A matcher which has just successfully matched. Interrogate
	 *            this for group info, etc.
	 */
	void appendReplacementTo(StringBuilder sb, Matcher match);

}
