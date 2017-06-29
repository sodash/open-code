import React from 'react';
import { assert, assMatch } from 'sjtest';
import Login from 'you-again';
import _ from 'lodash';
import { XId } from 'wwutils';
import pivot from 'data-pivot';

import printer from '../utils/printer';
// import C from '../C';
import ServerIO from '../plumbing/ServerIO';
import DataStore from '../plumbing/DataStore';
// import ChartWidget from './ChartWidget';
import Misc from './Misc';
import ChartWidget from './ChartWidget';


class DashboardPage extends React.Component {
	render() {
		let user = Login.getUser();
		if ( ! user) {
			return (
				<div className="page DashboardPage">
					<h2>My Dashboard</h2>
					<a href='#' onClick={(e) => DataStore.setShow('LoginWidget', true)}>Login or register</a>.
				</div>
			);
		}

		let filters = {
			// everything
		};
		let breakdowns = ['time', 'evt', 'domain'];

		let dspec = JSON.stringify(filters)+" "+JSON.stringify(breakdowns);
		const dpath = ['widget', 'Dashboard', dspec];
		let mydata = DataStore.getValue(dpath);
		if ( ! mydata) {
			// Where does ad activity data go??
			// dataspaces: trk for evt.pxl, goodloop for evt.viewable, adview, click, close, open, opened
			// see https://github.com/good-loop/doc/wiki/Canonical-Terminology-for-Logging-Good-Loop-Events
			ServerIO.getData(filters, breakdowns)
			.then((res) => {
				console.warn("yeh", dpath, res);
				let mydata2 = res.cargo;
				DataStore.setValue(dpath, mydata2);
			});
			return (
				<div className="page DashboardPage">
					<h2>My Dashboard</h2>
					<h3>Fetching your data...</h3>
					<Misc.Loading />
				</div>
			);
		}

		// pivot the data
		let cdata = pivot(mydata, "'byEvent' -> 'buckets' -> bi -> {key, 'events_over_time' -> 'buckets' -> bi2 -> bucket}", 
						"key -> bucket");
		let xydata = pivot(cdata, "key -> {doc_count, key_as_string}", "key -> {'x' -> key_as_string, 'y' -> doc_count}");		
		// debug
		window.pivot = pivot;
		window.mydata = mydata;
		console.warn("pivot", xydata, "from", cdata, 'from', mydata);

		// display...
		return (
			<div className="page DashboardPage">
				<h2>My Dashboard</h2>
				<ChartWidget title='Events' dataFromLabel={cdata} />
			</div>
		);
	}
} // ./DashboardPage


const DashboardWidget = ({ children, iconClass, title }) =>
	<div className="panel panel-default">
		<div className="panel-heading">
			<h3 className="panel-title"><DashTitleIcon iconClass={iconClass} /> {title || ''}</h3>
		</div>
		<div className="panel-body">
			{children}
		</div>
	</div>;
// ./DashboardWidget

const DashTitleIcon = ({ iconClass }) =>
	<i className={iconClass} aria-hidden="true" />;

export default DashboardPage;
