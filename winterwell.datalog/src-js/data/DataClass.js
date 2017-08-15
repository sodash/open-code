/**
*/

import _ from 'lodash';
import {assert} from 'sjtest';
import {endsWith} from 'wwutils';

/**
 * Coding Style??
 * 
 * These files are all about defining a convention, so let's set some rules??
 * 
 * 
 */

/**
 * assert the type!
 */
const isa = function(obj, typ) {
	assert(_.isObject(obj) && ! obj.length, obj);
	// if ( ! obj['@type']) {
	// 	return true;
	// }
	assert(obj['@type'] === typ, obj);
	return true;
};

const getType = function(item) {
	// schema.org type?
	let type = item['@type'];
	if (type) return type;
	let klass = item['@class'];
	if ( ! klass) return null;
	type = klass.substr(klass.lastIndexOf('.')+1);
	return type;
};

/**
 * access functions for source, help, notes??
 */
const Meta = {};

/** {notes, source} if set
 * Never null (may create an empty map). Do NOT edit the returned value! */
// If foo is an object and bar is a primitive node, then foo.bar has meta info stored at foo.meta.bar
Meta.get = (obj, fieldName) => {
	if ( ! fieldName) {
		return obj.meta || {};
	}
	let fv = obj[fieldName];
	if (fv && fv.meta) return fv.meta;
	if (obj.meta && obj.meta[fieldName]) {
		return obj.meta[fieldName];
	}
	// nope
	return {};
};

export {isa, getType, Meta};
	
