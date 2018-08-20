package com.winterwell.web.app;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.winterwell.utils.MathUtils;
import com.winterwell.utils.log.Log;

/**
 * What browser is this? Browser sniffing -- shouldn't be needed often, but it
 * does have some uses. Status: only recognises a handful of settings
 * 
 * Want to do more? See http://browscap.org/
 * @author daniel
 * 
 */
public class BrowserType {

	/**
	 * 
	 * @return lower-cased user-agent
	 */
	public String getUserAgent() {
		return ua;
	}
	
	public static final String OS_ANDROID = "android";

	public static final String OS_BLACKBERRY = "blackberry";

	/**
	 * iOS / Apple (iPhone/iPod/iPad/Mac)
	 */
	public static final String OS_IOS = "ios";


	private static final String OS_MAC_DESKTOP = "applemac";
	private static final String OS_WINDOWS = "windows";
	
	/**
	 * Internet Explorer
	 */
	public static final String MAKE_IE = "ie";

	public static final String FACEBOOK = "facebook";

	public static final String OS_KINDLE = "kindle";

	public static final String OS_WINDOWSPHONE = "windowsphone";

	public static final String OS_IPAD = "ipad";

	public static final String OS_IPHONE = "iphone";

	public static final String OS_CHROMEBOOK = "chromebook";


	private String os;
	
	public String getOS() {
		if (os==null) os = getOS2();
		return os;
	}
	
	private String getOS2() {
		if (ua.contains("android")) {
			return OS_ANDROID;
		}
		if (ua.contains("blackberry")) {
			return OS_BLACKBERRY;
		}
		if (ua.contains("intel mac os")) {
			return OS_MAC_DESKTOP;
		}
		if (ua.contains("windows nt")) {
			return OS_WINDOWS;
		}
		if (ua.contains("kindle")) {
			return OS_KINDLE;
		}
		if (ua.contains("windows phone")) {
			return OS_WINDOWSPHONE;
		}
		if (userAgent.contains("iPad;")) {
			return OS_IPAD;
		}
		if (userAgent.contains("CPU iPhone OS;")) {
			return OS_IPHONE;
		}
		if (userAgent.contains("CrOS")) {
			return OS_CHROMEBOOK;
		}
		if (ua.contains("apple")) {
			return OS_IOS; // This may be a bit loose -- looks like all webkit
							// browsers namecheck apple
		}
		return "?";
	}

	/**
	 * Lower-case version of userAgent
	 */
	private final String ua;
	/**
	 * Can be "", never null
	 */
	private final String userAgent;

	/**
	 * e.g. "ie"
	 */
	private String make;

	private double version;

	public BrowserType(String userAgent) {
		this.userAgent = userAgent == null ? "" : userAgent;
		this.ua = this.userAgent.toLowerCase();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((ua == null) ? 0 : ua.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		BrowserType other = (BrowserType) obj;
		if (ua == null) {
			if (other.ua != null)
				return false;
		} else if (!ua.equals(other.ua))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "BrowserType[os=" + os + ", make=" + make + ", version=" + version + ", isMobile()=" + isMobile() + "]";
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
				"ipad", "ipod", "blackberry", "kindle", "windows phone"}) {
			if (ua.contains(m))
				return true;
		}
		return false;
	}
	
	/**
	 * Is it a Facebook-app embedded browser?
	 * @return
	 */
	public String getApp() {
		// see https://mobiforge.com/research-analysis/webviews-and-user-agent-strings
		if (ua.contains("FBAV")) return FACEBOOK;
		// Are there others we could recognise? 
		return null;
	}

	private static final Pattern MSIE = Pattern.compile("\\bMSIE (\\d+\\.\\d+)");
	
	/**
	 * TODO What Browser software is it? e.g. IE vs chrome Status: only half-implemented!
	 * @return Never null (can be "other")
	 */
	public String getBrowserMake() {
		Matcher m = MSIE.matcher(userAgent);
		if (m.find()) {
			make = MAKE_IE;
			version = Double.valueOf(m.group(1));
		}		
		// TODO safari, edge, firefox, chrome, opera
		return make==null? "other" : make;
	}
	
	public double getVersion() {
		return version;
	}

	public void setBrowserMake(String family) {
		this.make = family;
	}
	/**
	 * 
	 * @param version String or Number
	 */
	public void setVersion(Object version) {
		try {
			this.version = MathUtils.toNum(version);
		} catch(Exception ex) {
			Log.e("BrowserType", ex);
		}
	}

	public void setOS(String family) {
		this.os = family==null? null : family.toLowerCase();
	}
}
