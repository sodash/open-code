
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
		const scoreNames = Object.keys(scores).sort();
		console.log("scoreNames", scoreNames);
		let expIds = pivot(results, "'cargo' -> i -> '_id' -> id", 'id');
		console.log("expIds", expIds);
		// Build a table of results
		const $toggles = $('<div class="well">Toggle columns: </div>');
		$('#results').append($toggles);
		const $tbl = $('<table class="table compact table-striped"></table>');
		{	// header
			let $tr = $('<tr></tr>');
			$tr.append('<th>Experiment Name</th>'); // exp name
			$tr.append('<th></th>'); // exp controls
			$tr.append('<th>Time</th>'); // time
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
			console.log(ename, e);			
			let link = 'http://localhost:8766/assist/experiment/'+e._id;
			if (spec) {
				let odir = spec.output_dir;
				
				link = '/project/assist/view'+odir+'/html/output-viewer.html';
//				"/home/daniel/winterwell/tv2/assist/test-data-out/AssistData_Expedia_start_KFL_Attrib_cols=all_columns_v1"
//				file:///home/daniel/winterwell/tv2/assist/test-data-out/AssistData_Expedia_season_LR2_Spend/html/output-viewer.html					
			}
			$tr.append('<th title=""><a target="_new" href="'+link+'">'+ename.substr(0,140)+'</a></th>');
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
			// time
			let etime = new Date(e._source.storageTime);
			let $tdtime = $('<td>'+etime+'</td>');
			$tr.append($tdtime);
			for(let si=0; si<scoreNames.length; si++) {
				let score = flatScores[scoreNames[si]];
//				console.log('flatScores', flatScores, scoreNames[si]);
				let klass = judge(scoreNames[si], score);
				$tr.append('<td class="'+klass+'">'+(_.isNumber(score)? printer.prettyNumber(score) : _.isUndefined(score)? -1 : printer.str(score))+'</td>');
			}
			$tbody.append($tr);
		}
		$tbl.append($tbody);
		$('#results').append($tbl);
		setTimeout(function(){
			let table = $tbl.DataTable({
				fixedColumns: true,
				scrollX:true,
				scrollY:600,				
				scrollCollapse:true
			});
			for(let i=0; i<scoreNames.length; i++) {
				let $a = $('<a style="margin-right:4px;">'+scoreNames[i]+'</a>;')
				$a.on('click', function (e) {
			        e.preventDefault();
			        var column = table.column(i+3);
			        // Toggle the visibility
			        if (column.visible()) {
			        	column.visible( false );
			        	$a.addClass('off').removeClass('on');
					} else {
						column.visible( true);
			        	$a.addClass('on').removeClass('off');
					}
			    } );
				$toggles.append($a);
				if (scoreNames[i].indexOf('stddev') !== -1) {
					$a.click();
				}
			}
		}, 50);
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
	if (scoreName==='stopwatch') {
		// NB: kfold will slow stuff down
		if (score<60) return GOOD;
		if (score>200) return BAD;
		if (score>120) return WARNING;		
	}
	if (scoreName.indexOf('uplift ratio mean') != -1) {
		if (score<0 || score >20) return BAD;
		if (score < 0.5 || score>10) return WARNING;
		if (score > 2 && score <10) return GOOD;		
	}
	if (scoreName.indexOf('caused by')!=-1) {
		if (score<0 || score >0.8) return BAD;
		if (score<0.1 || score>0.7) return WARNING;				
	}
	if (scoreName.indexOf('correlation')!=-1) {
		if ( ! _.isNumber(score)) return '';
		let as = Math.abs(score);
		if (as < 0.4 || as>1.1) return BAD;
		if (as < 0.6) return WARNING;				
	}
	if (scoreName==='NRMSE') {
		if (score<0.1) return GOOD;
		if (score>0.6) return BAD;
		if (score>0.4) return WARNING;		
	}
	return '';
}