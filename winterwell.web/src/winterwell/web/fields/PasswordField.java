package winterwell.web.fields;

/**
 * This will never send out the user's password. It will re-set itself to blank
 * every time! A null return value therefore does not mean anything.
 * 
 * @author daniel
 */
public class PasswordField extends AField<String> {

	private static final long serialVersionUID = 1L;

	public PasswordField(String name) {
		super(name, "password");
	}

	/**
	 * @WARNING To avoid exposing passwords, this field NEVER encodes a value!
	 * @return null!
	 */
	@Override
	public String toString(String value) {
		return null;
	}

}
