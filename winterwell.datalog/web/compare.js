
$(function(){
	
	$.get('http://localhost:8765/project/assist?action=get')
	.then(function(results){
		console.log("results",results);
		let scoreNames = pivot(results, "'cargo' -> i -> '_source' -> 'scores' -> scoreName -> scoreValue", 'scoreName', {mode:'set'});
		console.log("scoreNames", scoreNames);
		let expIds = pivot(results, "'cargo' -> i -> '_id' -> id", 'id');
		console.log("expIds", expIds);
		// Build a table of results
		const $tbl = $('<table class="table"></table>');
		{	// header
			let $tr = $('<tr></tr>');
			$tr.append('<th></th>'); // exp name
			$tr.append('<th></th>'); // exp controls
			for(let i=0; i<scoreNames.length; i++) {
				$tr.append('<th>'+scoreNames[i]+'</th>');
			}
			$tbl.append($tr);
		}
		let experiments = results.cargo;
		for(let ri=0; ri<experiments.length; ri++) {
			const e = experiments[ri];
			const scores = e._source.scores;
			const spec = e._source.spec;
			let $tr = $('<tr></tr>');
			let ename = spec.name;
			if ( ! ename) {
				ename = spec.planname;
			}
			if ( ! ename) {
				ename = e._id;
			} //data_source			
			$tr.append('<th title="'+printer.str(spec).replace(/["']/g,'')+'">'+ename.substr(0,60)+'</th>');
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
				let score = scores[scoreNames[si]];
				let klass = judge(scoreNames[si], score);
				$tr.append('<td class="'+klass+'">'+(_.isNumber(score)? printer.prettyNumber(score) : printer.str(score))+'</td>');
			}
			$tbl.append($tr);
		}
		$('#results').append($tbl);
	});
	
});

function judge(scoreName, score) {
	const GOOD = 'good', WARNING='warning', BAD='bad';
	if (scoreName==='R2' || scoreName==='adjusted_R2') {
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