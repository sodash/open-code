
function flattenObject(object) {
	let out = {};
	flattenObject2(object, false, out);
	return out;
}

function flattenObject2(object, key, out) {
	if ( ! _.isObject(object)) {
		if ( ! key) key = 'no-key';
		out[key] = object;
		return;
	}
	// Hack for prettier output: avoid extra nesting in key if we can do so without potential clashes
	let prevKey = key+' ';
	if (true || ! key || Object.keys(object).length === 1 || _.isArray(object)) {
		prevKey = '';
	}
	_.forIn(object, function(value, key2) {
		flattenObject2(value, prevKey+key2, out);
	});
}

$(function(){

	$.get('http://localhost:8765/project/assist?action=get')
	.then(function(results){
		console.log("results",results);
		let scores = pivot(results, "'cargo' -> i -> '_source' -> 'results' -> scores", 'scores');
		scores = flattenObject(scores);
		let scoreNames = Object.keys(scores);
		console.log("scoreNames", scoreNames);
		let expIds = pivot(results, "'cargo' -> i -> '_id' -> id", 'id');
		console.log("expIds", expIds);
		// Build a table of results
		const $tbl = $('<table class="table compact table-striped"></table>');
		{	// header
			let $tr = $('<tr></tr>');
			$tr.append('<th>Experiment Name</th>'); // exp name
			$tr.append('<th></th>'); // exp controls
			for(let i=0; i<scoreNames.length; i++) {
				$tr.append('<th>'+scoreNames[i].replace(/[_\-]/g, ' ')+'</th>');
			}
			const $thead = $("<thead></thead>"); 
			$thead.append($tr);
			$tbl.append($thead);
		}
		let experiments = results.cargo;
		const $tbody = $("<tbody></tbody>"); 
		for(let ri=0; ri<experiments.length; ri++) {
			const e = experiments[ri];
			const scores = e._source.results;
			let flatScores = flattenObject(scores);
			const spec = e._source.spec;
			let $tr = $('<tr class="result"></tr>');
			let ename = e._source.name;
			if ( ! ename && spec) {
				ename = spec.planname;
			}
			if ( ! ename) {
				ename = e._id;
			} //data_source			
			$tr.append('<th title="">'+ename.substr(0,60)+'</th>');
			let $td = $('<td></td>');
			let $delBtn = $('<button><span class="glyphicon glyphicon-trash"></span></button>');
			$delBtn.click(function() {
				let ok = confirm("Trash this experiment?");
				if ( ! ok) return;
				$.get('http://localhost:8765/project/assist?action=delete&id='+escape(e._id))
				.then(function(){
					$tr.fadeOut();
				});
			});
			$td.append($delBtn);
			$tr.append($td);
			for(let si=0; si<scoreNames.length; si++) {
				let score = flatScores[scoreNames[si]];
				console.log('flatScores', flatScores, scoreNames[si]);
				let klass = judge(scoreNames[si], score);
				$tr.append('<td class="'+klass+'">'+(_.isNumber(score)? printer.prettyNumber(score) : printer.str(score))+'</td>');
			}
			$tbody.append($tr);
		}
		$tbl.append($tbody);
		$('#results').append($tbl);
		setTimeout(function(){$tbl.DataTable();},50);
	});
	
});

function judge(scoreName, score) {
	const GOOD = 'success', WARNING='warning', BAD='danger';
	if (scoreName.indexOf('R2') !== -1) {
		if (score>0.8) return GOOD;
		if (score<0.5) return BAD;
		if (score<0.65) return WARNING;
	}
	if (scoreName==='kfold_overfitting_indicator') {
		if (score<0.02) return GOOD;
		if (score>0.15) return BAD;
		if (score<0.05) return WARNING;
	}
	if (scoreName==='durbin_watson') {
		// 1 to 2 is the good range
		if (score < 0.75) return BAD; // successive error terms are positively correlated
		if (score > 2.25) return BAD; // successive error terms are negatively correlated		
		if (score>=1 && score <= 2) return GOOD;
	}
	return '';
}1