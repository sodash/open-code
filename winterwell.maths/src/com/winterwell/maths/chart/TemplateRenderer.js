/**
 * This acts as a template for a renderer that adheres to the Winterwell charting
 * API.
 * It uses the ['amdWeb'][amdWeb] boilerplate code to handle the use of the
 * library both with and without an AMD module loader (such as RequireJS) being
 * used.
 * 
 * [amdWeb]: https://github.com/umdjs/umd/blob/master/amdWeb.js
 *
 * @author Steven King <steven@winterwell.com>
**/
(function (root, factory) {
	if (typeof define === 'function' && define.amd) {
	 // AMD. Register as an anonymous module.
		define(['jquery', 'RenderingLibrary'], factory);
	} else {
		// Browser globals
		// We always want to name the library 'Chart' when it is set as a global
		// variable, as that is expected from the API.
		root.Chart = factory(root.jQuery, root.RenderingLibrary);
	}
}(this, function ($, RenderingLibrary) {
	return {
		render: function (chart) {
			// This is where the rendering logic takes place. It should return
			// the created chart object, so that it can be accessed by the
			// caller for later use.
			return RenderingLibrary.render(chart);
		}
	};
}));
