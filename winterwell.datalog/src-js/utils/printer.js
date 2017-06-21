/**
 * file: Printer.js Purpose: converts objects into html.
 *
 * Example Usage:
 *
 * printer.str(myObject);
 *
 * or
 *
 * new Printer().str(myObject)
 *
 * Good points: passing a Printer object in to another
 * method allows custom tweaking of the html in a modular fashion.
 *
 * @depends underscore.js
 */

function Printer() {
}

/**
 * Matches
 *
 * @you. Use group 2 to get the name *without* the @
 */
Printer.AT_YOU_SIR = /(^|\W)@([\w\-]+)/g;

/**
 * Matches #tag. Use group 2 to get the tag *without* the #
 */
Printer.HASHTAG = /(^|[^&A-Za-z0-9/])#([\w\-]+)/g;

Printer.URL_REGEX = /https?\:\/\/[0-9a-zA-Z]([-.\w]*[0-9a-zA-Z])*(:(0-9)*‌​)*(\/?)([a-zA-Z0-9\-‌​\.\?\,\'\/\\\+&amp;%‌​\$#_]*)?/g;

/**
 * Ported from StrUtils.java. TODO javascript's toPrecision could be
 * used to simplify this
 *
 * @param x
 * @param n
 * @return x to n significant figures
 */
Printer.prototype.toNSigFigs = function(x, n) {
		if (x==0) return "0";
		assert(n > 0, "Printer.js - toNSigFigs: n is not greater than 0");
		var sign = x < 0 ? "-" : "";
		var v = Math.abs(x);
		var lv = Math.floor(Math.log(v)/Math.log(10));
		var keeper = Math.pow(10, n - 1);
		var tens = Math.pow(10, lv);
		var keepMe = Math.round(v * keeper / tens);
		// avoid scientific notation for fairly small decimals
		if (lv < 0) {
			var s = this.toNSigFigs2_small(n, sign, lv, keepMe);
			if (s != null) return s;
		}
		var vt = keepMe * tens / keeper;
		var num = ""+vt;
		return sign + num;
	};

/**
 * Helper method
 */
Printer.prototype.toNSigFigs2_small = function(n, sign, lv, keepMe) {
	// use scientific notation for very small
	if (lv < -8) return null;
	var sb = ""+sign;
	var zs = -lv;
	var sKeepMe = ""+keepMe;
	if (sKeepMe.length > n) {
		assert(sKeepMe.charAt(sKeepMe.length - 1) == '0', "Printer.js - toNSigFigs2_small: error");
		// we've rounded up from 9 to 10, so lose a decimal place
		zs--;
		sKeepMe = sKeepMe.substring(0, sKeepMe.length - 1);
		if (zs == 0) {
			return null;
		}
	}
	sb += "0.";
	for (var i = 1; i < zs; i++) {
		sb += '0';
	}
	sb += sKeepMe;
	return sb;
};

Printer.prototype.prettyNumber = function(x, sigFigs) {
	if ( ! sigFigs) sigFigs = 3;
	// to 3 sig figs
	var x3 = this.toNSigFigs(x, sigFigs);
	if (x < 1000) return x3;
	// add commas
	var rx = "";
	for(var i=0; i<x3.length; i++) {
		rx += x3[x3.length - i - 1];
		if (i % 3 == 2 && i != x3.length-1) rx += ",";
	}
	var cx = "";
	for(var i=0; i<rx.length; i++) {
		cx += rx[rx.length - i - 1];
	}
	return cx;
};

/**
* Converts objects to a human-readable string. Uses `JSON.stringify`, with the
* ability to handle circular structures. The returned string uses the following
* notation:
*
* Circular-object: {circ} Circular-array: [circ] jQuery: {jQuery}
*
* @param {Object}
*            object The object to convert to a string.
* @returns {String} Representation of the supplied object.
*/
Printer.prototype.str = function (object) {
		if (typeof(object)==='string') return object;
		try {
			return JSON.stringify(object);
		} catch (error) {
			return JSON.stringify(escapeCircularReferences(object));
		}
	};

	function escapeCircularReferences(object, cache) {
		var escapedObject;

		if (!cache) {
			cache = [];
		}

		if (object instanceof jQuery) {
			return '{jQuery}';
		} else if (_.isObject(object)) {
			if (cache.indexOf(object) > -1) {
				return '{circ}';
			}

			cache.push(object);

			escapedObject = {};

			for (var key in object) {
				if (object.hasOwnProperty(key)) {
					var value = escapeCircularReferences(object[key], cache);

					if (value !== undefined) {
						escapedObject[key] = value;
					}
				}
			}
		} else if (_.isArray(object)) {
			if (cache.indexOf(object) > -1) {
				return '[circ]';
			}

			cache.push(object);

			escapedObject = [];

			for (var i = 0, j = object.length; i < j; i++) {
				var value = escapeCircularReferences(object[i], cache);

				if (value) {
					escapedObject.push(value);
				} else {
					escapedObject.push(null);
				}
			}
		} else {
			escapedObject = object;
		}

		return escapedObject;
	}

/**
 * Convert user text (eg a tweet) into html. Performs a clean, converts
 * links, and some markdown
 *
 * @param contents -
 *            The text context to be replaced. Can be null/undefined (returns "").
 * @param context -
 *            The message item (gives us the service this message is
 *            from for internal links)
 * @param external -
 *            When set true will write links to the service instead of
 *            internally
 *
 */
Printer.prototype.textToHtml = function (contents, context, external) {
	if ( ! contents) return "";
	var service = context && context.service? context.service : null;
	// TODO This is too strong! e.g. it would clean away < this >, or "1<2 but 3>2"
	// TODO convert @you #tag and links ??emoticons -- See TwitterPlugin
	// contents = cleanPartial(contents);

	// convert & > into html entities (before we add any tags ourselves)
	contents = contents.replace(/</g,'&lt;');
	contents = contents.replace(/>/g,'&gt;');
	// &s (but protect &s in urls)
	contents = contents.replace(/(\s|^)&(\s|$)/g,'$1&amp;$2');

	// Paragraphs & markdown linebreaks
	if (service != 'twitter' && service != 'facebook' && service != 'youtube') {
		// only one br for a paragraph??
		contents = contents.replace(/\n\n+/g,"<br/>");
		contents = contents.replace(/   \n/g,"<br/>");
	}

	// TODO lists +
	// var ulli = /^ ?-\s*(.+)\s*$/gm;
	// contents = contents.replace(ulli, "<li>$1</li>");
	if (service==='TODOsoda.sh') {
		// Checkboxes from github style []s
		contents = contents.replace(/\[( |x|X)\](.+$)/gm, function(r) {
			console.log(r);
			var on = r[1] === 'x' || r[1] === 'X';
			return "<label><input class='subtask' type='checkbox' "+(on?"checked='true'":'')+" /> "+r.substring(3).trim()+"</label>";
		});
	}

	// normalise whitespace
	contents = contents.replace(/\s+/g," ");

	// links
	if(external) {
		// NOTE: _parent required for IFRAME embed
		contents = contents.replace(Printer.URL_REGEX, "<a href='$1' target='_blank' rel='nofollow' target='_parent'>$1</a>");
	} else {
		contents = contents.replace(Printer.URL_REGEX, "<a href='$1' target='_blank' rel='nofollow'>$1</a>");
	}

	// TODO break-up over-long urls?
	// @username to their profile page
	if(external) {
		if(service == 'twitter') {
			contents = contents.replace(Printer.AT_YOU_SIR, "$1<a href='https://twitter.com/$2' target='_parent'>@$2</a>");
		} else if(service == 'facebook') {
			// TODO: Is linking @Name in facebook even possible?
		}
	} else {
		contents = contents.replace(Printer.AT_YOU_SIR, "$1<a href='/profile?xid=$2%40"+service+"'>@$2</a>");
	}

	// hashtag to a twitter search
	if(external) {
		if(service == 'twitter') {
			contents = contents.replace(Printer.HASHTAG, "$1<a href='https://twitter.com/search/%23$2' target='_parent'>#$2</a>");
		} else if(service == 'facebook') {
			// TODO: Is linking @Name in facebook even possible?
		}
	} else {
		if (service == 'soda.sh') { /* hashtags in notes are sodash tags */
			contents = contents.replace(Printer.HASHTAG, "$1<a href='/stream?tag=$2'>#$2</a>");
		} else {
			contents = contents.replace(Printer.HASHTAG, "$1<a href='/stream?q=%23$2'>#$2</a>");
		}
	}

	// a bit of markdown/email-markup
	contents = contents.replace(/(^|\s)\*(\w+|\w.+?\w)\*($|\s)/g, "$1<i>*$2*</i>$3");
	contents = contents.replace(/(^|\s)_(\w+|\w.+?\w)_($|\s)/g, "$1<i>_$2_</i>$3");

	// correct for common numpty errors, such as encoded <b> or <i> tags
	contents = contents.replace(/&lt;(\/?[biBI])&gt;/g, "<$1>");
	// ?? special effects, e.g. logos or emoticons?

	return contents;
}; // ./Printer.textToHtml()

/**
 * Convert milliseconds into a nice description.
 * TODO a better idea, would be to convert into some sort of Dt object, with a nice toString()
 * @param msecs {number} a time length in milliseconds
 */
Printer.prototype.dt = function(msecs) {
	// days?
	if (msecs > 1000*60*60*24) {
		var v = msecs / (1000*60*60*24);
		return this.toNSigFigs(v, 2)+" days";
	}
	if (msecs > 1000*60*60) {
		var v = msecs / (1000*60*60);
		return this.toNSigFigs(v, 2)+" hours";
	}
	if (msecs > 1000*60) {
		var v = msecs / (1000*60);
		return this.toNSigFigs(v, 2)+" minutes";
	}
	var v = msecs / 1000;
	return this.toNSigFigs(v, 2)+" seconds";
};

function encodeHashtag(text, service) {
	service = (service || '')
		.toLowerCase()
		.replace(/\W/g, '');

	switch (service) {
	case 'sodash':
		return text.replace(HASHTAG, '$1<a href="/stream?tag=$2">#$2</a>');
	default: // Return internal link by default.
		return text.replace(HASHTAG, '$1<a href="/stream?q=$2">#$2</a>');
	}
}

function encodeReference(text, service) {
	service = (service || '')
		.toLowerCase()
		.replace(/\W/g, '');

	switch (service) {
	case 'twitter':
		return text.replace(AT_YOU_SIR, '$1<a href="https://twitter.com/%2" target="_blank">@$2</a>');
	default: // Return internal link by default.
		return text.replace(AT_YOU_SIR, '$1<a href="/profile?who=$2">@$2</a>');
	}
}

//	export
/** Default Printer -- can be replaced. */
const printer = new Printer();
if (typeof module !== 'undefined') {
	module.exports = printer;
}	