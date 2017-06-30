import React from 'react';
import ReactDOM from 'react-dom';
import _ from 'lodash';
import SJTest, {assert} from 'sjtest';
import C from'../C.js';

import chartjs from 'chart.js';
import RC2 from 'react-chartjs2';

/**
	@param dataFromLabel e.g. (label)adview -> time -> number
 */
const ChartWidget = ({title, dataFromLabel}) => {
	assert(dataFromLabel);
	title = title || "Junk Data";
	let label = "Stuff";
	let timeFormat = 'MM/DD/YYYY HH:mm';
	// function newDateString(days) {
	// 	return moment().add(days, 'd').format(timeFormat);
	// }
	// let labels = ["January", "February", "March", "April", "May", "June", "July"];
	let datasets = [];
	let keys = Object.keys(dataFromLabel);
	for(let i=0; i<keys.length; i++) {
		let key = keys[i];
		let data = dataFromLabel[key];
		if ( ! _.isArray(data)) {
			console.warn("skip", key, data);
			continue;
		}
		let dset = makeDataSet(i, keys[i], data);
		console.warn(dset);
		datasets.push(dset);
	}
	let chartData = {
		// labels: labels,
		datasets: datasets
	}; //./data
	let chartOptions = {
		title: title,
			scales: {
				yAxes: [{
					ticks: {
						beginAtZero:true
					}
				}],
				xAxes: [{
					time: {
						displayFormats: {							
                        	quarter: 'MMM YYYY'
                    	}
					}
				}]
			}
		}; // ./options;
	return (<div>
				<RC2 data={chartData} options={chartOptions} type='line' />
			</div>);
}; //./ChartWidget

const makeDataSet = (i, label, data) => {	
	let xydata = data.map(d => { return {x:d.key_as_string, y: d.doc_count}; });
	console.log(label, data, xydata);	
	let color = ["rgba(75,192,192,1)", "rgba(192,75,192,1)", "rgba(192,192,75,1)", "rgba(75,75,192,1)", "rgba(75,192,75,1)", "rgba(192,75,75,1)"][i];
	return {
		label: label,
		fill: false,

		// lineTension: 0.1,
		backgroundColor: color, // TODO 0.4 alpha
		borderColor: color,
		// borderCapStyle: 'butt',
		// borderDash: [],
		// borderDashOffset: 0.0,
		// borderJoinStyle: 'miter',
		pointBorderColor: color,
		pointBackgroundColor: "#fff",
		// pointBorderWidth: 3,
		// pointHoverRadius: 7,
		pointHoverBackgroundColor: color,
		pointHoverBorderColor: "rgba(220,220,220,1)",
		// pointHoverBorderWidth: 2,
		// pointRadius: 1,
		// pointHitRadius: 10,
		// or {x: time-string, y: value}
		data: xydata,
		// spanGaps: false,
	};
};


export default ChartWidget;
