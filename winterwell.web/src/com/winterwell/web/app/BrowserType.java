package com.winterwell.web.app;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * What browser is this? Browser sniffing -- shouldn't be needed often, but it
 * does have some uses. Status: only recognises a handful of settings
 * 
 * Want to do more? See http://browscap.org/
 * @author daniel
 * 
 */
public class BrowserType {

	public static final String OS_ANDROID = "android";

	public static final String OS_BLACKBERRY = "blackberry";

	/**
	 * iOS / Apple (iPhone/iPod/iPad/Mac)
	 */
	public static final String OS_IOS = "ios";

	/**
	 * Internet Explorer
	 */
	public static final String MAKE_IE = "ie";

	private String os;
	
	public String getOS() {
		if (ua.contains("android")) {
			os = OS_ANDROID;
		} else if (ua.contains("blackberry")) {
			os = OS_BLACKBERRY;
		} else if (ua.contains("apple")) {
			os = OS_IOS; // This may be a bit loose -- looks like all webkit
							// browsers namecheck apple
		} else {
			os = "?";
		}
		return os;
	}
	
	private final String ua;
	/**
	 * Can be "", never null
	 */
	private final String userAgent;

	private String make;

	private double version;

	public BrowserType(String userAgent) {
		this.userAgent = userAgent == null ? "" : userAgent;
		this.ua = this.userAgent.toLowerCase();
	}

	/**
	 * Did this come from a mobile device? Egregious hack on the User-Agent
	 * header
	 * 
	 * @param state
	 * @return
	 */
	public boolean isMobile() {
		// constants suggested by
		// http://stackoverflow.com/questions/3514784/what-is-the-best-way-to-detect-a-handheld-device-in-jquery/3540295#3540295
		// See http://detectmobilebrowsers.com/ for a bigger list
		for (String m : new String[] { "mobile", "android", "webos", "iphone",
				"ipad", "ipod", "blackberry", "kindle" }) {
			if (ua.contains(m))
				return true;
		}
		return false;
	}

	private static final Pattern MSIE = Pattern.compile("\\bMSIE (\\d+\\.\\d+)");
	
	public String getMake() {
		Matcher m = MSIE.matcher(userAgent);
		if (m.find()) {
			make = MAKE_IE;
			version = Double.valueOf(m.group(1));
		}
		return make;
	}
	
	public double getVersion() {
		return version;
	}
}
