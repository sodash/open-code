/**
*/

import _ from 'lodash';
import {assert} from 'sjtest';

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
	let type = item['@Type'];
	if (type) return type;
	let klass = item['@class'];
	if ( ! klass) return null;
	type = klass.substr(klass.lastIndexOf('.')+1);
	return type;
};

export {isa, getType};
