package com.winterwell.web.email;

import java.security.cert.X509Certificate;

import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

/**
 * Trust everything!
 * See http://www.rgagnon.com/javadetails/java-fix-certificate-problem-in-HTTPS.html
 * @author daniel
 *
 */
public class DummyTrustManager implements TrustManager, X509TrustManager  {
	
    public java.security.cert.X509Certificate[] getAcceptedIssuers() {
      return null;
    }

    public void checkClientTrusted(X509Certificate[] certs, String authType) {  }

    public void checkServerTrusted(X509Certificate[] certs, String authType) {  }
}
