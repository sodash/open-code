package winterwell.web.fields;

import winterwell.utils.web.XStreamUtils;

/**
 * Use XStream to send any old object across a request. NB: best to use POST
 * with these fields.
 * 
 * @author daniel
 * 
 * @param <T>
 */
public class XStreamField<T> extends AField<T> {

	private static final long serialVersionUID = 1L;

	public XStreamField(String name) {
		super(name, "hidden");
	}

	@Override
	public T fromString(String v) throws Exception {
		return XStreamUtils.serialiseFromXml(v);
	}

	@Override
	public String toString(T value) {
		return XStreamUtils.serialiseToXml(value);
	}

}
