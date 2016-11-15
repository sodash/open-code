package winterwell.utils;

import com.winterwell.utils.Printer;
import com.winterwell.utils.ReflectionUtils;

/**
 * Indicates an unwritten bit of code.
 * 
 */
public class TodoException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public TodoException() {
		this("");
	}

	public TodoException(Object obj) {
		this(Printer.toString(obj));
	}

	/**
	 * @param string
	 *            capped at 512 chars
	 */
	public TodoException(String string) {
		super(StrUtils.ellipsize(string, 512)+"\t"+ReflectionUtils.getSomeStack(3));
	}

}
