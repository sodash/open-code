package winterwell.utils.io;


/**
 * @deprecated Use the com.winterwell.utils.io version instead
 * 
 * Parse Unix style command line arguments. Also handles Java properties objects
 * (as created from .properties files).
 * 
 * Sets fields annotated with {@link Option} in a settings object.
 * 
 * @author daniel
 */
public class ArgsParser extends com.winterwell.utils.io.ArgsParser {

	public ArgsParser(Object settings) {
		super(settings);
	}

}
