import React from 'react';
import ReactDOM from 'react-dom';
import _ from 'lodash';
import SJTest from 'sjtest';
import C from'../C.js';

import chartjs from 'chart.js';
import RC2 from 'react-chartjs2';

export default class ChartWidget extends React.Component {

	render() {		
		let chartData = {
			labels: ["January", "February", "March", "April", "May", "June", "July"],
			datasets: [
				{
					label: "£s donated",
					fill: false,
					lineTension: 0.1,
					backgroundColor: "rgba(75,192,192,0.4)",
					borderColor: "rgba(75,192,192,1)",
					borderCapStyle: 'butt',
					borderDash: [],
					borderDashOffset: 0.0,
					borderJoinStyle: 'miter',
					pointBorderColor: "rgba(75,192,192,1)",
					pointBackgroundColor: "#fff",
					pointBorderWidth: 3,
					pointHoverRadius: 7,
					pointHoverBackgroundColor: "rgba(75,192,192,1)",
					pointHoverBorderColor: "rgba(220,220,220,1)",
					pointHoverBorderWidth: 2,
					pointRadius: 1,
					pointHitRadius: 10,
					data: [15, 25, 34, 45, 56, 60, 72],
					spanGaps: false,
				}
			]
		}; //./data
		let chartOptions = {
				scales: {
					yAxes: [{
						ticks: {
							beginAtZero:true
						}
					}],
					xAxes: [{
						ticks: {
						}
					}]
				}
			} // ./options;
		return (<div>
					<RC2 data={chartData} options={chartOptions} type='line' />;
				</div>);
	}

} //./ChartWidget
