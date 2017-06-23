import React from 'react';
import { assert, assMatch } from 'sjtest';
import Login from 'you-again';
import _ from 'lodash';
import { XId } from 'wwutils';

import printer from '../utils/printer';
// import C from '../C';
import ServerIO from '../plumbing/ServerIO';
import DataStore from '../plumbing/DataStore';
// import ChartWidget from './ChartWidget';
import Misc from './Misc';


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
		let mydata = DataStore.getValue('widget', 'Dashboard', dspec);
		if ( ! mydata) {
			// Where does ad activity data go??
			// dataspaces: trk for evt.pxl, goodloop for evt.viewable, adview, click, close, open, opened
			// see https://github.com/good-loop/doc/wiki/Canonical-Terminology-for-Logging-Good-Loop-Events
			ServerIO.getData(filters, breakdowns)
			.then((res) => {
				DataStore.setValue(['widget', 'Dashboard', dspec], mydata);
			});
			return (
				<div className="page DashboardPage">
					<h2>My Dashboard</h2>
					<h3>Fetching your data...</h3>
					<Misc.Loading />
				</div>
			);
		}
		// display...
		return (
			<div className="page DashboardPage">
				<h2>My Dashboard</h2>

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
