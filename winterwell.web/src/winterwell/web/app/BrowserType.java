package winterwell.web.app;


/**
 * @deprecated
 * What browser is this? Browser sniffing -- shouldn't be needed often, but it
 * does have some uses. Status: only recognises a handful of settings
 * 
 * Want to do more? See http://browscap.org/
 * @author daniel
 * 
 */
public class BrowserType extends com.winterwell.web.app.BrowserType {

	public BrowserType(String userAgent) {
		super(userAgent);
	}
}
