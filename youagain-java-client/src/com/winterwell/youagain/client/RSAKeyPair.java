package com.winterwell.youagain.client;

import java.io.Serializable;
import java.security.KeyPair;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;

import com.winterwell.utils.Utils;

public class RSAKeyPair implements Serializable {
	private static final long serialVersionUID = 1L;
	RSAPrivateKey privateKey;
    RSAPublicKey publicKey;
    /**
     * A random ID to identify this key-pair. Not used in the algorithm.
     */
	String id;
	
	public RSAKeyPair(KeyPair _kp) {
		this.privateKey = (RSAPrivateKey) _kp.getPrivate();
		this.publicKey = (RSAPublicKey) _kp.getPublic();
		id = Utils.getUID();
	}

	public RSAPublicKey getPublic() {
		return publicKey;
	}

}
