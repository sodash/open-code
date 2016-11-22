package winterwell.web.fields;

/**
 * Strings! Get your Strings, Fresh off the request.
 * 
 * This is equivalent to {@link AField} with no mods.
 * 
 * @see SafeString which guards against html injection.
 * @author daniel
 */
public final class StringField extends AField<String> {

	private static final long serialVersionUID = 1L;

	public StringField(String name) {
		super(name);
	}

}
