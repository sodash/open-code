package winterwell.utils.io;


/**
 * Interface for anything which can convert objects to & from Strings. The main
 * use is in web form fields.
 * <p>
 * Implementations SHOULD have a zero-arg constructor.
 * 
 * <p>
 * If &lt;X> is set to Object and no exceptions are thrown, then this is
 * identical to XStream's SingleValueConverter, and classes can implement both.
 * <p>
 * Why have this? It avoids a dependency on XStream.
 * 
 * @author daniel
 * 
 */
public interface ISerialize<X> {
	
	/**
	 * Determines whether the converter can marshall a particular type.
	 * 
	 * @param type
	 *            the Class representing the object type to be converted
	 */
	boolean canConvert(Class type);

	/**
	 * Subclasses should override and convert from String to whatever. This is
	 * also the place to perform validation.
	 * 
	 * @param v
	 *            Never null or blank. "false" is the preferred coding for
	 *            false/null/off. This has already been url-decoded.
	 * @return value converted into correct form. Can be null for unset
	 * @throws Exception
	 *             This will be converted into a {@link WebInputException} by
	 *             {@link #getValue(HttpServletRequest)}.
	 */
	X fromString(String v) throws Exception;

	/**
	 * @param value
	 *            never null
	 * @return string representation for value. This is a normal unencoded
	 *         string. attribute character encoding is handled elsewhere. The
	 *         default implementation just uses toString().<br>
	 * @see convertString(String) which is the inverse of this
	 */
	String toString(X value) throws Exception;

	/**
	 * Must be the name of the Key / field.
	 */
	@Override
	String toString();
}
