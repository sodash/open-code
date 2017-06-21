
const XId = {};
/**
 * @param xid
 * @returns the id part of the XId, e.g. "winterstein" from "winterstein@twitter"
 */
XId.id = function(xid) {
	if ( ! xid) {
		throw new Error("XId.id - no input "+xid);
	}
	var i = xid.lastIndexOf('@');
	assert(i!=-1, "orla-utils.js - id: No @ in xid "+xid);
	return xid.substring(0, i);
};

/**
 * Convenience for nice display. Almost equivalent to XId.id() -- except this dewarts the XId.
 * So it's better for public display but cannot be used in ajax requests.
 * @param xid Does not have to be a valid XId! You can pass in just a name, or null.
 * @returns the id part of the XId, e.g. "winterstein" from "winterstein@twitter", "bob" from "p_bob@youtube"
 */
XId.dewart = function(xid) {
	if ( ! xid) return "";
	assert(_.isString(xid), "orla-utils.js - dewart: xid is not a string! "+xid);
	// NB: handle invalid XId (where its just a name fragment)
	var id = xid.indexOf('@')==-1? xid : XId.id(xid);
	if (id.length < 3) return id;
	if (id.charAt(1)!='_') return id;
	var c0 = id.charAt(0);
	if (c0!='p' && c0!='v' && c0!='g' && c0!='c') return id;
	// so there (probably) is a wart...
	var s = xid.indexOf('@')==-1? '' : XId.service(xid);
	if (s !== 'twitter' && s !=='facebook') {
		return id.substring(2);
	}
	return id;
};

/**
 * @param xid {!String}
 * @returns the service part of the XId, e.g. "twitter"
 */
XId.service = function(xid) {
	assert(_.isString(xid), "orla-utils.js service(): xid is not a string! "+xid);
	var i = xid.lastIndexOf('@');
	assert(i!=-1, "orla-utils.js dewart(): No @ in xid: "+xid);
	return xid.substring(i+1);
};

/**
 * @param xid
 * @returns the first chunk of the XId, e.g. "daniel" from "daniel@winterwell.com@soda.sh"
 * Also dewarts. Will put a leading @ on Twitter handles.
 */
XId.prettyName = function(xid) {
	var id = XId.dewart(xid);
	var i = id.indexOf('@');
	if (i!=-1) {
		id = id.substring(0,i);
	}
	if (XId.service(xid)==='twitter') {
		id = '@'+id;
	}
	return id;
};




/** Parse url arguments
 * @param [url] Optional, the string to be parsed, will default to window.location when not provided.
 * @returns a map */
function getUrlVars(url = window.location.href) {
	url = url.replace(/#.*/, '');
	var s = url.indexOf("?");

	if (s == -1 || s == url.length - 1) return {};

	var varstr = url.substring(s + 1);
	var kvs = varstr.split("&");
	var urlVars = {};

	for(var i = 0; i < kvs.length; i++) {
		var kv = kvs[i];
		if ( ! kv) continue; // ignore trailing &
		var e = kv.indexOf("=");

		if (e != -1 && e != kv.length - 1) {
			let k = kv.substring(0, e);
			k = decodeURIComponent(k.replace(/\+/g, ' '));
			let v = kv.substring(e + 1);
			v = decodeURIComponent(v.replace(/\+/g, ' '));
			urlVars[k] = v;
		} else {
			urlVars[kv] = '';
		}
	}

	return urlVars;
};

/**
	Validate email addresses using what http://emailregex.com/ assures me is the
	standard regex prescribed by the W3C for <input type="email"> validation.
	https://www.w3.org/TR/html-markup/input.email.html#input.email.attrs.value.single
	NB this is more lax
	@param string A string which may or may not be an email address
	@returns True if the input is a string & a (probably) legitimate email address.
*/
function isEmail(string) {
	return !!("string" == typeof(string).toLowerCase() && string.match(emailRegex));
}

var emailRegex = /^[a-zA-Z0-9.!#$%&’*+/=?^_`{|}~-]+@[a-zA-Z0-9-]+(?:\.[a-zA-Z0-9-]+)*$/;


/**
Like truthy, but [] amd [''] are also false
*/
const yessy = function(val) {
	if ( ! val) return false;
	if (val.length === 0) {
		return false;
	}
	if (val.length) {
		for (let i =0; i<val.length; i++) {
			if (val[i]) return true;
		}
		return false;
	}
	return true;
}

/**
 * @return {string} a unique ID
 */
const uid = function() {
    // A Type 4 RFC 4122 UUID, via http://stackoverflow.com/a/873856/346629
    var s = [];
    var hexDigits = "0123456789abcdef";
    for (var i = 0; i < 36; i++) {
        s[i] = hexDigits.substr(Math.floor(Math.random() * 0x10), 1);
    }
    s[14] = "4";  // bits 12-15 of the time_hi_and_version field to 0010
    s[19] = hexDigits.substr((s[19] & 0x3) | 0x8, 1);  // bits 6-7 of the clock_seq_hi_and_reserved to 01
    s[8] = s[13] = s[18] = s[23] = "-";
    var uuid = s.join("");
    return uuid;
};


const endsWith = function(s, ending) {
	assertMatch(s, String, ending, String);
	if (s.length < ending.length) return false;
	const end = s.substring(s.length-ending.length, s.length);
	return end === ending;
}

export {XId, getUrlVars, isEmail, yessy, endsWith, uid}