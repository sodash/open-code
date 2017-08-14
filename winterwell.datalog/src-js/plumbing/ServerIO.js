/** 
 * Wrapper for server calls.
 *
 */
import _ from 'lodash';
import $ from 'jquery';
import {SJTest, assert, assMatch} from 'sjtest';
import C from '../C.js';

import Login from 'you-again';

const ServerIO = {};

//ServerIO.DATALOG_ENDPOINT = 'https://testlg.good-loop.com/data';
ServerIO.DATALOG_ENDPOINT = '/data';

let getData = (filters = {}, breakdowns) => {
	let specs = {
		dataspace: filters.dataspace
	};
	return load(ServerIO.DATALOG_ENDPOINT, {data: specs});
};

/**
 * Submits an AJAX request. This is the key base method
 *
 * @param {String} url The url to which the request should be made.
 *
 * @param {Object} [params] Optional map of settings to modify the request.
 * See <a href="http://api.jquery.com/jQuery.ajax/">jQuery.ajax</a> for details.
 * IMPORTANT: To specify form data, use params.data
 *
 * To swallow any messages returned by the server - use params.swallow=true
 *
 * @returns A <a href="http://api.jquery.com/jQuery.ajax/#jqXHR">jqXHR object</a>.
**/
let load = function(url, params) {
	assMatch(url,String);
	console.log("ServerIO.load", url, params);
	params = params || {};
	if ( ! params.data) params.data = {};
	// sanity check: no Objects except arrays
	_.values(params.data).map(
		v => assert( ! _.isObject(v) || _.isArray(v), v)
	);
	// add the base
	if (url.substring(0,4) !== 'http' && ServerIO.base) {
		url = ServerIO.base + url;
	}
	params.url = url;
	// send cookies
	params.xhrFields = {withCredentials: true};
	// add auth
	if (Login.isLoggedIn()) {
		params.data.as = Login.getId();
		params.data.jwt = Login.getUser().jwt;
	}
	// debug: add stack
	if (window.DEBUG) {
		try {
			const stack = new Error().stack;			
			// stacktrace, chop leading "Error at Object." bit
			params.data.stacktrace = (""+stack).replace(/\s+/g,' ').substr(16);
		} catch(error) {
			// oh well
		}
	}
	// Make the ajax call
	let defrd = $.ajax(params); // The AJAX request.
	if (params.swallow) {
		// no message display
		return defrd;
	}
	defrd = defrd
			.then(handleMessages)
			.fail(function(response, huh, bah) {
				console.error('fail',url,params,response,huh,bah);
				// ServerIO.ActionMan.perform({
				// 	verb:C.action.notify,
				// 	messages:[{
				// 		type:'error',
				// 		text:'Failed to load: '+url
				// 	}]
				// });
				return response;
			}.bind(this));
	return defrd;
};

let post = function(url, data) {
	return load(url, {data, method:'POST'});
};

let handleMessages = function(r) {
	console.log("TODO handleMessages", r);
	return r;
};

ServerIO.post = post;
ServerIO.load = load;
ServerIO.getData = getData;
export default ServerIO;

// for debug
window.ServerIO = ServerIO;
