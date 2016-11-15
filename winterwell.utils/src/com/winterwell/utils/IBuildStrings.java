/**
 * 
 */
package winterwell.utils;

/**
 * Implementing {@link Appendable} isn't much use 'cos it throws exceptions all
 * over the place. So instead we declare that an object exposes a StringBuilder.
 * 
 * @author daniel
 * 
 */
public interface IBuildStrings {

	/**
	 * @param text
	 *            Append this text
	 * 
	 * @return Please do not use the returned object! It is only for overlap
	 *         compatibility with the {@link Appendable} interface.
	 */
	Appendable append(CharSequence text);

	/**
	 * Expose the underlying {@link StringBuilder}
	 */
	StringBuilder sb();

}
