/** 
 * Wrapper for server calls.
 *
 */
import _ from 'lodash';
import $ from 'jquery';
import {SJTest, assert, assMatch} from 'sjtest';
import C from '../C.js';

import Login from 'you-again';
import NGO from '../data/charity/NGO';

const ServerIO = {};
export default ServerIO;

// for debug
window.ServerIO = ServerIO;

/**
 * @param query {!String} query string
 */
ServerIO.search = function(query) {
	assert(_.isString(query), query);
	return ServerIO.load('/search.json', {data: {q: query}} );
};


ServerIO.getCharity = function(charityId, version) {
	assMatch(charityId, String);
	return ServerIO.load('/charity/'+escape(charityId)+'.json', {data: {version: version}});
};


ServerIO.donate = function(data) {
	// Anything to assert here?
	return ServerIO.post('/donation', data);
};

ServerIO.getDonations = function() {
	return ServerIO.load('/donation/list');
};


ServerIO.saveCharity = function(charity, version) {
	assert(NGO.isa(charity), charity);
	let params = {		
		data: {action: 'save', item: JSON.stringify(charity), version: version},
		method: 'PUT'};
	return ServerIO.load('/charity/'+escape(NGO.id(charity))+'.json', params);
};

ServerIO.publish = function(charity, version) {
	assert(NGO.isa(charity), charity);
	let params = {		
		data: {action: 'publish', version: version}
	};
	return ServerIO.load('/charity/'+escape(NGO.id(charity))+'.json', params);
};

/**
 * @param charity {name:String}
 */
ServerIO.addCharity = function(charity, version='draft') {
	let params = {		
		data: {action: 'add', item: JSON.stringify(charity), version: version},
		method: 'PUT'};
	return ServerIO.load('/charity.json', params);
};

ServerIO.discardEdits = function(charity, version) {
	assert(NGO.isa(charity), charity);
	let params = {		
		data: {action: 'discard-edits', version: version}
	};
	return ServerIO.load('/charity/'+escape(NGO.id(charity))+'.json', params);
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
ServerIO.load = function(url, params) {
	assMatch(url,String);
	console.log("ServerIO.load", url, params);
	params = ServerIO.addDefaultParams(params);
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
			.then(ServerIO.handleMessages)
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

ServerIO.post = function(url, data) {
	return ServerIO.load(url, {data, method:'POST'});
};

ServerIO.saveDraft = function(item) {
	return ServerIO.post('/charity/'+escape(NGO.id(item))+'.json', item);
};

ServerIO.handleMessages = function(response) {
	console.log('handleMessages',response);
	const newMessages = response && response.messages;
	if ( ! newMessages || newMessages.length===0) {
		return response;
	}
	ServerIO.ActionMan.perform({verb:C.action.notify, messages:newMessages});
	return response;
};

ServerIO.addDefaultParams = function(params) {
	if ( ! params) params = {};
	if ( ! params.data) params.data = {};
	return params;
};
