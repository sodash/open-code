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
		const donations = this.state && this.state.donations;

		let content;

		if ( ! user) {
			content = (
				<div>
					<a href='#' onClick={(e) => DataStore.setShow('LoginWidget', true)}>Login or register</a> to track your donations.
				</div>
			);
		} else if (!donations) {
			ServerIO.getDonations()
			.then(function(result) {
				let dons = result.cargo.hits;
				this.setState({donations: dons});
			}.bind(this));
			content = <Misc.Loading />;
		} else {
			content = (
				<DashboardWidget title="Donation History">
					<DonationList donations={this.state.donations} />
				</DashboardWidget>
			);
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


const DonationList = ({donations}) => {
	return <div>{ _.map(donations, d => <Donation key={'d'+d.id} donation={d} />) }</div>;
};

const Donation = ({donation}) => {
	let niceName = XId.id(donation.to).split('-');
	niceName = niceName.map((word) => word.charAt(0).toUpperCase() + word.slice(1));
	niceName = niceName.join(' ');
	const impact = donation.impact? <div>Your donation funded {printer.prettyNumber(donation.impact.count, 2)} {donation.impact.unit}</div> : null;
	return (
		<div className='well'>
			<Misc.Time time={donation.date} /> &nbsp;
			You donated <Misc.Money precision={false} amount={donation.total} /> to <a href={'#charity?charityId='+XId.id(donation.to)}>{niceName}</a>.
			{impact}
			<div>GiftAid? {donation.giftAid? 'yes' : 'no'} <br />
			<small>Payment ID: {donation.paymentId}</small></div>
		</div>
	);
};

		/*<h2>Version 2+...</h2>
		<DashboardWidget title="News Feed">
			Updates from projects you support and people you follow.
		</DashboardWidget>

		<DashboardWidget title="Your Donations over Time">
			<ChartWidget type="line" />
		</DashboardWidget>

		<DashboardWidget title="Donations by Category">
			Pie chart of what you give to
		</DashboardWidget>

		<DashboardWidget title="Your Badges">
			Badges (encouraging use of all features, and repeated use -- but not extra £s)
		</DashboardWidget>

		<DashboardWidget title="Recent Donations">
			List of recent donations and impacts, with a link to the full history
		</DashboardWidget>*/


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
