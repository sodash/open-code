/**
 * TODO we'll want more search capabilities as we go on
 */
class SearchQuery {

	constructor(query, options) {
		this.query = query || "";
		this.options = Object.assign(options || {}, SearchQuery.options);
		this.parse();
	}

	parse() {
        // HACK just space separate and crude key:value for now!
        let bits = this.query.split(" ");
		let bits2 = bits.map(bit => {
			let kv = bit.match(/^([a-z]+):(.+)/);
			if (kv) return [kv[1], kv[2]];
			return bit;
		});
		/**
		 * Return the expression tree, which is a nested array
		 * E.g. "a OR (b AND near:c)" --> ["OR", "a", ["AND", "b", ["near", "c"]]]
		 */
        this.tree = [SearchQuery.AND, ...bits2];
	}

	/**
	 * Convenience method.
	 * IF propName occurs at the top-level, then return the value
	 * @param {*} propName 
	 */
	prop(propName) {
		let prop = this.tree.filter(bit => bit[0] === propName);
		// What to return if prop:value is present but its complex??
		return prop && prop[0]? prop[0][1] : null;
	}

	/**
	 * 
	 * @param {*} propName 
	 * @param {*} propValue 
	 * @returns a NEW SearchQuery
	 */
	setProp(propName, propValue) {
		let newq = this.query;
		// renove the old
		if (this.prop(propName)) {
			newq = newq.replace(new RegExp(propName+":\\S+"), "").trim();
		}
		// quote the value?
		let qpropValue = propValue.indexOf(" ") === -1? propValue : '"'+propValue+'"';
		newq += " "+propName+":"+qpropValue;
		return new SearchQuery(newq.trim());
	}

} // ./SearchQuery

// The built in boolean operators
SearchQuery.AND = "AND";
SearchQuery.OR = "OR";
SearchQuery.NOT = "NOT";

SearchQuery.options = {
	OR: ["OR", ","],
	AND: ["AND"],
	NOT: ["-"]
};

// debug hack
window.SearchQuery = SearchQuery;

// Export
if (typeof module !== 'undefined') {
  module.exports = SearchQuery;
}
// export default SearchQuery;
