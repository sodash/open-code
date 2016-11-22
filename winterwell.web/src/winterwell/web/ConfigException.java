package winterwell.web;

@Deprecated /** use the com.winterwell version */
public class ConfigException extends com.winterwell.web.ConfigException {
	public ConfigException(String string) {
		super(string);
	}

	public ConfigException(String msg, String service) {
		super(msg, service);
	}

}
