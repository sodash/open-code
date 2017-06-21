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


export {isa};
