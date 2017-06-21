/**
 * Make a bag of string constants, kind of like a Java enum.
 * e.g. var MyKind = new Enum('TEXT PERSON');
 * gives you MyKind.TEXT == 'TEXT', MyKind.PERSON == 'PERSON'
 *
 * Also, each of the constants has an isCONSTANT() function added, so you can write:
 * MyKind.isTEXT(myvar) -- which has the advantage that it will create a noisy error if
 * if myvar is invalid, e.g. a typo. isCONSTANT() allows falsy values, but an unrecognised
 * non-false value indicates an error.
 *
 * MyKind.values holds the full list.
 *
 * Use-case: It's safer than using strings for constants, especially around refactoring.
 *
 * @author Daniel
 * Ref: http://stijndewitt.wordpress.com/2014/01/26/enums-in-javascript/
 */
class Enum {

	/** @param values {string|string[]}
	*/
	constructor(values) {
		// Set the values array
		if (typeof(values)==='string') {
			this.values = values.split(' ');
		} else {
			this.values = values;
		}
		for(var i=0; i<this.values.length; i++) {
			var k = this.values[i];
			this[k] = k;
			/** isCONSTANT: {string} -> {boolean} */
			this['is'+k] = function(v) {
				if ( ! v) return false;
				if ( ! this.enumerator[v]) throw 'Invalid Enum value: '+v;
				return v===this.k;
			}.bind({enumerator:this, k:k});
		}
		// Prevent edits, if we can
		if (Object.freeze) {
			Object.freeze(this);
		}
	}

	/**
	 * @param s {string}
	 * @returns true if s is a value of this enum, false otherwise.
	 * Use-case: assert(isOK(someInput));
	 */
	isOK(s) {
		return this.values.indexOf(s) != -1;
	}
}

//Export the module
// if (typeof module !== 'undefined') {
//   module.exports = Enum;
// }
export default Enum;
