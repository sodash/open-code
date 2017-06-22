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
		let content = null;

		if ( ! user) {
			content = (
				<div>
					<a href='#' onClick={(e) => DataStore.setShow('LoginWidget', true)}>Login or register</a>.
				</div>
			);
		} else {
			// Where does ad activity data go??
			// dataspaces: trk for evt.pxl, goodloop for evt.viewable, adview, click, close
			ServerIO.getData();
		}

		// display...
		return (
			<div className="page DashboardPage">
				<h2>My Dashboard</h2>
				<h3>In development...</h3>
				<p>Thank you for joining SoGive at this early stage.
					This is our first release, and there's still lots of work to do.
					By the way, we release all our code as open-source. If you would
					like to contribute to building SoGive, please get in touch.
				</p>
				{ content }
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
