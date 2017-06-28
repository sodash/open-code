import React from 'react';
import ReactDOM from 'react-dom';
import _ from 'lodash';
import SJTest from 'sjtest';
import C from'../C.js';

import chartjs from 'chart.js';
import RC2 from 'react-chartjs2';

/**
	@param dataFromLabel e.g. (label)adview -> time -> number
 */
const ChartWidget = ({title, dataFromLabel}) => {
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
		datasets.push(makeDataSet(i, keys[i], dataFromLabel[keys[i]]));
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
				<RC2 data={chartData} options={chartOptions} type='line' />;
			</div>);
}; //./ChartWidget

const makeDataSet = (i, label, data) => {
	console.log(label, data);
	return {
		label: label,
		fill: false,
		// lineTension: 0.1,
		// backgroundColor: "rgba(75,192,192,0.4)",
		// borderColor: "rgba(75,192,192,1)",
		// borderCapStyle: 'butt',
		// borderDash: [],
		// borderDashOffset: 0.0,
		// borderJoinStyle: 'miter',
		// pointBorderColor: "rgba(75,192,192,1)",
		// pointBackgroundColor: "#fff",
		// pointBorderWidth: 3,
		// pointHoverRadius: 7,
		// pointHoverBackgroundColor: "rgba(75,192,192,1)",
		// pointHoverBorderColor: "rgba(220,220,220,1)",
		// pointHoverBorderWidth: 2,
		// pointRadius: 1,
		// pointHitRadius: 10,
		// or {x: time-string, y: value}
		data: [15, 25, 34, 45, 56, 60, 72],
		spanGaps: false,
	};
};


export default ChartWidget;
