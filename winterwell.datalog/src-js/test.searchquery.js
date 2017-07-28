
// let Utils;
// if (typeof Utils === undefined && typeof require !== undefined) {
// Utils = require('../bin/wwutils.js');
// }
// if ( ! Utils && ! blockProp) {
// 	console.error("No wwutils :(");
// }

let assMatch = SJTest.assertMatch;

if (typeof(assert) === 'undefined') {
    function assert(ok, msg) {
        if (ok) return;
        console.error(msg);
        throw new Error(msg);
    }
}

// const blockProp = Utils.blockProp;
// const yessy = Utils.yessy;


describe('SearchQuery', function() {
    this.timeout(200);

	it('smoke-test', function() {				
		let sq = new SearchQuery("foo bar");
		console.log(sq);
		assert(sq.tree, sq);
		assert(sq.tree[0] === SearchQuery.AND, sq.tree);
		assMatch(sq.tree, [SearchQuery.AND, "foo", "bar"]);					
    });

	it('parses key:value', function() {				
		let sq = new SearchQuery("pub:local");
		console.log(sq, sq.prop('pub'));
		assert(sq.tree, sq);
		assMatch(sq.prop('pub'), "local");
		// assMatch(sq.tree, ["pub", "local"]);					
    });

	it('reset key:value', function() {				
		let sq = new SearchQuery("beer pub:local");
		let sq2 = sq.setProp("pub", "wine-bar");
		assMatch(sq2.prop('pub'), "wine-bar");
    });

	it('set key:value', function() {
		{				
			let sq = new SearchQuery("beer");
			let sq2 = sq.setProp("pub", "wine-bar");
			assMatch(sq2.prop('pub'), "wine-bar");
		}
		// {				
		// 	let sq = new SearchQuery("");
		// 	let sq2 = sq.setProp("pub", "wine-bar");
		// 	assMatch(sq2.prop('pub'), "wine-bar");
		// }
    });
}); //./describe()
