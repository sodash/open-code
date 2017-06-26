
// copy-pasta from adserver/src!

import C from '../C.js';
import _ from 'lodash';
import {getType} from '../data/DataClass';
import {assert,assMatch} from 'sjtest';
import {yessy} from 'wwutils';

/**
 * Hold data in a simple json tree, and provide some utility methods to update it - and to attach a listener.
 * E.g. in a top-of-the-app React container, you might do `DataStore.addListener((mystate) => this.setState(mystate));`
 */
class Store {	

	constructor() {
		this.callbacks = [];
		this.appstate = {data:{}, focus:{}, show:{}, misc:{}};
	}

	/**
	 * It is a good idea to wrap your callback in _.debounce()
	 */
	addListener(callback) {
		// add in a debounce for the callbacks??
		this.callbacks.push(callback);
	}

	/**
	 * Update and trigger the on-update callbacks.
	 * @param newState {?Object} This will do an overwrite merge with the existing state.
	 * Note: This means you cannot delete/clear an object using this - use direct modification instead.
	 * Can be null, which still triggers the on-update callbacks.
	 */
	update(newState) {
		console.log('update', newState);
		if (newState) {
			_.merge(this.appstate, newState);
		}
		this.callbacks.forEach(fn => fn(this.appstate));
	}

	/**
	 * Convenience for getting from the data sub-node (as opposed to e.g. focus or misc) of the state tree.
	 * type, id
	 * @returns a "data-item", such as a person or document, or undefined.
	 */
	getData(type, id) {
		assert(C.TYPES.has(type));
		assert(id, type);
		return this.appstate.data[type][id];
	}

	getValue(...path) {
		assert(_.isArray(path), path);
		// If a path array was passed in, use it correctly.
		if (path.length===1 && _.isArray(path[0])) {
			path = path[0];
		}
		assert(this.appstate[path[0]], 
			path[0]+" is not a node in appstate - As a safety check against errors, the root node must already exist to use getValue()");		
		let tip = this.appstate;
		for(let pi=0; pi < path.length; pi++) {
			let pkey = path[pi];			
			assert(pkey || pkey===0, path); // no falsy in a path - except that 0 is a valid key
			let newTip = tip[pkey];
			// Test for hard null -- falsy are valid values
			if (newTip===null || newTip===undefined) return null;
			tip = newTip;
		}
		return tip;
	}

	/**
	 * Update a single path=value.
	 * Unlike update(), this can set {} or null values.
	 * @param {String[]} path This path will be created if it doesn't exist (except if value===null)
	 * @param {*} value 
	 */
	// TODO handle setValue(pathbit, pathbit, pathbit, value) too
	setValue(path, value) {
		assert(_.isArray(path), path+" is not an array.");
		assert(this.appstate[path[0]], 
			path[0]+" is not a node in appstate - As a safety check against errors, the root node must already exist to use setValue()");
		// console.log('DataStore.setValue', path, value);
		let tip = this.appstate;
		for(let pi=0; pi < path.length; pi++) {
			let pkey = path[pi];
			if (pi === path.length-1) {
				tip[pkey] = value;
				break;
			}
			assert(pkey || pkey===0, "falsy in path "+path.join(" -> ")); // no falsy in a path - except that 0 is a valid key
			let newTip = tip[pkey];
			if ( ! newTip) {
				if (value===null) {
					// don't make path for null values
					return;
				}
				newTip = tip[pkey] = {};
			}
			tip = newTip;
		}
		this.update();
	}

	/**
	* Set widget.thing.show
	 * @param {String} thing The name of the widget.
	 * @param {boolean} showing 
	 */
	setShow(thing, showing) {
		assMatch(thing, String);
		this.setValue(['widget', thing, 'show'], showing);
	}

	/**
	 * Convenience for widget.thing.show
	 * @param {String} widgetName 
	 * @returns {boolean} true if widget is set to show
	 */
	getShow(widgetName) {
		assMatch(widgetName, String);
		return this.getValue('widget', widgetName, 'show');
	}

	updateFromServer(res) {
		console.log("updateFromServer", res);
		let hits = res.cargo && res.cargo.hits;
		if ( ! hits) return;
		let itemstate = {data:{}};
		hits.forEach(item => {
			try {
				let type = getType(item);
				if ( ! type) {
					// skip
					return;
				}
				assert(C.TYPES.has(type), item);
				let typemap = itemstate.data[type];
				if ( ! typemap) {
					typemap = {};
					itemstate.data[type] = typemap;
				}
				if (item.id) {
					typemap[item.id] = item;
				} else {
					console.warn("No id?!", item, "from", res);
				}
			} catch(err) {
				// swallow and carry on
				console.error(err);
			}
		});
		this.update(itemstate);
		return res;
	}

} // ./Store

const DataStore = new Store();
export default DataStore;
// accessible to debug
if (typeof(window) !== 'undefined') window.DataStore = DataStore;

/**
 * Store all the state in one big object??
 */
DataStore.update({
	data: {
		Charity: {},
		User: {}
	},
	draft: {
		Charity: {},
		User: {}
	},
	focus: {
		Charity: null,
		User: null,
	},	
	widget: {},
	misc: {
	},
	/** status of server requests, for displaying 'loading' spinners 
	 * Normally: transient.$item_id.status
	*/
	transient: {}
});
