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
	let dataPoints = 0;
	for(let i=0; i<keys.length; i++) {
		let key = keys[i];
		// if (key !== 'mem_used') continue; Debug hack
		let data = dataFromLabel[key];
		// if ( ! _.isArray(data)) {
			// console.warn("skip not-an-array", key, data);
			// continue;
		// }
		let xydata = Object.keys(data).map(k => { return {x:k, y:data[k]}; });
		xydata = xydata.filter(xy => xy.y);
		dataPoints += xydata.length;
		let dset = makeDataSet(i, keys[i], xydata);
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
				type: 'time',
				time: {
					displayFormats: {							
						quarter: 'MMM YYYY'
					}
				}
			}]
		}
	}; // ./options;
	console.warn("Draw chart", chartOptions, chartData);
	return (<div><h3>{title}</h3>
				<RC2 data={chartData} options={chartOptions} type='line' />
				<div>
					<small>Labels: {JSON.stringify(keys)}, Total data points: {dataPoints}</small>
				</div>
			</div>);
}; //./ChartWidget

/**
 * @param data Array of {x (which can be a Time string), y}
 */
const makeDataSet = (i, label, xydata) => {	
	console.log(label, xydata);	
	// HACK pick a colour
	let colors = ["rgba(75,192,192,1)", "rgba(192,75,192,1)", "rgba(192,192,75,1)", "rgba(75,75,192,1)", "rgba(75,192,75,1)", "rgba(192,75,75,1)"];
	let color = colors[i % colors.length];
	return {
		label: label,
		fill: false,

		lineTension: 0.05, // higher, and spikes can turn into loopy calligraphy
		// cubicInterpolationMode: 'monotone',
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
